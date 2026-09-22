package com.korealm.lumina.ui.dashboard

/**
 * User intents emitted by the dashboard composable.
 *
 * Responsibility: bundle the dashboard's callbacks so the composable stays a pure renderer and the
 * parameter list does not explode (FE-INV-051). The composable never calls a repository directly;
 * it only invokes these.
 *
 * @param onVolumeChange the slider moved to a new percent (optimistic; the view model debounces).
 * @param onVolumeChangeFinished the slider gesture ended; commit immediately.
 * @param onMuteToggle the mute switch changed.
 * @param onRuntimeToggle "Iniciar/Detener Lúmina" was pressed.
 * @param onDismissMessage the shown message should be cleared.
 */
data class DashboardActions(
    val onVolumeChange: (Int) -> Unit,
    val onVolumeChangeFinished: () -> Unit,
    val onMuteToggle: (Boolean) -> Unit,
    val onRuntimeToggle: () -> Unit,
    val onDismissMessage: () -> Unit,
)
