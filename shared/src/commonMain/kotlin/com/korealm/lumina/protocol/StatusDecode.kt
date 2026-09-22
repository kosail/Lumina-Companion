package com.korealm.lumina.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/**
 * The single protocol version this client implements (`API_CONTRACT.md` §10). A breaking change
 * bumps the device's `proto`; we must not silently interpret a frame we do not understand.
 */
const val SUPPORTED_PROTO: Int = 1

/** Result of decoding one UDP telemetry datagram. */
sealed interface StatusDecodeResult {

    /** A valid `status` frame at [SUPPORTED_PROTO], already interpreted. */
    data class Decoded(val status: DeviceStatus) : StatusDecodeResult

    /** The datagram was not a well-formed `status` frame; [message] says why. */
    data class Malformed(val message: String) : StatusDecodeResult

    /**
     * The frame is a `status` but declares a protocol version this client does not support. The
     * caller should treat the device as incompatible rather than showing possibly-wrong values.
     */
    data class UnsupportedProto(val proto: Int) : StatusDecodeResult
}

/**
 * Decodes and validates one telemetry datagram (`API_CONTRACT.md` §3.1, §4.1, §10).
 *
 * Why this exists (instead of decoding [WireStatus] directly): the raw `WireStatus` model has
 * defaults for `t`/`proto`, so on its own it would happily accept a non-`status` object or a future
 * protocol version. This function is the single entry point that enforces:
 * 1. the text is a JSON object,
 * 2. `t == "status"`,
 * 3. `proto` is present, an integer, and equal to [SUPPORTED_PROTO],
 * before mapping to the domain type.
 *
 * Malformed input and version mismatches are returned as values, never thrown.
 *
 * @param line one JSON object (the datagram payload, without framing).
 * @param receivedAtMs local arrival time to stamp on the result (the device `ts` may jump).
 */
fun decodeStatus(line: String, receivedAtMs: Long): StatusDecodeResult {
    val obj = try {
        LuminaJson.parseToJsonElement(line).jsonObject
    } catch (e: SerializationException) {
        return StatusDecodeResult.Malformed("not valid JSON: ${e.message}")
    } catch (e: IllegalArgumentException) {
        return StatusDecodeResult.Malformed("not a JSON object: ${e.message}")
    }

    val type = (obj["t"] as? JsonPrimitive)?.contentOrNull
    if (type != "status") {
        return StatusDecodeResult.Malformed("not a status frame (t=${type ?: "<missing>"})")
    }

    // `proto` is required by the contract; a missing/non-integer value is treated as malformed
    // rather than defaulted, so a version gate cannot be bypassed by omitting the field.
    val proto = (obj["proto"] as? JsonPrimitive)?.intOrNull
        ?: return StatusDecodeResult.Malformed("missing or non-integer 'proto'")
    if (proto != SUPPORTED_PROTO) {
        return StatusDecodeResult.UnsupportedProto(proto)
    }

    val wire = try {
        LuminaJson.decodeFromJsonElement(WireStatus.serializer(), obj)
    } catch (e: SerializationException) {
        return StatusDecodeResult.Malformed("bad 'status' payload: ${e.message}")
    }
    return StatusDecodeResult.Decoded(wire.toDomain(receivedAtMs))
}
