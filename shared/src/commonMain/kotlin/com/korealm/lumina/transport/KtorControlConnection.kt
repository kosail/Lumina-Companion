package com.korealm.lumina.transport

import com.korealm.lumina.protocol.MAX_LINE_BYTES
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readLineStrict
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.withContext

/**
 * Production [ControlConnectionFactory] backed by Ktor's raw TCP API (`ktor-network`).
 *
 * Responsibility: the single place that touches `io.ktor.network` for TCP (FE-INV-022). It connects
 * to the gateway's control port; framing is one JSON object per `\n`-terminated line
 * (`API_CONTRACT.md` §3.2).
 *
 * @param selector the shared Ktor selector used to multiplex socket I/O.
 */
class KtorControlConnectionFactory(
    private val selector: SelectorManager,
) : ControlConnectionFactory {

    override suspend fun open(endpoint: Endpoint): ControlConnection = withContext(ioDispatcher) {
        // Resolve the gateway and connect off the caller's thread. On Android the caller can be the
        // main thread (a command launched from viewModelScope), and main-thread address resolution
        // throws NetworkOnMainThreadException — the Phase 6 device bug (FE-INV-052, CHG-FE-0027).
        KtorControlConnection(aSocket(selector).tcp().connect(endpoint.host, endpoint.tcpPort))
    }
}

/** A [ControlConnection] over one Ktor [Socket]'s read/write channels. */
private class KtorControlConnection(private val socket: Socket) : ControlConnection {

    // autoFlush = true so each write reaches the device immediately; the protocol is one line at a
    // time and the device never sends unsolicited data (contract §3.2).
    private val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
    private val input: ByteReadChannel = socket.openReadChannel()

    override suspend fun writeLine(line: String) {
        // Channel I/O is dispatched to the socket's own context; keep it off the caller's (Android
        // main) thread as well (FE-INV-052, CHG-FE-0027).
        withContext(ioDispatcher) { output.writeStringUtf8(line + "\n") }
    }

    /**
     * Reads one line. `readLineStrict` throws `TooLongLineException` past [MAX_LINE_BYTES] and
     * `EOFException` if the channel closes mid-line; [KtorControlClient] maps both to
     * `ControlResult.Io`. A clean EOF returns `null`.
     */
    override suspend fun readLine(): String? = withContext(ioDispatcher) {
        input.readLineStrict(limit = MAX_LINE_BYTES)
    }

    override fun close() {
        socket.close()
    }
}
