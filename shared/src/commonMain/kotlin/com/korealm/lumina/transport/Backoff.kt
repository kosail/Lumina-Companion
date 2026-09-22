package com.korealm.lumina.transport

/**
 * Reconnect backoff schedule (`API_CONTRACT.md` §3.3).
 *
 * Responsibility: a pure function for "how long to wait before the next attempt", so the retry
 * policy is testable and has no hidden state. The schedule is 1 s, 2 s, 5 s, then 10 s for every
 * further attempt (capped).
 */
private val BACKOFF_STEPS_MS: LongArray = longArrayOf(1_000L, 2_000L, 5_000L, 10_000L)

/**
 * Returns the delay before retry number [attempt].
 *
 * @param attempt 1-based consecutive failure count (1 = first failure).
 * @return `0` for a non-positive attempt, otherwise `1000/2000/5000/10000` capped at `10000`.
 */
fun backoffDelayMs(attempt: Int): Long {
    if (attempt <= 0) return 0L
    val index = (attempt - 1).coerceAtMost(BACKOFF_STEPS_MS.lastIndex)
    return BACKOFF_STEPS_MS[index]
}
