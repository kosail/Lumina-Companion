package com.korealm.lumina.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.korealm.lumina.data.EnrollmentRepository
import com.korealm.lumina.data.PeopleRepository
import com.korealm.lumina.data.PeopleSnapshot
import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.ENROLL_FRAMES_DEFAULT
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.EnrollmentFailure
import com.korealm.lumina.protocol.RuntimeAfterEnroll
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.ui.ConnectionState
import com.korealm.lumina.ui.picker.MAX_ENROLL_PHOTOS
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The device `exitCode` that marks a cancelled enrollment (contract §4.7). */
private const val CANCELLED_EXIT_CODE: Int = -1

/**
 * View model for the Personas tab.
 *
 * Responsibility: collect the live people list into an immutable [PeopleUiState] and run the people
 * commands — refresh, camera/photo enrollment, cancel, and the "Start Lúmina" recovery — mapping
 * their outcomes to state (FE-INV-051). All reductions are pure top-level functions, so the logic is
 * unit-tested without coroutines or Android.
 *
 * Accessibility: enrollment progress and terminal outcomes are exposed as state; the composable
 * announces phase/result changes (not every frame) via a `liveRegion` (FE-INV-010).
 *
 * Kotlin note: `viewModelScope` is cancelled when the ViewModel is cleared (FE-INV-052), so an
 * in-flight enrollment collection is cancelled with the screen. That closes the socket but does
 * **not** cancel the device enrollment; the "Cancelar" action sends `enroll.camera.cancel` on a
 * second connection (contract §6.3).
 *
 * @param peopleRepository live list + refresh + runtime recovery.
 * @param enrollmentRepository camera/photo enrollment streams.
 */
class PeopleViewModel(
    private val peopleRepository: PeopleRepository,
    private val enrollmentRepository: EnrollmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PeopleUiState())

    /** Read-only state for the people composable. */
    val uiState: StateFlow<PeopleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.people().collect { snapshot ->
                _uiState.value = reducePeopleSnapshot(_uiState.value, snapshot)
            }
        }
    }

    /** The "add person" name field changed; clears its validation error. */
    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value, nameError = false)
    }

    /** `people.list` refresh; disabled by the UI while another action runs. */
    fun onRefresh() {
        if (_uiState.value.pending != null || _uiState.value.enrolling) return
        _uiState.value = _uiState.value.copy(
            refreshing = true,
            pending = PendingPeopleAction.Refresh,
            message = null,
        )
        viewModelScope.launch {
            val result = runCommand { peopleRepository.refresh() }
            _uiState.value = reduceRefreshResult(_uiState.value, result)
        }
    }

    /** Start a camera enrollment for the entered name (contract §5.8). */
    fun onEnrollFromCamera() {
        startEnrollment { name -> enrollmentRepository.enrollFromCamera(name, ENROLL_FRAMES_DEFAULT) }
    }

    /**
     * Start a photo enrollment from the picked raw images (contract §5.9).
     *
     * An empty list means the user cancelled the picker, so it is a no-op (no message); more than
     * [MAX_ENROLL_PHOTOS] is rejected before any work.
     */
    fun onImagesPicked(images: List<ByteArray>) {
        when {
            images.isEmpty() -> Unit
            images.size > MAX_ENROLL_PHOTOS ->
                _uiState.value = _uiState.value.withMessage(PeopleMessage.TooManyPhotos)

            else -> startEnrollment { name -> enrollmentRepository.enrollFromImages(name, images) }
        }
    }

    /**
     * Sends `enroll.camera.cancel` on a second connection while an enrollment runs. The enrolling
     * stream ends by itself with `enroll.error` (`exitCode == -1`), which is announced then.
     */
    fun onCancelEnrollment() {
        if (!_uiState.value.enrolling || _uiState.value.pending == PendingPeopleAction.Cancel) return
        _uiState.value = _uiState.value.copy(pending = PendingPeopleAction.Cancel)
        viewModelScope.launch {
            val result = runCommand { enrollmentRepository.cancel() }
            _uiState.value = reduceCancelResult(_uiState.value, result)
        }
    }

    /** "Iniciar Lúmina" after a failed enrollment left the runtime down (contract §6.5). */
    fun onStartRuntime() {
        if (_uiState.value.pending != null) return
        _uiState.value = _uiState.value.copy(pending = PendingPeopleAction.Runtime, message = null)
        viewModelScope.launch {
            val result = runCommand { peopleRepository.startRuntime() }
            _uiState.value = reduceRuntimeResult(_uiState.value, result)
        }
    }

    /** Clears the message once the shell has shown it. */
    fun onDismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    /**
     * Validates the name, marks the state as enrolling, then collects [flowFactory]'s stream into
     * state. Guarded against re-entry so a double tap cannot start two enrollments (the device would
     * answer `busy` anyway). On success it quietly refreshes the list, because `personCount` may lag
     * (contract §4.6).
     */
    private fun startEnrollment(flowFactory: (String) -> Flow<EnrollEvent>) {
        val current = _uiState.value
        if (current.enrolling || current.pending != null) return
        val name = current.name.trim()
        if (name.isEmpty()) {
            _uiState.value = current.copy(nameError = true).withMessage(PeopleMessage.NeedName)
            return
        }
        _uiState.value = current.copy(
            enrolling = true,
            phase = null,
            captured = null,
            total = null,
            noFaceNotice = false,
            canRestartRuntime = false,
            pending = PendingPeopleAction.Enroll,
            message = null,
        )
        viewModelScope.launch {
            try {
                flowFactory(name).collect { event ->
                    _uiState.value = reduceEnrollEvent(_uiState.value, event)
                    if (event is EnrollEvent.Done) refreshQuietly()
                }
                // Defensive: the transport always ends with a terminal event, but never leave the
                // screen stuck in "enrolling" if a stream ever completes without one.
                if (_uiState.value.enrolling) {
                    _uiState.value = _uiState.value.copy(
                        enrolling = false,
                        pending = null,
                        phase = null,
                        captured = null,
                        total = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // The repository returns values, so this is only a safety net (AGENTS §7).
                _uiState.value = reduceEnrollEvent(
                    _uiState.value,
                    EnrollEvent.Failure(EnrollmentFailure.Io),
                )
            }
        }
    }

    /** Best-effort `people.list` after a success; failures are ignored (the telemetry will catch up). */
    private suspend fun refreshQuietly() {
        val result = try {
            peopleRepository.refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            return
        }
        if (result is ControlResult.Ok<List<String>>) {
            _uiState.value = _uiState.value.copy(people = result.value)
        }
    }

    /** Runs a command, converting any unexpected throwable into an [ControlResult.Io] value. */
    private suspend fun <T> runCommand(block: suspend () -> ControlResult<T>): ControlResult<T> = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        ControlResult.Io(e.message ?: "unexpected")
    }
}

/**
 * Pure reduction of one telemetry-driven people snapshot.
 *
 * On online it adopts the live names; offline it keeps the last-known list so a temporary drop does
 * not erase the screen (contract §5.4: people may be shown from the last-known list).
 */
internal fun reducePeopleSnapshot(current: PeopleUiState, snapshot: PeopleSnapshot): PeopleUiState {
    val connection = if (snapshot.online) ConnectionState.Online else ConnectionState.Offline
    return if (snapshot.online) {
        current.copy(connection = connection, people = snapshot.names)
    } else {
        current.copy(connection = connection)
    }
}

/** Applies a `people.list` outcome. */
internal fun reduceRefreshResult(
    current: PeopleUiState,
    result: ControlResult<List<String>>,
): PeopleUiState {
    val cleared = current.copy(refreshing = false, pending = null)
    return when (result) {
        is ControlResult.Ok<List<String>> -> cleared
            .copy(people = result.value, needsToken = false)
            .withMessage(PeopleMessage.Refreshed)

        else -> cleared.withFailure(result)
    }
}

/** Applies a `runtime.start` recovery outcome. */
internal fun reduceRuntimeResult(
    current: PeopleUiState,
    result: ControlResult<RuntimeState>,
): PeopleUiState {
    val cleared = current.copy(pending = null)
    return when (result) {
        is ControlResult.Ok<RuntimeState> -> cleared
            .copy(canRestartRuntime = false, needsToken = false)
            .withMessage(PeopleMessage.RuntimeStarted)

        else -> cleared.withFailure(result)
    }
}

/** Applies the acknowledgement of `enroll.camera.cancel`; the cancellation itself is announced by the stream. */
internal fun reduceCancelResult(current: PeopleUiState, result: ControlResult<Unit>): PeopleUiState {
    val cleared = current.copy(pending = null)
    return when (result) {
        is ControlResult.Ok<Unit> -> cleared
        else -> cleared.withFailure(result)
    }
}

/**
 * Pure reduction of one enrollment event.
 *
 * A terminal event always clears the in-flight enrollment and posts exactly one message. The device
 * `enroll.error` text is intentionally **not** shown (FE-INV-041); only [PeopleMessage] keys are.
 */
internal fun reduceEnrollEvent(current: PeopleUiState, event: EnrollEvent): PeopleUiState = when (event) {
    is EnrollEvent.Progress -> {
        val notice = event.message != null
        current.copy(
            enrolling = true,
            phase = event.phase,
            captured = event.captured,
            total = event.total,
            noFaceNotice = notice,
            // Increment only on a false->true edge: the screen announces the notice once per
            // occurrence, not on every frame it is present (FE-INV-010, no screen-reader spam).
            noFaceNoticeSeq = if (notice && !current.noFaceNotice) {
                current.noFaceNoticeSeq + 1
            } else {
                current.noFaceNoticeSeq
            },
        )
    }

    is EnrollEvent.Done -> current
        .terminalEnrollment()
        // A success proves the token is valid and the name was accepted; clear both so the warning
        // card disappears and the field is ready for the next person.
        .copy(needsToken = false, name = "", nameError = false)
        .withMessage(PeopleMessage.PersonEnrolled)

    is EnrollEvent.Error -> current
        .terminalEnrollment()
        // Contract §6.5 is deliberately conservative: any state other than "started" offers the
        // recovery action (including "unchanged", which can mean the runtime was already down).
        .copy(canRestartRuntime = event.runtime != RuntimeAfterEnroll.Started)
        .withMessage(
            if (event.exitCode == CANCELLED_EXIT_CODE) {
                PeopleMessage.EnrollCancelled
            } else {
                PeopleMessage.EnrollFailed
            },
        )

    EnrollEvent.Cancelled -> current
        .terminalEnrollment()
        .withMessage(PeopleMessage.EnrollCancelled)

    is EnrollEvent.Failure -> current
        .terminalEnrollment()
        .withFailure(enrollmentFailureMessage(event.failure))
}

/** Clears every enrollment-progress field, leaving the list and connection untouched. */
private fun PeopleUiState.terminalEnrollment(): PeopleUiState = copy(
    enrolling = false,
    pending = null,
    phase = null,
    captured = null,
    total = null,
    noFaceNotice = false,
)

/** Attaches a control-failure message, raising [PeopleUiState.needsToken] for `unauthorized`. */
private fun PeopleUiState.withFailure(result: ControlResult<*>): PeopleUiState {
    val message = controlFailureMessage(result) ?: return this
    return withFailure(message)
}

/** Attaches an enrollment-failure message, raising [PeopleUiState.needsToken] for `unauthorized`. */
private fun PeopleUiState.withFailure(message: PeopleMessage): PeopleUiState {
    val withMessage = withMessage(message)
    return if (message == PeopleMessage.Unauthorized) withMessage.copy(needsToken = true) else withMessage
}

/** Sets a fresh [PeopleUiMessage] with the next id so the UI shows it exactly once. */
private fun PeopleUiState.withMessage(message: PeopleMessage): PeopleUiState {
    val nextId = messageSeq + 1
    return copy(message = PeopleUiMessage(nextId, message), messageSeq = nextId)
}
