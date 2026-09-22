package com.korealm.lumina.ui.people

import com.korealm.lumina.protocol.ControlResult
import com.korealm.lumina.protocol.EnrollmentFailure

/**
 * A user-facing outcome of a people-screen action, as a resource-agnostic key.
 *
 * Responsibility: let the view model report "what happened" without hardcoding Spanish text, so the
 * composable maps it with `stringResource` (FE-INV-041). Device-provided English messages are never
 * surfaced; only these fixed keys are (Phase 3 rule, extended to enrollment).
 */
enum class PeopleMessage {
    /** `people.list` succeeded. */
    Refreshed,

    /** `enroll.done` arrived; a person was registered. */
    PersonEnrolled,

    /** `enroll.error` with a real failure (not a cancellation). */
    EnrollFailed,

    /** The enrollment was cancelled (device `exitCode == -1` or an `enroll.cancelled`). */
    EnrollCancelled,

    /** The name field was empty when enrollment was attempted. */
    NeedName,

    /** More than [com.korealm.lumina.ui.picker.MAX_ENROLL_PHOTOS] images were picked. */
    TooManyPhotos,

    /** One of the picked images could not be decoded/resized/encoded. */
    PhotosNotPrepared,

    /** The token was missing or wrong (contract §4.9). */
    Unauthorized,

    /** An enrollment is already running (contract §4.9). */
    Busy,

    /** The request was rejected (contract §4.9). */
    BadRequest,

    /** A device-side failure (contract §4.9). */
    Internal,

    /** A local socket/timeout failure. */
    Io,

    /** `runtime.start` succeeded (the recovery action after a failed enrollment). */
    RuntimeStarted,
}

/**
 * One message to show once, identified by a monotonically increasing [id].
 *
 * Kotlin note: the [id] lets the shell key its `LaunchedEffect` so the same message can be shown
 * again later (the message is UI state, not an event stream; FE-INV-051).
 */
data class PeopleUiMessage(val id: Long, val message: PeopleMessage)

/**
 * Maps a control outcome to its failure message, or `null` when the command succeeded.
 * Pure and top-level so it is unit-tested without coroutines (AGENTS §5).
 */
internal fun controlFailureMessage(result: ControlResult<*>): PeopleMessage? = when (result) {
    is ControlResult.Ok<*> -> null
    ControlResult.Unauthorized -> PeopleMessage.Unauthorized
    ControlResult.Busy -> PeopleMessage.Busy
    is ControlResult.BadRequest -> PeopleMessage.BadRequest
    is ControlResult.Internal -> PeopleMessage.Internal
    is ControlResult.Io -> PeopleMessage.Io
}

/**
 * Maps an enrollment failure kind to its message.
 *
 * `Busy`/`Unauthorized`/`BadRequest`/`Internal` reuse the dashboard's wording; the two client-only
 * cases ([EnrollmentFailure.Io], [EnrollmentFailure.ImagePreparation]) have their own.
 */
internal fun enrollmentFailureMessage(failure: EnrollmentFailure): PeopleMessage = when (failure) {
    EnrollmentFailure.Unauthorized -> PeopleMessage.Unauthorized
    EnrollmentFailure.Busy -> PeopleMessage.Busy
    EnrollmentFailure.BadRequest -> PeopleMessage.BadRequest
    EnrollmentFailure.Internal -> PeopleMessage.Internal
    EnrollmentFailure.Io -> PeopleMessage.Io
    EnrollmentFailure.ImagePreparation -> PeopleMessage.PhotosNotPrepared
}
