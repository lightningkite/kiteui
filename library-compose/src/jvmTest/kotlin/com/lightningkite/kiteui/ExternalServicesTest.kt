package com.lightningkite.kiteui

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetSocketAddress
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalServicesTest {
    private val home = Files.createTempDirectory("kiteui-home").toFile()
    private val services = DesktopExternalServices(home)

    @Test
    fun downloadsGoToTheChosenFolderWithoutOverwriting() = runBlocking {
        services.download("report.pdf", Blob("first".toByteArray(), "application/pdf"), DownloadLocation.Downloads)
        services.download("report.pdf", Blob("second".toByteArray(), "application/pdf"), DownloadLocation.Downloads)
        services.download("report.pdf", Blob("third".toByteArray(), "application/pdf"), DownloadLocation.Downloads)
        services.download("notes", Blob("a".toByteArray(), "text/plain"), DownloadLocation.Downloads)
        services.download("notes", Blob("b".toByteArray(), "text/plain"), DownloadLocation.Downloads)
        services.download("photo.png", Blob("png".toByteArray(), "image/png"), DownloadLocation.Pictures)

        val downloads = File(home, "Downloads")
        assertEquals("first", File(downloads, "report.pdf").readText())
        assertEquals("second", File(downloads, "report (1).pdf").readText())
        assertEquals("third", File(downloads, "report (2).pdf").readText())
        assertEquals("b", File(downloads, "notes (1)").readText())
        assertEquals("png", File(home, "Pictures/photo.png").readText())
    }

    @Test
    fun downloadNamesCantLeaveTheFolder() {
        assertFailsWith<IllegalArgumentException> {
            runBlocking { services.download("../escape.txt", Blob(ByteArray(0), "text/plain"), DownloadLocation.Downloads) }
        }
    }

    @Test
    fun downloadsFromUrls() = runBlocking {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/file.txt") {
            val body = "served".toByteArray()
            it.sendResponseHeaders(200, body.size.toLong())
            it.responseBody.use { out -> out.write(body) }
        }
        server.createContext("/missing") {
            it.sendResponseHeaders(404, -1)
            it.close()
        }
        server.start()
        try {
            val base = "http://127.0.0.1:${server.address.port}"
            services.download("file.txt", "$base/file.txt", DownloadLocation.Downloads, onDownloadProgress = null)
            assertEquals("served", File(home, "Downloads/file.txt").readText())
            assertFailsWith<IllegalStateException> {
                services.download("missing.txt", "$base/missing", DownloadLocation.Downloads, onDownloadProgress = null)
            }
            assertFalse(File(home, "Downloads/missing.txt").exists(), "a failed download leaves no file")
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun fileFilterReadsMimeTypesLikeAnHtmlAccept() {
        val png = File(home, "a.png").apply { writeBytes(byteArrayOf()) }
        val pdf = File(home, "b.PDF").apply { writeBytes(byteArrayOf()) }
        assertTrue(fileMatches(png, listOf("*/*")))
        assertTrue(fileMatches(png, listOf("image/*")))
        assertTrue(fileMatches(png, listOf("image/png")))
        assertFalse(fileMatches(png, listOf("image/jpeg", "video/*")))
        assertTrue(fileMatches(pdf, listOf(".pdf")))
        assertFalse(fileMatches(pdf, listOf("image/*")))
    }

    @Test
    fun sharingIsUnsupported() {
        assertFailsWith<UnsupportedOperationException> { services.share("Title", "Message", "https://example.com") }
        assertFailsWith<UnsupportedOperationException> { runBlocking { services.share(listOf("a.txt" to Blob(ByteArray(0), "text/plain"))) } }
    }
}
