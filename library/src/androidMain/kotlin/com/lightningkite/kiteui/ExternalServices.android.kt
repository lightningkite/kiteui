package com.lightningkite.kiteui

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.Context.NOTIFICATION_SERVICE
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.lightningkite.kiteui.views.AndroidAppContext
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import java.io.File
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
    ): FileReference? = suspendCoroutineCancellable { cont ->
        val fileProviderAuthority = AndroidAppContext.applicationCtx.packageName + ".fileprovider"
        val file = File(AndroidAppContext.applicationCtx.cacheDir, "images").also { it.mkdirs() }
            .let { File.createTempFile("image", ".jpg", it) }
            .let { FileProvider.getUriForFile(AndroidAppContext.applicationCtx, fileProviderAuthority, it) }

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

    @SuppressLint("MissingPermission")
    actual fun download(name: String, url: String, onProgress: (Double) -> Unit) {
        if(VERSION.SDK_INT < VERSION_CODES.Q) {
            AndroidAppContext.requestPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                if(it.accepted) {
                    downloadContinued(name, url, onProgress)
                }
            }
        } else {
            downloadContinued(name, url, onProgress)
        }
    }
    private fun downloadContinued(name: String, url: String, onProgress: (Double) -> Unit) {
        val request = DownloadManager.Request(Uri.parse(url)) // 5.
            .setNotificationVisibility( // 6.
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir( // 7.
                Environment.DIRECTORY_DOWNLOADS, name)
        (AndroidAppContext.applicationCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request) // 8.
        Toast.makeText( // 9.
            AndroidAppContext.activityCtx!!, "Download started", Toast.LENGTH_SHORT
        ).show()
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