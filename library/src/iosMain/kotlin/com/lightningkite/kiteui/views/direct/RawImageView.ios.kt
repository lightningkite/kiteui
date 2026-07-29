package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.utils.cg
import com.lightningkite.kiteui.utils.div
import com.lightningkite.kiteui.utils.local
import com.lightningkite.kiteui.utils.minus
import com.lightningkite.kiteui.utils.plus
import com.lightningkite.kiteui.utils.times
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.cinterop.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.ImageIO.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.objc.sel_registerName
import platform.posix.QOS_CLASS_DEFAULT

actual abstract class RawImageViewLike constructor(
    context: ElementContext,
    actual val source: ImageSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : NativeElement(context) {
    actual abstract val state: Reactive<Unit>

    protected suspend fun load(value: ImageSource?, size: Size?): UIImage? = value.load(size)

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.clipsToBounds = true
        applyBackgroundChanges(theme)
    }

    override val disableBackground = true
}

// Helper function to create an animated UIImage from data (supports GIF)
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun processImageOrAnimatedImage(data: NSData): UIImage? {
    val source =
            CGImageSourceCreateWithData(interpretCPointer(data.objcPtr()), null)
                    ?: return UIImage.imageWithData(data)

    val frameCount = CGImageSourceGetCount(source).toInt()
    if (frameCount <= 1) {
        // Not animated, return regular image
        return UIImage.imageWithData(data)
    }

    val frames = mutableListOf<UIImage>()
    var totalDuration = 0.0

    for (i in 0 until frameCount) {
        val cgImage = CGImageSourceCreateImageAtIndex(source, i.toULong(), null) ?: continue
        val image = UIImage.imageWithCGImage(cgImage)
        frames.add(image)

        // Get frame duration
        val properties = CGImageSourceCopyPropertiesAtIndex(source, i.toULong(), null) as? Map<*, *>
        val gifProperties = properties?.get(kCGImagePropertyGIFDictionary) as? Map<*, *>
        val frameDuration =
                (gifProperties?.get(kCGImagePropertyGIFDelayTime) as? Double)
                        ?: (gifProperties?.get(kCGImagePropertyGIFUnclampedDelayTime) as? Double)
                                ?: 0.1 // Default to 100ms if not specified
        totalDuration += frameDuration
    }

    if (frames.isEmpty()) {
        return UIImage.imageWithData(data)
    }

    // Create animated image
    return UIImage.animatedImageWithImages(frames, totalDuration)
}

@Suppress("USELESS_CAST")
suspend fun ImageSource?.load(size: Size?): UIImage? =
        when (val value = this) {
            null -> null
            is ImageRaw -> processImageOrAnimatedImage(value.data.data)
            is ImageResource -> UIImage.imageNamed(value.name)
            is ImageVector -> ImageCache.get(value.hashCode().toString()) { value.render() }
            is ImageRemote -> {
                val cacheKey = value.cacheKey
                val loader = suspend {
                    inBackground {
                        val data =
                                NSData.dataWithContentsOfURL(
                                        NSURL.URLWithString(value.url)
                                                ?: throw IllegalStateException(
                                                        "Invalid URL ${value.url}"
                                                )
                                )
                                        ?: throw IllegalStateException(
                                                "No data found at URL ${value.url}"
                                        )
                        processImageOrAnimatedImage(data)
                                ?: throw IllegalStateException(
                                        "Failed to create image from URL ${value.url}"
                                )
                    }
                }
                val image =
                        size?.let {
                            ImageCache.get(cacheKey, it.width.toInt(), it.height.toInt(), loader)
                        }
                                ?: ImageCache.get(cacheKey, load = { loader() })
                image
            }
            is ImageLocal -> {
                val loader = suspend {
                    suspendCancellableCoroutine { cont ->
                        loadImageFromProvider(value.file.provider) { data, err ->
                            if (err != null) cont.resumeWithException(Exception(err.description))
                            else if (data is UIImage) {
                                dispatch_async(
                                        queue = dispatch_get_main_queue(),
                                        block = {
                                            val image = data
                                            cont.resume(image)
                                        }
                                )
                            } else {
                                cont.resumeWithException(
                                        Exception("No data found for image?  Got $data instead")
                                )
                            }
                        }
                    }
                }
                val image =
                        size?.let {
                            ImageCache.get(
                                    value.hashCode().toString(),
                                    it.width.toInt(),
                                    it.height.toInt(),
                                    loader
                            )
                        }
                                ?: ImageCache.get(
                                        value.file.hashCode().toString(),
                                        load = { loader() }
                                )
                image
            }
            else -> null
        }

actual class RawImageView actual constructor(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    private val _state = RawReactive<Unit>()
    actual override val state: Reactive<Unit> = _state

    override val native = UIImageViewFixedSizing()

    init {
        native.contentMode =
                when (scaleType) {
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
                _state.state = ReactiveState(Unit)
                native.image = img
                // Start animating if this is an animated image (e.g., GIF)
                if (img?.images != null) {
                    native.startAnimating()
                }
                native.informParentOfSizeChange()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.state = ReactiveState.exception(e)
            }
        }
    }
}

actual class SizelessRawImageView actual constructor(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    private val _state = RawReactive<Unit>()
    actual override val state: Reactive<Unit> = _state

    override val native = UIImageViewFixedSizing().also { it.ignoreNaturalSize = true }

    init {
        native.contentMode =
                when (scaleType) {
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
                _state.state = ReactiveState(Unit)
                native.image = img
                // Start animating if this is an animated image (e.g., GIF)
                if (img?.images != null) {
                    native.startAnimating()
                }
                native.informParentOfSizeChange()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.state = ReactiveState.exception(e)
            }
        }
    }
}

class UIImageViewFixedSizing() : UIImageView(CGRectZero.readValue()) {
    var ignoreNaturalSize: Boolean = false
        set(value) {
            field = value
            informParentOfSizeChange()
        }

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        if (ignoreNaturalSize) return CGSizeMake(0.0, 0.0)
        return this.image?.size?.useContents {
            val original = this
            size.useContents {
                val max = this
                val smallerRatio =
                        (max.width / original.width).coerceAtMost(max.height / original.height)
                val imageScale =
                        smallerRatio.coerceAtMost(
                                if (naturalSize) 1.0 else (1 / UIScreen.mainScreen.scale)
                        )
                CGSizeMake(original.width * imageScale, original.height * imageScale)
            }
        }
                ?: CGSizeMake(0.0, 0.0)
    }

    var naturalSize: Boolean = false
}

actual class RawImageViewZoomable actual constructor(
    context: ElementContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {

    val doubleTapTarget: NSObject =
            object : NSObject() {
                @ObjCAction
                fun handleDoubleTap(sender: UITapGestureRecognizer) {
                    if (sender.state != UIGestureRecognizerStateEnded) return

                    val midZoom =
                            (native.maximumZoomScale - native.minimumZoomScale) / 2.0 +
                                    native.minimumZoomScale
                    if (native.zoomScale < midZoom) {
                        val touch = sender.locationInView(native).local
                        val origin = native.frame.useContents { size.width / 2 to size.height / 2 }

                        val offset = touch - origin
                        val scaledOffset =
                                (native.frame.useContents { size.width to size.height } *
                                        (native.maximumZoomScale - 1)) / 2
                        val newContentOffset = scaledOffset + offset * native.maximumZoomScale

                        UIView.animateWithDuration(0.3) {
                            native.setZoomScale(native.maximumZoomScale)
                            native.setContentOffset(newContentOffset.cg)
                        }
                    } else {
                        native.setZoomScale(native.minimumZoomScale, true)
                    }
                }
            }
    val doubleTapRecognizer =
            UITapGestureRecognizer(doubleTapTarget, sel_registerName("handleDoubleTap:")).apply {
                numberOfTapsRequired = 2UL
            }
    override val native =
            UIScrollView(CGRectZero.readValue()).apply {
                addGestureRecognizer(doubleTapRecognizer)
                showsHorizontalScrollIndicator = false
                showsVerticalScrollIndicator = false
                contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
                minimumZoomScale = 1.0
                maximumZoomScale = 4.0
                showsHorizontalScrollIndicator = false
                showsVerticalScrollIndicator = false
            }
    val imageView =
            UIImageView(CGRectZero.readValue()).apply {
                contentMode =
                        when (scaleType) {
                            ImageScaleType.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
                            ImageScaleType.Crop ->
                                    UIViewContentMode.UIViewContentModeScaleAspectFill
                            ImageScaleType.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
                            ImageScaleType.NoScale -> UIViewContentMode.UIViewContentModeCenter
                        }
            }
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    val dg: UIScrollViewDelegateProtocol = run {
        // Use weak reference to avoid retain cycle
        val weakSelf = kotlin.native.ref.WeakReference(this)
        object : NSObject(), UIScrollViewDelegateProtocol {
            override fun viewForZoomingInScrollView(scrollView: UIScrollView): UIView? {
                return imageView
            }

            override fun scrollViewDidScroll(scrollView: UIScrollView) {
                weakSelf.get()?._zoomState?.value = scrollView.zs
            }

            override fun scrollViewDidZoom(scrollView: UIScrollView) {
                weakSelf.get()?._zoomState?.value = scrollView.zs
            }
        }
    }
    init {
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
    private val _state = RawReactive<Unit>()
    actual override val state: Reactive<Unit> = _state
    private val UIScrollView.zs
        get() = ZoomState(this.contentOffset, this.zoomScale)

    private val _zoomState = Signal<ZoomState>(native.zs)
    actual val zoomState: MutableReactiveValue<ZoomState> = _zoomState

    init {
        native.clipsToBounds = true
        native.accessibilityLabel = description
        launch {
            delay(10)
            try {
                val img = load(source, native.bounds.useContents { Size(size.width, size.height) })
                _state.state = ReactiveState(Unit)
                imageView.image = img
                // Start animating if this is an animated image (e.g., GIF)
                if (img?.images != null) {
                    imageView.startAnimating()
                }
            } catch (e: Exception) {
                _state.state = ReactiveState.exception(e)
            }
        }
    }
}

actual data class ZoomState(val offset: CValue<CGPoint>, val zoom: Double)

object ImageCache {
    val imageCache = NSCache()
    fun get(key: String): UIImage? = imageCache.objectForKey(key) as? UIImage
    fun set(key: String, value: UIImage) {
        imageCache.setObject(value, key, value.size.useContents { width * height * 4 }.toULong())
    }

    inline fun get(key: String, load: () -> UIImage): UIImage {
        (imageCache.objectForKey(key) as? UIImage)?.let {
            return it
        }
        val loaded = load()
        imageCache.setObject(loaded, key, loaded.size.useContents { width * height * 4 }.toULong())
        return loaded
    }

    val imageCacheSized = NSCache()
    suspend fun get(key: String, minWidth: Int, minHeight: Int, load: suspend () -> UIImage): UIImage {
        val sizeKey = "$key//$minWidth//$minHeight"
        (imageCacheSized.objectForKey(sizeKey) as? UIImage)?.let {
            return it
        }
        val baseCached = get(key, {
            load()
        })
        if (minWidth == 0 || minHeight == 0) return baseCached
        // Don't resize animated images - resizing would lose the animation frames
        if (baseCached.images != null) return baseCached
        val scaling =
                max(
                        minWidth.toFloat() / baseCached.size.useContents { width },
                        minHeight.toFloat() / baseCached.size.useContents { height }
                )
        if (scaling >= 1f) return baseCached
        return inBackground {
            val newWidth = baseCached.size.useContents { width * scaling }.roundToInt().toDouble()
            val newHeight = baseCached.size.useContents { height * scaling }.roundToInt().toDouble()
            UIGraphicsBeginImageContextWithOptions(CGSizeMake(newWidth, newHeight), false, 0.0)
            val image =
                    try {
                        baseCached.drawInRect(CGRectMake(0.0, 0.0, newWidth, newHeight))
                        UIGraphicsGetImageFromCurrentImageContext()
                    } finally {
                        UIGraphicsEndImageContext()
                    }
            if (image == null) return@inBackground baseCached
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