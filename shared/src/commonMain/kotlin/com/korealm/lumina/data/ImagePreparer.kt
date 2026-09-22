package com.korealm.lumina.data

import com.korealm.lumina.protocol.ImageBatchValidation
import com.korealm.lumina.protocol.validateImageBatch

/**
 * Downscales and re-encodes one picked image before it is sent to `enroll.images`
 * (`API_CONTRACT.md` §4.10, FE-INV-033).
 *
 * Responsibility: enforce the device's image guidance — height **≤ 1080 px**, **JPEG** (~quality
 * 80) — so a batch stays well under the 8 MiB decoded / 16 MiB line caps. The platform work
 * (Android `Bitmap`/`BitmapFactory`, JVM `ImageIO`) is behind `expect`/`actual`; this interface is
 * the seam the repository and its tests use.
 *
 * Threading: `prepare` decodes and re-encodes an image, so it must be called off the main thread
 * (the repository runs it on `Dispatchers.Default`).
 */
fun interface ImagePreparer {

    /**
     * Decodes [raw], scales it down to at most 1080 px high (`RECOMMENDED_MAX_IMAGE_HEIGHT_PX`) and
     * returns JPEG bytes.
     *
     * @param raw the bytes of a user-picked image (JPEG, PNG, …).
     * @return the prepared JPEG bytes, or `null` when the input cannot be decoded (the caller maps
     *   that to an enrollment failure rather than sending a corrupt image).
     */
    fun prepare(raw: ByteArray): ByteArray?
}

/** JPEG quality used for enrollment images (contract §4.10: "~quality 80"). */
const val JPEG_QUALITY: Int = 80

/**
 * The platform [ImagePreparer].
 *
 * Android uses `BitmapFactory`/`Bitmap.compress`; the JVM uses `javax.imageio.ImageIO`. Declared as
 * an `expect` factory (not a constructor) so the common code never imports a platform class, mirroring
 * `defaultEndpointHost`.
 */
expect fun createImagePreparer(): ImagePreparer

/** Outcome of preparing and validating a picked image batch against the contract caps (FE-INV-033). */
sealed interface PreparedImageBatch {

    /** Every image decoded and the batch is within the hard caps. */
    data class Ok(val images: List<ByteArray>) : PreparedImageBatch

    /** At least one image could not be decoded/resized/encoded; the batch must not be sent. */
    data object NotPrepared : PreparedImageBatch

    /** The batch is empty or exceeds a hard cap (too many / too large). */
    data class Rejected(val validation: ImageBatchValidation) : PreparedImageBatch
}

/**
 * Prepares every raw image with [preparer], then validates the batch with [validateImageBatch]
 * (count ≤ 12, ≤ 8 MiB decoded). Pure over its inputs, so it is unit-tested with a fake preparer;
 * the repository runs it on a background dispatcher because the real preparer is CPU-heavy.
 */
fun prepareImageBatch(preparer: ImagePreparer, raw: List<ByteArray>): PreparedImageBatch {
    if (raw.isEmpty()) return PreparedImageBatch.Rejected(ImageBatchValidation.Empty)
    val prepared = ArrayList<ByteArray>(raw.size)
    for (image in raw) {
        val ready = try {
            preparer.prepare(image)
        } catch (_: Exception) {
            // A platform decoder must never crash the caller: an image that cannot be prepared is a
            // value ([NotPrepared]), not a thrown exception (AGENTS §7).
            null
        } ?: return PreparedImageBatch.NotPrepared
        prepared += ready
    }
    return when (val validation = validateImageBatch(prepared)) {
        ImageBatchValidation.Ok -> PreparedImageBatch.Ok(prepared)
        else -> PreparedImageBatch.Rejected(validation)
    }
}

