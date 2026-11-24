package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageCompressionUtil {
    private const val MAX_DIMENSION_PX = 1280
    private const val MAX_UPLOAD_BYTES = 1_024 * 1024 // 1 MB limit after compression

    suspend fun compressPickedImage(context: Context, uri: Uri): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            if (mimeType.isNotEmpty() && !mimeType.startsWith("image/")) {
                return@withContext Result.failure(IllegalArgumentException("Only image files are allowed."))
            }

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                return@withContext Result.failure(IllegalArgumentException("Unable to read selected image."))
            }

            var sampleSize = 1
            val longestEdge = max(bounds.outWidth, bounds.outHeight)
            while (longestEdge / sampleSize > MAX_DIMENSION_PX * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return@withContext Result.failure(IllegalArgumentException("Failed to decode selected image."))

            val scaled = scaleDownIfNeeded(decoded, MAX_DIMENSION_PX)
            if (scaled != decoded) {
                decoded.recycle()
            }

            var quality = 85
            var compressedBytes: ByteArray
            do {
                val out = ByteArrayOutputStream()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                compressedBytes = out.toByteArray()
                quality -= 10
            } while (compressedBytes.size > MAX_UPLOAD_BYTES && quality >= 35)

            scaled.recycle()

            if (compressedBytes.size > MAX_UPLOAD_BYTES) {
                return@withContext Result.failure(IllegalArgumentException("Image is too large even after compression (limit 1 MB)."))
            }

            Result.success(compressedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scaleDownIfNeeded(source: Bitmap, maxEdge: Int): Bitmap {
        val w = source.width
        val h = source.height
        val longest = max(w, h)
        if (longest <= maxEdge) return source
        val ratio = maxEdge.toFloat() / longest.toFloat()
        val newW = (w * ratio).toInt().coerceAtLeast(1)
        val newH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
