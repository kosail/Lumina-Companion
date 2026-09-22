package com.korealm.lumina.transport

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.protocol.peopleListLine
import com.korealm.lumina.protocol.runtimeStartLine
import com.korealm.lumina.protocol.runtimeStateLine
import com.korealm.lumina.protocol.runtimeStopLine
import com.korealm.lumina.protocol.volumeGetLine
import com.korealm.lumina.protocol.volumeMuteLine
import com.korealm.lumina.protocol.volumeSetLine
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for [KtorControlClient]: exact request line, reply decoding, error mapping, malformed/
 * unexpected replies and timeouts — all against a scripted [FakeControlConnection] (PLAN Phase 2).
 */
class ControlClientTest {

    private fun client(
        connection: ControlConnection,
        token: String = "tok",
    ): ControlClient = KtorControlClient(
        endpointProvider = { Endpoint("gateway") },
        tokenProvider = { token },
        connections = FakeControlConnectionFactory(connection),
    )

    @Test
    fun volumeGetSendsTheExactLineAndMapsTheReply() = runTest {
        val connection = FakeControlConnection("""{"t":"volume.state","ok":true,"value":70,"muted":false}""")

        val result = client(connection).volumeGet()

        assertEquals(ControlResult.Ok(VolumeState(percent = 70, muted = false, ok = true)), result)
        assertEquals(volumeGetLine("tok") + "\n", connection.written.single())
        assertTrue(connection.closed)
    }

    @Test
    fun volumeSetAndMuteUseTheirBuilders() = runTest {
        val setConnection = FakeControlConnection("""{"t":"volume.state","ok":true,"value":55,"muted":false}""")
        client(setConnection).volumeSet(55)
        assertEquals(volumeSetLine("tok", 55) + "\n", setConnection.written.single())

        val muteConnection = FakeControlConnection("""{"t":"volume.state","ok":true,"value":55,"muted":true}""")
        client(muteConnection).setMuted(true)
        assertEquals(volumeMuteLine("tok", true) + "\n", muteConnection.written.single())
    }

    @Test
    fun peopleMapsNames() = runTest {
        val connection = FakeControlConnection("""{"t":"people","names":["Ana","David Solís"]}""")

        val result = client(connection).people()

        assertEquals(ControlResult.Ok(listOf("Ana", "David Solís")), result)
        assertEquals(peopleListLine("tok") + "\n", connection.written.single())
    }

    @Test
    fun runtimeStateMapsSink() = runTest {
        val connection = FakeControlConnection("""{"t":"runtime.state","running":true,"sink":"ready"}""")

        val result = client(connection).runtimeState()

        assertEquals(ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Ready)), result)
        assertEquals(runtimeStateLine("tok") + "\n", connection.written.single())
    }

    @Test
    fun runtimeStartAndStopUseTheirBuilders() = runTest {
        val startConnection = FakeControlConnection("""{"t":"runtime.state","running":true,"sink":"ready"}""")
        client(startConnection).runtimeStart()
        assertEquals(runtimeStartLine("tok") + "\n", startConnection.written.single())

        val stopConnection = FakeControlConnection("""{"t":"runtime.state","running":false,"sink":"absent"}""")
        client(stopConnection).runtimeStop()
        assertEquals(runtimeStopLine("tok") + "\n", stopConnection.written.single())
    }

    @Test
    fun mapsUnauthorizedAndBusyErrors() = runTest {
        val unauthorized = FakeControlConnection(
            """{"t":"error","code":"unauthorized","message":"bad or missing token"}""",
        )
        assertEquals(ControlResult.Unauthorized, client(unauthorized).volumeGet())

        val busy = FakeControlConnection(
            """{"t":"error","code":"busy","message":"an enrollment is already running"}""",
        )
        assertEquals(ControlResult.Busy, client(busy).volumeGet())
    }

    @Test
    fun mapsBadRequestWithItsMessage() = runTest {
        val connection = FakeControlConnection(
            """{"t":"error","code":"bad_request","message":"volume must be 0..100"}""",
        )

        val result = client(connection).volumeSet(101)

        assertEquals(ControlResult.BadRequest("volume must be 0..100"), result)
    }

    @Test
    fun malformedReplyBecomesIo() = runTest {
        val connection = FakeControlConnection("not json")

        assertIs<ControlResult.Io>(client(connection).volumeGet())
    }

    @Test
    fun unexpectedReplyBecomesIo() = runTest {
        val connection = FakeControlConnection("""{"t":"people","names":[]}""")

        assertIs<ControlResult.Io>(client(connection).volumeGet())
    }

    @Test
    fun closedConnectionBecomesIo() = runTest {
        val connection = FakeControlConnection(null)

        assertIs<ControlResult.Io>(client(connection).volumeGet())
    }

    @Test
    fun readTimeoutBecomesIo() = runTest {
        val hanging = object : ControlConnection {
            override suspend fun writeLine(line: String) = Unit
            override suspend fun readLine(): String? {
                delay(Long.MAX_VALUE)
                return null
            }

            override fun close() = Unit
        }

        assertIs<ControlResult.Io>(client(hanging).volumeGet())
    }
}
