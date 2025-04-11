package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.div
import com.lightningkite.kiteui.models.plus
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.objc.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.objc.sel_registerName
import platform.posix.QOS_CLASS_DEFAULT
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.max
import kotlin.math.roundToInt
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import platform.darwin.NSObject
import kotlin.compareTo

actual abstract class RawImageViewLike constructor(
    context: RContext,
    actual val source: ImageSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : RView(context){
    actual abstract val state: Readable<Unit>

    protected suspend fun load(value: ImageSource?, size: Size?) = when (value) {
        null -> null
        is ImageRaw -> UIImage(data = value.data.data)
        is ImageResource -> UIImage.imageNamed(value.name)
        is ImageVector -> ImageCache.get(value) { value.render() }
        is ImageRemote -> {
            val loader = suspend {
                inBackground {
                    UIImage(
                        data = NSData.dataWithContentsOfURL(
                            NSURL.URLWithString(value.url)
                                ?: throw IllegalStateException("Invalid URL ${value.url}")
                        ) ?: throw IllegalStateException("No data found at URL ${value.url}")
                    )
                }
            }
            val image = size?.let {
                ImageCache.get(
                    value,
                    it.width.toInt(),
                    it.height.toInt(),
                    loader
                )
            } ?: ImageCache.get(value, load = { loader() })
            image
        }
        is ImageLocal -> {
            val loader = suspend {
                suspendCancellableCoroutine { cont ->
                    loadImageFromProvider(value.file.provider) { data, err ->
                        if (err != null) cont.resumeWithException(Exception(err.description))
                        else if (data is UIImage) {
                            dispatch_async(queue = dispatch_get_main_queue(), block = {
                                val image = data
                                cont.resume(image)
                            })
                        } else {
                            cont.resumeWithException(Exception("No data found for image?  Got $data instead"))
                        }
                    }
                }
            }
            val image = size?.let {
                ImageCache.get(
                    value,
                    it.width.toInt(),
                    it.height.toInt(),
                    loader
                )
            } ?: ImageCache.get(value, load = { loader() })
            image
        }
        else -> null
    }
}

actual class RawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    private val _state = RawReadable<Unit>()
    actual override val state: Readable<Unit> = _state

    override val cannotBeCovered: Boolean get() = false
    override val native = UIImageViewFixedSizing()

    init {
        native.clipsToBounds = true
        native.contentMode = when (scaleType) {
            ImageScaleType.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
            ImageScaleType.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
            ImageScaleType.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
            ImageScaleType.NoScale -> UIViewContentMode.UIViewContentModeCenter
        }
        native.accessibilityLabel = description
        launch {
            delay(10)
            try {
                val img = load(source, native.bounds.useContents { Size(size.width, size.height) })
                _state.state = ReadableState(Unit)
                native.image = img
                native.informParentOfSizeChange()
            } catch(e: Exception) {
                _state.state = ReadableState.exception(e)
            }
        }
    }
}

class UIImageViewFixedSizing(): UIImageView(CGRectZero.readValue()) {

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return this.image?.size?.useContents {
            val original = this
            size.useContents {
                val max = this
                val smallerRatio = (max.width / original.width)
                    .coerceAtMost(max.height / original.height)
                val imageScale = smallerRatio
                    .coerceAtMost(if (naturalSize) 1.0 else (1 / UIScreen.mainScreen.scale))
                CGSizeMake(
                    original.width * imageScale,
                    original.height * imageScale
                )
            }
        } ?: CGSizeMake(0.0, 0.0)
    }

    var naturalSize: Boolean = false
}

actual class RawImageViewZoomable actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    override val cannotBeCovered: Boolean get() = false
    val doubleTapTarget: NSObject = object: NSObject() {
        @ObjCAction
        fun handleDoubleTap(sender: UITapGestureRecognizer) {
            println("Doubletap")
            val midZoom = (native.maximumZoomScale - native.minimumZoomScale) / 2.0 + native.minimumZoomScale
            if (native.zoomScale < midZoom) {
                native.setZoomScale(native.maximumZoomScale, true)
            } else {
                native.setZoomScale(native.minimumZoomScale, true)
            }
        }
    }
    val doubleTapRecognizer = UITapGestureRecognizer(doubleTapTarget, sel_registerName("handleDoubleTap:"))
    override val native = UIScrollView(CGRectZero.readValue()).apply {
        addGestureRecognizer(doubleTapRecognizer)
        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false
        contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        minimumZoomScale = 1.0
        maximumZoomScale = 4.0
        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false

    }
    val imageView = UIImageView(CGRectZero.readValue()).apply {
        contentMode = when (scaleType) {
            ImageScaleType.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
            ImageScaleType.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
            ImageScaleType.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
            ImageScaleType.NoScale -> UIViewContentMode.UIViewContentModeCenter
        }
    }
    val dg: UIScrollViewDelegateProtocol = object: NSObject(), UIScrollViewDelegateProtocol {
        override fun viewForZoomingInScrollView(scrollView: UIScrollView): UIView? {
            return imageView
        }

        override fun scrollViewDidScroll(scrollView: UIScrollView) {
            _zoomState.value = scrollView.zs
        }

        override fun scrollViewDidZoom(scrollView: UIScrollView) {
            _zoomState.value = scrollView.zs
        }
    }
    init {
        val doubleTapRecognizer = UITapGestureRecognizer(this, sel_registerName("handleDoubleTap:"))
        doubleTapRecognizer.numberOfTapsRequired = 2UL
        imageView.translatesAutoresizingMaskIntoConstraints = false
        native.delegate = dg
        native.addSubview(imageView)
        NSLayoutConstraint.activateConstraints(
            listOf(
                imageView.widthAnchor.constraintEqualToAnchor(native.widthAnchor),
                imageView.heightAnchor.constraintEqualToAnchor(native.heightAnchor),
                imageView.centerXAnchor.constraintEqualToAnchor(native.centerXAnchor),
                imageView.centerYAnchor.constraintEqualToAnchor(native.centerYAnchor),
            )
        )
    }
    private val _state = RawReadable<Unit>()
    actual override val state: Readable<Unit> = _state
    private val UIScrollView.zs get() = ZoomState(this.contentOffset, this.zoomScale)

    private val _zoomState = Property<ZoomState>(native.zs)
    actual val zoomState: ImmediateWritable<ZoomState> = _zoomState


    init {
        native.clipsToBounds = true
        native.accessibilityLabel = description
        launch {
            delay(10)
            try {
                val img = load(source, native.bounds.useContents { Size(size.width, size.height) })
                _state.state = ReadableState(Unit)
                imageView.image = img
            } catch(e: Exception) {
                _state.state = ReadableState.exception(e)
            }
        }
    }
}

actual data class ZoomState(val offset: CValue<CGPoint>, val zoom: Double)


object ImageCache {
    val imageCache = NSCache()
    fun get(key: ImageSource): UIImage? = imageCache.objectForKey(key) as? UIImage
    fun set(key: ImageSource, value: UIImage) {
        imageCache.setObject(value, key, value.size.useContents { width * height * 4 }.toULong())
    }

    inline fun get(key: ImageSource, load: () -> UIImage): UIImage {
        (imageCache.objectForKey(key) as? UIImage)?.let { return it }
        val loaded = load()
        imageCache.setObject(loaded, key, loaded.size.useContents { width * height * 4 }.toULong())
        return loaded
    }

    val imageCacheSized = NSCache()
    suspend fun get(key: ImageSource, minWidth: Int, minHeight: Int, load: suspend () -> UIImage): UIImage {
        val sizeKey = Triple(key, minWidth, minHeight)
        (imageCacheSized.objectForKey(sizeKey) as? UIImage)?.let { return it }
        val baseCached = get(key, { load() })
        if (minWidth == 0 || minHeight == 0) return baseCached
        val scaling = max(
            minWidth.toFloat() / baseCached.size.useContents { width },
            minHeight.toFloat() / baseCached.size.useContents { height }
        )
        if (scaling >= 1f) return baseCached
        return inBackground {
            val newWidth = baseCached.size.useContents { width * scaling }.roundToInt().toDouble()
            val newHeight = baseCached.size.useContents { height * scaling }.roundToInt().toDouble()
//        println("Resized image will be ${ "$newWidth x $newHeight" }")
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), true, 0.0)
            val image = try {
                baseCached.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
                UIGraphicsGetImageFromCurrentImageContext()
            } finally {
                UIGraphicsEndImageContext()
            }
            if (image == null) return@inBackground baseCached
//        println("Resized image is be ${image.size.useContents { "$width x $height" }}")
            imageCacheSized.setObject(image, key, image.size.useContents { minWidth * minHeight * 4 }.toULong())
            image
        }
    }
}

internal suspend fun <T> inBackground(action: () -> T): T {
    return suspendCancellableCoroutine<T> { cont ->
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_DEFAULT.toLong(), 0UL)) {
            try {
                val result = action()
                dispatch_async(dispatch_get_main_queue(), { cont.resume(result) })
            } catch (e: Exception) {
                dispatch_async(dispatch_get_main_queue(), { cont.resumeWithException(e) })
            }
        }
    }
}
