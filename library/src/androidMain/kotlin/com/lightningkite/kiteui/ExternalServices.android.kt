package com.lightningkite.kiteui

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.lightningkite.kiteui.views.AndroidAppContext
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
    ): List<FileReference> = suspendCoroutineCancellable{

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
                if (code == Activity.RESULT_OK) {
                    cont.resume(data?.data?.let(::FileReference))
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
//    actual fun download(blob: Blob) {
//
//    }
//    actual fun download(url: String) {
//
//    }
}