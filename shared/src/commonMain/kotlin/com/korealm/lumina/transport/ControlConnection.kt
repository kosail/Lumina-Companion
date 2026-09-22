package com.korealm.lumina.transport

/**
 * Minimal TCP seam used by [KtorControlClient].
 *
 * Responsibility: expose "write one line / read one line" so the request/reply logic can be unit
 * tested against a scripted fake instead of a real server (FE-INV-050). Framing is newline-delimited
 * JSON per `API_CONTRACT.md` §3.2: [writeLine] appends the `\n`, [readLine] strips it.
 *
 * Threading: both are `suspend`; a connection is single-reader/single-writer and is used for one
 * request at a time (the device processes requests sequentially).
 */
interface ControlConnection {

    /** Writes one JSON object followed by a single `\n`. */
    suspend fun writeLine(line: String)

    /** Reads one `\n`-terminated line, or `null` on a clean EOF. */
    suspend fun readLine(): String?

    /** Closes the connection. Idempotent. */
    fun close()
}

/** Opens a [ControlConnection] to the endpoint's TCP control port. */
fun interface ControlConnectionFactory {

    /** Connects to `endpoint.host:endpoint.tcpPort`. */
    suspend fun open(endpoint: Endpoint): ControlConnection
}
