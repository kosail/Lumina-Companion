package com.korealm.lumina.transport

import kotlin.time.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Injected time sources for the telemetry loop.
 *
 * Responsibility: make the transport's timing deterministic in tests. The liveness reducer and the
 * re-subscribe cadence depend on "now" and on a periodic tick; both are injected rather than read
 * from the environment (AGENTS §5, FE-INV-052). Production uses [MonotonicClock]/[DelayTicker];
 * tests pass fakes and advance time by hand.
 */

/** Monotonic wall-clock in milliseconds. Monotonic so a device `ts` jump never affects liveness. */
fun interface Clock {
    /** Current monotonic time in milliseconds. */
    fun nowMs(): Long
}

/** A source of periodic ticks used for liveness checks and re-subscription. */
fun interface Ticker {
    /** Emits [Unit] every [periodMs] until cancelled. */
    fun ticks(periodMs: Long): Flow<Unit>
}

/**
 * Default [Clock] backed by `TimeSource.Monotonic`.
 *
 * Kotlin note: `TimeSource.Monotonic` is a common (multiplatform) monotonic clock, so this works on
 * both Android and the JVM without platform code. `markNow()` captures the origin at construction.
 */
object MonotonicClock : Clock {
    private val origin = TimeSource.Monotonic.markNow()
    override fun nowMs(): Long = origin.elapsedNow().inWholeMilliseconds
}

/** Default [Ticker] backed by `delay`, cancellable via the collecting coroutine. */
object DelayTicker : Ticker {
    override fun ticks(periodMs: Long): Flow<Unit> = flow {
        while (true) {
            delay(periodMs)
            emit(Unit)
        }
    }
}
