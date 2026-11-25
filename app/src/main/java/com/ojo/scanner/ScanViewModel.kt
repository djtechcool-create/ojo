package com.ojo.scanner

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ojo.scanner.ocr.DataExtractor
import com.ojo.scanner.ocr.DetectedData
import com.ojo.scanner.ocr.OcrResult
import com.ojo.scanner.ocr.TextAnalyzer
import kotlinx.coroutines.launch

/** Estado de la pantalla. Sobrevive a la rotación del teléfono. */
sealed interface ScanState {
    data object Camera : ScanState
    data class Processing(val image: Bitmap) : ScanState
    data class Done(
        val image: Bitmap,
        val result: OcrResult,
        val data: List<DetectedData>,
    ) : ScanState
    data class Error(val message: String) : ScanState
}

class ScanViewModel : ViewModel() {

    private val analyzer = TextAnalyzer()

    var state by mutableStateOf<ScanState>(ScanState.Camera)
        private set

    fun analyze(image: Bitmap) {
        state = ScanState.Processing(image)
        viewModelScope.launch {
            state = try {
                val result = analyzer.recognize(image)
                if (result.lines.isEmpty()) {
                    ScanState.Error("No se encontró texto. Intenta con más luz o acercándote al documento.")
                } else {
                    ScanState.Done(image, result, DataExtractor.extract(result.fullText))
                }
            } catch (e: Exception) {
                ScanState.Error("No se pudo leer la imagen: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        state = ScanState.Camera
    }

    override fun onCleared() {
        analyzer.close()
    }
}
