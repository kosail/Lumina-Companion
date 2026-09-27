package com.korealm.lumina.ui.dashboard

import com.korealm.lumina.protocol.ControlResult

/**
 * A user-facing outcome of a control command, as a resource-agnostic key.
 *
 * Responsibility: let the view model report "what happened" without hardcoding Spanish text, so the
 * composable maps it with `stringResource` (FE-INV-041). One member per message the dashboard shows.
 */
enum class ControlMessage {
    /** `volume.set` succeeded. */
    VolumeChanged,

    /** `volume.mute(true)` succeeded. */
    Muted,

    /** `volume.mute(false)` succeeded. */
    Unmuted,

    /** `runtime.start` succeeded. */
    RuntimeStarted,

    /** `runtime.stop` succeeded. */
    RuntimeStopped,

    /** `runtime.start` was accepted but the device never confirmed it within the grace window. */
    RuntimeStartUnconfirmed,

    /** `runtime.start` was rejected: the device replied that the runtime is not running. */
    RuntimeStartFailed,

    /** `runtime.stop` was rejected: the device replied that the runtime is still running. */
    RuntimeStopFailed,

    /** The token was missing or wrong (contract §4.9); requires the operator to fix Ajustes. */
    Unauthorized,

    /** An enrollment is already running (contract §4.9); retry later. */
    Busy,

    /** The request was rejected (contract §4.9). */
    BadRequest,

    /** A device-side failure (contract §4.9); retryable. */
    Internal,

    /** A local socket/timeout failure; retryable. */
    Io;

    /** True for the failure messages, which the UI styles as errors rather than confirmations. */
    val isError: Boolean
        get() = this == Unauthorized || this == Busy || this == BadRequest || this == Internal ||
            this == Io || this == RuntimeStartUnconfirmed || this == RuntimeStartFailed ||
            this == RuntimeStopFailed
}

/**
 * One message to show once, identified by a monotonically increasing [id].
 *
 * Kotlin note: the [id] exists because the message is UI *state* (FE-INV-051), not an event stream;
 * the composable keys its `LaunchedEffect` on [id] so the same message can be shown again later.
 */
data class UiMessage(val id: Long, val message: ControlMessage)

/**
 * Maps a control outcome to its failure message, or `null` when the command succeeded.
 *
 * Kept pure and top-level so the mapping is unit-tested without coroutines (AGENTS §5).
 *
 * @param result the outcome returned by a repository command.
 */
internal fun failureMessage(result: ControlResult<*>): ControlMessage? = when (result) {
    is ControlResult.Ok<*> -> null
    ControlResult.Unauthorized -> ControlMessage.Unauthorized
    ControlResult.Busy -> ControlMessage.Busy
    is ControlResult.BadRequest -> ControlMessage.BadRequest
    is ControlResult.Internal -> ControlMessage.Internal
    is ControlResult.Io -> ControlMessage.Io
}
