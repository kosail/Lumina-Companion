package com.korealm.lumina.data

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [imageOrientationFromExif]: the 8 EXIF orientation values plus the `UNKNOWN`/`UNDEFINED`
 * fallback, asserting the **ordered** operation list so the rotation/mirror sequence cannot regress
 * (the Phase 5 review found orientations 5/7 applied the mirror before the rotation — CHG-FE-0024).
 * Pure, no Android (FE-INV-050).
 */
class ImageOrientationTest {

    @Test
    fun mapsEveryExifOrientationToItsOrderedTransform() {
        val expected = mapOf(
            1 to ImageOrientation.Normal,
            2 to ImageOrientation(listOf(ImageOperation.Mirror(ImageFlip.Horizontal))),
            3 to ImageOrientation(listOf(ImageOperation.Rotate(180))),
            4 to ImageOrientation(listOf(ImageOperation.Mirror(ImageFlip.Vertical))),
            5 to ImageOrientation(
                listOf(ImageOperation.Rotate(90), ImageOperation.Mirror(ImageFlip.Horizontal)),
            ),
            6 to ImageOrientation(listOf(ImageOperation.Rotate(90))),
            7 to ImageOrientation(
                listOf(ImageOperation.Rotate(270), ImageOperation.Mirror(ImageFlip.Horizontal)),
            ),
            8 to ImageOrientation(listOf(ImageOperation.Rotate(270))),
        )

        expected.forEach { (exif, orientation) ->
            assertEquals(orientation, imageOrientationFromExif(exif), "EXIF orientation $exif")
        }
    }

    @Test
    fun anUnknownValueIsTreatedAsNormal() {
        assertEquals(ImageOrientation.Normal, imageOrientationFromExif(0))
        assertEquals(ImageOrientation.Normal, imageOrientationFromExif(99))
    }
}
