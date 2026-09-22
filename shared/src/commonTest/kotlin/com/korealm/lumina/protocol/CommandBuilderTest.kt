package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Exact-output tests for the command builders (`API_CONTRACT.md` §4.10, §5).
 *
 * The wire text is asserted byte-for-byte, including the `token` in every control request and the
 * `frames` default. These strings are the contract; if a builder changes, this test must change with
 * a contract reference.
 */
class CommandBuilderTest {

    @Test
    fun subscribeHasNoToken() {
        assertEquals("""{"t":"subscribe"}""", subscribeLine())
    }

    @Test
    fun buildsEverySimpleControlLine() {
        assertEquals("""{"t":"volume.get","token":"T"}""", volumeGetLine("T"))
        assertEquals("""{"t":"volume.set","token":"T","value":70}""", volumeSetLine("T", 70))
        assertEquals("""{"t":"volume.mute","token":"T","value":true}""", volumeMuteLine("T", true))
        assertEquals("""{"t":"volume.mute","token":"T","value":false}""", volumeMuteLine("T", false))
        assertEquals("""{"t":"people.list","token":"T"}""", peopleListLine("T"))
        assertEquals("""{"t":"runtime.state","token":"T"}""", runtimeStateLine("T"))
        assertEquals("""{"t":"runtime.start","token":"T"}""", runtimeStartLine("T"))
        assertEquals("""{"t":"runtime.stop","token":"T"}""", runtimeStopLine("T"))
        assertEquals("""{"t":"enroll.camera.cancel","token":"T"}""", enrollCameraCancelLine("T"))
    }

    @Test
    fun buildsCameraStartWithDefaultFrames() {
        assertEquals(
            """{"t":"enroll.camera.start","token":"T","name":"Ana","frames":10}""",
            enrollCameraStartLine("T", "Ana"),
        )
    }

    @Test
    fun clampsCameraFramesToContractRange() {
        assertEquals(
            """{"t":"enroll.camera.start","token":"T","name":"Ana","frames":1}""",
            enrollCameraStartLine("T", "Ana", frames = 0),
        )
        assertEquals(
            """{"t":"enroll.camera.start","token":"T","name":"Ana","frames":10}""",
            enrollCameraStartLine("T", "Ana", frames = 99),
        )
        assertEquals(
            """{"t":"enroll.camera.start","token":"T","name":"Ana","frames":5}""",
            enrollCameraStartLine("T", "Ana", frames = 5),
        )
    }

    @Test
    fun buildsImagesRequest() {
        assertEquals(
            """{"t":"enroll.images","token":"T","name":"Ana","images":["AA","BB"]}""",
            enrollImagesLine("T", "Ana", listOf("AA", "BB")),
        )
    }

    @Test
    fun preservesUtf8Names() {
        // Names may contain UTF-8 (contract §4.1); the serializer must not mangle them.
        assertEquals(
            """{"t":"enroll.camera.start","token":"T","name":"Solís","frames":10}""",
            enrollCameraStartLine("T", "Solís"),
        )
    }
}
