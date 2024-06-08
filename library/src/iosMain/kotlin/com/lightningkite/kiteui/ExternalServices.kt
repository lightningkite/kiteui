package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.extensionStrongRef
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.datetime.*
import platform.CoreGraphics.CGRectMake
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.EventKit.EKEntityType
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKitUI.EKEventEditViewAction
import platform.EventKitUI.EKEventEditViewController
import platform.EventKitUI.EKEventEditViewDelegateProtocol
import platform.Foundation.*
import platform.MapKit.MKMapItem
import platform.MapKit.MKPlacemark
import platform.Photos.PHAccessLevelAddOnly
import platform.Photos.PHAssetChangeRequest
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import platform.PhotosUI.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.int64_t
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual object ExternalServices {
    actual fun openTab(url: String) {
        UIApplication.sharedApplication.openURL(
            url = NSURL(string = url),
            options = mapOf<Any?, Any?>(),
            completionHandler = {})
    }

    val mostTypes = listOf(
        UTTypeData,
        UTTypeText,
        UTTypeImage,
        UTTypeAudio,
        UTTypeVideo,
        UTTypeMovie,
        UTTypeSourceCode,
    )
    var currentPresenter: (UIViewController) -> Unit = {}
    lateinit var rootView: UIView
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
            val controller = UIDocumentPickerViewController(forOpeningContentTypes = mimeTypes.flatMap {
                if (it == "*/*") mostTypes
                else UTType.typeWithMIMEType(it)?.let { listOf(it) } ?: listOf()
            }, asCopy = true)
            controller.allowsMultipleSelection = false
            val delegate =
                object : NSObject(), UIDocumentMenuDelegateProtocol, UIDocumentPickerDelegateProtocol,
                    UINavigationControllerDelegateProtocol {
                    override fun documentMenu(
                        documentMenu: UIDocumentMenuViewController,
                        didPickDocumentPicker: UIDocumentPickerViewController
                    ) {
                        didPickDocumentPicker.delegate = this
                        currentPresenter(didPickDocumentPicker)
                    }

                    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                        cont.resume(null)
                        controller.dismissViewControllerAnimated(true, {})
                    }

                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentAtURL: NSURL
                    ) {
                        cont.resume(FileReference(NSItemProvider(contentsOfURL = didPickDocumentAtURL)))
                        controller.dismissViewControllerAnimated(true, {})
                    }

                    override fun documentPicker(
                        controller: UIDocumentPickerViewController,
                        didPickDocumentsAtURLs: List<*>
                    ) {
                        cont.resume(
                            didPickDocumentsAtURLs.filterIsInstance<NSURL>().firstOrNull()
                                ?.let { FileReference(NSItemProvider(contentsOfURL = it)) })
                        controller.dismissViewControllerAnimated(true, {})
                    }
//                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
//                        picker.dismissViewControllerAnimated(true) {
//                            dispatch_async(queue = dispatch_get_main_queue(), block = {
//                                (didFinishPicking.firstOrNull() as? PHPickerResult)?.let { result ->
//                                    cont.resume(FileReference(result.itemProvider))
//                                } ?: cont.resume(null)
//                            })
//                        }
//                    }
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
                val controller = UIDocumentPickerViewController(forOpeningContentTypes = mimeTypes.flatMap {
                    if (it == "*/*") mostTypes
                    else UTType.typeWithMIMEType(it)?.let { listOf(it) } ?: listOf()
                }, asCopy = true)
                controller.allowsMultipleSelection = true
                val delegate =
                    object : NSObject(), UIDocumentMenuDelegateProtocol, UIDocumentPickerDelegateProtocol,
                        UINavigationControllerDelegateProtocol {
                        override fun documentMenu(
                            documentMenu: UIDocumentMenuViewController,
                            didPickDocumentPicker: UIDocumentPickerViewController
                        ) {
                            didPickDocumentPicker.delegate = this
                            currentPresenter(didPickDocumentPicker)
                        }

                        override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                            cont.resume(listOf())
                            controller.dismissViewControllerAnimated(true, {})
                        }

                        override fun documentPicker(
                            controller: UIDocumentPickerViewController,
                            didPickDocumentAtURL: NSURL
                        ) {
                            cont.resume(listOf(FileReference(NSItemProvider(contentsOfURL = didPickDocumentAtURL))))
                            controller.dismissViewControllerAnimated(true, {})
                        }

                        override fun documentPicker(
                            controller: UIDocumentPickerViewController,
                            didPickDocumentsAtURLs: List<*>
                        ) {
                            cont.resume(
                                didPickDocumentsAtURLs.filterIsInstance<NSURL>()
                                    .map { FileReference(NSItemProvider(contentsOfURL = it)) })
                            controller.dismissViewControllerAnimated(true, {})
                        }
//                    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
//                        picker.dismissViewControllerAnimated(true) {
//                            dispatch_async(queue = dispatch_get_main_queue(), block = {
//                                (didFinishPicking.firstOrNull() as? PHPickerResult)?.let { result ->
//                                    cont.resume(FileReference(result.itemProvider))
//                                } ?: cont.resume(null)
//                            })
//                        }
//                    }
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
                        val u = NSURL(string = "${NSUUID()}.jpg", relativeToURL = p)
                        NSFileManager.defaultManager.createDirectoryAtPath(
                            path = p.path!!,
                            withIntermediateDirectories = true,
                            attributes = null,
                            error = null
                        )
                        if (UIImageJPEGRepresentation(it, 0.98)!!.writeToURL(url = u, atomically = true)) {
                            FileReference(NSItemProvider(contentsOfURL = u), UTTypeJPEG)
                        } else {
                            dispatch_async(queue = dispatch_get_main_queue(), block = {
                                cont.resumeWithException(Exception("Failed to write image file to $u"))
                            })
                            return
                        }
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

    actual suspend fun download(
        name: String,
        url: String,
        preferredDestination: DownloadLocation,
        onDownloadProgress: ((progress: Float) -> Unit)?
    ) = downloadMultiple(mapOf(url to name), preferredDestination, onDownloadProgress)

    suspend fun downloadMultiple(
        urlToNames: Map<String, String>,
        preferredDestination: DownloadLocation,
        onDownloadProgress: ((progress: Float) -> Unit)?
    ) {
        coroutineScope {
            val temporaryFiles = suspendCoroutine {
                var updateProgressJob: Job? = null
                val delegate = NSURLDownloadAndCopyDelegate(
                    { temporaryFiles -> it.resume(temporaryFiles) }
                ) { progress ->
                    onDownloadProgress?.let { updateProgressCallback ->
                        updateProgressJob?.cancel()
                        updateProgressJob = launch(Dispatchers.Main) {
                            updateProgressCallback(progress)
                        }
                    }
                }

                val session = NSURLSession.sessionWithConfiguration(
                    NSURLSessionConfiguration.defaultSessionConfiguration,
                    delegate,
                    null
                )
                for ((url, name) in urlToNames) {
                    val task = session.downloadTaskWithURL(NSURL(string = url))
                    delegate.setFilenameForDownloadTask(task, name)
                    task.resume()
                }
                session.finishTasksAndInvalidate()
            }

            when (preferredDestination) {
                DownloadLocation.Downloads -> {
                    afterTimeout(1) {
                        showShareSheet(items = temporaryFiles)
                    }
                }
                DownloadLocation.Pictures -> {
                    copyFilesToCameraRoll(temporaryFiles)
                }
            }
        }
    }

    private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")
    private fun getTemporaryDestinationPath(name: String): NSURL {
        if (!name.matches(validDownloadName)) throw IllegalArgumentException("Illegal download name $name")
        return NSURL(fileURLWithPath = NSTemporaryDirectory()).URLByAppendingPathComponent(name) ?: throw IllegalStateException("Unable to find a temporary path for file")
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun Blob.saveToTemporaryFile(name: String): NSURL {
        val type = UTType.typeWithMIMEType(type)
        val tmpFile = NSURL(fileURLWithPath = NSTemporaryDirectory()).URLByAppendingPathComponent("$name.${type?.preferredFilenameExtension ?: "tmp"}")!!
        val persistSuccess = data.writeToURL(tmpFile, 0u, null)
        if (!persistSuccess) throw Exception("Unable to copy in-memory Blob to disk")
        return tmpFile
    }

    private suspend fun copyFilesToCameraRoll(files: List<NSURL>) {
        val hasPermission = PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelAddOnly) == PHAuthorizationStatusAuthorized
        if (!hasPermission) {
            println("Lacking Camera Roll add access")
            val newPermission = withContext(Dispatchers.Main) {
                suspendCoroutine { continuation ->
                    PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelAddOnly) {
                        continuation.resume(it)
                    }
                }
            }

            if (newPermission != PHAuthorizationStatusAuthorized) throw Exception("User rejected Camera Roll add permission")
        }

        return suspendCoroutine {
            PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                files.forEach {
                    PHAssetChangeRequest.creationRequestForAssetFromImageAtFileURL(it)
                }
            }) { success, _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (success) {
                        it.resume(Unit)
                    } else {
                        it.resumeWithException(Exception("Unable to make changes to shared photo library"))
                    }
                }
            }
        }
    }

    actual suspend fun download(
        name: String,
        blob: Blob,
        preferredDestination: DownloadLocation
    ) {
        val temporaryFiles = listOf(blob.saveToTemporaryFile(name))
        when (preferredDestination) {
            DownloadLocation.Downloads -> {
                withContext(Dispatchers.Main) {
                    showShareSheet(items = temporaryFiles)
                }
            }
            DownloadLocation.Pictures -> {
                copyFilesToCameraRoll(temporaryFiles)
            }
        }
    }

    actual suspend fun share(namesToBlobs: List<Pair<String, Blob>>) =
        showShareSheet(items = namesToBlobs.map { it.second.saveToTemporaryFile(it.first) })

    actual fun share(title: String, message: String?, url: String?) =
        showShareSheet(messages = listOf(message), items = listOf(url?.let { NSURL(string = it) }))

    @OptIn(ExperimentalForeignApi::class)
    private fun showShareSheet(messages: List<String?> = listOf(), items: List<NSURL?> = listOf()) {
        currentPresenter(UIActivityViewController(messages + items, null).apply {
            popoverPresentationController?.sourceView = rootView
            popoverPresentationController?.sourceRect = CGRectMake(rootView.frame.useContents { origin.x + size.width / 2 }, rootView.frame.useContents { origin.y + size.height / 2 }, 1.0, 1.0)
        })
    }

    private class NSURLDownloadAndCopyDelegate(
        private val onDownloadComplete: (temporaryFiles: List<NSURL>) -> Unit,
        private val onDownloadProgress: ((progress: Float) -> Unit)?
    ) : NSObject(), NSURLSessionDelegateProtocol, NSURLSessionDownloadDelegateProtocol {

        private class DownloadTaskState(val filename: String, var progress: Float)

        private val progressOfTasks = mutableMapOf<NSURLSessionDownloadTask, DownloadTaskState>()
        private val temporaryFiles = mutableListOf<NSURL>()

        fun setFilenameForDownloadTask(task: NSURLSessionDownloadTask, filename: String) {
            progressOfTasks[task] = DownloadTaskState(filename, 0f)
        }

        @OptIn(ExperimentalForeignApi::class)
        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didFinishDownloadingToURL: NSURL
        ) {
            val destination = getTemporaryDestinationPath(progressOfTasks[downloadTask]!!.filename)
            if (destination.path?.let { NSFileManager.defaultManager.fileExistsAtPath(it) } != false) {
                NSFileManager.defaultManager.removeItemAtURL(destination, null)
            }
            val copySuccess = NSFileManager.defaultManager.copyItemAtURL(didFinishDownloadingToURL, destination, null)

            if (copySuccess) {
                temporaryFiles.add(destination)
            }
        }

        override fun URLSession(
            session: NSURLSession,
            didBecomeInvalidWithError: NSError?
        ) {
            dispatch_async(dispatch_get_main_queue()) {
                onDownloadComplete(temporaryFiles)
            }
        }

        override fun URLSession(
            session: NSURLSession,
            downloadTask: NSURLSessionDownloadTask,
            didWriteData: int64_t,
            totalBytesWritten: int64_t,
            totalBytesExpectedToWrite: int64_t
        ) {
            val percent = (totalBytesWritten / totalBytesExpectedToWrite).toFloat()
            progressOfTasks[downloadTask]?.progress = percent
            dispatch_async(dispatch_get_main_queue()) {
                onDownloadProgress?.invoke(progressOfTasks.values.map { it.progress }
                    .reduce { acc, progress -> acc + progress } / progressOfTasks.size)
            }
        }
    }

    actual fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone){
        val store = EKEventStore()
        store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { hasPermission, error ->
            if (hasPermission) {
                afterTimeout(1) {
                    val addController = EKEventEditViewController()
                    addController.eventStore = store
                    val dg = object: NSObject(), EKEventEditViewDelegateProtocol {
                        override fun eventEditViewController(
                            controller: EKEventEditViewController,
                            didCompleteWithAction: EKEventEditViewAction
                        ) {
                            controller.dismissViewControllerAnimated(true, null)
                        }
                    }
                    addController.editViewDelegate = dg
                    addController.extensionStrongRef = dg
                    val event = EKEvent.eventWithEventStore(store)
                    event.title = title
                    event.notes = description
                    event.location = location
                    event.startDate = start.toInstant(zone).toNSDate()
                    event.endDate = end.toInstant(zone).toNSDate()
                    addController.event = event
                    currentPresenter(addController)
                }
            }
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {

        val options = arrayListOf(
            "Apple Maps" to {
                val mapItem = MKMapItem(placemark = MKPlacemark(CLLocationCoordinate2DMake(
                    latitude, longitude
                )))
                mapItem.name = label
                mapItem.openInMapsWithLaunchOptions(mapOf<Any?, Any?>())
            }
        )
        if (UIApplication.sharedApplication.canOpenURL(NSURL(string = "comgooglemaps://"))) {
            options += ("Google Maps" to {
                var url = "string: comgooglemaps://?center=${latitude},${longitude}"
                zoom?.let { zoom ->
                    url += "&zoom=${zoom}"
                }
                label?.let { label ->
                    url += "&q=${label}"
                }
                UIApplication.sharedApplication.openURL(NSURL(string = url))
            })
        }
        if (options.size == 1) {
            options[0].second()
        } else {
            val optionsView = UIAlertController.alertControllerWithTitle(
                title = "Open in Maps",
                message = null,
                preferredStyle = UIAlertControllerStyleAlert
            )
            for (option in options) {
                optionsView.addAction(UIAlertAction.actionWithTitle(
                    title = option.first,
                    style = UIAlertActionStyleDefault,
                    handler = { optionsView.dismissViewControllerAnimated(true, null); option.second() }
                ))
//                optionsView.addAction(UIAlertAction(title = option.first, style: .default, handler: { (action) in
//                        optionsView.dismiss(animated: true, completion: nil)
//                    option.1()
//                }))
            }
            currentPresenter(optionsView)
        }
    }
}