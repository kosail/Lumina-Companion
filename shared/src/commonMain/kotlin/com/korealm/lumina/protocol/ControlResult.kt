package com.korealm.lumina.protocol

/**
 * The outcome of a control command.
 *
 * Responsibility: represent expected failures as **values**, never as thrown exceptions, so the UI
 * can render a Spanish message without a crash and nothing crosses a coroutine boundary as a
 * `Throwable` (AGENTS §7, FE-INV-052).
 *
 * Kotlin note: `out T` is a declaration-site variance annotation ("covariant"), like Java's
 * `? extends T`. It lets `ControlResult<VolumeState>` be used where `ControlResult<Any>` is expected,
 * and lets the error cases implement `ControlResult<Nothing>` (`Nothing` is the bottom type).
 *
 * @param T the success payload type for the command (e.g. [VolumeState]).
 */
sealed interface ControlResult<out T> {

    /** The command succeeded; [value] is the interpreted reply. */
    data class Ok<T>(val value: T) : ControlResult<T>

    /** The token was missing or wrong; the device closes the connection (contract §4.9). */
    data object Unauthorized : ControlResult<Nothing>

    /** Another enrollment is already running; wait for it to finish (contract §4.9). */
    data object Busy : ControlResult<Nothing>

    /** The request was malformed/out of range; the connection stays open (contract §4.9). */
    data class BadRequest(val message: String) : ControlResult<Nothing>

    /** A device-side failure (e.g. temp dir); surface it and allow a retry (contract §4.9). */
    data class Internal(val message: String) : ControlResult<Nothing>

    /**
     * A local I/O failure (socket connect/read/write, timeout, EOF, malformed JSON). This is the only
     * "transport" failure the protocol layer models; the transport supplies [message].
     */
    data class Io(val message: String) : ControlResult<Nothing>
}

/** The contract's `error.code` values (`API_CONTRACT.md` §4.9, §12.1). */
enum class ErrorCode {
    Unauthorized,
    BadRequest,
    Busy,
    Internal,

    /** An unrecognized code from a newer device; treated as a generic failure. */
    Unknown;

    companion object {
        /**
         * Maps a wire `code` string.
         *
         * Deliberately does **not** string-match the `message` (the contract forbids it); only the
         * `code` is meaningful.
         */
        fun fromWire(raw: String): ErrorCode = when (raw) {
            "unauthorized" -> Unauthorized
            "bad_request" -> BadRequest
            "busy" -> Busy
            "internal" -> Internal
            else -> Unknown
        }
    }
}
