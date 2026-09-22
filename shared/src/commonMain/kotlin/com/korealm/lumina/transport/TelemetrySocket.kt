package com.korealm.lumina.transport

/**
 * Minimal UDP seam used by [KtorTelemetrySource].
 *
 * Responsibility: hide Ktor's raw datagram API behind two suspend calls so the telemetry loop can be
 * driven by a fake in unit tests with no network (FE-INV-050/052). The only production
 * implementation is [KtorTelemetrySocket]; the only place that imports `io.ktor.network` is
 * `KtorTelemetrySocket.kt`.
 *
 * Threading: [send] and [receive] are `suspend`; `receive` suspends until a datagram arrives. The
 * implementation is single-reader (one `receive` at a time).
 */
interface TelemetrySocket {

    /** Sends one datagram payload to the remote telemetry address. */
    suspend fun send(payload: ByteArray)

    /** Suspends until one datagram arrives and returns its raw bytes. */
    suspend fun receive(): ByteArray

    /** Releases the underlying socket. Idempotent. */
    fun close()
}

/**
 * Opens a [TelemetrySocket] for an [Endpoint].
 *
 * Kotlin note: `fun interface` is a SAM interface — production uses [KtorTelemetrySocketFactory]
 * and tests pass a lambda/fake, which is how the transport is tested without sockets.
 */
fun interface TelemetrySocketFactory {

    /** Opens and binds a socket ready to [TelemetrySocket.send] to the endpoint's UDP port. */
    suspend fun open(endpoint: Endpoint): TelemetrySocket
}
