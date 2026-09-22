package com.korealm.lumina.transport

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.BoundDatagramSocket
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.aSocket
import kotlinx.io.Buffer
import kotlinx.io.readByteArray

/**
 * Production [TelemetrySocketFactory] backed by Ktor's raw UDP API (`ktor-network`).
 *
 * Responsibility: the single place in the app that touches `io.ktor.network` for UDP (FE-INV-022).
 * It binds an ephemeral local port (`0`) and remembers the gateway address; the device unicasts its
 * `status` datagrams back to that source port after a `subscribe` (`API_CONTRACT.md` §3.1).
 *
 * Kotlin note: `aSocket(selector).udp().bind(...)` is a `suspend` builder; `BoundDatagramSocket`
 * sends and receives `Datagram`s, whose payload is a `kotlinx.io.Source`.
 *
 * @param selector the shared Ktor selector used to multiplex socket I/O.
 */
class KtorTelemetrySocketFactory(
    private val selector: SelectorManager,
) : TelemetrySocketFactory {

    override suspend fun open(endpoint: Endpoint): TelemetrySocket {
        // Bind to an ephemeral port on all interfaces; the gateway replies to this source port.
        val socket = aSocket(selector).udp().bind(hostname = "0.0.0.0", port = 0)
        return KtorTelemetrySocket(socket, InetSocketAddress(endpoint.host, endpoint.udpPort))
    }
}

/** A [TelemetrySocket] over one Ktor [BoundDatagramSocket] and a fixed remote address. */
private class KtorTelemetrySocket(
    private val socket: BoundDatagramSocket,
    private val remote: InetSocketAddress,
) : TelemetrySocket {

    override suspend fun send(payload: ByteArray) {
        // `Buffer` is both a Source and a Sink; wrap the bytes and address them to the gateway.
        socket.send(Datagram(Buffer().apply { write(payload) }, remote))
    }

    override suspend fun receive(): ByteArray = socket.receive().packet.readByteArray()

    override fun close() {
        socket.close()
    }
}
