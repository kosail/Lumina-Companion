package com.korealm.lumina.transport

import com.korealm.lumina.protocol.DeviceStatus

/**
 * What the telemetry channel reports to the app.
 *
 * Responsibility: give the UI one exhaustive type for connection state. The transport owns the
 * 5-second liveness rule (`API_CONTRACT.md` §2/§9) so consumers never re-implement it; the domain
 * values themselves live in `protocol/` ([DeviceStatus]).
 *
 * Kotlin note: `sealed interface` is like a Java sealed interface — a `when` over it is exhaustive
 * without an `else`.
 */
sealed interface TelemetryEvent {

    /** A valid `status` frame arrived; the device is online. */
    data class Online(val status: DeviceStatus) : TelemetryEvent

    /**
     * No `status` has arrived for `>= 5000 ms` (or the socket failed and is being retried). The UI
     * must stop presenting live values and show "Sin conexión" (FE-INV-031).
     */
    data object Offline : TelemetryEvent

    /**
     * The device speaks a protocol version this client does not support (`proto != 1`). The UI must
     * present an "incompatible device" state rather than possibly-wrong values (contract §10).
     */
    data class Incompatible(val proto: Int) : TelemetryEvent
}
