package com.korealm.lumina.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.korealm.lumina.protocol.RECOMMENDED_MAX_IMAGE_HEIGHT_PX
import java.io.ByteArrayOutputStream

/**
 * Android [ImagePreparer]: `BitmapFactory` decode + downscale + `Bitmap.compress(JPEG)`.
 *
 * Kotlin note: `actual` provides the platform implementation of the common `expect` factory. Both
 * decode calls are CPU-bound and must run off the main thread (the repository uses
 * `Dispatchers.Default`).
 */
actual fun createImagePreparer(): ImagePreparer = AndroidImagePreparer

private object AndroidImagePreparer : ImagePreparer {

    override fun prepare(raw: ByteArray): ByteArray? {
        // First pass: read only the dimensions so we can pick an efficient power-of-two sample size
        // without allocating the full bitmap (large phone photos would otherwise risk an OOM).
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outHeight, RECOMMENDED_MAX_IMAGE_HEIGHT_PX)
        }
        val decoded = BitmapFactory.decodeByteArray(raw, 0, raw.size, options) ?: return null

        val scaled = scaleToMaxHeight(decoded, RECOMMENDED_MAX_IMAGE_HEIGHT_PX)
        return try {
            ByteArrayOutputStream().use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    null
                } else {
                    out.toByteArray()
                }
            }
        } finally {
            // Free the native pixel buffers promptly; `recycle` twice is harmless.
            if (scaled !== decoded) scaled.recycle()
            decoded.recycle()
        }
    }
}

/** Largest power-of-two subsample that keeps the height at or above [maxHeight]. */
private fun sampleSize(height: Int, maxHeight: Int): Int {
    var sample = 1
    var current = height
    while (current / 2 >= maxHeight) {
        current /= 2
        sample *= 2
    }
    return sample
}

/** Scales [bitmap] down so its height is at most [maxHeight], preserving the aspect ratio. */
private fun scaleToMaxHeight(bitmap: Bitmap, maxHeight: Int): Bitmap {
    if (bitmap.height <= maxHeight) return bitmap
    val targetWidth = (bitmap.width.toLong() * maxHeight / bitmap.height).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(bitmap, targetWidth, maxHeight, true)
}
