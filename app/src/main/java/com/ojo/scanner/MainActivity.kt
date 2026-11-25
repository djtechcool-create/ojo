package com.ojo.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ojo.scanner.ui.CameraScreen
import com.ojo.scanner.ui.ResultScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OjoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OjoApp()
                }
            }
        }
    }
}

@Composable
fun OjoApp(vm: ScanViewModel = viewModel()) {
    when (val state = vm.state) {
        ScanState.Camera -> CameraScreen(onImage = vm::analyze, onFreeze = vm::showFrozen)

        is ScanState.Processing -> CenteredMessage {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("Leyendo el documento…")
        }

        is ScanState.Done -> {
            BackHandler(onBack = vm::reset)
            ResultScreen(state = state, onBack = vm::reset)
        }

        is ScanState.Error -> {
            BackHandler(onBack = vm::reset)
            CenteredMessage {
                Text(state.message, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = vm::reset) { Text("Volver a intentar") }
            }
        }
    }
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
fun OjoTheme(content: @Composable () -> Unit) {
    val blue = Color(0xFF1565C0)
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF90CAF9))
    } else {
        lightColorScheme(primary = blue)
    }
    MaterialTheme(colorScheme = colors, content = content)
}
