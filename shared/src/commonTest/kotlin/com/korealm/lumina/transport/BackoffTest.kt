package com.korealm.lumina.transport

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure tests for the reconnect backoff schedule (contract §3.3). */
class BackoffTest {

    @Test
    fun followsTheContractScheduleAndCapsAtTenSeconds() {
        assertEquals(1_000L, backoffDelayMs(1))
        assertEquals(2_000L, backoffDelayMs(2))
        assertEquals(5_000L, backoffDelayMs(3))
        assertEquals(10_000L, backoffDelayMs(4))
        assertEquals(10_000L, backoffDelayMs(5))
        assertEquals(10_000L, backoffDelayMs(100))
    }

    @Test
    fun returnsZeroForNonPositiveAttempts() {
        assertEquals(0L, backoffDelayMs(0))
        assertEquals(0L, backoffDelayMs(-3))
    }
}
