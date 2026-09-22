package com.korealm.lumina.ui.settings

/**
 * Immutable UI state for the settings screen (FE-INV-051).
 *
 * Responsibility: a single snapshot the Ajustes composable renders. The fields are kept as raw
 * strings (what the user typed) so partial input is preserved; [validateSettings] turns them into an
 * `Endpoint` only on save.
 *
 * The token is held here only to render the field; it is never logged (FE-INV-053).
 *
 * @param host gateway host or IP.
 * @param udpPort telemetry port as text.
 * @param tcpPort control port as text.
 * @param token control token.
 * @param tokenRevealed true when the token is shown in clear text.
 * @param errors per-field validation flags.
 * @param saved true after a successful save, until the next edit.
 */
data class SettingsUiState(
    val host: String = "",
    val udpPort: String = "",
    val tcpPort: String = "",
    val token: String = "",
    val tokenRevealed: Boolean = false,
    val errors: SettingsErrors = SettingsErrors(),
    val saved: Boolean = false,
)
