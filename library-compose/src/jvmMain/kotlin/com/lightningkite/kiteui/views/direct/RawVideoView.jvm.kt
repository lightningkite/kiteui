package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.loadResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// The player plays URLs and files, so in-memory videos and classpath resources go through temporary files.
// Raw videos get ".mp4", as on Android; the extension is how some backends pick a container format.
internal actual suspend fun VideoSource.toPlayerUri(): String = when (this) {
    is VideoRemote -> url
    is VideoLocal -> file.file.toPath().toUri().toString()
    is VideoRaw -> withContext(Dispatchers.IO) { tempFile(data.data, ".mp4") }
    is VideoResource -> withContext(Dispatchers.IO) { tempFile(loadResource(relativeUrl), "." + relativeUrl.substringAfterLast('.')) }
}

private fun tempFile(bytes: ByteArray, extension: String): String = File.createTempFile("kiteui-video-", extension).apply {
    deleteOnExit()
    writeBytes(bytes)
}.toPath().toUri().toString()
