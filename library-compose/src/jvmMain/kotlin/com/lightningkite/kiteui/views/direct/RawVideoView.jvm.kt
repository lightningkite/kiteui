package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Blob
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.loadResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.ref.Cleaner
import java.util.WeakHashMap

// The player plays URLs and files, so in-memory videos and classpath resources are copied to temporary files,
// once per resource or blob however many views show it.
// Raw videos get ".mp4", as on Android; the extension is how some backends pick a container format.
internal actual suspend fun VideoSource.toPlayerUri(): String = when (this) {
    is VideoRemote -> url
    is VideoLocal -> file.file.toPath().toUri().toString()
    is VideoRaw -> withContext(Dispatchers.IO) {
        synchronized(blobFiles) {
            blobFiles.getOrPut(data) {
                tempFile(data.data, ".mp4").also { file -> cleaner.register(data) { file.delete() } }
            }
        }.toPath().toUri().toString()
    }
    is VideoResource -> withContext(Dispatchers.IO) {
        synchronized(resourceFiles) {
            resourceFiles.getOrPut(relativeUrl) { tempFile(loadResource(relativeUrl), "." + relativeUrl.substringAfterLast('.')) }
        }.toPath().toUri().toString()
    }
}

// Resources are a fixed set, so their files live until exit.
private val resourceFiles = HashMap<String, File>()

// A blob's file is deleted once the blob is collected, since by then no view can be showing it.
private val blobFiles = WeakHashMap<Blob, File>()
private val cleaner = Cleaner.create()

private fun tempFile(bytes: ByteArray, extension: String): File = File.createTempFile("kiteui-video-", extension).apply {
    deleteOnExit()
    writeBytes(bytes)
}
