package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.extensionStrongRef
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual object ExternalServices {
    actual fun openTab(url: String) {
        UIApplication.sharedApplication.openURL(
            url = NSURL(string = url),
            options = mapOf<Any?, Any?>(),
            completionHandler = {})
    }

    var currentPresenter: (UIViewController) -> Unit = {}
    actual suspend fun requestFile(mimeTypes: List<String>): FileReference? = suspendCoroutineCancellable { cont ->
        val imagePickerCompat = mimeTypes.all { it.startsWith("image/") || it.startsWith("video/") }
        if (imagePickerCompat) {
            val controller = PHPickerViewController(PHPickerConfiguration(PHPhotoLibrary.sharedPhotoLibrary()).apply {
                filter = PHPickerFilter.anyFilterMatchingSubfilters(
                    listOfNotNull(
                        PHPickerFilter.imagesFilter.takeIf { mimeTypes.any { it.startsWith("image/") } },
                        PHPickerFilter.videosFilter.takeIf { mimeTypes.any { it.startsWith("video/") } },
                    )
                )
                preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCompatible
                selectionLimit = 1
            })
            val delegate =
                object : NSObject(), PHPickerViewControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                        picker.dismissViewControllerAnimated(true) {
                            dispatch_async(queue = dispatch_get_main_queue(), block = {
                                (didFinishPicking.firstOrNull() as? PHPickerResult)?.let { result ->
                                    cont.resume(FileReference(result.itemProvider))
                                } ?: cont.resume(null)
                            })
                        }
                    }
                }
            controller.delegate = delegate
            controller.extensionStrongRef = delegate
            currentPresenter(controller)
            return@suspendCoroutineCancellable {
                try {
                    controller.dismissViewControllerAnimated(true, {})
                } catch (e: Exception) { /*squish*/
                }
            }
        } else {
            cont.resumeWithException(Exception("Docs picker not implemented yet"))
            return@suspendCoroutineCancellable {}
        }
    }

    actual suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> =
        suspendCoroutineCancellable { cont ->
            val imagePickerCompat = mimeTypes.all { it.startsWith("image/") || it.startsWith("video/") }
            if (imagePickerCompat) {
                val controller =
                    PHPickerViewController(PHPickerConfiguration(PHPhotoLibrary.sharedPhotoLibrary()).apply {
                        filter = PHPickerFilter.anyFilterMatchingSubfilters(
                            listOfNotNull(
                                PHPickerFilter.imagesFilter.takeIf { mimeTypes.any { it.startsWith("image/") } },
                                PHPickerFilter.videosFilter.takeIf { mimeTypes.any { it.startsWith("video/") } },
                            )
                        )
                        preferredAssetRepresentationMode = PHPickerConfigurationAssetRepresentationModeCompatible
                        selectionLimit = Int.MAX_VALUE.toLong()
                    })
                val delegate =
                    object : NSObject(), PHPickerViewControllerDelegateProtocol,
                        UINavigationControllerDelegateProtocol {
                        override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                            picker.dismissViewControllerAnimated(true) {
                                dispatch_async(queue = dispatch_get_main_queue(), block = {
                                    didFinishPicking.filterIsInstance<PHPickerResult>()
                                        .map { result -> FileReference(result.itemProvider) }
                                        .let { cont.resume(it) }
                                })
                            }
                        }
                    }
                controller.delegate = delegate
                controller.extensionStrongRef = delegate
                currentPresenter(controller)
                return@suspendCoroutineCancellable {
                    try {
                        controller.dismissViewControllerAnimated(true, {})
                    } catch (e: Exception) { /*squish*/
                    }
                }
            } else {
                cont.resumeWithException(Exception("Docs picker not implemented yet"))
                return@suspendCoroutineCancellable {}
            }
        }

    actual suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? {
        return if (mimeTypes.all { it.startsWith("image/") }) {
            requestCapture(
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront,
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto,
            )
        } else if (mimeTypes.all { it.startsWith("video/") }) {
            requestCapture(
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront,
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModeVideo,
            )
        } else {
            requestCapture(
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceFront,
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto,
            )
        }
    }

    actual suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? {
        return if (mimeTypes.all { it.startsWith("image/") }) {
            requestCapture(
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear,
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto,
            )
        } else if (mimeTypes.all { it.startsWith("video/") }) {
            requestCapture(
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear,
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModeVideo,
            )
        } else {
            requestCapture(
                UIImagePickerControllerCameraDevice.UIImagePickerControllerCameraDeviceRear,
                UIImagePickerControllerCameraCaptureMode.UIImagePickerControllerCameraCaptureModePhoto,
            )
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    suspend fun requestCapture(
        camera: UIImagePickerControllerCameraDevice,
        mode: UIImagePickerControllerCameraCaptureMode,
    ): FileReference? = suspendCoroutineCancellable { cont ->
        val controller = UIImagePickerController()
        controller.sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        controller.cameraDevice = camera
        controller.cameraCaptureMode = mode
        val delegate =
            object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
                override fun imagePickerController(
                    picker: UIImagePickerController,
                    didFinishPickingMediaWithInfo: Map<Any?, *>
                ) {
                    val url = didFinishPickingMediaWithInfo[UIImagePickerControllerMediaURL] as? NSURL
                        ?: didFinishPickingMediaWithInfo[UIImagePickerControllerImageURL] as? NSURL

                    url?.let {
                        dispatch_async(queue = dispatch_get_main_queue(), block = {
                            cont.resume(FileReference(NSItemProvider(contentsOfURL = it)))
                        })
                        return
                    }

                    val image = didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage] as? UIImage
                        ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage

                    val asFile = image?.let {
                        val p = NSURL(fileURLWithPath = NSTemporaryDirectory())
                        val u = NSURL(string = "${NSUUID()}", relativeToURL = p)
                        NSFileManager.defaultManager.createDirectoryAtPath(
                            path = p.path!!,
                            withIntermediateDirectories = true,
                            attributes = null,
                            error = null
                        )
                        UIImageJPEGRepresentation(it, 0.98)!!.writeToURL(url = u, atomically = true)
                        FileReference(NSItemProvider(contentsOfURL = u))
                    }

                    picker.dismissViewControllerAnimated(true) {
                        dispatch_async(queue = dispatch_get_main_queue(), block = {
                            cont.resume(asFile)
                        })
                    }
                }
            }
        controller.delegate = delegate
        controller.extensionStrongRef = delegate
        currentPresenter(controller)
        return@suspendCoroutineCancellable {
            try {
                controller.dismissViewControllerAnimated(true, null)
            } catch (e: Exception) { /*squish*/
            }
        }
    }

    actual fun setClipboardText(value: String) {
        UIPasteboard.generalPasteboard.string = value
    }
}