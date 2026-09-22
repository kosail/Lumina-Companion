package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Decoding tests for control replies (`API_CONTRACT.md` §4.2–§4.9).
 *
 * Uses the contract's own examples and checks the `t` dispatch, unknown-`t` tolerance, and
 * malformed-input handling (which must return a value, not throw).
 */
class ReplyDecodeTest {

    /** Decodes a line and fails the test if it is not well-formed. */
    private fun decoded(line: String): Reply {
        val result = decodeReply(line)
        assertIs<ReplyDecodeResult.Decoded>(result, "expected a decoded reply for: $line")
        return result.reply
    }

    @Test
    fun decodesVolumeState() {
        val reply = assertIs<Reply.Volume>(decoded("""{"t":"volume.state","ok":true,"value":70,"muted":false}"""))
        assertEquals(70, reply.state.percent)
        assertEquals(false, reply.state.muted)
        assertEquals(true, reply.state.ok)
    }

    @Test
    fun decodesVolumeStateSentinel() {
        val reply = assertIs<Reply.Volume>(decoded("""{"t":"volume.state","ok":true,"value":-1,"muted":true}"""))
        assertNull(reply.state.percent)
    }

    @Test
    fun decodesPeople() {
        val reply = assertIs<Reply.People>(decoded("""{"t":"people","names":["David Solís"]}"""))
        assertEquals(listOf("David Solís"), reply.names)
    }

    @Test
    fun decodesRuntimeState() {
        val reply = assertIs<Reply.Runtime>(decoded("""{"t":"runtime.state","running":true,"sink":"ready"}"""))
        assertEquals(true, reply.state.running)
        assertEquals(SinkState.Ready, reply.state.sink)
    }

    @Test
    fun decodesEveryEnrollProgressVariant() {
        val stopping = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.progress","phase":"stopping_runtime"}"""),
        )
        assertEquals(EnrollEvent.Progress(EnrollPhase.StoppingRuntime, null, null, null), stopping.event)

        val capturing = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.progress","phase":"capturing","captured":3,"total":10}"""),
        )
        assertEquals(EnrollEvent.Progress(EnrollPhase.Capturing, 3, 10, null), capturing.event)

        val notice = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.progress","phase":"capturing","captured":3,"total":10,"message":"no face detected"}"""),
        )
        assertEquals("no face detected", assertIs<EnrollEvent.Progress>(notice.event).message)

        val starting = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.progress","phase":"starting_runtime"}"""),
        )
        assertEquals(EnrollPhase.StartingRuntime, assertIs<EnrollEvent.Progress>(starting.event).phase)
    }

    @Test
    fun decodesEnrollDone() {
        val reply = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.done","ok":true,"personCount":2,"embeddingsAdded":10}"""),
        )
        assertEquals(EnrollEvent.Done(personCount = 2, embeddingsAdded = 10), reply.event)
    }

    @Test
    fun decodesEnrollError() {
        val reply = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.error","exitCode":2,"message":"no face was captured; no data written","runtime":"started"}"""),
        )
        val event = assertIs<EnrollEvent.Error>(reply.event)
        assertEquals(2, event.exitCode)
        assertEquals(RuntimeAfterEnroll.Started, event.runtime)
    }

    @Test
    fun decodesEnrollCancelled() {
        val reply = assertIs<Reply.Enroll>(decoded("""{"t":"enroll.cancelled"}"""))
        assertEquals(EnrollEvent.Cancelled, reply.event)
    }

    @Test
    fun decodesEveryErrorCode() {
        val cases = mapOf(
            "unauthorized" to ErrorCode.Unauthorized,
            "bad_request" to ErrorCode.BadRequest,
            "busy" to ErrorCode.Busy,
            "internal" to ErrorCode.Internal,
        )
        for ((wire, expected) in cases) {
            val reply = assertIs<Reply.Error>(decoded("""{"t":"error","code":"$wire","message":"m"}"""))
            assertEquals(expected, reply.code, "code $wire")
        }
    }

    @Test
    fun treatsUnknownTypeAsForwardCompatible() {
        val reply = assertIs<Reply.Unknown>(decoded("""{"t":"future.thing","x":1}"""))
        assertEquals("future.thing", reply.type)
    }

    @Test
    fun returnsMalformedForBadInput() {
        assertIs<ReplyDecodeResult.Malformed>(decodeReply("not json at all"))
        assertIs<ReplyDecodeResult.Malformed>(decodeReply("""[1,2,3]"""))
        assertIs<ReplyDecodeResult.Malformed>(decodeReply("""{"no":"discriminator"}"""))
        // Valid JSON object with the right `t` but a wrong payload type.
        assertIs<ReplyDecodeResult.Malformed>(decodeReply("""{"t":"volume.state","ok":"nope"}"""))
    }

    @Test
    fun normalizesEnrollProgressCounts() {
        // Contract minimums: captured >= 0, total >= 1; out-of-range values become null.
        val bad = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.progress","phase":"capturing","captured":-1,"total":0}"""),
        )
        val badProgress = assertIs<EnrollEvent.Progress>(bad.event)
        assertNull(badProgress.captured)
        assertNull(badProgress.total)

        val good = assertIs<Reply.Enroll>(
            decoded("""{"t":"enroll.progress","phase":"capturing","captured":3,"total":10}"""),
        )
        val goodProgress = assertIs<EnrollEvent.Progress>(good.event)
        assertEquals(3, goodProgress.captured)
        assertEquals(10, goodProgress.total)
    }

    @Test
    fun mapsAnUnknownErrorCode() {
        val reply = assertIs<Reply.Error>(decoded("""{"t":"error","code":"teapot","message":"m"}"""))
        assertEquals(ErrorCode.Unknown, reply.code)
    }

    @Test
    fun decodesAVolumeFailure() {
        val reply = assertIs<Reply.Volume>(decoded("""{"t":"volume.state","ok":false,"value":-1,"muted":false}"""))
        assertEquals(false, reply.state.ok)
        assertNull(reply.state.percent)
    }
}
