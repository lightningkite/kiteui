// by Claude
package com.lightningkite.kiteui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.View
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.ImageLocal
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class CameraPreview actual constructor(context: ElementContext) : RView(context) {
    private val _native = PreviewView(context.activity).apply {
        setBackgroundColor(0xFF000000.toInt())
    }
    override val native: View get() = _native

    private val cameraController = LifecycleCameraController(context.activity)
    private val analysisPipeline = mutableListOf<(ImageProxy, () -> Unit) -> Unit>()

    private val _hasPermissions = Signal(false)
    actual val hasPermissions: MutableReactive<Boolean> get() = _hasPermissions

    @OptIn(ExperimentalGetImage::class)
    actual fun onBarcode(formats: Set<BarcodeFormat>, action: (List<BarcodeResult>) -> Unit) {
        val mlKitFormats = if (formats.isEmpty()) {
            // Default formats
            intArrayOf(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93
            )
        } else {
            formats.mapNotNull { it.toMlKitFormat() }.toIntArray()
        }

        if (mlKitFormats.isEmpty()) return

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(mlKitFormats.first(), *mlKitFormats.drop(1).toIntArray())
            .build()
        val barcodeScanner = BarcodeScanning.getClient(options)

        analysisPipeline.add { imageProxy, release ->
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val results = barcodes.mapNotNull { barcode ->
                            val rawValue = barcode.rawValue ?: return@mapNotNull null
                            val format = barcode.format.fromMlKitFormat() ?: return@mapNotNull null
                            BarcodeResult(rawValue, format, imageProxy.imageInfo.timestamp)
                        }
                        if (results.isNotEmpty()) {
                            action(results)
                        }
                        release()
                    }
                    .addOnFailureListener {
                        release()
                    }
            } else {
                release()
            }
        }
    }

    override fun postSetup() {
        super.postSetup()
        cameraController.apply {
            bindToLifecycle(context.activity)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            isPinchToZoomEnabled = false
            _native.controller = this

            setImageAnalysisAnalyzer(AndroidAppContext.executor) { imageProxy ->
                var pendingAnalyses = analysisPipeline.size
                if (pendingAnalyses == 0) {
                    imageProxy.close()
                    return@setImageAnalysisAnalyzer
                }
                val imageProxyRelease = {
                    if (--pendingAnalyses == 0) {
                        imageProxy.close()
                    }
                }
                analysisPipeline.forEach { it(imageProxy, imageProxyRelease) }
            }
        }

        if (ContextCompat.checkSelfPermission(context.activity, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_DENIED
        ) {
            AndroidAppContext.requestPermissions(
                Manifest.permission.CAMERA,
                onResult = { result: KiteUiActivity.PermissionResult ->
                    _hasPermissions.value = result.accepted
                }
            )
        } else {
            _hasPermissions.value = true
        }
    }

    private fun timestamp(): String {
        val df = SimpleDateFormat("MMddyyHHmmss", Locale.getDefault())
        return df.format(Date())
    }

    actual suspend fun capture(): ImageLocal? = suspendCancellableCoroutine { cont ->
        val internalCallback = object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val translated = imageProxy.toBitmap()
                    .let {
                        val matrix = Matrix()
                        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                        Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
                    }

                val file = File(AndroidAppContext.applicationCtx.filesDir, "${timestamp()}.jpg")
                val outputStream = FileOutputStream(file)
                translated.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()

                val outputUri = file.toUri()
                val image = ImageLocal(FileReference(outputUri))
                native.post {
                    cont.resume(image)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                cont.resumeWithException(exception)
            }
        }

        cameraController.takePicture(AndroidAppContext.executor, internalCallback)
    }
}

private fun BarcodeFormat.toMlKitFormat(): Int? = when (this) {
    BarcodeFormat.QR_CODE -> Barcode.FORMAT_QR_CODE
    BarcodeFormat.CODE_128 -> Barcode.FORMAT_CODE_128
    BarcodeFormat.CODE_39 -> Barcode.FORMAT_CODE_39
    BarcodeFormat.CODE_93 -> Barcode.FORMAT_CODE_93
    BarcodeFormat.EAN_8 -> Barcode.FORMAT_EAN_8
    BarcodeFormat.EAN_13 -> Barcode.FORMAT_EAN_13
    BarcodeFormat.UPC_A -> Barcode.FORMAT_UPC_A
    BarcodeFormat.UPC_E -> Barcode.FORMAT_UPC_E
    BarcodeFormat.PDF_417 -> Barcode.FORMAT_PDF417
    BarcodeFormat.DATA_MATRIX -> Barcode.FORMAT_DATA_MATRIX
    BarcodeFormat.AZTEC -> Barcode.FORMAT_AZTEC
    BarcodeFormat.ITF -> Barcode.FORMAT_ITF
    BarcodeFormat.CODABAR -> Barcode.FORMAT_CODABAR
}

private fun Int.fromMlKitFormat(): BarcodeFormat? = when (this) {
    Barcode.FORMAT_QR_CODE -> BarcodeFormat.QR_CODE
    Barcode.FORMAT_CODE_128 -> BarcodeFormat.CODE_128
    Barcode.FORMAT_CODE_39 -> BarcodeFormat.CODE_39
    Barcode.FORMAT_CODE_93 -> BarcodeFormat.CODE_93
    Barcode.FORMAT_EAN_8 -> BarcodeFormat.EAN_8
    Barcode.FORMAT_EAN_13 -> BarcodeFormat.EAN_13
    Barcode.FORMAT_UPC_A -> BarcodeFormat.UPC_A
    Barcode.FORMAT_UPC_E -> BarcodeFormat.UPC_E
    Barcode.FORMAT_PDF417 -> BarcodeFormat.PDF_417
    Barcode.FORMAT_DATA_MATRIX -> BarcodeFormat.DATA_MATRIX
    Barcode.FORMAT_AZTEC -> BarcodeFormat.AZTEC
    Barcode.FORMAT_ITF -> BarcodeFormat.ITF
    Barcode.FORMAT_CODABAR -> BarcodeFormat.CODABAR
    else -> null
}
