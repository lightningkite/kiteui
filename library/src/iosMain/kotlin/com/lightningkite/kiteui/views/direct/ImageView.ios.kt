package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.objc.*
import kotlinx.cinterop.*
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.objc.sel_registerName
import platform.posix.QOS_CLASS_DEFAULT
import platform.posix.QOS_CLASS_USER_INITIATED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

actual class ImageView actual constructor(context: RContext) : RView(context) {
    override val native = MyImageView()

    init {
        native.clipsToBounds = true
        native.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    
    actual var source: ImageSource?
        get() = native.imageSource
        set(value) {
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (native.imageSource as? ImageRemote)?.url) return
            } else if (value == native.imageSource) return
            if (native.bounds.useContents { size.height } == 0.0) {
                afterTimeout(10) {
                    setImageInternal(value)
                }
                return
            }
            setImageInternal(value)
        }

    
    private fun setImageInternal(value: ImageSource?) {
        if (!com.lightningkite.kiteui.views.animationsEnabled) {
            native.image = null
            native.informParentOfSizeChange()
        }
        native.imageSource = value
        when (value) {
            null -> {
                animateIfAllowed { native.image = null }
                native.informParentOfSizeChange()
            }

            is ImageRaw -> {
                try {
                    animateIfAllowed { native.image = UIImage(data = value.data.data) }
                    native.informParentOfSizeChange()
                } catch (_: Exception) {
                }
            }

            is ImageRemote -> {
                native.startLoad()
                launch {
                    val image = ImageCache.get(
                        value,
                        native.bounds.useContents { size.width.toInt() },
                        native.bounds.useContents { size.height.toInt() }) {
                        inBackground {
                            UIImage(
                                data = NSData.dataWithContentsOfURL(
                                    NSURL.URLWithString(value.url)
                                        ?: throw IllegalStateException("Invalid URL ${value.url}")
                                ) ?: throw IllegalStateException("No data found at URL ${value.url}")
                            )
                        }
                    }
                    if (native.imageSource != value) return@launch
                    native.endLoad()
                    animateIfAllowed {
                        native.image = image
                    }
                    native.informParentOfSizeChange()
                }
            }

            is ImageResource -> {
                animateIfAllowed { native.image = UIImage.imageNamed(value.name) }
                native.informParentOfSizeChange()
            }

            is ImageVector -> {
                animateIfAllowed { native.image = ImageCache.get(value) { value.render() } }
                native.informParentOfSizeChange()
            }

            is ImageLocal -> {
                native.startLoad()
                launch {
                    if (native.imageSource != value) return@launch
                    val image = ImageCache.get(
                        value,
                        native.bounds.useContents { size.width.toInt() },
                        native.bounds.useContents { size.height.toInt() }) {
                        suspendCoroutineCancellable { cont ->
                            loadImageFromProvider(value.file.provider) { data, err ->
                                if (err != null) cont.resumeWithException(Exception(err.description))
                                else if (data is UIImage) {
                                    dispatch_async(queue = dispatch_get_main_queue(), block = {
                                        val image = data
                                        if (native.imageSource != value) return@dispatch_async
                                        cont.resume(image)
                                    })
                                } else {
                                    cont.resumeWithException(Exception("No data found for image?  Got $data instead"))
                                }
                            }
                            return@suspendCoroutineCancellable {}
                        }
                    }
                    native.endLoad()
                    animateIfAllowed {
                        native.image = image
                    }
                    native.informParentOfSizeChange()
                }
            }

            else -> {}
        }
    }

    actual inline var scaleType: ImageScaleType
        get() = TODO()
        set(value) {
            native.contentMode = when (value) {
                ImageScaleType.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
                ImageScaleType.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
                ImageScaleType.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
                ImageScaleType.NoScale -> UIViewContentMode.UIViewContentModeCenter
            }
        }
    actual inline var description: String?
        get() = TODO()
        set(value) {
            native.accessibilityLabel = value
        }

    actual var refreshOnParamChange: Boolean = false

    /**
     * When true, images are dimensioned according to the platform logical coordinate space as opposed to the physical
     * coordinate space. This will cause images to appear closer to their natural size on supported platforms with high
     * density screens.
     */
    actual var naturalSize: Boolean by native::naturalSize
}



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
    return suspendCoroutineCancellable<T> { cont ->
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_DEFAULT.toLong(), 0UL)) {
            try {
                val result = action()
                dispatch_async(dispatch_get_main_queue(), { cont.resume(result) })
            } catch (e: Exception) {
                dispatch_async(dispatch_get_main_queue(), { cont.resumeWithException(e) })
            }
        }
        return@suspendCoroutineCancellable {}
    }
}

// What should the image cache do?
// - Cache loaded data into the disk
// - Cache loaded data in RAM however much it can
// - Cache sized image in RAM


class MyImageView : UIImageView(CGRectZero.readValue()) {

    var imageSource: ImageSource? = null
    var onImageChange: ((UIImage?) -> Unit)? = null

    override fun setImage(image: UIImage?) {
        super.setImage(image)
        onImageChange?.invoke(image)
    }

    val loadingIndicator = UIActivityIndicatorView(CGRectMake(0.0, 0.0, 0.0, 0.0))

    init {
        loadingIndicator.hidden = true
        addSubview(loadingIndicator)
    }

    fun startLoad() {
        loadingIndicator.startAnimating()
        loadingIndicator.hidden = false
    }

    fun endLoad() {
        loadingIndicator.stopAnimating()
        loadingIndicator.hidden = true
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        bounds.useContents {
            val outerSize = this.size
            loadingIndicator.bounds.useContents {
                val mySize = this.size
                loadingIndicator.setPsuedoframe(
                    outerSize.width / 2 - mySize.width / 2,
                    outerSize.height / 2 - mySize.height / 2,
                    mySize.width,
                    mySize.height
                )
            }
        }
    }

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


class PanZoomImageView : UIScrollView(CGRectZero.readValue()), UIScrollViewDelegateProtocol {

    val imageView = MyImageView()

    init {

        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        imageView.onImageChange = {
            setZoomScale(minimumZoomScale, false)
        }
        addSubview(imageView)

        NSLayoutConstraint.activateConstraints(
            listOf(
                imageView.widthAnchor.constraintEqualToAnchor(widthAnchor),
                imageView.heightAnchor.constraintEqualToAnchor(heightAnchor),
                imageView.centerXAnchor.constraintEqualToAnchor(centerXAnchor),
                imageView.centerYAnchor.constraintEqualToAnchor(centerYAnchor),
            )
        )


        val doubleTapRecognizer = UITapGestureRecognizer(this, sel_registerName("handleDoubleTap:"))
        doubleTapRecognizer.numberOfTapsRequired = 2UL
        addGestureRecognizer(doubleTapRecognizer)

        minimumZoomScale = 1.0
        maximumZoomScale = 4.0
        showsHorizontalScrollIndicator = false
        showsVerticalScrollIndicator = false

        delegate = this
    }

    override fun viewForZoomingInScrollView(scrollView: UIScrollView): UIView? {
        return imageView
    }

    @ObjCAction
    fun handleDoubleTap(sender: UITapGestureRecognizer) {
        val midZoom = (maximumZoomScale - minimumZoomScale) / 2.0 + minimumZoomScale
        if (zoomScale < midZoom) {
            setZoomScale(maximumZoomScale, true)
        } else {
            setZoomScale(minimumZoomScale, true)
        }
    }

}


actual class ZoomableImageView actual constructor(context: RContext) : RView(context) {
    override val native = PanZoomImageView()

    init {
        native.clipsToBounds = true
        native.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    
    actual var source: ImageSource? = null
        set(value) {
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (field as? ImageRemote)?.url) return
            } else if (value == field) return
            field = value
            if (native.bounds.useContents { size.height } == 0.0) {
                afterTimeout(10) {
                    setImageInternal(value)
                }
                return
            }
            setImageInternal(value)
        }

    
    private fun setImageInternal(value: ImageSource?) {
        if (!com.lightningkite.kiteui.views.animationsEnabled) {
            native.imageView.image = null
            native.informParentOfSizeChange()
        }
        native.imageView.imageSource = value
        when (value) {
            null -> {
                animateIfAllowed { native.imageView.image = null }
                native.informParentOfSizeChange()
            }

            is ImageRaw -> {
                animateIfAllowed { native.imageView.image = UIImage(data = value.data.data) }
                native.informParentOfSizeChange()
            }

            is ImageRemote -> {
                native.imageView.startLoad()
                launch {
                    val image = ImageCache.get(
                        value,
                        // TODO: Cyclical requirement for size!!!
                        native.bounds.useContents { size.width.toInt() },
                        native.bounds.useContents { size.height.toInt() }) {
                        inBackground {
                            UIImage(
                                data = NSData.dataWithContentsOfURL(
                                    NSURL.URLWithString(value.url)
                                        ?: throw IllegalStateException("Invalid URL ${value.url}")
                                ) ?: throw IllegalStateException("No data found at URL ${value.url}")
                            )
                        }
                    }
                    if (native.imageView.imageSource != value) return@launch
                    native.imageView.endLoad()
                    animateIfAllowed {
                        native.imageView.image = image
                    }
                    native.informParentOfSizeChange()
                }
            }

            is ImageResource -> {
                animateIfAllowed { native.imageView.image = UIImage.imageNamed(value.name) }
                native.informParentOfSizeChange()
            }

            is ImageVector -> {
                animateIfAllowed { native.imageView.image = ImageCache.get(value) { value.render() } }
                native.informParentOfSizeChange()
            }

            is ImageLocal -> {
                native.imageView.startLoad()
                launch {
                    if (native.imageView.imageSource != value) return@launch
                    val image = ImageCache.get(
                        value,
                        native.bounds.useContents { size.width.toInt() },
                        native.bounds.useContents { size.height.toInt() }) {
                        suspendCoroutineCancellable { cont ->
                            loadImageFromProvider(value.file.provider) { data, err ->
                                if (err != null) cont.resumeWithException(Exception(err.description))
                                else if (data is UIImage) {
                                    dispatch_async(queue = dispatch_get_main_queue(), block = {
                                        val image = data
                                        if (native.imageView.imageSource != value) return@dispatch_async
                                        cont.resume(image)
                                    })
                                } else {
                                    cont.resumeWithException(Exception("No data found for image?  Got $data instead"))
                                }
                            }
                            return@suspendCoroutineCancellable {}
                        }
                    }
                    native.imageView.endLoad()
                    animateIfAllowed {
                        native.imageView.image = image
                    }
                    native.informParentOfSizeChange()
                }
            }

            else -> {}
        }
    }

    actual inline var scaleType: ImageScaleType
        get() = TODO()
        set(value) {
            native.contentMode = when (value) {
                ImageScaleType.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
                ImageScaleType.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
                ImageScaleType.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
                ImageScaleType.NoScale -> UIViewContentMode.UIViewContentModeCenter
            }
        }
    actual var refreshOnParamChange: Boolean = false
    actual inline var description: String?
        get() = TODO()
        set(value) {
            native.accessibilityLabel = value
        }
}