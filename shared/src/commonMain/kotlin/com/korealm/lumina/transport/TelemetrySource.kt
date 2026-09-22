package com.korealm.lumina.transport

import kotlinx.coroutines.flow.Flow

/**
 * Read-only telemetry stream (UDP `47600`).
 *
 * Responsibility: abstract "the live device status" so the UI and view models never touch sockets
 * (FE-INV-022/050). Implementations own the subscribe handshake, the 1 Hz receive loop, the 5 s
 * re-subscribe, and the liveness timeout; consumers only collect [TelemetryEvent]s.
 *
 * Threading: [status] is cold. Each collection opens its own socket and runs until the collector is
 * cancelled; implementations must honour cancellation and close their socket (FE-INV-052).
 */
interface TelemetrySource {

    /**
     * A cold flow of connection state. It emits [TelemetryEvent.Online] for each decoded frame and
     * [TelemetryEvent.Offline] once the device has been silent for 5 s; it keeps retrying with
     * backoff instead of completing.
     */
    fun status(): Flow<TelemetryEvent>
}
