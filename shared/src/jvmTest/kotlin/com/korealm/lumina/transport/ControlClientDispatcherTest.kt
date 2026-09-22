package com.korealm.lumina.transport

import com.korealm.lumina.protocol.ControlResult
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Regression test for the Phase 6 physical-device bug (CHG-FE-0027).
 *
 * Control commands are launched from `viewModelScope`, which is `Dispatchers.Main` on Android. Ktor's
 * `connect(host, port)` resolves the gateway address on the caller's thread, so without a dispatcher
 * switch it ran on the main thread and threw `NetworkOnMainThreadException` — surfaced to the user as
 * "No se pudo conectar con el dispositivo", with no TCP connection ever opened. The control path must
 * therefore run on [ioDispatcher] (FE-INV-052).
 *
 * This test would fail on the pre-fix code: the caller (test) thread would be the one opening the
 * socket.
 */
class ControlClientDispatcherTest {

    @Test
    fun controlIoDoesNotRunOnTheCallersThread() = runTest {
        var openThreadName: String? = null
        val factory = object : ControlConnectionFactory {
            override suspend fun open(endpoint: Endpoint): ControlConnection {
                openThreadName = Thread.currentThread().name
                return FakeControlConnection(
                    """{"t":"volume.state","ok":true,"value":50,"muted":false}""",
                )
            }
        }
        val client = KtorControlClient(
            endpointProvider = { Endpoint("gateway") },
            tokenProvider = { "tok" },
            connections = factory,
        )

        val callerThreadName = Thread.currentThread().name
        val result = client.volumeGet()

        assertTrue(result is ControlResult.Ok<*>, "the fake replies successfully")
        assertNotEquals(
            callerThreadName,
            openThreadName,
            "control socket open must not run on the caller's (main) thread",
        )
    }
}
