package com.lightningkite.kiteui

import android.graphics.*
import android.net.Uri
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.AndroidAppContext
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageCompressionTestAndroid : ImageCompressionTest() {

    private fun ensureContext() {
        try {
            AndroidAppContext.applicationCtx
        } catch (_: UninitializedPropertyAccessException) {
            AndroidAppContext.applicationCtx = RuntimeEnvironment.getApplication()
        }
    }

    override suspend fun createTestImage(width: Int, height: Int, name: String): FileReference {
        ensureContext()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw gradient stripes
        for (y in 0 until height step 10) {
            val r = (255 * y / height)
            val b = 255 - r
            paint.color = android.graphics.Color.rgb(r, 100, b)
            canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + 10).toFloat(), paint)
        }
        // Draw a green circle
        paint.color = android.graphics.Color.rgb(0, 200, 0)
        canvas.drawCircle(width / 2f, height / 2f, minOf(width, height) / 4f, paint)

        val ext = name.substringAfterLast('.')
        val cacheDir = AndroidAppContext.applicationCtx.cacheDir
        val tempFile = File.createTempFile(name.substringBeforeLast('.'), ".$ext", cacheDir)
        tempFile.deleteOnExit()
        tempFile.outputStream().use { out ->
            bitmap.compress(
                when (ext) {
                    "png" -> Bitmap.CompressFormat.PNG
                    else -> Bitmap.CompressFormat.JPEG
                }, 100, out
            )
        }
        bitmap.recycle()

        return FileReference(Uri.fromFile(tempFile))
    }

    override suspend fun ByteArray.getImageSize(): Size {
        val decoded = BitmapFactory.decodeByteArray(this, 0, this.size)
        return Size(decoded.width.toDouble(), decoded.height.toDouble())
    }

    override suspend fun FileReference.getRawBytes(): ByteArray = AndroidAppContext.applicationCtx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
    override suspend fun FileReference.getSize(): Long = this.bytes()

    override fun createNonImageFile(): FileReference {
        ensureContext()
        val cacheDir = AndroidAppContext.applicationCtx.cacheDir
        val tempFile = File.createTempFile("not-an-image", ".text", cacheDir)
        return FileReference(Uri.fromFile(tempFile))
    }

}
