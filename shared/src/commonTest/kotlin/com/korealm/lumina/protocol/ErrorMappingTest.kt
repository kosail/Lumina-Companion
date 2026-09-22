package com.korealm.lumina.protocol

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for [ErrorCode.toControlResult] and [ErrorCode.fromWire] (`API_CONTRACT.md` §4.9).
 *
 * The mapping must use the `code` only, and an unknown code must degrade to a retryable failure
 * rather than crash.
 */
class ErrorMappingTest {

    @Test
    fun mapsEachKnownCode() {
        assertIs<ControlResult.Unauthorized>(ErrorCode.Unauthorized.toControlResult("bad or missing token"))
        assertIs<ControlResult.Busy>(ErrorCode.Busy.toControlResult("an enrollment is already running"))

        val bad = assertIs<ControlResult.BadRequest>(ErrorCode.BadRequest.toControlResult("volume must be 0..100"))
        assertEquals("volume must be 0..100", bad.message)

        val internalResult = assertIs<ControlResult.Internal>(ErrorCode.Internal.toControlResult("cannot create temp dir"))
        assertEquals("cannot create temp dir", internalResult.message)
    }

    @Test
    fun mapsUnknownCodeToInternal() {
        val result = ErrorCode.Unknown.toControlResult("mystery")
        assertIs<ControlResult.Internal>(result)
        assertEquals("mystery", result.message)
    }

    @Test
    fun parsesWireCodes() {
        assertEquals(ErrorCode.Unauthorized, ErrorCode.fromWire("unauthorized"))
        assertEquals(ErrorCode.BadRequest, ErrorCode.fromWire("bad_request"))
        assertEquals(ErrorCode.Busy, ErrorCode.fromWire("busy"))
        assertEquals(ErrorCode.Internal, ErrorCode.fromWire("internal"))
        assertEquals(ErrorCode.Unknown, ErrorCode.fromWire("something_new"))
    }

    @Test
    fun mapsAnErrorReply() {
        val reply = Reply.Error(ErrorCode.Busy, "an enrollment is already running")
        assertIs<ControlResult.Busy>(reply.toControlResult())
    }
}
