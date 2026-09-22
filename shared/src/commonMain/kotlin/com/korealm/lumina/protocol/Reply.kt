package com.korealm.lumina.protocol

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

/**
 * A single decoded control reply, already interpreted.
 *
 * Responsibility: normalize the many `t`-discriminated reply shapes from `API_CONTRACT.md`
 * §4.2–§4.9 into one exhaustive type the transport/repository can switch over.
 */
sealed interface Reply {

    /** A `volume.state` reply. */
    data class Volume(val state: VolumeState) : Reply

    /** A `people` reply. */
    data class People(val names: List<String>) : Reply

    /** A `runtime.state` reply. */
    data class Runtime(val state: RuntimeState) : Reply

    /** Any enrollment event (`enroll.progress`/`done`/`error`/`cancelled`). */
    data class Enroll(val event: EnrollEvent) : Reply

    /** An `error` reply; [code] is the meaningful field, not [message] (contract §4.9). */
    data class Error(val code: ErrorCode, val message: String) : Reply

    /** A reply whose `t` this client does not know (forward compatibility, contract §10). */
    data class Unknown(val type: String) : Reply
}

/** Result of decoding one line: either a [Reply] or a reason the line was malformed. */
sealed interface ReplyDecodeResult {
    data class Decoded(val reply: Reply) : ReplyDecodeResult
    data class Malformed(val message: String) : ReplyDecodeResult
}

/**
 * Decodes one newline-delimited control reply.
 *
 * Design: the contract has **no request `id`** and replies are correlated by order, so we dispatch
 * purely on the `t` discriminator. An unknown `t` is not an error — it becomes [Reply.Unknown] so a
 * newer device cannot break the client (contract §10). Malformed JSON is returned as a value, never
 * thrown.
 *
 * @param line one JSON object without the trailing `\n`.
 * @return [ReplyDecodeResult.Decoded] or [ReplyDecodeResult.Malformed].
 */
fun decodeReply(line: String): ReplyDecodeResult {
    // `jsonObject` throws if the text is not a JSON object; `parseToJsonElement` throws on bad JSON.
    val obj = try {
        LuminaJson.parseToJsonElement(line).jsonObject
    } catch (e: SerializationException) {
        return ReplyDecodeResult.Malformed("not valid JSON: ${e.message}")
    } catch (e: IllegalArgumentException) {
        return ReplyDecodeResult.Malformed("not a JSON object: ${e.message}")
    }

    val type = (obj["t"] as? JsonPrimitive)?.contentOrNull
        ?: return ReplyDecodeResult.Malformed("missing 't' discriminator")

    return try {
        ReplyDecodeResult.Decoded(decodeKnown(type, obj))
    } catch (e: SerializationException) {
        ReplyDecodeResult.Malformed("bad '$type' payload: ${e.message}")
    }
}

/**
 * Decodes a reply whose `t` is known. Throws [SerializationException] if the payload does not match
 * the expected schema; the caller converts that to [ReplyDecodeResult.Malformed].
 */
private fun decodeKnown(type: String, obj: JsonObject): Reply = when (type) {
    "volume.state" -> {
        val wire = LuminaJson.decodeFromJsonElement(WireVolumeState.serializer(), obj)
        Reply.Volume(
            VolumeState(
                // Sentinel `-1` means "unknown", not zero (FE-INV-031).
                percent = wire.value.takeIf { it >= 0 },
                muted = wire.muted,
                ok = wire.ok,
            ),
        )
    }

    "people" -> {
        val wire = LuminaJson.decodeFromJsonElement(WirePeople.serializer(), obj)
        Reply.People(wire.names)
    }

    "runtime.state" -> {
        val wire = LuminaJson.decodeFromJsonElement(WireRuntimeState.serializer(), obj)
        Reply.Runtime(RuntimeState(running = wire.running, sink = SinkState.fromWire(wire.sink)))
    }

    "enroll.progress" -> {
        val wire = LuminaJson.decodeFromJsonElement(WireEnrollProgress.serializer(), obj)
        Reply.Enroll(
            EnrollEvent.Progress(
                phase = EnrollPhase.fromWire(wire.phase),
                // Contract minimums (captured >= 0, total >= 1); normalize so the progress UI can
                // never divide by zero or show a negative count (same rule as StatusMapper).
                captured = wire.captured?.takeIf { it >= 0 },
                total = wire.total?.takeIf { it > 0 },
                message = wire.message,
            ),
        )
    }

    "enroll.done" -> {
        val wire = LuminaJson.decodeFromJsonElement(WireEnrollDone.serializer(), obj)
        Reply.Enroll(EnrollEvent.Done(wire.personCount, wire.embeddingsAdded))
    }

    "enroll.error" -> {
        val wire = LuminaJson.decodeFromJsonElement(WireEnrollError.serializer(), obj)
        Reply.Enroll(
            EnrollEvent.Error(
                exitCode = wire.exitCode,
                message = wire.message,
                runtime = RuntimeAfterEnroll.fromWire(wire.runtime),
            ),
        )
    }

    "enroll.cancelled" -> Reply.Enroll(EnrollEvent.Cancelled)

    "error" -> {
        val wire = LuminaJson.decodeFromJsonElement(WireError.serializer(), obj)
        Reply.Error(code = ErrorCode.fromWire(wire.code), message = wire.message)
    }

    else -> Reply.Unknown(type)
}
