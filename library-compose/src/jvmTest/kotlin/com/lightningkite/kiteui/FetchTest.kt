package com.lightningkite.kiteui

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.net.InetSocketAddress
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertTrue

class FetchTest {
    // As on the web, progress arrives on the main thread, where it can safely set the signals a view reads.
    @Test
    fun progressIsReportedOnTheMainThread() = runBlocking(Dispatchers.Main) {
        val body = ByteArray(1_000_000)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/echo") {
            it.requestBody.readBytes()
            it.sendResponseHeaders(200, body.size.toLong())
            it.responseBody.use { out -> out.write(body) }
        }
        server.start()
        try {
            val threads = mutableListOf<Boolean>()
            fetch(
                url = "http://127.0.0.1:${server.address.port}/echo",
                method = HttpMethod.PUT,
                body = RequestBodyBlob(Blob(body, "application/octet-stream")),
                onUploadProgress = { _, _ -> threads += SwingUtilities.isEventDispatchThread() },
                onDownloadProgress = { _, _ -> threads += SwingUtilities.isEventDispatchThread() },
            ).blob()
            assertTrue(threads.isNotEmpty(), "no progress was reported")
            assertTrue(threads.all { it }, "${threads.count { !it }} of ${threads.size} progress reports were off the main thread")
        } finally {
            server.stop(0)
        }
    }
}
