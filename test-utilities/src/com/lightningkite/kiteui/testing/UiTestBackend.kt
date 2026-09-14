package com.lightningkite.kiteui.testing

/**
 * Abstraction for sending driver commands and receiving text responses.
 * Implementations may be local (in-process) or remote (via HTTP to a running daemon).
 */
interface UiTestBackend {
    suspend fun command(command: String): String
}
