package com.korealm.lumina.ui.people

import com.korealm.lumina.protocol.EnrollPhase
import com.korealm.lumina.ui.ConnectionState

/** A people-screen command that is currently in flight, used to disable the matching control. */
enum class PendingPeopleAction {
    Refresh,
    Enroll,
    Cancel,
    Runtime,
}

/**
 * Immutable UI state for the Personas tab (FE-INV-051).
 *
 * Responsibility: a single snapshot the people composable renders. [people] is the live list from
 * telemetry (or the last successful refresh); it is kept across a temporary disconnect rather than
 * cleared (contrast with the dashboard, which clears stale live values — FE-INV-031).
 *
 * @param connection current connection state (shared with the dashboard).
 * @param people the enrolled names to show.
 * @param refreshing true while a `people.list` refresh is in flight.
 * @param name the "add person" name field.
 * @param nameError true when the name was empty on a submit attempt.
 * @param enrolling true while an enrollment stream is active.
 * @param phase the current enrollment phase, or `null` before the first progress line.
 * @param captured frames captured so far (camera), or `null`.
 * @param total frame target (camera), or `null`.
 * @param noFaceNotice true when the device reported a transient "no face detected" notice.
 * @param noFaceNoticeSeq increments only when the notice appears after an absence, so the screen can
 *   announce it once per occurrence instead of on every frame (FE-INV-010).
 * @param canRestartRuntime true after an `enroll.error` whose `runtime != "started"` (contract §6.5).
 * @param pending the command in flight, or `null`.
 * @param message the last action outcome to announce once, or `null`.
 * @param messageSeq monotonic counter feeding [PeopleUiMessage.id]; never shown.
 * @param needsToken true after an `unauthorized` reply until a later command succeeds.
 */
data class PeopleUiState(
    val connection: ConnectionState = ConnectionState.Connecting,
    val people: List<String> = emptyList(),
    val refreshing: Boolean = false,
    val name: String = "",
    val nameError: Boolean = false,
    val enrolling: Boolean = false,
    val phase: EnrollPhase? = null,
    val captured: Int? = null,
    val total: Int? = null,
    val noFaceNotice: Boolean = false,
    val noFaceNoticeSeq: Long = 0L,
    val canRestartRuntime: Boolean = false,
    val pending: PendingPeopleAction? = null,
    val message: PeopleUiMessage? = null,
    val messageSeq: Long = 0L,
    val needsToken: Boolean = false,
)
