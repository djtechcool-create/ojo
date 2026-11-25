package com.ojo.scanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ojo.scanner.ScanState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(state: ScanState.Done, onBack: () -> Unit) {
    val context = LocalContext.current
    val lines = state.result.lines
    val image = state.image

    // Índices de las líneas que el usuario tocó
    var selected by remember(state) { mutableStateOf(setOf<Int>()) }
    var editableText by rememberSaveable(state.result.fullText) { mutableStateOf(state.result.fullText) }

    val selectedText = selected.sorted().joinToString("\n") { lines[it].text }
    val primary = MaterialTheme.colorScheme.primary
    val imageBitmap = remember(image) { image.asImageBitmap() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Texto detectado") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Nuevo escaneo")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Toca el texto en la imagen para seleccionarlo. Los códigos de barras se marcan en verde.",
                style = MaterialTheme.typography.bodyMedium,
            )

            // Imagen con recuadros sobre cada línea detectada
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .aspectRatio(image.width.toFloat() / image.height, matchHeightConstraintsFirst = false)
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Documento escaneado",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(lines) {
                            detectTapGestures { tap ->
                                val (scale, ox, oy) = fitTransform(size.width.toFloat(), size.height.toFloat(), image.width, image.height)
                                val x = (tap.x - ox) / scale
                                val y = (tap.y - oy) / scale
                                val hit = lines.indexOfFirst { it.box.contains(x.toInt(), y.toInt()) }
                                if (hit >= 0) {
                                    selected = if (hit in selected) selected - hit else selected + hit
                                }
                            }
                        }
                ) {
                    val (scale, ox, oy) = fitTransform(size.width, size.height, image.width, image.height)
                    lines.forEachIndexed { i, line ->
                        val r = line.box
                        val topLeft = Offset(ox + r.left * scale, oy + r.top * scale)
                        val boxSize = Size(r.width() * scale, r.height() * scale)
                        val isSelected = i in selected
                        val baseColor = if (line.isBarcode) Color(0xFF00C853) else Color(0xFF42A5F5)
                        drawRoundRect(
                            color = if (isSelected) primary.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.15f),
                            topLeft = topLeft,
                            size = boxSize,
                            cornerRadius = CornerRadius(4f),
                        )
                        drawRoundRect(
                            color = if (isSelected) primary else baseColor,
                            topLeft = topLeft,
                            size = boxSize,
                            cornerRadius = CornerRadius(4f),
                            style = Stroke(width = if (isSelected) 3f else 1.5f),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { copyToClipboard(context, selectedText) },
                    enabled = selected.isNotEmpty(),
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text("  Copiar selección (${selected.size})")
                }
                TextButton(
                    onClick = {
                        selected = if (selected.size == lines.size) emptySet() else lines.indices.toSet()
                    }
                ) {
                    Text(if (selected.size == lines.size) "Quitar todo" else "Seleccionar todo")
                }
            }

            // Datos detectados automáticamente
            if (state.data.isNotEmpty()) {
                Text("Datos encontrados", style = MaterialTheme.typography.titleMedium)
                Text("Toca una etiqueta para copiarla.", style = MaterialTheme.typography.bodySmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.data.forEach { item ->
                        AssistChip(
                            onClick = { copyToClipboard(context, item.value) },
                            label = {
                                Text(
                                    "${item.type.label}: ${item.value}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 300.dp),
                                )
                            },
                        )
                    }
                }
            }

            // Texto completo editable
            Text("Todo el texto", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { copyToClipboard(context, editableText) }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null)
                    Text("  Copiar todo")
                }
                OutlinedButton(onClick = { shareText(context, editableText) }) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Text("  Compartir")
                }
            }
        }
    }
}

/** Escala y desplazamiento para dibujar una imagen con ContentScale.Fit. */
private fun fitTransform(viewW: Float, viewH: Float, imgW: Int, imgH: Int): Triple<Float, Float, Float> {
    val scale = minOf(viewW / imgW, viewH / imgH)
    val ox = (viewW - imgW * scale) / 2f
    val oy = (viewH - imgH * scale) / 2f
    return Triple(scale, ox, oy)
}
