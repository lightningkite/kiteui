package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.reactive.sub
import kotlinx.cinterop.*
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue
import platform.darwin.dispatch_get_main_queue
import platform.objc.sel_registerName
import platform.posix.QOS_CLASS_USER_INITIATED
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.max
import kotlin.math.roundToInt

actual class ImageView actual constructor(context: RContext): RView(context) {
    override val native = MyImageView()
    init {
        native.clipsToBounds = true
        native.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    @OptIn(ExperimentalForeignApi::class)
    actual var source: ImageSource?
        get() = native.imageSource
        set(value) {
            if(native.bounds.useContents { size.height } == 0.0) {
                afterTimeout(10) {
                    setImageInternal(value)
                }
                return
            }
            setImageInternal(value)
        }
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun setImageInternal(value: ImageSource?) {
        if (!com.lightningkite.kiteui.views.animationsEnabled) {
            native.image = null
            native.informParentOfSizeChange()
        }
        native.imageSource = value
        when (value) {
            null -> {
                native.animateIfAllowed { native.image = null }
                native.informParentOfSizeChange()
            }

            is ImageRaw -> {
                native.animateIfAllowed { native.image = UIImage(data = value.data.data) }
                native.informParentOfSizeChange()
            }

            is ImageRemote -> {
                native.startLoad()
                calculationContext.sub().launch {
                    val image = ImageCache.get(value, native.bounds.useContents { size.width.toInt() }, native.bounds.useContents { size.height.toInt() }) {
                        inBackground {
                            UIImage(data = NSData.dataWithContentsOfURL(NSURL.URLWithString(value.url) ?: throw IllegalStateException("Invalid URL ${value.url}")) ?: throw IllegalStateException("No data found at URL ${value.url}"))
                        }
                    }
                    if (native.imageSource != value) return@launch
                    native.endLoad()
                    native.animateIfAllowed {
                        native.image = image
                    }
                    native.informParentOfSizeChange()
                }
            }

            is ImageResource -> {
                native.animateIfAllowed { native.image = UIImage.imageNamed(value.name) }
                native.informParentOfSizeChange()
            }

            is ImageVector -> {
                native.animateIfAllowed { native.image = ImageCache.get(value) { value.render() } }
                native.informParentOfSizeChange()
            }

            is ImageLocal -> {
                native.startLoad()
                calculationContext.sub().launch {
                    if (native.imageSource != value) return@launch
                    val image = ImageCache.get(value, native.bounds.useContents { size.width.toInt() }, native.bounds.useContents { size.height.toInt() }) {
                        suspendCoroutineCancellable { cont ->
                            loadImageFromProvider(value.file.provider, ) { data, err ->
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
                    native.animateIfAllowed {
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

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        super.applyBackground(theme, true)
    }
}


@OptIn(ExperimentalForeignApi::class)
object ImageCache {
    val imageCache = NSCache()
    inline fun get(key: ImageSource): UIImage? = imageCache.objectForKey(key) as? UIImage
    inline fun set(key: ImageSource, value: UIImage) {
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
        if(minWidth == 0 || minHeight == 0) return baseCached
        val scaling = max(
            minWidth.toFloat() / baseCached.size.useContents { width } ,
            minHeight.toFloat() / baseCached.size.useContents { height }
        )
        if(scaling >= 1f) return baseCached
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
            if(image == null) return@inBackground baseCached
//        println("Resized image is be ${image.size.useContents { "$width x $height" }}")
            imageCacheSized.setObject(image, key, image.size.useContents { minWidth * minHeight * 4 }.toULong())
            image
        }
    }
}

internal suspend fun <T> inBackground(action: ()->T): T {
    return suspendCoroutineCancellable<T> { cont ->
        dispatch_async(dispatch_get_global_queue(QOS_CLASS_USER_INITIATED.toLong(), 0UL)) {
            try {
                val result = action()
                dispatch_async(dispatch_get_main_queue(), { cont.resume(result) })
            } catch(e: Exception) {
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

@OptIn(ExperimentalForeignApi::class)
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
}

@OptIn(ExperimentalForeignApi::class)
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



actual class ZoomableImageView actual constructor(context: RContext): RView(context) {
    override val native = PanZoomImageView()
    init {
        native.clipsToBounds = true
        native.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    @OptIn(ExperimentalForeignApi::class)
    actual var source: ImageSource?
        get() = native.imageView.imageSource
        set(value) {
            if(native.bounds.useContents { size.height } == 0.0) {
                afterTimeout(10) {
                    setImageInternal(value)
                }
                return
            }
            setImageInternal(value)
        }
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    private fun setImageInternal(value: ImageSource?) {
        if (!com.lightningkite.kiteui.views.animationsEnabled) {
            native.imageView.image = null
            native.informParentOfSizeChange()
        }
        native.imageView.imageSource = value
        when (value) {
            null -> {
                native.animateIfAllowed { native.imageView.image = null }
                native.informParentOfSizeChange()
            }

            is ImageRaw -> {
                native.animateIfAllowed { native.imageView.image = UIImage(data = value.data.data) }
                native.informParentOfSizeChange()
            }

            is ImageRemote -> {
                native.imageView.startLoad()
                calculationContext.sub().launch {
                    val image = ImageCache.get(value, native.bounds.useContents { size.width.toInt() }, native.bounds.useContents { size.height.toInt() }) {
                        inBackground {
                            UIImage(data = NSData.dataWithContentsOfURL(NSURL.URLWithString(value.url) ?: throw IllegalStateException("Invalid URL ${value.url}")) ?: throw IllegalStateException("No data found at URL ${value.url}"))
                        }
                    }
                    if (native.imageView.imageSource != value) return@launch
                    native.imageView.endLoad()
                    native.animateIfAllowed {
                        native.imageView.image = image
                    }
                    native.informParentOfSizeChange()
                }
            }

            is ImageResource -> {
                native.animateIfAllowed { native.imageView.image = UIImage.imageNamed(value.name) }
                native.informParentOfSizeChange()
            }

            is ImageVector -> {
                native.animateIfAllowed { native.imageView.image = ImageCache.get(value) { value.render() } }
                native.informParentOfSizeChange()
            }

            is ImageLocal -> {
                native.imageView.startLoad()
                calculationContext.sub().launch {
                    if (native.imageView.imageSource != value) return@launch
                    val image = ImageCache.get(value, native.bounds.useContents { size.width.toInt() }, native.bounds.useContents { size.height.toInt() }) {
                        suspendCoroutineCancellable { cont ->
                            loadImageFromProvider(value.file.provider, ) { data, err ->
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
                    native.animateIfAllowed {
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
    actual inline var description: String?
        get() = TODO()
        set(value) {
            native.accessibilityLabel = value
        }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        super.applyBackground(theme, true)
    }
}