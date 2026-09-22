package com.korealm.lumina.data

/**
 * The mirror axis for an [ImageOperation.Mirror].
 *
 * Kotlin note: an `enum` (like Java) but used as a small closed set so the Android `when` is
 * exhaustive.
 */
enum class ImageFlip {
    /** Mirror left/right. */
    Horizontal,

    /** Mirror top/bottom. */
    Vertical,
}

/**
 * One ordered step of the transform that makes a stored image display upright.
 *
 * Responsibility: describe the EXIF correction as plain data so the **order** is explicit and
 * unit-testable in common code. The Android `actual` applies the operations in list order; the JVM
 * path does not (no EXIF reader). Representing order as data is deliberate — applying the mirror
 * before vs. after the rotation yields different pixels for EXIF orientations 5 and 7, which was a
 * defect found in the Phase 5 review (CHG-FE-0024).
 */
sealed interface ImageOperation {
    /** Clockwise rotation by [degrees] (`90`, `180` or `270`). */
    data class Rotate(val degrees: Int) : ImageOperation

    /** Mirror across the axis named by [flip]. */
    data class Mirror(val flip: ImageFlip) : ImageOperation
}

/**
 * The ordered transform a photo needs, derived from its EXIF orientation tag.
 *
 * @param operations the steps, applied in order (first to last) to the stored pixels.
 */
data class ImageOrientation(val operations: List<ImageOperation>) {
    companion object {
        /** EXIF orientation 1 (and any unknown value): no change. */
        val Normal = ImageOrientation(emptyList())
    }
}

/**
 * Maps a raw EXIF orientation value to an [ImageOrientation] whose [ImageOperation] list is in
 * application order.
 *
 * Responsibility: keep the 8-value mapping pure and unit-tested in common code (FE-INV-001: the
 * numeric values are the EXIF/TIFF constants documented at
 * `developer.android.com/reference/android/media/ExifInterface`; confirmed at first compile — see
 * `docs/API_VERIFICATION.md` §9.1). Unknown values (including 0 `UNDEFINED`) are treated as
 * [ImageOrientation.Normal].
 *
 * The order matters for the transpose/transverse cases:
 * - 5 (transpose) = rotate 90 then mirror horizontal.
 * - 7 (transverse) = rotate 270 then mirror horizontal.
 *
 * @param value the EXIF orientation constant.
 */
fun imageOrientationFromExif(value: Int): ImageOrientation = when (value) {
    2 -> ImageOrientation(listOf(ImageOperation.Mirror(ImageFlip.Horizontal)))
    3 -> ImageOrientation(listOf(ImageOperation.Rotate(180)))
    4 -> ImageOrientation(listOf(ImageOperation.Mirror(ImageFlip.Vertical)))
    5 -> ImageOrientation(
        listOf(ImageOperation.Rotate(90), ImageOperation.Mirror(ImageFlip.Horizontal)),
    )
    6 -> ImageOrientation(listOf(ImageOperation.Rotate(90)))
    7 -> ImageOrientation(
        listOf(ImageOperation.Rotate(270), ImageOperation.Mirror(ImageFlip.Horizontal)),
    )
    8 -> ImageOrientation(listOf(ImageOperation.Rotate(270)))
    else -> ImageOrientation.Normal
}
