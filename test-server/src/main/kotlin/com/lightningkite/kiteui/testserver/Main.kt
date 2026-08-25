package com.lightningkite.kiteui.testserver

/**
 * Runs the echo server for a Gradle build.
 *
 * Two servers on adjacent ports: the first sends CORS headers, the second deliberately does not, so
 * that a browser test can exercise the "the server answered but the browser refused" path that
 * [com.lightningkite.kiteui.RequestBlockedException] reports.
 *
 * Prints `LISTENING <port>` once both are accepting connections; the build waits for that line
 * rather than polling, so no test starts before the server can answer it.
 *
 * Exits when stdin reaches end of file. The parent closes the pipe whether it shuts down cleanly or
 * is killed, so a crashed build cannot leave a server holding the port.
 */
public fun main(args: Array<String>) {
    val port = args[0].toInt()
    val servers = listOf(
        startEchoServer(port, cors = true),
        startEchoServer(port + 1, cors = false),
    )
    println("LISTENING $port")
    System.out.flush()
    @Suppress("ControlFlowWithEmptyBody")
    while (System.`in`.read() != -1) {}
    servers.forEach { it.stop(0, 0) }
}
