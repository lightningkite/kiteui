// by Claude
package com.lightningkite.kiteui.camera

import com.lightningkite.kiteui.FileReference
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.report
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.reactive.context.onRemove
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeJPEG
import platform.darwin.*
import kotlin.coroutines.resume

actual class CameraPreview actual constructor(context: ElementContext) : NativeElement(context) {
    val _native = PreviewView()
    override val native: UIView get() = _native

    private val sessionQueue = dispatch_queue_create("camera_session_queue", null)
    private val captureSession = AVCaptureSession().also(_native::setCaptureSession)
    private val captureDevice = AVCaptureDevice.defaultDeviceWithDeviceType(
        AVCaptureDeviceTypeBuiltInWideAngleCamera,
        AVMediaTypeVideo,
        AVCaptureDevicePositionBack
    )

    private val stillCaptureOutput = AVCapturePhotoOutput().apply {
        highResolutionCaptureEnabled = true
        livePhotoCaptureEnabled = false
    }

    private val _hasPermissions = Signal(false)
    actual val hasPermissions: MutableReactive<Boolean> get() = _hasPermissions

    init {
        checkPermissions()
    }

    private fun checkPermissions() {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> {
                _hasPermissions.value = true
            }
            AVAuthorizationStatusNotDetermined -> {
                dispatch_suspend(sessionQueue)
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    _hasPermissions.value = granted
                    dispatch_resume(sessionQueue)
                }
            }
            else -> {
                _hasPermissions.value = false
            }
        }

        dispatch_async(sessionQueue) {
            setupCaptureSession()
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun setupCaptureSession() {
        if (!_hasPermissions.value) return

        captureSession.beginConfiguration()

        try {
            captureDevice?.let { device ->
                AVCaptureDeviceInput.deviceInputWithDevice(device, null)?.let { input ->
                    if (captureSession.canAddInput(input)) {
                        captureSession.addInput(input)
                    }
                }
            }

            if (captureSession.canAddOutput(stillCaptureOutput)) {
                captureSession.addOutput(stillCaptureOutput)
            }
        } finally {
            captureSession.commitConfiguration()
        }
        captureSession.startRunning()
    }

    private val metadataOutputQueue by lazy { dispatch_queue_create("metadata_objects_queue", null) }
    private val metadataOutput by lazy { AVCaptureMetadataOutput() }
    private var metadataDelegate: AVCaptureMetadataOutputObjectsDelegateProtocol? = null

    actual fun onBarcode(formats: Set<BarcodeFormat>, action: (List<BarcodeResult>) -> Unit) {
        dispatch_async(sessionQueue) {
            captureSession.configure {
                if (captureSession.canAddOutput(metadataOutput)) {
                    captureSession.addOutput(metadataOutput)
                    metadataOutput.apply {
                        metadataDelegate = MetadataBarcodeDelegate(formats, action).also {
                            setMetadataObjectsDelegate(it, metadataOutputQueue)
                        }

                        val requestedObjectTypes = if (formats.isEmpty()) {
                            // Default formats
                            setOf(
                                AVMetadataObjectTypeQRCode,
                                AVMetadataObjectTypeCode39Code,
                                AVMetadataObjectTypeCode93Code,
                                AVMetadataObjectTypeCode128Code
                            )
                        } else {
                            formats.mapNotNull { it.toAVMetadataObjectType() }.toSet()
                        }
                        metadataObjectTypes = availableMetadataObjectTypes.intersect(requestedObjectTypes).toList()
                    }
                }
            }
        }
    }

    private var lastCaptureDelegate: StillCaptureDelegate? = null
    init { onRemove { lastCaptureDelegate = null } }

    actual suspend fun capture(): ImageLocal? {
        if (!_hasPermissions.value) return null
        return suspendCancellableCoroutine { continuation ->
            val defaultCaptureSettings = AVCapturePhotoSettings.photoSettingsWithFormat(
                mapOf(AVVideoCodecKey to AVVideoCodecTypeJPEG)
            ).apply {
                flashMode = AVCaptureFlashModeOff
            }
            stillCaptureOutput.capturePhotoWithSettings(
                defaultCaptureSettings,
                StillCaptureDelegate(
                    onError = {
                        lastCaptureDelegate = null
                        dispatch_async(queue = dispatch_get_main_queue(), block = {
                            continuation.resume(null)
                        })
                    }
                ) { result ->
                    lastCaptureDelegate = null
                    dispatch_async(queue = dispatch_get_main_queue(), block = {
                        continuation.resume(result)
                    })
                }.also { lastCaptureDelegate = it }
            )
        }
    }
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
class PreviewView : UIView(CGRectZero.readValue()) {
    private var videoPreviewLayer: AVCaptureVideoPreviewLayer? = null

    override fun layoutSubviews() {
        super.layoutSubviews()
        videoPreviewLayer?.apply {
            frame = this@PreviewView.bounds
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    fun setCaptureSession(session: AVCaptureSession) {
        videoPreviewLayer = AVCaptureVideoPreviewLayer.layerWithSession(session).apply {
            frame = this@PreviewView.bounds
            videoGravity = AVLayerVideoGravityResizeAspectFill
            this@PreviewView.layer.addSublayer(this)
        }
    }
}

class MetadataBarcodeDelegate(
    private val formats: Set<BarcodeFormat>,
    private val barcodeHandler: (List<BarcodeResult>) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private val barcodeResultHandlerMutex = Mutex()

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (barcodeResultHandlerMutex.tryLock()) {
            val results = didOutputMetadataObjects
                .filterIsInstance<AVMetadataMachineReadableCodeObject>()
                .mapNotNull { obj ->
                    val rawValue = obj.stringValue ?: return@mapNotNull null
                    val format = obj.type?.fromAVMetadataObjectType() ?: return@mapNotNull null
                    BarcodeResult(rawValue, format, 0)
                }
            if (results.isNotEmpty()) {
                dispatch_async(dispatch_get_main_queue()) {
                    barcodeHandler(results)
                    barcodeResultHandlerMutex.unlock()
                }
            } else {
                barcodeResultHandlerMutex.unlock()
            }
        }
    }
}

class StillCaptureDelegate(
    private val onError: () -> Unit,
    private val onSuccess: (ImageLocal?) -> Unit,
) : NSObject(), AVCapturePhotoCaptureDelegateProtocol {
    private var callbackLockFlag = false

    @OptIn(ExperimentalForeignApi::class)
    override fun captureOutput(
        output: AVCapturePhotoOutput,
        didFinishProcessingPhoto: AVCapturePhoto,
        error: NSError?
    ) {
        if (callbackLockFlag) return
        callbackLockFlag = true

        try {
            if (error != null) {
                try { onError() } catch (e: Exception) { e.report() }
                return
            }

            val image = didFinishProcessingPhoto.fileDataRepresentation()
                ?.let { UIImage(it) }

            if (image == null) {
                try { onError() } catch (e: Exception) { e.report() }
                return
            }

            val p = NSURL(fileURLWithPath = NSTemporaryDirectory())
            val u = NSURL(string = "${NSUUID()}.jpg", relativeToURL = p)
            NSFileManager.defaultManager.createDirectoryAtPath(
                path = p.path!!,
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
            if (UIImageJPEGRepresentation(image, 0.98)!!.writeToURL(url = u, atomically = true)) {
                val result = FileReference(NSItemProvider(contentsOfURL = u), UTTypeJPEG).let { ImageLocal(it) }
                try { onSuccess(result) } catch (e: Exception) { e.report() }
            } else {
                try { onError() } catch (e: Exception) { e.report() }
            }
        } catch (e: Exception) {
            e.report()
            try { onError() } catch (e: Exception) { e.report() }
        }
    }
}

fun AVCaptureSession.configure(configure: () -> Unit) {
    beginConfiguration()
    try {
        configure()
    } finally {
        commitConfiguration()
    }
}

private fun BarcodeFormat.toAVMetadataObjectType(): String? = when (this) {
    BarcodeFormat.QR_CODE -> AVMetadataObjectTypeQRCode
    BarcodeFormat.CODE_128 -> AVMetadataObjectTypeCode128Code
    BarcodeFormat.CODE_39 -> AVMetadataObjectTypeCode39Code
    BarcodeFormat.CODE_93 -> AVMetadataObjectTypeCode93Code
    BarcodeFormat.EAN_8 -> AVMetadataObjectTypeEAN8Code
    BarcodeFormat.EAN_13 -> AVMetadataObjectTypeEAN13Code
    BarcodeFormat.UPC_E -> AVMetadataObjectTypeUPCECode
    BarcodeFormat.PDF_417 -> AVMetadataObjectTypePDF417Code
    BarcodeFormat.DATA_MATRIX -> AVMetadataObjectTypeDataMatrixCode
    BarcodeFormat.AZTEC -> AVMetadataObjectTypeAztecCode
    BarcodeFormat.ITF -> AVMetadataObjectTypeITF14Code
    BarcodeFormat.UPC_A -> null // UPC-A is handled as EAN-13 on iOS
    BarcodeFormat.CODABAR -> null // Not supported on iOS
}

private fun String.fromAVMetadataObjectType(): BarcodeFormat? = when (this) {
    AVMetadataObjectTypeQRCode -> BarcodeFormat.QR_CODE
    AVMetadataObjectTypeCode128Code -> BarcodeFormat.CODE_128
    AVMetadataObjectTypeCode39Code -> BarcodeFormat.CODE_39
    AVMetadataObjectTypeCode93Code -> BarcodeFormat.CODE_93
    AVMetadataObjectTypeEAN8Code -> BarcodeFormat.EAN_8
    AVMetadataObjectTypeEAN13Code -> BarcodeFormat.EAN_13
    AVMetadataObjectTypeUPCECode -> BarcodeFormat.UPC_E
    AVMetadataObjectTypePDF417Code -> BarcodeFormat.PDF_417
    AVMetadataObjectTypeDataMatrixCode -> BarcodeFormat.DATA_MATRIX
    AVMetadataObjectTypeAztecCode -> BarcodeFormat.AZTEC
    AVMetadataObjectTypeITF14Code -> BarcodeFormat.ITF
    else -> null
}
