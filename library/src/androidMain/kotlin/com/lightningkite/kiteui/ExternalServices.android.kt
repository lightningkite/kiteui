package com.lightningkite.kiteui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.kiteui.views.RContext
import kotlinx.coroutines.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
import kotlin.coroutines.resume

actual fun RContext.openLink(url: String, newTab: Boolean) {
    AndroidAppContext.activityCtx?.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
}

actual suspend fun RContext.requestFile(
    mimeTypes: List<String>,
) = suspendCancellableCoroutine { cont ->

    if (mimeTypes.any { !it.startsWith("image/") || !it.startsWith("video/") }) {
        val od = ActivityResultContracts.OpenDocument()

        this@requestFile.activity.startActivityForResult(
            od.createIntent(this@requestFile.activity, mimeTypes.toTypedArray())
        ) { code, result ->
            cont.resume(od.parseResult(code, result)?.let(::FileReference))
        }
    } else {

        val pvm = ActivityResultContracts.PickVisualMedia()
        val request = when {
            mimeTypes.size == 1 -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType(mimeTypes.first()))
            mimeTypes.all { it.startsWith("image/") } -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

            mimeTypes.all { it.startsWith("video/") } -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)

            else -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        }
        this@requestFile.activity.startActivityForResult(
            pvm.createIntent(this@requestFile.activity, request)
        ) { code, result ->
            cont.resume(pvm.parseResult(code, result)?.let(::FileReference))
        }
    }
}

actual suspend fun RContext.requestFiles(
    mimeTypes: List<String>,
) = suspendCancellableCoroutine { cont ->

    if (mimeTypes.any { !it.startsWith("image/") || !it.startsWith("video/") }) {
        val od = ActivityResultContracts.OpenDocument()

        this@requestFiles.activity.startActivityForResult(
            od.createIntent(this@requestFiles.activity, mimeTypes.toTypedArray())
        ) { code, result ->
            val uri = od.parseResult(code, result) ?: run {
                cont.resume(emptyList())
                return@startActivityForResult
            }
            cont.resume(listOf(FileReference(uri)))
        }
    } else {

        val pvm = ActivityResultContracts.PickMultipleVisualMedia()
        val request = when {
            mimeTypes.size == 1 -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.SingleMimeType(mimeTypes.first()))
            mimeTypes.all { it.startsWith("image/") } -> {
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            }

            mimeTypes.all { it.startsWith("video/") } -> {
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            }

            else -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        }
        this@requestFiles.activity.startActivityForResult(
            pvm.createIntent(this@requestFiles.activity, request)
        ) { code, result ->
            cont.resume(pvm.parseResult(code, result).map(::FileReference))
        }
    }
}

actual suspend fun RContext.requestCaptureSelf(
    mimeTypes: List<String>,
): FileReference? {
    return if (mimeTypes.all { it.startsWith("image/") }) requestImageCamera(
        true,
        MediaStore.ACTION_IMAGE_CAPTURE
    )
    else if (mimeTypes.all { it.startsWith("video/") }) requestImageCamera(
        true,
        MediaStore.ACTION_VIDEO_CAPTURE
    )
    else throw Exception("Captures besides images and video not supported yet. Requested $mimeTypes")
}

actual suspend fun RContext.requestCaptureEnvironment(
    mimeTypes: List<String>,
): FileReference? {
    return if (mimeTypes.all { it.startsWith("image/") }) requestImageCamera(
        false,
        MediaStore.ACTION_IMAGE_CAPTURE
    )
    else if (mimeTypes.all { it.startsWith("video/") }) requestImageCamera(
        false,
        MediaStore.ACTION_VIDEO_CAPTURE
    )
    else throw Exception("Captures besides images and video not supported yet. Requested $mimeTypes")
}

private suspend fun requestImageCamera(
    front: Boolean = false,
    capture: String = MediaStore.ACTION_IMAGE_CAPTURE,
): FileReference? = suspendCancellableCoroutine { cont ->
    val file = File(AndroidAppContext.applicationCtx.cacheDir, "images").also { it.mkdirs() }
        .let { File.createTempFile("image", ".jpg", it) }
        .let {
            FileProvider.getUriForFile(
                AndroidAppContext.applicationCtx,
                AndroidAppContext.fileProviderAuthority,
                it
            )
        }

    AndroidAppContext.requestPermissions(android.Manifest.permission.CAMERA) {
        if (!it.accepted) return@requestPermissions cont.resume(null)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, file)
        if (front) {
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
        AndroidAppContext.startActivityForResult(intent) { code, data ->
            Log.info("Result is $code $data")
            if (code == Activity.RESULT_OK) {
                cont.resume((data?.data ?: file)?.let(::FileReference))
            } else {
                cont.resume(null)
            }
        }
    }
}

actual fun RContext.setClipboardText(value: String) {
    (AndroidAppContext.activityCtx?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText(value, value))
}

private val DownloadNotificationId: String = "downloads"

val logger = LogRoot.tag("ExternalServices")

private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")

@SuppressLint("MissingPermission")
actual suspend fun RContext.download(
    name: String,
    url: String,
    preferredDestination: DownloadLocation,
    onDownloadProgress: ((progress: Float) -> Unit)?,
) {
    // TODO: Implement photo library storage for both overloads of download
    // TODO: Add progress update callbacks
    if (!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
    if (VERSION.SDK_INT < VERSION_CODES.Q) {
        AndroidAppContext.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (it.accepted) {
                downloadContinued(name, url)
            }
        }
    } else {
        downloadContinued(name, url)
    }
}

private fun downloadContinued(name: String, url: String) {
    val request = DownloadManager.Request(url.toUri()) // 5.
        .setNotificationVisibility( // 6.
            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
        )
        .setDestinationInExternalPublicDir( // 7.
            Environment.DIRECTORY_DOWNLOADS, name
        )
    request.allowScanningByMediaScanner()
    (AndroidAppContext.applicationCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request) // 8.
}

@SuppressLint("MissingPermission")
actual suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
    if (!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
    if (VERSION.SDK_INT < VERSION_CODES.Q) {
        AndroidAppContext.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
            if (it.accepted) {
                CoroutineScope(Dispatchers.IO).launch {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = File(downloadsDir, name)
                    withContext(Dispatchers.IO) {
                        file.writeBytes(blob.data)
                    }
                }
            }
        }
    } else {
        val resolver = AndroidAppContext.applicationCtx.contentResolver
        val mimeType = blob.type
        val downloadsCollection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(downloadsCollection, contentValues)
            ?: return

        withContext(Dispatchers.IO) {
            resolver.openOutputStream(uri)?.use { output ->
                output.write(blob.data)
            }
        }
        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    }
}

private fun <T> List<T>.identity(): T? = first().takeIf { all { it == first() } }

actual suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>) {
    val files = namesToBlobs.map { it.second.saveToTemporaryFile(it.first) }
        .map {
            FileProvider.getUriForFile(
                AndroidAppContext.applicationCtx,
                AndroidAppContext.fileProviderAuthority,
                it
            )
        }
    val commonMimeType = namesToBlobs.map { it.second.type }.identity() ?: "*/*"

    val shareIntent = Intent().apply {
        if (files.size == 1) {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, files.first())
        } else {
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files))
        }
        type = commonMimeType
    }
    AndroidAppContext.activityCtx?.startActivity(shareIntent)
}

actual fun RContext.share(title: String, message: String?, url: String?) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_TITLE, title)
    listOfNotNull(message, url).joinToString("\n").let { i.putExtra(Intent.EXTRA_TEXT, it) }
    AndroidAppContext.startActivityForResult(Intent.createChooser(i, title)) { _, _ -> }
}

actual fun RContext.openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
    AndroidAppContext.startActivityForResult(
        intent = Intent(Intent.ACTION_VIEW).apply {
            if (label == null) {
                if (zoom == null) {
                    data = Uri.parse("geo:${latitude},${longitude}")
                } else {
                    data = Uri.parse("geo:${latitude},${longitude}?z=$zoom")
                }
            } else {
                if (zoom == null) {
                    data = Uri.parse("geo:${latitude},${longitude}?q=${Uri.encode(label)}")
                } else {
                    data =
                        Uri.parse("geo:${latitude},${longitude}?q=${Uri.encode(label)}&z=$zoom")
                }
            }
        }
    ) { _, _ -> }
}

actual fun RContext.openEvent(
    title: String,
    description: String,
    location: String,
    start: LocalDateTime,
    end: LocalDateTime,
    zone: TimeZone,
) {
    AndroidAppContext.startActivityForResult(
        intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.toInstant(zone).toEpochMilliseconds())
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.toInstant(zone).toEpochMilliseconds())
            putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        }
    ) { _, _ -> }
}

private fun Blob.saveToTemporaryFile(name: String): File {
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type)
    return File(AndroidAppContext.applicationCtx.cacheDir, "$name.$extension").apply { writeBytes(data) }
}