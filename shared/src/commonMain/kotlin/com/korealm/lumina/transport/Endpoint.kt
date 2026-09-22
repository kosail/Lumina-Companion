package com.korealm.lumina.transport

/**
 * Transport-layer entry point configuration.
 *
 * Responsibility: hold the gateway address and ports the app connects to. It is deliberately a
 * small value type in `transport/` (not a setting) so the socket layer never reads preferences
 * directly; the `data/SettingsStore` produces an [Endpoint] and injects it (FE-INV-050/053).
 *
 * The ports are contract constants (`API_CONTRACT.md` §2); only [host] normally changes (gateway,
 * desktop loopback, emulator alias, or a LAN IP). Nothing here is hardcoded at a call site.
 *
 * @param host gateway host name or IP (e.g. `10.42.0.1`, `127.0.0.1`, `10.0.2.2`).
 * @param udpPort UDP telemetry port; default [UDP_TELEMETRY_PORT].
 * @param tcpPort TCP control port; default [TCP_CONTROL_PORT].
 */
data class Endpoint(
    val host: String,
    val udpPort: Int = UDP_TELEMETRY_PORT,
    val tcpPort: Int = TCP_CONTROL_PORT,
)

/** UDP telemetry send + subscribe-receive port (`API_CONTRACT.md` §2). */
const val UDP_TELEMETRY_PORT: Int = 47600

/** TCP control port (`API_CONTRACT.md` §2). */
const val TCP_CONTROL_PORT: Int = 47601
