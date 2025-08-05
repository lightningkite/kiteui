package com.lightningkite.kiteui

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.provider.CalendarContract
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.kiteui.views.RContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
import kotlin.coroutines.resume

@InternalKiteUi
public actual fun RContext.openTab(url: String) {
    AndroidAppContext.activityCtx?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
}

@InternalKiteUi
public actual suspend fun RContext.requestFile(
    mimeTypes: List<String>,
):FileReference? = requestFiles(mimeTypes, false).firstOrNull()

@InternalKiteUi
public actual suspend fun RContext.requestFiles(
    mimeTypes: List<String>,
): List<FileReference>
        = requestFiles(mimeTypes, true)

public suspend fun RContext.requestFiles(
    mimeTypes: List<String>,
    allowMultiple: Boolean = true
): List<FileReference> = suspendCancellableCoroutine {

    val type = mimeTypes.joinToString(",")

    val getIntent = Intent(Intent.ACTION_GET_CONTENT)
    getIntent.type = type
    getIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

    val chooserIntent = Intent.createChooser(getIntent, "Select items")

    AndroidAppContext.startActivityForResult(chooserIntent) { code, data ->
        if (code == Activity.RESULT_OK) {
            it.resume(
                data?.clipData?.let {
                    (0 until it.itemCount).map { index ->
                        it.getItemAt(index).uri.let(::FileReference)
                    }
                }
                    ?: data?.data?.let(::FileReference)?.let(::listOf)
                    ?: listOf()
            )
        } else {
            it.resume(listOf())
        }
    }
}

@InternalKiteUi
public actual suspend fun RContext.requestCaptureSelf(
    mimeTypes: List<String>
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

@InternalKiteUi
public actual suspend fun RContext.requestCaptureEnvironment(
    mimeTypes: List<String>
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

    AndroidAppContext.requestPermissions(Manifest.permission.CAMERA) {
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

@InternalKiteUi
public actual fun RContext.setClipboardText(value: String) {
    (AndroidAppContext.activityCtx?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
        .setPrimaryClip(ClipData.newPlainText(value, value))
}

private val DownloadNotificationId: String = "downloads"

public val logger: Log = LogRoot.tag("ExternalServices")

@InternalKiteUi
private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")

@SuppressLint("MissingPermission")
public actual suspend fun RContext.download(
    name: String,
    url: String,
    preferredDestination: DownloadLocation,
    onDownloadProgress: ((progress: Float) -> Unit)?
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
    val request = DownloadManager.Request(Uri.parse(url)) // 5.
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
public actual suspend fun RContext.download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
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

@InternalKiteUi
public actual suspend fun RContext.share(namesToBlobs: List<Pair<String, Blob>>) {
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

@InternalKiteUi
public actual fun RContext.share(title: String, message: String?, url: String?) {
    val i = Intent(Intent.ACTION_SEND)
    i.type = "text/plain"
    i.putExtra(Intent.EXTRA_TITLE, title)
    listOfNotNull(message, url).joinToString("\n").let { i.putExtra(Intent.EXTRA_TEXT, it) }
    AndroidAppContext.startActivityForResult(Intent.createChooser(i, title)) { _, _ -> }
}

@InternalKiteUi
public actual fun RContext.openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
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

@InternalKiteUi
public actual fun RContext.openEvent(
    title: String,
    description: String,
    location: String,
    start: LocalDateTime,
    end: LocalDateTime,
    zone: TimeZone
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