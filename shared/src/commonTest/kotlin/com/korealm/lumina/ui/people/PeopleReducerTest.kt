package com.korealm.lumina.ui.people

import com.korealm.lumina.data.PeopleSnapshot
import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollEvent
import com.korealm.lumina.protocol.EnrollPhase
import com.korealm.lumina.protocol.EnrollmentFailure
import com.korealm.lumina.protocol.RuntimeAfterEnroll
import com.korealm.lumina.protocol.RuntimeState
import com.korealm.lumina.protocol.SinkState
import com.korealm.lumina.ui.ConnectionState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Pure tests for the people reductions (no coroutines, no Android, no device).
 *
 * The telemetry-snapshot mapping and the command/enrollment-result mappings are the people screen's
 * only logic, so they are covered here; the view model wiring is covered in `jvmTest`.
 */
class PeopleReducerTest {

    @Test
    fun onlineSnapshotAdoptsTheNames() {
        val state = reducePeopleSnapshot(
            PeopleUiState(),
            PeopleSnapshot(online = true, names = listOf("Ana", "David Solís")),
        )

        assertEquals(ConnectionState.Online, state.connection)
        assertEquals(listOf("Ana", "David Solís"), state.people)
    }

    @Test
    fun offlineSnapshotKeepsTheLastKnownNames() {
        val online = reducePeopleSnapshot(
            PeopleUiState(),
            PeopleSnapshot(online = true, names = listOf("Ana")),
        )

        val offline = reducePeopleSnapshot(online, PeopleSnapshot(online = false, names = emptyList()))

        assertEquals(ConnectionState.Offline, offline.connection)
        assertEquals(listOf("Ana"), offline.people)
    }

    @Test
    fun progressCarriesPhaseAndCountsAndTheNotice() {
        val started = reduceEnrollEvent(
            PeopleUiState(),
            EnrollEvent.Progress(EnrollPhase.Capturing, captured = 3, total = 10, message = null),
        )

        assertTrue(started.enrolling)
        assertEquals(EnrollPhase.Capturing, started.phase)
        assertEquals(3, started.captured)
        assertEquals(10, started.total)
        assertFalse(started.noFaceNotice)

        val notice = reduceEnrollEvent(
            started,
            EnrollEvent.Progress(EnrollPhase.Capturing, 3, 10, message = "no face detected"),
        )
        assertTrue(notice.noFaceNotice)
    }

    @Test
    fun doneEndsTheEnrollmentAndAnnounces() {
        val running = PeopleUiState(
            enrolling = true,
            pending = PendingPeopleAction.Enroll,
            phase = EnrollPhase.Capturing,
            needsToken = true,
            name = "Ana",
            nameError = true,
        )

        val state = reduceEnrollEvent(running, EnrollEvent.Done(personCount = 2, embeddingsAdded = 10))

        assertFalse(state.enrolling)
        assertNull(state.pending)
        assertEquals(PeopleMessage.PersonEnrolled, state.message?.message)
        // A success proves the token is valid and the name was accepted.
        assertFalse(state.needsToken)
        assertEquals("", state.name)
        assertFalse(state.nameError)
    }

    @Test
    fun theNoFaceNoticeAnnouncesOncePerOccurrence() {
        val first = reduceEnrollEvent(
            PeopleUiState(),
            EnrollEvent.Progress(EnrollPhase.Capturing, 1, 10, message = "no face detected"),
        )
        assertEquals(1L, first.noFaceNoticeSeq)
        assertTrue(first.noFaceNotice)

        // A second notice frame (still present) must not re-announce.
        val still = reduceEnrollEvent(
            first,
            EnrollEvent.Progress(EnrollPhase.Capturing, 1, 10, message = "no face detected"),
        )
        assertEquals(1L, still.noFaceNoticeSeq)

        // A frame without the notice clears it...
        val cleared = reduceEnrollEvent(still, EnrollEvent.Progress(EnrollPhase.Capturing, 2, 10, null))
        assertFalse(cleared.noFaceNotice)

        // ...so the next notice is a new occurrence and announces again.
        val again = reduceEnrollEvent(
            cleared,
            EnrollEvent.Progress(EnrollPhase.Capturing, 2, 10, message = "no face detected"),
        )
        assertEquals(2L, again.noFaceNoticeSeq)
    }

    @Test
    fun cancelledErrorIsReportedAsCancellation() {
        val running = PeopleUiState(enrolling = true, pending = PendingPeopleAction.Enroll)

        val state = reduceEnrollEvent(
            running,
            EnrollEvent.Error(exitCode = -1, message = "enrollment failed", runtime = RuntimeAfterEnroll.Started),
        )

        assertEquals(PeopleMessage.EnrollCancelled, state.message?.message)
        assertFalse(state.canRestartRuntime)
        assertFalse(state.enrolling)
    }

    @Test
    fun aRealErrorOffersRuntimeRecoveryWhenNotStarted() {
        val running = PeopleUiState(enrolling = true)

        val state = reduceEnrollEvent(
            running,
            EnrollEvent.Error(exitCode = 2, message = "no face", runtime = RuntimeAfterEnroll.Absent),
        )

        assertEquals(PeopleMessage.EnrollFailed, state.message?.message)
        assertTrue(state.canRestartRuntime)
    }

    @Test
    fun startedRuntimeDoesNotOfferRecovery() {
        val state = reduceEnrollEvent(
            PeopleUiState(enrolling = true),
            EnrollEvent.Error(exitCode = 2, message = "no face", runtime = RuntimeAfterEnroll.Started),
        )

        assertFalse(state.canRestartRuntime)
    }

    @Test
    fun unauthorizedFailureRaisesNeedsToken() {
        val state = reduceEnrollEvent(
            PeopleUiState(enrolling = true),
            EnrollEvent.Failure(EnrollmentFailure.Unauthorized),
        )

        assertTrue(state.needsToken)
        assertEquals(PeopleMessage.Unauthorized, state.message?.message)
    }

    @Test
    fun busyFailureIsSurfaced() {
        val state = reduceEnrollEvent(PeopleUiState(enrolling = true), EnrollEvent.Failure(EnrollmentFailure.Busy))

        assertEquals(PeopleMessage.Busy, state.message?.message)
        assertFalse(state.enrolling)
    }

    @Test
    fun imagePreparationFailureHasItsOwnMessage() {
        val state = reduceEnrollEvent(
            PeopleUiState(enrolling = true),
            EnrollEvent.Failure(EnrollmentFailure.ImagePreparation),
        )

        assertEquals(PeopleMessage.PhotosNotPrepared, state.message?.message)
    }

    @Test
    fun refreshSuccessSetsTheListAndClearsPending() {
        val refreshing = PeopleUiState(refreshing = true, pending = PendingPeopleAction.Refresh)

        val state = reduceRefreshResult(refreshing, ControlResult.Ok(listOf("Ana")))

        assertFalse(state.refreshing)
        assertNull(state.pending)
        assertEquals(listOf("Ana"), state.people)
        assertEquals(PeopleMessage.Refreshed, state.message?.message)
    }

    @Test
    fun refreshFailurePostsAMessage() {
        val state = reduceRefreshResult(
            PeopleUiState(refreshing = true, pending = PendingPeopleAction.Refresh),
            ControlResult.Io("timeout"),
        )

        assertEquals(PeopleMessage.Io, state.message?.message)
        assertNull(state.pending)
    }

    @Test
    fun runtimeStartSuccessClearsRecovery() {
        val state = reduceRuntimeResult(
            PeopleUiState(canRestartRuntime = true, pending = PendingPeopleAction.Runtime),
            ControlResult.Ok(RuntimeState(running = true, sink = SinkState.Ready)),
        )

        assertFalse(state.canRestartRuntime)
        assertNull(state.pending)
        assertEquals(PeopleMessage.RuntimeStarted, state.message?.message)
    }

    @Test
    fun cancelSuccessPostsNoMessageButClearsPending() {
        val state = reduceCancelResult(
            PeopleUiState(enrolling = true, pending = PendingPeopleAction.Cancel),
            ControlResult.Ok(Unit),
        )

        assertNull(state.pending)
        assertNull(state.message)
        // The stream's own terminal event announces the cancellation, not this acknowledgement.
        assertTrue(state.enrolling)
    }

    @Test
    fun messageIdsIncreaseSoTheUiShowsEachOne() {
        val first = reduceRefreshResult(PeopleUiState(), ControlResult.Ok(emptyList()))
        val second = reduceRefreshResult(first, ControlResult.Ok(emptyList()))

        val firstId = first.message?.id ?: 0
        val secondId = second.message?.id ?: 0
        assertTrue(secondId > firstId)
    }

    @Test
    fun controlFailureMappingCoversEveryCase() {
        assertNull(controlFailureMessage(ControlResult.Ok(Unit)))
        assertEquals(PeopleMessage.Unauthorized, controlFailureMessage(ControlResult.Unauthorized))
        assertEquals(PeopleMessage.Busy, controlFailureMessage(ControlResult.Busy))
        assertEquals(PeopleMessage.BadRequest, controlFailureMessage(ControlResult.BadRequest("x")))
        assertEquals(PeopleMessage.Internal, controlFailureMessage(ControlResult.Internal("x")))
        assertEquals(PeopleMessage.Io, controlFailureMessage(ControlResult.Io("x")))
    }
}
