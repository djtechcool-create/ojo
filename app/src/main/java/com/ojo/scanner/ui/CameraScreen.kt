package com.ojo.scanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.SystemClock
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ojo.scanner.ocr.DetectedData
import com.ojo.scanner.ocr.Detectors
import com.ojo.scanner.ocr.OcrResult
import kotlinx.coroutines.delay
import androidx.compose.ui.geometry.Size as GeoSize

/** Cuánto tiempo sigue visible un dato después de dejar de verse en la cámara. */
private const val CHIP_MEMORY_MS = 3000L

private val BarcodeGreen = Color(0xFF00E676)
private val PanelColor = Color(0xF01B1B1F)

/**
 * Cámara en vivo: reconoce texto y códigos de barras mientras apuntas, como Google Lens.
 * - Toca un texto en pantalla para copiarlo al instante.
 * - Los datos importantes aparecen abajo como etiquetas que se copian con un toque.
 * - "Seleccionar texto" congela la imagen para elegir varias líneas con calma.
 */
@Composable
fun CameraScreen(
    onImage: (Bitmap) -> Unit,
    onFreeze: (Bitmap, OcrResult) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }
    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                onImage(loadBitmap(context, uri))
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val openGallery = {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    // Resultado del último cuadro analizado, en coordenadas de la pantalla.
    var live by remember { mutableStateOf(OcrResult("", emptyList())) }
    // Datos vistos recientemente (para que las etiquetas no parpadeen).
    val recent = remember { LinkedHashMap<String, Pair<DetectedData, Long>>() }
    var chips by remember { mutableStateOf(listOf<DetectedData>()) }
    var copiedText by remember { mutableStateOf<String?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    val textRecognizer = remember { Detectors.textRecognizer() }
    val barcodeScanner = remember { Detectors.barcodeScanner() }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            // Más resolución = lee mejor letras pequeñas y códigos de barras largos.
            imageAnalysisResolutionSelector = ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(1920, 1080),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    )
                )
                .build()
            val mainExecutor = ContextCompat.getMainExecutor(context)
            setImageAnalysisAnalyzer(
                mainExecutor,
                MlKitAnalyzer(
                    listOf(textRecognizer, barcodeScanner),
                    ImageAnalysis.COORDINATE_SYSTEM_VIEW_REFERENCED,
                    mainExecutor,
                ) { result ->
                    val frame = OcrResult.from(
                        result.getValue(textRecognizer),
                        result.getValue(barcodeScanner),
                    )
                    live = frame

                    val now = SystemClock.uptimeMillis()
                    frame.detectedData().forEach { recent[it.value] = it to now }
                    recent.entries.removeAll { now - it.value.second > CHIP_MEMORY_MS }
                    chips = recent.values.map { it.first }
                },
            )
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            previewView.controller = controller
            controller.bindToLifecycle(lifecycleOwner)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.unbind()
            textRecognizer.close()
            barcodeScanner.close()
        }
    }
    LaunchedEffect(torchOn) { controller.enableTorch(torchOn) }
    LaunchedEffect(copiedText) {
        if (copiedText != null) {
            delay(1200)
            copiedText = null
        }
    }

    val currentLive by rememberUpdatedState(live)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            // Recuadros sobre el texto detectado en vivo
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { tap ->
                            val hit = currentLive.lines.firstOrNull { it.box.containsWithMargin(tap, 16) }
                            if (hit != null) {
                                copyToClipboard(context, hit.text)
                                copiedText = hit.text
                            } else {
                                // Tocar fuera del texto enfoca la cámara en ese punto
                                val point = previewView.meteringPointFactory.createPoint(tap.x, tap.y)
                                controller.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(point).build()
                                )
                            }
                        }
                    }
            ) {
                live.lines.forEach { line ->
                    val r = line.box
                    val topLeft = Offset(r.left.toFloat(), r.top.toFloat())
                    val boxSize = GeoSize(r.width().toFloat(), r.height().toFloat())
                    val justCopied = line.text == copiedText
                    val color = when {
                        justCopied -> Color(0xFF64B5F6)
                        line.isBarcode -> BarcodeGreen
                        else -> Color.White
                    }
                    drawRoundRect(
                        color = color.copy(alpha = if (justCopied) 0.55f else 0.22f),
                        topLeft = topLeft,
                        size = boxSize,
                        cornerRadius = CornerRadius(8f),
                    )
                    drawRoundRect(
                        color = color.copy(alpha = 0.9f),
                        topLeft = topLeft,
                        size = boxSize,
                        cornerRadius = CornerRadius(8f),
                        style = Stroke(width = if (line.isBarcode) 4f else 2f),
                    )
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Ojo necesita la cámara para leer documentos en vivo.\nTambién puedes elegir una foto de tu galería.",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Permitir cámara")
                }
            }
        }

        // Barra superior
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Ojo",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (hasPermission) {
                IconButton(onClick = { torchOn = !torchOn }) {
                    Icon(
                        if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = if (torchOn) "Apagar linterna" else "Encender linterna",
                        tint = Color.White,
                    )
                }
            }
        }

        // Panel inferior con los datos detectados y los botones
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(PanelColor)
                .navigationBarsPadding()
                .padding(top = 14.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                when {
                    copiedText != null -> "✓ Copiado"
                    live.isEmpty -> "Apunta la cámara a un documento"
                    else -> "Toca un texto en la pantalla para copiarlo"
                },
                color = if (copiedText != null) Color(0xFF64B5F6) else Color(0xFFB0B4BC),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp),
            )

            if (chips.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(chips, key = { it.value }) { item ->
                        DataChip(item) {
                            copyToClipboard(context, item.value)
                            copiedText = item.value
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = openGallery, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = "Elegir de la galería", tint = Color.White)
                }
                Button(
                    onClick = {
                        val frame = previewView.bitmap
                        if (frame == null || currentLive.isEmpty) {
                            Toast.makeText(context, "Todavía no se detecta texto", Toast.LENGTH_SHORT).show()
                        } else {
                            onFreeze(frame, currentLive)
                        }
                    },
                    enabled = hasPermission && !live.isEmpty,
                ) {
                    Icon(Icons.Filled.TextFields, contentDescription = null)
                    Text("  Seleccionar texto")
                }
                IconButton(
                    onClick = {
                        val all = (live.barcodes + live.fullText).filter { it.isNotBlank() }.joinToString("\n")
                        copyToClipboard(context, all)
                        copiedText = all
                    },
                    enabled = !live.isEmpty,
                    modifier = Modifier.size(52.dp),
                ) {
                    Icon(
                        Icons.Filled.ContentCopy,
                        contentDescription = "Copiar todo",
                        tint = if (live.isEmpty) Color.Gray else Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun DataChip(item: DetectedData, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2C2F36))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .widthIn(max = 260.dp)
    ) {
        Text(item.type.label, color = Color(0xFF90CAF9), fontSize = 12.sp)
        Text(
            item.value,
            color = Color.White,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Rect.containsWithMargin(p: Offset, margin: Int): Boolean =
    p.x >= left - margin && p.x <= right + margin && p.y >= top - margin && p.y <= bottom + margin
