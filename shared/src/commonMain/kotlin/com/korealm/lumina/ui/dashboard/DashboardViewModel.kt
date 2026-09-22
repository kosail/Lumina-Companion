package com.korealm.lumina.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.korealm.lumina.data.DeviceRepository
import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.VolumeState
import com.korealm.lumina.transport.TelemetryEvent
import com.korealm.lumina.ui.ConnectionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * How long the volume slider waits after the last change before sending `volume.set`.
 *
 * A pure "on release" trigger is not enough: TalkBack adjusts the slider through its
 * `setProgress` semantics, which calls `onValueChange` without `onValueChangeFinished`, so a
 * release-only send would make the control unusable with a screen reader. A short debounce sends one
 * command per gesture for touch and one per adjustment burst for TalkBack.
 */
internal const val VOLUME_DEBOUNCE_MS: Long = 300L

/**
 * View model for the dashboard tab.
 *
 * Responsibility: collect the [DeviceRepository] telemetry into an immutable [DashboardUiState] and
 * run the user's control commands (volume/mute/runtime), mapping their [ControlResult] outcomes to
 * state (FE-INV-051). All reductions are pure top-level functions, so the logic is unit-tested
 * without coroutines or Android.
 *
 * Kotlin note: `viewModelScope` is cancelled when the ViewModel is cleared (FE-INV-052); the
 * debounce [Job] is a child of it, so leaving the screen cancels any pending `volume.set`.
 *
 * @param repository the device state + command facade (interface; faked in tests).
 */
class DashboardViewModel(
    private val repository: DeviceRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())

    /** Read-only state for the dashboard composable. */
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    /** Pending debounce for the volume slider; cancelled on each new change and on release. */
    private var volumeJob: Job? = null

    init {
        viewModelScope.launch {
            repository.status().collect { event ->
                _uiState.value = reduceDashboardState(_uiState.value, event)
            }
        }
    }

    /**
     * The slider moved. Echoes the value immediately so the UI tracks the gesture, then schedules the
     * command after [VOLUME_DEBOUNCE_MS] of inactivity.
     */
    fun onVolumeChange(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        val current = _uiState.value.volume ?: VolumeState(percent = null, muted = false, ok = true)
        _uiState.value = _uiState.value.copy(
            volume = current.copy(percent = clamped),
            volumeFromControl = true,
        )
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch {
            delay(VOLUME_DEBOUNCE_MS)
            sendVolume()
        }
    }

    /** The slider gesture ended: commit at once instead of waiting for the debounce. */
    fun onVolumeChangeFinished() {
        volumeJob?.cancel()
        volumeJob = viewModelScope.launch { sendVolume() }
    }

    /** The mute switch changed; echoes immediately and sends `volume.mute`. */
    fun onMuteToggle(muted: Boolean) {
        val current = _uiState.value.volume ?: VolumeState(percent = null, muted = muted, ok = true)
        _uiState.value = _uiState.value.copy(
            volume = current.copy(muted = muted),
            volumeFromControl = true,
        )
        launchAction(PendingAction.Mute) {
            reduceMuteResult(_uiState.value, repository.setMuted(muted))
        }
    }

    /** "Iniciar/Detener Lúmina": sends `runtime.start` or `runtime.stop` based on the last status. */
    fun onRuntimeToggle() {
        val running = _uiState.value.status?.runtime?.running ?: false
        launchAction(PendingAction.Runtime) {
            val result = if (running) repository.runtimeStop() else repository.runtimeStart()
            reduceRuntimeResult(_uiState.value, result)
        }
    }

    /** Clears the message once the UI has shown it. */
    fun onDismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /** Sends the current slider percent; safe to call when nothing changed (no-op without a value). */
    private suspend fun sendVolume() {
        val percent = _uiState.value.volume?.percent ?: return
        _uiState.value = beginAction(_uiState.value, PendingAction.Volume)
        _uiState.value = try {
            reduceVolumeResult(_uiState.value, repository.volumeSet(percent))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // The repository contract returns values, so this is only a safety net (AGENTS §7).
            reduceFailure(_uiState.value, ControlResult.Io(e.message ?: "unexpected"))
        }
    }

    /**
     * Runs a control command guarded against re-entry (a double tap while one is in flight is
     * ignored), then applies [block]'s reduced state.
     */
    private fun launchAction(action: PendingAction, block: suspend () -> DashboardUiState) {
        if (_uiState.value.pending != null) return
        _uiState.value = beginAction(_uiState.value, action)
        viewModelScope.launch {
            _uiState.value = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                reduceFailure(_uiState.value, ControlResult.Io(e.message ?: "unexpected"))
            }
        }
    }
}

/**
 * Pure reduction of one telemetry event into the next dashboard state.
 *
 * Kept top-level (not a method) so it can be tested directly with no coroutines and no
 * `Dispatchers.Main`. On [TelemetryEvent.Offline] the last status and volume are dropped so stale
 * values are never shown as live (FE-INV-031).
 *
 * The `volumeFromControl` guard skips exactly one telemetry frame after a control reply: a frame
 * generated *before* the command could otherwise arrive *after* the reply and snap the slider back.
 *
 * @param current the state before the event.
 * @param event the transport event to apply.
 */
internal fun reduceDashboardState(
    current: DashboardUiState,
    event: TelemetryEvent,
): DashboardUiState = when (event) {
    is TelemetryEvent.Online -> current.copy(
        connection = ConnectionState.Online,
        status = event.status,
        incompatibleProto = null,
        volume = if (current.volumeFromControl) {
            current.volume
        } else {
            VolumeState(
                percent = event.status.sensors.volumePercent,
                muted = event.status.sensors.muted,
                ok = true,
            )
        },
        volumeFromControl = false,
    )

    TelemetryEvent.Offline -> current.copy(
        connection = ConnectionState.Offline,
        status = null,
        incompatibleProto = null,
        volume = null,
        volumeFromControl = false,
    )

    is TelemetryEvent.Incompatible -> current.copy(
        connection = ConnectionState.Incompatible,
        status = null,
        incompatibleProto = event.proto,
        volume = null,
        volumeFromControl = false,
    )
}

/** Marks a control command as in flight and clears any message from the previous action. */
internal fun beginAction(current: DashboardUiState, action: PendingAction): DashboardUiState =
    current.copy(pending = action, message = null)

/** Applies a `volume.set` outcome. */
internal fun reduceVolumeResult(
    current: DashboardUiState,
    result: ControlResult<VolumeState>,
): DashboardUiState {
    val cleared = current.copy(pending = null)
    return when (result) {
        is ControlResult.Ok<VolumeState> -> cleared
            .copy(volume = result.value, volumeFromControl = true, needsToken = false)
            .withMessage(ControlMessage.VolumeChanged)

        else -> cleared.withFailure(result)
    }
}

/** Applies a `volume.mute` outcome; the message reflects the resulting mute state. */
internal fun reduceMuteResult(
    current: DashboardUiState,
    result: ControlResult<VolumeState>,
): DashboardUiState {
    val cleared = current.copy(pending = null)
    return when (result) {
        is ControlResult.Ok<VolumeState> -> cleared
            .copy(volume = result.value, volumeFromControl = true, needsToken = false)
            .withMessage(if (result.value.muted) ControlMessage.Muted else ControlMessage.Unmuted)

        else -> cleared.withFailure(result)
    }
}

/** Applies a `runtime.start`/`runtime.stop` outcome; the message reflects the resulting state. */
internal fun reduceRuntimeResult(
    current: DashboardUiState,
    result: ControlResult<RuntimeState>,
): DashboardUiState {
    val cleared = current.copy(pending = null)
    return when (result) {
        is ControlResult.Ok<RuntimeState> -> cleared
            .copy(needsToken = false)
            .withMessage(if (result.value.running) ControlMessage.RuntimeStarted else ControlMessage.RuntimeStopped)

        else -> cleared.withFailure(result)
    }
}

/** Clears the in-flight flag and applies a failure outcome (used by the VM's safety-net catch). */
internal fun reduceFailure(current: DashboardUiState, result: ControlResult<*>): DashboardUiState =
    current.copy(pending = null).withFailure(result)

/** Attaches the failure message and, for `unauthorized`, raises the persistent token flag. */
private fun DashboardUiState.withFailure(result: ControlResult<*>): DashboardUiState {
    val message = failureMessage(result) ?: return this
    val next = withMessage(message)
    return if (result == ControlResult.Unauthorized) next.copy(needsToken = true) else next
}

/** Sets a fresh [UiMessage] with the next id so the UI shows it exactly once. */
private fun DashboardUiState.withMessage(message: ControlMessage): DashboardUiState {
    val nextId = messageSeq + 1
    return copy(message = UiMessage(nextId, message), messageSeq = nextId)
}
