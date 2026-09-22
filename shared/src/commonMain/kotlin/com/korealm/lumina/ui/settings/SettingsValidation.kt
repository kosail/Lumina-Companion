package com.korealm.lumina.ui.settings

import com.korealm.lumina.transport.Endpoint

/**
 * Which settings fields failed validation, so the screen can highlight them.
 *
 * Kotlin note: a `data class` of booleans instead of a `Set<String>` keeps the state immutable and
 * comparable, and lets the composable check `errors.host` directly.
 */
data class SettingsErrors(
    val host: Boolean = false,
    val udpPort: Boolean = false,
    val tcpPort: Boolean = false,
) {
    /** True when at least one field is invalid. */
    val hasAny: Boolean get() = host || udpPort || tcpPort
}

/**
 * The result of validating the settings form.
 *
 * @param errors per-field flags for the UI.
 * @param endpoint the parsed endpoint, or `null` when the form is invalid.
 * @param token the trimmed token (may be empty until the operator pastes it).
 */
data class SettingsValidation(
    val errors: SettingsErrors,
    val endpoint: Endpoint?,
    val token: String,
)

/**
 * Validates the raw settings form into an [Endpoint].
 *
 * Kept pure and top-level so it is unit-tested without Compose (AGENTS §5). Rules: the host must be
 * non-blank; each port must parse to an integer in `1..65535`. The token is only trimmed — it may be
 * empty (the device will then answer `unauthorized`, which the dashboard already surfaces).
 *
 * @param host gateway host or IP as typed.
 * @param udpPort telemetry port as typed.
 * @param tcpPort control port as typed.
 * @param token control token as typed.
 */
internal fun validateSettings(
    host: String,
    udpPort: String,
    tcpPort: String,
    token: String,
): SettingsValidation {
    val trimmedHost = host.trim()
    val hostOk = trimmedHost.isNotEmpty()
    // `takeIf` returns null for an out-of-range port, folding "not a number" and "out of range"
    // into the same invalid case.
    val udp = udpPort.trim().toIntOrNull()?.takeIf { it in 1..65535 }
    val tcp = tcpPort.trim().toIntOrNull()?.takeIf { it in 1..65535 }

    val errors = SettingsErrors(host = !hostOk, udpPort = udp == null, tcpPort = tcp == null)
    val endpoint = if (hostOk && udp != null && tcp != null) {
        Endpoint(host = trimmedHost, udpPort = udp, tcpPort = tcp)
    } else {
        null
    }
    return SettingsValidation(errors = errors, endpoint = endpoint, token = token.trim())
}
