package com.korealm.lumina.protocol

/**
 * Pure liveness reducer for telemetry.
 *
 * Responsibility: decide whether the device is [Liveness.Online] or [Liveness.Offline] given when the
 * last `status` datagram arrived and the current time. It is a plain function with an injected clock
 * so tests are deterministic (contract §2, §9).
 *
 * Rule (FE-INV-031): the device is offline once **no `status` has arrived for `>= 5000 ms`**. The
 * boundary is inclusive so the transition is deterministic and testable (4999 ms = online,
 * 5000 ms = offline).
 */

/** Client-side liveness window: no `status` for this long means offline (contract §2 `LIVENESS_TIMEOUT`). */
const val LIVENESS_TIMEOUT_MS: Long = 5_000L

/**
 * Reduces liveness from the last arrival time.
 *
 * @param lastStatusArrivalMs local arrival time of the most recent `status`, or `null` if none has
 *   ever arrived (which is [Liveness.Offline]).
 * @param nowMs current local time in the same clock domain as [lastStatusArrivalMs].
 * @param timeoutMs the offline threshold; defaults to [LIVENESS_TIMEOUT_MS].
 * @return [Liveness.Offline] when no status has arrived or the elapsed time is `>= timeoutMs`.
 */
fun reduceLiveness(
    lastStatusArrivalMs: Long?,
    nowMs: Long,
    timeoutMs: Long = LIVENESS_TIMEOUT_MS,
): Liveness {
    if (lastStatusArrivalMs == null) return Liveness.Offline
    // A non-monotonic clock (or a status "from the future") must not wrap to a huge elapsed time;
    // clamp at 0 so it reads as "just arrived".
    val elapsedMs = (nowMs - lastStatusArrivalMs).coerceAtLeast(0L)
    return if (elapsedMs >= timeoutMs) Liveness.Offline else Liveness.Online
}
