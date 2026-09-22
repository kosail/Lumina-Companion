package com.korealm.lumina.protocol

/**
 * Image-enrollment caps and validation (`API_CONTRACT.md` §4.10/§5.9, FE-INV-033).
 *
 * Responsibility: reject an image batch **before** it is base64-encoded and sent, so the app never
 * triggers the device's `bad_request` (and never builds a line larger than the control limit).
 *
 * The hard caps are the device's; the recommended values are guidance from the contract that keeps a
 * batch well under the hard caps. Validation is pure and takes sizes, so it is cheap to test.
 */

/** Hard cap on the number of images in one `enroll.images` request (contract §2). */
const val MAX_ENROLL_IMAGES: Int = 12

/** Hard cap on total **decoded** image bytes per request, 8 MiB (contract §2). */
const val MAX_ENROLL_IMAGE_BYTES: Long = 8L * 1024 * 1024

/** Hard cap on one control line, 16 MiB; base64 of 8 MiB fits comfortably (contract §2). */
const val MAX_LINE_BYTES: Long = 16L * 1024 * 1024

/** Recommended per-image size ceiling, 500 KB (contract §4.10). */
const val RECOMMENDED_MAX_IMAGE_BYTES: Long = 500L * 1024

/** Recommended number of photos per person (contract §4.10). Not `const`: an [IntRange] is not a
 * compile-time constant. */
val RECOMMENDED_PHOTOS: IntRange = 3..5

/** Recommended maximum image height before sending (contract §4.10). */
const val RECOMMENDED_MAX_IMAGE_HEIGHT_PX: Int = 1080

/** Outcome of validating an image batch. */
sealed interface ImageBatchValidation {

    /** The batch is within every hard cap. */
    data object Ok : ImageBatchValidation

    /** No images were supplied (`bad_request "no decodable images"`). */
    data object Empty : ImageBatchValidation

    /** More than [MAX_ENROLL_IMAGES] images (`bad_request "too many images (max 12)"`). */
    data class TooManyImages(val count: Int, val max: Int) : ImageBatchValidation

    /** Total decoded size above [MAX_ENROLL_IMAGE_BYTES] (`bad_request "images exceed ..."`). */
    data class TooLarge(val totalDecodedBytes: Long, val max: Long) : ImageBatchValidation

    /**
     * Images were supplied but none has any bytes, so nothing is decodable. The device rejects this
     * with `bad_request "no decodable images"`; we refuse it before sending.
     */
    data object Undecodable : ImageBatchValidation
}

/**
 * Returns the base64-encoded length of [decodedBytes] (standard base64 with `=` padding).
 *
 * Why: the contract caps the **decoded** bytes, but the wire carries base64, so the line-size guard
 * must reason about the encoded length. `4 * ceil(n / 3)`.
 *
 * Overflow-safe: for a nonsensical huge input the result **saturates** at [Long.MAX_VALUE] rather
 * than wrapping (a wrapped value could make [fitsControlLine] wrongly return `true`).
 */
fun base64EncodedSize(decodedBytes: Long): Long {
    if (decodedBytes <= 0L) return 0L
    val groups = decodedBytes / 3
    val remainder = decodedBytes % 3
    if (groups > (Long.MAX_VALUE - 4L) / 4L) return Long.MAX_VALUE
    return groups * 4L + if (remainder == 0L) 0L else 4L
}

/**
 * Whether a batch of [decodedTotalBytes] fits within the 16 MiB control line ([MAX_LINE_BYTES])
 * once base64-encoded.
 *
 * Kept separate from [validateImageBatchSizes] because the 8 MiB decoded cap already implies this
 * for any batch that passes it; this function exists as an explicit, testable guard the transport
 * can also assert.
 */
fun fitsControlLine(decodedTotalBytes: Long): Boolean {
    if (decodedTotalBytes <= 0L) return true
    // base64 is never shorter than the decoded input, so anything past the line cap cannot fit;
    // returning early also keeps the multiplication below inside a safe range.
    if (decodedTotalBytes > MAX_LINE_BYTES) return false
    return base64EncodedSize(decodedTotalBytes) <= MAX_LINE_BYTES
}

/**
 * Validates a batch by count and total decoded size.
 *
 * Checks run cheapest-first and short-circuit; the order matches the device's own error ordering
 * (count, then decodability, then size). The 16 MiB line cap is covered separately by
 * [fitsControlLine].
 *
 * @param count number of images.
 * @param totalDecodedBytes sum of the decoded image sizes in bytes.
 */
fun validateImageBatchSizes(count: Int, totalDecodedBytes: Long): ImageBatchValidation = when {
    count <= 0 -> ImageBatchValidation.Empty
    count > MAX_ENROLL_IMAGES -> ImageBatchValidation.TooManyImages(count, MAX_ENROLL_IMAGES)
    totalDecodedBytes <= 0L -> ImageBatchValidation.Undecodable
    totalDecodedBytes > MAX_ENROLL_IMAGE_BYTES ->
        ImageBatchValidation.TooLarge(totalDecodedBytes, MAX_ENROLL_IMAGE_BYTES)

    else -> ImageBatchValidation.Ok
}

/**
 * Validates a concrete batch of decoded images.
 *
 * Convenience overload; prefer [validateImageBatchSizes] when the bytes are not yet materialized.
 */
fun validateImageBatch(images: List<ByteArray>): ImageBatchValidation =
    validateImageBatchSizes(images.size, images.sumOf { it.size.toLong() })
