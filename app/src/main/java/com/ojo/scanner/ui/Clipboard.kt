package com.ojo.scanner.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

fun copyToClipboard(context: Context, text: String) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Ojo", text))
    // Android 13+ ya muestra su propio aviso al copiar
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        val preview = if (text.length > 40) text.take(40) + "…" else text
        Toast.makeText(context, "Copiado: $preview", Toast.LENGTH_SHORT).show()
    }
}

fun shareText(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir texto"))
}
