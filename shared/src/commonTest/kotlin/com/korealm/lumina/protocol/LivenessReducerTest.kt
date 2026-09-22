package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [reduceLiveness] — the 5-second offline rule (FE-INV-031, contract §2/§9).
 *
 * The boundary is inclusive: `>= 5000 ms` is offline.
 */
class LivenessReducerTest {

    @Test
    fun noStatusEverMeansOffline() {
        assertEquals(Liveness.Offline, reduceLiveness(lastStatusArrivalMs = null, nowMs = 1_000L))
    }

    @Test
    fun justArrivedMeansOnline() {
        assertEquals(Liveness.Online, reduceLiveness(lastStatusArrivalMs = 1_000L, nowMs = 1_000L))
    }

    @Test
    fun staysOnlineJustBeforeTheBoundary() {
        assertEquals(Liveness.Online, reduceLiveness(lastStatusArrivalMs = 0L, nowMs = 4_999L))
    }

    @Test
    fun goesOfflineExactlyAtTheBoundary() {
        assertEquals(Liveness.Offline, reduceLiveness(lastStatusArrivalMs = 0L, nowMs = 5_000L))
    }

    @Test
    fun staysOfflineWellPastTheBoundary() {
        assertEquals(Liveness.Offline, reduceLiveness(lastStatusArrivalMs = 0L, nowMs = 60_000L))
    }

    @Test
    fun aBackwardsClockReadsAsOnline() {
        // Guards against a non-monotonic clock producing a huge elapsed time.
        assertEquals(Liveness.Online, reduceLiveness(lastStatusArrivalMs = 10_000L, nowMs = 9_000L))
    }

    @Test
    fun honoursACustomTimeout() {
        assertEquals(Liveness.Offline, reduceLiveness(lastStatusArrivalMs = 0L, nowMs = 2_000L, timeoutMs = 2_000L))
        assertEquals(Liveness.Online, reduceLiveness(lastStatusArrivalMs = 0L, nowMs = 1_999L, timeoutMs = 2_000L))
    }
}
