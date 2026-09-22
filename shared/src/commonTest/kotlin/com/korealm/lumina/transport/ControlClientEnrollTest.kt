package com.korealm.lumina.transport

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.EnrollPhase
import com.korealm.lumina.protocol.EnrollmentFailure
import com.korealm.lumina.protocol.enrollCameraCancelLine
import com.korealm.lumina.protocol.enrollCameraStartLine
import com.korealm.lumina.protocol.enrollImagesLine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for the streaming enrollment methods of [KtorControlClient]: the emitted event sequence, the
 * exact wire line, failure mapping and the second-connection cancel — all against scripted
 * [ScriptedControlConnection]s (Phase 4).
 */
class ControlClientEnrollTest {

    private fun client(
        connections: ControlConnectionFactory,
        token: String = "tok",
    ): ControlClient = KtorControlClient(
        endpointProvider = { Endpoint("gateway") },
        tokenProvider = { token },
        connections = connections,
    )

    @Test
    fun cameraStreamsProgressThenDoneAndCloses() = runTest {
        val connection = ScriptedControlConnection(
            listOf(
                """{"t":"enroll.progress","phase":"stopping_runtime"}""",
                """{"t":"enroll.progress","phase":"capturing","captured":1,"total":10}""",
                """{"t":"enroll.progress","phase":"starting_runtime"}""",
                """{"t":"enroll.done","ok":true,"personCount":2,"embeddingsAdded":10}""",
            ),
        )

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(4, events.size)
        assertEquals(
            EnrollEvent.Progress(EnrollPhase.StoppingRuntime, captured = null, total = null, message = null),
            events.first(),
        )
        assertEquals(EnrollEvent.Done(personCount = 2, embeddingsAdded = 10), events.last())
        assertEquals(enrollCameraStartLine("tok", "Ana") + "\n", connection.written.single())
        assertTrue(connection.closed)
    }

    @Test
    fun busyReplyBecomesFailure() = runTest {
        val connection = ScriptedControlConnection(
            listOf("""{"t":"error","code":"busy","message":"an enrollment is already running"}"""),
        )

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(listOf(EnrollEvent.Failure(EnrollmentFailure.Busy)), events)
        assertTrue(connection.closed)
    }

    @Test
    fun unauthorizedReplyBecomesFailure() = runTest {
        val connection = ScriptedControlConnection(
            listOf("""{"t":"error","code":"unauthorized","message":"bad or missing token"}"""),
        )

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(listOf(EnrollEvent.Failure(EnrollmentFailure.Unauthorized)), events)
    }

    @Test
    fun malformedStreamBecomesFailure() = runTest {
        val connection = ScriptedControlConnection(listOf("not json"))

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(listOf(EnrollEvent.Failure(EnrollmentFailure.Io)), events)
    }

    @Test
    fun closedStreamBecomesFailure() = runTest {
        val connection = ScriptedControlConnection(listOf(null))

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(listOf(EnrollEvent.Failure(EnrollmentFailure.Io)), events)
    }

    @Test
    fun aWriteFailureBecomesFailureInsteadOfThrowing() = runTest {
        val connection = ThrowingWriteControlConnection(IllegalStateException("connection reset"))

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(listOf(EnrollEvent.Failure(EnrollmentFailure.Io)), events)
        assertTrue(connection.closed)
    }

    @Test
    fun anUnknownMessageDuringTheStreamIsIgnored() = runTest {
        val connection = ScriptedControlConnection(
            listOf(
                """{"t":"enroll.progress","phase":"capturing","captured":1,"total":10}""",
                // Contract §10: an unrecognized message type must not abort the enrollment.
                """{"t":"future.notice","detail":"hi"}""",
                """{"t":"enroll.done","ok":true,"personCount":1,"embeddingsAdded":1}""",
            ),
        )

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(2, events.size)
        assertIs<EnrollEvent.Progress>(events.first())
        assertEquals(EnrollEvent.Done(personCount = 1, embeddingsAdded = 1), events.last())
    }

    @Test
    fun imagesAreBase64EncodedIntoTheLine() = runTest {
        val connection = ScriptedControlConnection(
            listOf("""{"t":"enroll.done","ok":true,"personCount":1,"embeddingsAdded":1}"""),
        )
        // 0xC0 0xFF 0xEE encodes to "wP/u" (RFC 4648, Kotlin stdlib Base64).
        val bytes = byteArrayOf(0xC0.toByte(), 0xFF.toByte(), 0xEE.toByte())

        val events = client(FakeControlConnectionFactory(connection))
            .enrollFromImages("Ana", listOf(bytes))
            .toList()

        assertEquals(EnrollEvent.Done(personCount = 1, embeddingsAdded = 1), events.single())
        assertEquals(enrollImagesLine("tok", "Ana", listOf("wP/u")) + "\n", connection.written.single())
    }

    @Test
    fun cancelUsesTheCancelLineAndSecondConnection() = runTest {
        val cancelConnection = ScriptedControlConnection(listOf("""{"t":"enroll.cancelled"}"""))

        val result = client(SequentialControlConnectionFactory(listOf(cancelConnection))).cancelEnrollment()

        assertEquals(ControlResult.Ok(Unit), result)
        assertEquals(enrollCameraCancelLine("tok") + "\n", cancelConnection.written.single())
        assertTrue(cancelConnection.closed)
    }

    @Test
    fun cancelErrorBecomesIoOrBusy() = runTest {
        val busy = ScriptedControlConnection(
            listOf("""{"t":"error","code":"busy","message":"an enrollment is already running"}"""),
        )

        val result = client(SequentialControlConnectionFactory(listOf(busy))).cancelEnrollment()

        assertEquals(ControlResult.Busy, result)
    }

    @Test
    fun cancelUnexpectedReplyBecomesIo() = runTest {
        val connection = ScriptedControlConnection(
            listOf("""{"t":"enroll.done","ok":true,"personCount":1,"embeddingsAdded":1}"""),
        )

        val result = client(SequentialControlConnectionFactory(listOf(connection))).cancelEnrollment()

        assertIs<ControlResult.Io>(result)
    }

    @Test
    fun aStalledStreamEndsWithFailureWhenTheReadWatchdogFires() = runTest {
        // `readLine` never returns; only the per-line 30 s watchdog can end the stream. `runTest`
        // advances virtual time to that timeout, so this completes instantly and proves the stream
        // yields a value instead of hanging (CHG-FE-0020).
        val connection = StallingControlConnection()

        val events = client(FakeControlConnectionFactory(connection)).enrollFromCamera("Ana").toList()

        assertEquals(listOf(EnrollEvent.Failure(EnrollmentFailure.Io)), events)
        assertEquals(enrollCameraStartLine("tok", "Ana") + "\n", connection.written.single())
        assertTrue(connection.closed)
    }
}
