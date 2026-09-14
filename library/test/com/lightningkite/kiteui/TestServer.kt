package com.lightningkite.kiteui

/**
 * The local echo server that every `:library` test task starts, described from the tests' side.
 *
 * The server is the `:test-server` module; `library/build.gradle.kts` launches one process per build
 * and waits for it to bind before any test runs, so a test may assume it is answering. Its port is
 * generated into [TEST_SERVER_PORT] so the build and the tests cannot disagree about it.
 *
 * Everything is plain `http`/`ws` on loopback. That is the point: these tests are about KiteUI's own
 * request handling, and routing them through a public host over TLS makes them fail for reasons that
 * belong to a certificate store or an office network instead.
 */
internal object TestServer {
    val http: String = "http://127.0.0.1:$TEST_SERVER_PORT"
    val ws: String = "ws://127.0.0.1:$TEST_SERVER_PORT"

    /**
     * The same endpoints served without CORS headers, for the one failure a browser reports
     * differently from everyone else. Only the JS tests have any use for it.
     */
    val httpWithoutCors: String = "http://127.0.0.1:${TEST_SERVER_PORT + 1}"

    /**
     * An address on this machine that nothing is listening on, so connecting to it is refused
     * immediately rather than timing out. Deliberately not a low port: browsers refuse a list of
     * those outright, which is a different failure from the one under test.
     */
    val refused: String = "http://127.0.0.1:$refusedPort/hello"
    val refusedWs: String = "ws://127.0.0.1:$refusedPort/ws/echo"

    private const val refusedPort: Int = TEST_SERVER_PORT + 2

    // Mirrors of what the server answers. Duplicated rather than shared because the server is a JVM
    // module and these tests compile for four targets; keep them in step with :test-server.
    const val helloBody: String = "hello, kiteui"
    const val echoMethodHeader: String = "X-Echo-Method"
    const val retryAfterSeconds: Int = 7
    const val sessionPrefix: String = "session "
    const val dropCommand: String = "drop"

    /** Matches the server's `/bytes/{count}` body. */
    fun deterministicBytes(count: Int): ByteArray = ByteArray(count) { (it % 251).toByte() }
}
