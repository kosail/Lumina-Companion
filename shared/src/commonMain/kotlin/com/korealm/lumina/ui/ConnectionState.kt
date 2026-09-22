package com.korealm.lumina.ui

/**
 * The device connection state shared by the dashboard and the people screen.
 *
 * Responsibility: a UI-owned enum so composables never depend on transport types directly
 * (FE-INV-050). It mirrors the transport's `TelemetryEvent` states. It lives in `ui/` (not in one
 * feature package) because both the dashboard banner and the people screen use it.
 */
enum class ConnectionState {
    /** No frame received yet (initial state). */
    Connecting,

    /** Receiving live `status` frames. */
    Online,

    /** No `status` for 5 s (or the socket is retrying). */
    Offline,

    /** The device speaks an unsupported protocol version. */
    Incompatible,
}
