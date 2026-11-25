package com.ojo.scanner.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Una línea de texto (o un código de barras) detectada y su posición. */
data class OcrLine(val text: String, val box: Rect, val isBarcode: Boolean = false)

data class OcrResult(
    val fullText: String,
    val lines: List<OcrLine>,
    val barcodes: List<String> = emptyList(),
) {
    val isEmpty get() = lines.isEmpty()

    /** Datos útiles (RUC, clave de acceso, fechas...) encontrados en el resultado. */
    fun detectedData(): List<DetectedData> = DataExtractor.extract(fullText, barcodes)

    companion object {
        /** Une el resultado de ML Kit (texto + códigos de barras) en un solo objeto. */
        fun from(text: Text?, barcodes: List<Barcode>?): OcrResult {
            val codes = barcodes.orEmpty().filter { !it.rawValue.isNullOrBlank() }
            val barcodeLines = codes.mapNotNull { code ->
                code.boundingBox?.let { OcrLine(code.rawValue!!, it, isBarcode = true) }
            }
            val textLines = text?.textBlocks.orEmpty().flatMap { block ->
                block.lines.mapNotNull { line -> line.boundingBox?.let { OcrLine(line.text, it) } }
            }
            val fullText = text?.textBlocks.orEmpty().joinToString("\n\n") { it.text }
            return OcrResult(fullText, barcodeLines + textLines, codes.map { it.rawValue!! })
        }
    }
}

object Detectors {
    fun textRecognizer(): TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    fun barcodeScanner(): BarcodeScanner = BarcodeScanning.getClient()
}

/** Reconoce texto y códigos de barras en una foto (usado para imágenes de la galería). */
class TextAnalyzer {

    private val recognizer = Detectors.textRecognizer()
    private val scanner = Detectors.barcodeScanner()

    suspend fun recognize(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val text = recognizer.process(image).await()
        val barcodes = try {
            scanner.process(image).await()
        } catch (e: Exception) {
            emptyList()
        }
        return OcrResult.from(text, barcodes)
    }

    fun close() {
        recognizer.close()
        scanner.close()
    }
}
