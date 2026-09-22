package com.korealm.lumina.protocol

/**
 * Maps an `error.code` to the matching [ControlResult] failure value.
 *
 * Responsibility: keep the "what do I do about this error" decision in one place. The contract is
 * explicit that clients must use `code` and not the human `message` (contract §4.9), so this mapper
 * only switches on [ErrorCode].
 *
 * Recovery hints (contract §9):
 * - [ErrorCode.Unauthorized] -> the token is wrong; the device closed the connection. Prompt for a
 *   new token and reconnect.
 * - [ErrorCode.Busy] -> another enrollment is running; wait for `enroll.done`/`enroll.error`.
 * - [ErrorCode.BadRequest] -> the request itself is wrong; do not retry unchanged.
 * - [ErrorCode.Internal] / [ErrorCode.Unknown] -> a device-side failure; surface it and allow retry.
 *
 * @param message the device's message, passed through for [ControlResult.BadRequest] /
 *   [ControlResult.Internal]. It is only ever shown to the user, never parsed.
 */
fun ErrorCode.toControlResult(message: String): ControlResult<Nothing> = when (this) {
    ErrorCode.Unauthorized -> ControlResult.Unauthorized
    ErrorCode.Busy -> ControlResult.Busy
    ErrorCode.BadRequest -> ControlResult.BadRequest(message)
    ErrorCode.Internal -> ControlResult.Internal(message)
    // An unknown code from a newer device: treat as a generic, retryable device failure.
    ErrorCode.Unknown -> ControlResult.Internal(message)
}

/** Convenience: map an `error` [Reply] straight to a failure [ControlResult]. */
fun Reply.Error.toControlResult(): ControlResult<Nothing> = code.toControlResult(message)
