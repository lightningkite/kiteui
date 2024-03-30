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

actual object ExternalServices {
    actual fun openTab(url: String) {
        AndroidAppContext.activityCtx?.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    actual fun requestFile(
        mimeTypes: List<String>,
        onResult: (FileReference?) -> Unit,
    ) = requestFiles(mimeTypes, { onResult(it.getOrNull(0)) }, false)

    actual fun requestFiles(
        mimeTypes: List<String>,
        onResult: (List<FileReference>) -> Unit,
    ) = requestFiles(mimeTypes, onResult, true)

    fun requestFiles(
        mimeTypes: List<String>,
        onResult: (List<FileReference>) -> Unit,
        allowMultiple: Boolean = true
    ) {

        val type = mimeTypes.joinToString(",")

        val getIntent = Intent(Intent.ACTION_GET_CONTENT)
        getIntent.type = type
        getIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)

        val chooserIntent = Intent.createChooser(getIntent, "Select items")

        AndroidAppContext.startActivityForResult(chooserIntent) { code, data ->
            if (code == Activity.RESULT_OK) {
                onResult(
                    data?.clipData?.let {
                        (0 until it.itemCount).map { index ->
                            it.getItemAt(index).uri.let(::FileReference)
                        }
                    }
                        ?: data?.data?.let(::FileReference)?.let(::listOf)
                        ?: listOf()
                )
            } else {
                onResult(listOf())
            }
        }
    }

    actual fun requestCaptureSelf(
        mimeTypes: List<String>,
        onResult: (FileReference?) -> Unit,
    ) {
        if (mimeTypes.all { it.startsWith("image/") }) requestImageCamera(
            true,
            MediaStore.ACTION_IMAGE_CAPTURE,
            onResult
        )
        else if (mimeTypes.all { it.startsWith("video/") }) requestImageCamera(
            true,
            MediaStore.ACTION_VIDEO_CAPTURE,
            onResult
        )
        else System.err.println("Captures besides images and video not supported yet. Requested $mimeTypes")
    }

    actual fun requestCaptureEnvironment(
        mimeTypes: List<String>,
        onResult: (FileReference?) -> Unit,
    ) {
        if (mimeTypes.all { it.startsWith("image/") }) requestImageCamera(
            false,
            MediaStore.ACTION_IMAGE_CAPTURE,
            onResult
        )
        else if (mimeTypes.all { it.startsWith("video/") }) requestImageCamera(
            false,
            MediaStore.ACTION_VIDEO_CAPTURE,
            onResult
        )
        else System.err.println("Captures besides images and video not supported yet. Requested $mimeTypes")
    }

    private fun requestImageCamera(
        front: Boolean = false,
        capture: String = MediaStore.ACTION_IMAGE_CAPTURE,
        onResult: (FileReference?) -> Unit,
    ) {
        val fileProviderAuthority = AndroidAppContext.applicationCtx.packageName + ".fileprovider"
        val file = File(AndroidAppContext.applicationCtx.cacheDir, "images").also { it.mkdirs() }
            .let { File.createTempFile("image", ".jpg", it) }
            .let { FileProvider.getUriForFile(AndroidAppContext.applicationCtx, fileProviderAuthority, it) }

        AndroidAppContext.requestPermissions(android.Manifest.permission.CAMERA) {
            if (!it.accepted) return@requestPermissions onResult(null)
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, file)
            if (front) {
                intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
                intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
                intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            }
            AndroidAppContext.startActivityForResult(intent) { code, data ->
                if (code == Activity.RESULT_OK) {
                    onResult(data?.data?.let(::FileReference))
                } else {
                    onResult(null)
                }
            }
        }
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