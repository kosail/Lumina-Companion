package com.korealm.lumina.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import com.korealm.lumina.protocol.RECOMMENDED_MAX_IMAGE_HEIGHT_PX
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 * Android [ImagePreparer]: `BitmapFactory` decode + EXIF upright + downscale + `Bitmap.compress(JPEG)`.
 *
 * The EXIF tag is read with `android.media.ExifInterface` (platform, `minSdk 24`; the `InputStream`
 * constructor is available from API 24) and applied before scaling, so a portrait photo is not sent
 * sideways. The pure value→transform mapping and the operation **order** live in
 * [imageOrientationFromExif] and are unit-tested in common code; the `Matrix` application below is
 * Android-only.
 *
 * Kotlin note: `actual` provides the platform implementation of the common `expect` factory. Every
 * decode/rotate call is CPU-bound and must run off the main thread (the repository uses
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

        val oriented = applyExifOrientation(raw, decoded)
        val scaled = scaleToMaxHeight(oriented, RECOMMENDED_MAX_IMAGE_HEIGHT_PX)
        return try {
            ByteArrayOutputStream().use { out ->
                if (!scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)) {
                    null
                } else {
                    out.toByteArray()
                }
            }
        } finally {
            // Free the native pixel buffers promptly. Each `!==` guard avoids recycling a bitmap that
            // a later stage reused; recycling twice would also be harmless.
            if (scaled !== oriented) scaled.recycle()
            if (oriented !== decoded) oriented.recycle()
            decoded.recycle()
        }
    }
}

/**
 * Rotates/mirrors [bitmap] per its EXIF orientation in [raw]. Returns [bitmap] unchanged when the
 * orientation is normal or unreadable, so the common path allocates no extra bitmap.
 */
private fun applyExifOrientation(raw: ByteArray, bitmap: Bitmap): Bitmap {
    val orientation = try {
        ExifInterface(ByteArrayInputStream(raw)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    } catch (e: IOException) {
        // A malformed/absent EXIF block is not fatal: keep the pixels as decoded.
        ExifInterface.ORIENTATION_NORMAL
    }

    val transform = imageOrientationFromExif(orientation)
    if (transform.operations.isEmpty()) return bitmap

    // Apply the operations **in order**. Order is significant: for EXIF 5/7 the rotation must come
    // first, then the mirror (CHG-FE-0024).
    val matrix = Matrix()
    transform.operations.forEach { operation ->
        when (operation) {
            is ImageOperation.Rotate -> matrix.postRotate(operation.degrees.toFloat())
            is ImageOperation.Mirror -> when (operation.flip) {
                ImageFlip.Horizontal -> matrix.postScale(-1f, 1f)
                ImageFlip.Vertical -> matrix.postScale(1f, -1f)
            }
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
