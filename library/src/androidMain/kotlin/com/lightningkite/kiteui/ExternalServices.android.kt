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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
import java.net.URI
import kotlin.coroutines.resume

actual object ExternalServices {
    actual fun openTab(url: String) {
        AndroidAppContext.activityCtx?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    actual suspend fun requestFile(
        mimeTypes: List<String>,
    ) = requestFiles(mimeTypes, false).firstOrNull()

    actual suspend fun requestFiles(
        mimeTypes: List<String>,
    ) = requestFiles(mimeTypes, true)

    suspend fun requestFiles(
        mimeTypes: List<String>,
        allowMultiple: Boolean = true
    ): List<FileReference> = suspendCoroutineCancellable {

        val type = mimeTypes.joinToString(",")
        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        if (mimeTypes.size > 1) {
            getIntent.type = "*/*"
        } else {
            getIntent.type = mimeTypes.first()
            getIntent.putExtra(Intent.EXTRA_MIME_TYPES, type)
        }
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
        return@suspendCoroutineCancellable {}
    }

    actual suspend fun requestCaptureSelf(
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

    actual suspend fun requestCaptureEnvironment(
        mimeTypes: List<String>
    ): FileReference? {
        return requestImageCamera(false, MediaStore.ACTION_IMAGE_CAPTURE)
//        return if (mimeTypes.all { it.startsWith("image/") }) requestImageCamera(
//            false,
//            MediaStore.ACTION_IMAGE_CAPTURE
//        )
//        else if (mimeTypes.all { it.startsWith("video/") }) requestImageCamera(
//            false,
//            MediaStore.ACTION_VIDEO_CAPTURE
//        )
//        else throw Exception("Captures besides images and video not supported yet. Requested $mimeTypes")
    }

    private suspend fun requestImageCamera(
        front: Boolean = false,
        capture: String = MediaStore.ACTION_IMAGE_CAPTURE,
    ): FileReference? = suspendCoroutineCancellable { cont ->
        val fileProviderAuthority = AndroidAppContext.applicationCtx.packageName + ".fileprovider"
        val file = File(AndroidAppContext.applicationCtx.cacheDir, "images").also { it.mkdirs() }
            .let { File.createTempFile("image", ".jpg", it) }
            .let { FileProvider.getUriForFile(AndroidAppContext.applicationCtx, fileProviderAuthority, it) }

        AndroidAppContext.requestPermissions(android.Manifest.permission.CAMERA) {

//            if (!it.accepted) return@requestPermissions cont.resume(null)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file)
            if (front) {
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
            AndroidAppContext.startActivityForResult(intent) { code, data ->
                println("Result is $code $data")
                if (code == Activity.RESULT_OK) {
                    cont.resume((data?.data ?: file)?.let(::FileReference))
                } else {
                    cont.resume(null)
                }
            }
        }
        return@suspendCoroutineCancellable {}
    }

    actual fun setClipboardText(value: String) {
        (AndroidAppContext.activityCtx?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText(value, value))
    }

    private val DownloadNotificationId: String = "downloads"

    val logger = ConsoleRoot.tag("ExternalServices")

    private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")

    @SuppressLint("MissingPermission")
    actual suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?) {
        // TODO: Implement photo library storage for both overloads of download
        // TODO: Add progress update callbacks
        if(!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        if(VERSION.SDK_INT < VERSION_CODES.Q) {
            AndroidAppContext.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                if(it.accepted) {
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
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir( // 7.
                Environment.DIRECTORY_DOWNLOADS, name)
        request.allowScanningByMediaScanner()
        (AndroidAppContext.applicationCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request) // 8.
        Toast.makeText( // 9.
            AndroidAppContext.activityCtx!!, "Download started", Toast.LENGTH_SHORT
        ).show()
    }

    @SuppressLint("MissingPermission")
    actual suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
        if(!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        val permissionResult = AndroidAppContext.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permissionResult.accepted) {
            val base = AndroidAppContext.applicationCtx.getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS).firstOrNull() ?: throw Exception("No external download directory; unable to download file")
            downloadContinued(name, blob, base)
        }
    }
    private suspend fun downloadContinued(name: String, blob: Blob, base: File): URI {
        val nameWithoutExt = name.substringBeforeLast('.')
        val nameExt = name.substringAfterLast('.', "").takeUnless { it.isBlank() } ?: MimeTypeMap.getSingleton().getExtensionFromMimeType(blob.type) ?: when(blob.type) {
            "audio/mp4" -> "mp4"
            else -> "file"
        }

        var out = base.resolve("$nameWithoutExt.$nameExt")
        var num = 2
        while(out.exists()) {
            out = base.resolve("$nameWithoutExt-$num.$nameExt")
        }
        withContext(Dispatchers.IO) {
            out.writeBytes(blob.data)
        }
        return out.toURI()
    }

    actual suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {
        // Group items by MIME type
        val itemsGroupedByMimeType = namesToBlobs.groupBy { it.second.mimeType() }

        itemsGroupedByMimeType.map { mimeTypeGroup ->
            val uris = mimeTypeGroup.value.map {
                val tempFile = File(AndroidAppContext.applicationCtx.cacheDir, it.first)
                tempFile.writeBytes(it.second.data)
                println("test sharing")
                println("${AndroidAppContext.applicationCtx.packageName}.fileprovider")
                // Use FileProvider to get a content URI
                FileProvider.getUriForFile(
                    AndroidAppContext.applicationCtx,
                    "${AndroidAppContext.applicationCtx.packageName}.fileprovider",
                    tempFile
                )
            }

            // Convert the uris list to an ArrayList to match the required type
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = mimeTypeGroup.key
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to read URIs
            }

            // Start the share intent
            AndroidAppContext.applicationCtx.startActivity(
                Intent.createChooser(intent, "Share Files")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

    }

    actual fun share(title: String, message: String?, url: String?) {
        val i = Intent(Intent.ACTION_SEND)
        i.type = "text/plain"
        i.putExtra(Intent.EXTRA_TITLE, title)
        listOfNotNull(message, url).joinToString("\n").let { i.putExtra(Intent.EXTRA_TEXT, it) }
        AndroidAppContext.startActivityForResult(Intent.createChooser(i, title)) { _, _ -> }
    }

    actual fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
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

    actual fun openEvent(
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
}