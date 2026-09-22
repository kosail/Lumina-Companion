package com.korealm.lumina.protocol

/**
 * The events an enrollment produces, already interpreted.
 *
 * Responsibility: give the UI a single, exhaustive type to render for both enrollment routes
 * (camera and photos). A camera enrollment streams [Progress] zero or more times and then ends with
 * exactly one terminal event: [Done], [Error], [Cancelled], or [Failure] (contract §4.5–§4.8,
 * §6.2–§6.3).
 *
 * Kotlin note: a `sealed interface` is like a Java sealed interface — the permitted implementations
 * are known at compile time, so a `when` over it is exhaustive without an `else`.
 */
sealed interface EnrollEvent {

    /**
     * Progress while an enrollment runs.
     *
     * @param phase one of the three contract phases.
     * @param captured frames captured so far; non-null only during [EnrollPhase.Capturing].
     * @param total frame target; non-null only during [EnrollPhase.Capturing].
     * @param message a transient notice (e.g. `"no face detected"`), or `null`.
     */
    data class Progress(
        val phase: EnrollPhase,
        val captured: Int?,
        val total: Int?,
        val message: String?,
    ) : EnrollEvent

    /**
     * Terminal success.
     *
     * @param personCount may lag by ~1 s; refresh the people list afterwards (contract §4.6).
     * @param embeddingsAdded how many embeddings were stored.
     */
    data class Done(
        val personCount: Int,
        val embeddingsAdded: Int,
    ) : EnrollEvent

    /**
     * Terminal failure.
     *
     * @param exitCode `-1` when cancelled/killed, `1`/`2` for tool errors.
     * @param message device-provided message (shown to the user).
     * @param runtime the authoritative post-enrollment runtime state; when it is not
     *   [RuntimeAfterEnroll.Started] the UI must offer "Start Lúmina" (contract §6.5).
     */
    data class Error(
        val exitCode: Int,
        val message: String,
        val runtime: RuntimeAfterEnroll,
    ) : EnrollEvent

    /** The enrollment was cancelled. */
    data object Cancelled : EnrollEvent

    /**
     * The enrollment command was rejected, or the connection failed, before or instead of an
     * `enroll.*` stream.
     *
     * Why this exists: the device answers an enrollment request with a generic `error` line
     * (`busy`/`unauthorized`/`bad_request`/`internal`, contract §4.9/§5.8) or the socket fails
     * locally. [Error] only models a real `enroll.error`, so those outcomes need their own carrier.
     * This is a **client-side** interpretation, not a wire message: the [EnrollmentFailure] kind
     * lets the UI show Spanish copy (FE-INV-041) instead of the device's English text.
     */
    data class Failure(val failure: EnrollmentFailure) : EnrollEvent
}

/**
 * Why an enrollment ended without producing a stream.
 *
 * Mirrors the transport failure kinds ([ControlResult]) plus one client-only case, so an enrollment
 * failure can be rendered exactly like a control failure. It carries no device text (FE-INV-041).
 */
enum class EnrollmentFailure {
    /** The token was missing or wrong (contract §4.9). */
    Unauthorized,

    /** Another enrollment is already running (contract §4.9). */
    Busy,

    /** The request was malformed/out of range (contract §4.9). */
    BadRequest,

    /** A device-side failure (contract §4.9). */
    Internal,

    /** A local socket/timeout/parse failure. */
    Io,

    /** A picked image could not be decoded/resized/encoded (FE-INV-033). */
    ImagePreparation,
}
