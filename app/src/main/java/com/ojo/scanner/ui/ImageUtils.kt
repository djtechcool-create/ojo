package com.ojo.scanner.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.camera.core.ImageProxy
import kotlin.math.max

/** Tamaño máximo (lado más largo) para no gastar memoria de más. */
private const val MAX_SIDE = 2560

/** Convierte la foto de CameraX en Bitmap, ya enderezada. */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val rotation = imageInfo.rotationDegrees
    val source = toBitmap()
    val scale = MAX_SIDE.toFloat() / max(source.width, source.height)
    val matrix = Matrix().apply {
        if (scale < 1f) postScale(scale, scale)
        postRotate(rotation.toFloat())
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

/** Carga una imagen de la galería como Bitmap, respetando su orientación. */
fun loadBitmap(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = max(info.size.width, info.size.height)
            if (longest > MAX_SIDE) {
                val s = MAX_SIDE.toFloat() / longest
                decoder.setTargetSize((info.size.width * s).toInt(), (info.size.height * s).toInt())
            }
        }
    } else {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("Imagen no válida")
    }
}
