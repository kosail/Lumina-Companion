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
 * How long to wait for the device to confirm an app-initiated runtime start before giving up.
 *
 * The agent reports `initializing` within ~1 s of accepting the command, so this only covers a
 * lost/absent confirmation; on expiry the button is re-enabled with a neutral message (CHG-FE-0034).
 */
internal const val RUNTIME_START_GRACE_MS: Long = 10_000L

/**
 * Fallback for a start the device confirmed only as `running` (an agent that predates the additive
 * `initializing` flag): we cannot tell "ready" from "still starting", so hold the button until
 * telemetry reports the runtime running.
 *
 * Sized to the runtime's real worst case (docs/PERFORMANCE.md §14.3, docs/BLUETOOTH.md §2): ~21 s of
 * model load + up to 180 s waiting for the BlueALSA sink = ~201 s, plus margin. A genuinely failed
 * start normally resolves sooner: the device powers off / exits 2 after 180 s, which makes telemetry
 * go offline and clears the transition. This only fires if frames keep arriving but the runtime never
 * becomes ready (CHG-FE-0037).
 */
internal const val RUNTIME_START_FALLBACK_MS: Long = 240_000L

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

    /** Safety net for an unconfirmed runtime start; cancelled once the device confirms (CHG-FE-0034). */
    private var runtimeGraceJob: Job? = null

    init {
        viewModelScope.launch {
            repository.status().collect { event ->
                _uiState.value = reduceDashboardState(_uiState.value, event)
                // Once the device confirms (or the link drops) the transition is over, so the grace
                // job must not fire later with a spurious warning.
                if (_uiState.value.transition != RuntimeTransition.Starting) {
                    runtimeGraceJob?.cancel()
                }
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
        val current = _uiState.value
        // Ignore taps while a start/stop is settling or the device is initializing (external start):
        // the user must not re-send commands during the ~18-60 s startup (contract §4.1, CHG-FE-0034).
        val buttonState = runtimeButtonState(current)
        if (buttonState != RuntimeButtonState.Start && buttonState != RuntimeButtonState.Stop) return
        val command = if (current.status?.runtime?.running == true) RuntimeCommand.Stop else RuntimeCommand.Start
        launchAction(PendingAction.Runtime) {
            val result =
                if (command == RuntimeCommand.Stop) repository.runtimeStop() else repository.runtimeStart()
            val reduced = reduceRuntimeResult(_uiState.value, result, command)
            if (reduced.transition == RuntimeTransition.Starting) {
                // A reply that is initializing (new agent) is confirmed within ~1 s, so the short grace
                // is enough; a reply that is only running (older agent) needs the long fallback because
                // we cannot tell ready from still-starting.
                val onlyRunning = result is ControlResult.Ok<RuntimeState> && !result.value.initializing
                armStartGrace(longFallback = onlyRunning)
            }
            reduced
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
     * Safety net for a start the device never confirms: if telemetry does not report
     * `initializing`/`running` within the window, clear the transition so the button is usable again.
     * The telemetry collector cancels it as soon as the device confirms (or the link drops).
     *
     * @param longFallback true when the reply only said `running` (an agent without the additive
     *   `initializing` flag), which uses [RUNTIME_START_FALLBACK_MS] to cover the full startup; false
     *   for an `initializing` reply, which is confirmed within ~1 s and uses [RUNTIME_START_GRACE_MS].
     */
    private fun armStartGrace(longFallback: Boolean) {
        runtimeGraceJob?.cancel()
        runtimeGraceJob = viewModelScope.launch {
            delay(if (longFallback) RUNTIME_START_FALLBACK_MS else RUNTIME_START_GRACE_MS)
            if (_uiState.value.transition == RuntimeTransition.Starting) {
                _uiState.value = _uiState.value.copy(transition = null)
                    .withMessage(ControlMessage.RuntimeStartUnconfirmed)
            }
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
    is TelemetryEvent.Online -> {
        // The device owns the runtime truth. A `stopping` transition is held until telemetry stops
        // reporting the runtime; `initializing`/`running` clears a `starting` transition (the UI then
        // reads "starting" straight from `status.runtime.initializing`); an unconfirmed `starting`
        // stays until the device confirms or the grace job fires (CHG-FE-0034).
        val runtime = event.status.runtime
        val transition = when {
            current.transition == RuntimeTransition.Stopping ->
                if (runtime.running) RuntimeTransition.Stopping else null
            runtime.initializing || runtime.running -> null
            else -> current.transition
        }
        current.copy(
            connection = ConnectionState.Online,
            status = event.status,
            incompatibleProto = null,
            transition = transition,
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
    }

    TelemetryEvent.Offline -> current.copy(
        connection = ConnectionState.Offline,
        status = null,
        incompatibleProto = null,
        transition = null,
        volume = null,
        volumeFromControl = false,
    )

    is TelemetryEvent.Incompatible -> current.copy(
        connection = ConnectionState.Incompatible,
        status = null,
        incompatibleProto = event.proto,
        transition = null,
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

/**
 * Applies a `runtime.start`/`runtime.stop` outcome.
 *
 * The transition and the message both come from what the device reported, so a command that did not
 * take is never shown as a settling success:
 * - `start` → `Starting` while the unit is active (`initializing` or already `running`, which also
 *   covers an agent without the additive flag); neither → `RuntimeStartFailed` and no transition.
 * - `stop` → `Stopping` only when the device confirms it is no longer running; still `running` →
 *   `RuntimeStopFailed` and no transition (otherwise a failed stop would stick "stopping" forever,
 *   CHG-FE-0037).
 *
 * @param command which command was sent.
 */
internal fun reduceRuntimeResult(
    current: DashboardUiState,
    result: ControlResult<RuntimeState>,
    command: RuntimeCommand,
): DashboardUiState {
    val cleared = current.copy(pending = null)
    return when (result) {
        is ControlResult.Ok<RuntimeState> -> {
            val running = result.value.running
            val initializing = result.value.initializing
            val transition: RuntimeTransition?
            val message: ControlMessage
            when (command) {
                RuntimeCommand.Start -> {
                    val active = initializing || running
                    transition = if (active) RuntimeTransition.Starting else null
                    message = if (active) ControlMessage.RuntimeStarted else ControlMessage.RuntimeStartFailed
                }

                RuntimeCommand.Stop -> {
                    transition = if (running) null else RuntimeTransition.Stopping
                    message = if (running) ControlMessage.RuntimeStopFailed else ControlMessage.RuntimeStopped
                }
            }
            cleared.copy(transition = transition, needsToken = false).withMessage(message)
        }

        else -> cleared.copy(transition = null).withFailure(result)
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
