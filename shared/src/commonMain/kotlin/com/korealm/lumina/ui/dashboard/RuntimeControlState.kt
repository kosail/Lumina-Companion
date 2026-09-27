package com.korealm.lumina.ui.dashboard

/**
 * Runtime-control vocabulary for the dashboard.
 *
 * Responsibility: the small, pure types and derivation the start/stop control needs, kept out of
 * [DashboardUiState] so each file has one concern (AGENTS §5). The device owns the truth through the
 * additive `initializing` flag (contract §4.1/§4.4); the local [RuntimeTransition] only bridges the
 * gap between a command reply and the next telemetry frame (CHG-FE-0034/CHG-FE-0037).
 */

/** The runtime command the user asked for, so the reply can pick the right transition. */
enum class RuntimeCommand { Start, Stop }

/**
 * A runtime start/stop that is settling: the command was accepted but the device has not yet reported
 * the outcome in telemetry. Keeps the button disabled and labeled so a user cannot re-send start
 * during the ~18-60 s startup (contract §4.1/§4.4).
 */
enum class RuntimeTransition { Starting, Stopping }

/** The action the runtime button presents, derived from the status and any local transition. */
enum class RuntimeButtonState { Start, Stop, Starting, Stopping }

/**
 * Pure derivation of the runtime button's state.
 *
 * The device owns the truth: `initializing` shows "starting" even for an external start the app did
 * not send. The local [RuntimeTransition] covers only the short gap between a command reply and the
 * next telemetry frame, and [RuntimeTransition.Stopping] takes precedence so a settling stop is never
 * masked by a stale "running" frame.
 */
internal fun runtimeButtonState(state: DashboardUiState): RuntimeButtonState = when {
    state.transition == RuntimeTransition.Stopping -> RuntimeButtonState.Stopping
    state.transition == RuntimeTransition.Starting ||
        state.status?.runtime?.initializing == true -> RuntimeButtonState.Starting
    state.status?.runtime?.running == true -> RuntimeButtonState.Stop
    else -> RuntimeButtonState.Start
}
