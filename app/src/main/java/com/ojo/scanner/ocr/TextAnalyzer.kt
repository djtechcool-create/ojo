package com.ojo.scanner.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** Una línea de texto detectada y su posición dentro de la imagen. */
data class OcrLine(val text: String, val box: Rect)

data class OcrResult(val fullText: String, val lines: List<OcrLine>)

/** Reconoce texto en una imagen usando Google ML Kit (funciona sin internet). */
class TextAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(bitmap: Bitmap): OcrResult {
        val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
        val lines = text.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                line.boundingBox?.let { OcrLine(line.text, it) }
            }
        }
        val fullText = text.textBlocks.joinToString("\n\n") { it.text }
        return OcrResult(fullText, lines)
    }

    fun close() = recognizer.close()
}
