package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.toBlob
import com.lightningkite.kiteui.views.direct.toPlayerUri
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

// The player plays files, so in-memory and classpath videos are copied to temporary files.
class VideoFilesTest {
    private fun VideoSource.file(): File = File(URI(runBlocking { toPlayerUri() }))

    @Test
    fun eachResourceIsCopiedOnce() {
        val first = VideoResource("common/video/test-pattern.mp4").file()
        assertEquals(first, VideoResource("common/video/test-pattern.mp4").file())
        assertTrue(first.name.endsWith(".mp4"))
    }

    @Test
    fun eachBlobIsCopiedOnceAndDeletedWhenCollected() {
        val bytes = ByteArray(1000) { it.toByte() }
        // Built in a function so that nothing on this stack keeps the blob alive.
        fun copy(): File {
            val blob = bytes.toBlob("video/mp4")
            val first = VideoRaw(blob).file()
            assertEquals(first, VideoRaw(blob).file(), "a blob shown twice is copied once")
            assertNotEquals(first, VideoRaw(bytes.copyOf().toBlob("video/mp4")).file(), "another blob gets its own file")
            return first
        }
        val file = copy()
        assertTrue(file.readBytes().contentEquals(bytes))
        val deadline = System.currentTimeMillis() + 10_000
        while (file.exists() && System.currentTimeMillis() < deadline) {
            System.gc()
            Thread.sleep(50)
        }
        assertTrue(!file.exists(), "a collected blob's file is deleted")
    }
}
