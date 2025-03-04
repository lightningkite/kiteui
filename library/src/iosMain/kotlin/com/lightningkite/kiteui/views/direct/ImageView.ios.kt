package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.objc.*
import kotlinx.cinterop.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.max
import kotlin.math.roundToInt
import com.lightningkite.readable.*

actual class ImageView actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    override val native = MyImageView()

    init {
        native.clipsToBounds = true
        native.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    actual var showLoadingIndicator: Boolean = true
        set(value) {
            field = value
            native.useLoadingIndicator = value
        }

    actual var source: ImageSource?
        get() = native.targetSource
        set(value) {
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (native.targetSource as? ImageRemote)?.url) return
            } else if (value == native.targetSource) return
            native.targetSource = value
            if (native.bounds.useContents { size.height } == 0.0) {
                afterTimeout(10) {
                    native.setImageInternal(this, value, native.bounds.useContents { Size(size.width, size.height) })
                }
                return
            }
            native.setImageInternal(this, value, native.bounds.useContents { Size(size.width, size.height) })
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

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        native.loadingIndicator.color = theme.theme.foreground.closestColor().toUiColor()
    }
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

// What should the image cache do?
// - Cache loaded data into the disk
// - Cache loaded data in RAM however much it can
// - Cache sized image in RAM


class MyImageView : UIImageView(CGRectZero.readValue()) {

    var targetSource: ImageSource? = null
    var displayedSource: ImageSource? = null
    var onImageChange: ((UIImage?) -> Unit)? = null

    override fun setImage(image: UIImage?) {
        super.setImage(image)
        onImageChange?.invoke(image)
    }

    val loadingIndicator = UIActivityIndicatorView(CGRectMake(0.0, 0.0, 20.0, 20.0))

    var useLoadingIndicator: Boolean = true
        set(value) {
            field = value
            updateLoadingIndicator()
        }
    init {
        loadingIndicator.hidden = false
        loadingIndicator.startAnimating()
        addSubview(loadingIndicator)
    }

    var image2: UIImage?
        get() = super.image
        set(value) {
            super.image = value
            updateLoadingIndicator()
        }

    fun updateLoadingIndicator() {
        if(image == null && useLoadingIndicator) {
            loadingIndicator.startAnimating()
            loadingIndicator.hidden = false
        } else {
            loadingIndicator.stopAnimating()
            loadingIndicator.hidden = true
        }
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
                println("loadingIndicator.setPsuedoframe " +
                        "${outerSize.width / 2 - mySize.width / 2}, " +
                        "${outerSize.height / 2 - mySize.height / 2}, " +
                        "${mySize.width}, " +
                        "${mySize.height}, " +
                        "")
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

    fun setImageInternal(scope: RView, value: ImageSource?, size: Size?): Unit = with(scope) {
        if (!animationsEnabled) {
            image2 = null
            informParentOfSizeChange()
        }
        when (value) {
            null -> {
                transitionIfAllowed { image2 = null }
                informParentOfSizeChange()
            }

            is ImageRaw -> {
                try {
                    transitionIfAllowed { image2 = UIImage(data = value.data.data) }
                    informParentOfSizeChange()
                } catch (_: Exception) {
                }
            }

            is ImageRemote -> {
                launch {
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
                    if (targetSource == displayedSource) {
                        println("Cancelled ${targetSource}")
                        return@launch
                    }
                    transitionIfAllowed {
                        image2 = image
                        displayedSource = value
                    }
                    informParentOfSizeChange()
                }
            }

            is ImageResource -> {
                transitionIfAllowed {
                    image2 = UIImage.imageNamed(value.name)
                        displayedSource = value
                }
                informParentOfSizeChange()
            }

            is ImageVector -> {
                transitionIfAllowed {
                    image2 = ImageCache.get(value) { value.render() }
                        displayedSource = value
                }
                informParentOfSizeChange()
            }

            is ImageLocal -> {
                launch {
                    val loader = suspend {
                        suspendCancellableCoroutine { cont ->
                            loadImageFromProvider(value.file.provider) { data, err ->
                                if (err != null) cont.resumeWithException(Exception(err.description))
                                else if (data is UIImage) {
                                    dispatch_async(queue = dispatch_get_main_queue(), block = {
                                        val image = data
                                        if (targetSource == displayedSource) return@dispatch_async
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
                    if (targetSource == displayedSource) return@launch
                    transitionIfAllowed {
                        image2 = image
                        displayedSource = value
                    }
                    informParentOfSizeChange()
                }
            }

            else -> {}
        }
    }
}

@OptIn(ExperimentalNativeApi::class)
private fun pz(pziv: WeakReference<PanZoomImageView>): (UIImage?)->Unit {
    return label@{
        val v = pziv.value ?: return@label
        v.setZoomScale(v.minimumZoomScale, false)
    }
}

@OptIn(ExperimentalNativeApi::class)
class PanZoomImageView : UIScrollView(CGRectZero.readValue()), UIScrollViewDelegateProtocol {

    val imageView = MyImageView()

    init {

        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        val weakMe = WeakReference(this)
        imageView.onImageChange = pz(WeakReference(this))
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
    override val cannotBeCovered: Boolean get() = false
    override val native = PanZoomImageView()

    init {
        native.clipsToBounds = true
        native.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
    }

    actual var showLoadingIndicator: Boolean = true
        set(value) {
            field = value
            native.imageView.useLoadingIndicator = value
        }

    actual var source: ImageSource?
        get() = native.imageView.targetSource
        set(value) {
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (native.imageView.targetSource as? ImageRemote)?.url) return
            } else if (value == native.imageView.targetSource) return
            native.imageView.targetSource = value
            if (native.bounds.useContents { size.height } == 0.0) {
                afterTimeout(10) {
                    native.imageView.setImageInternal(this, value, native.bounds.useContents { Size(size.width, size.height) })
                }
                return
            }
            native.imageView.setImageInternal(this, value, native.bounds.useContents { Size(size.width, size.height) })
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

    override fun applyTheme(theme: ThemeAndBack) {
        native.imageView.loadingIndicator.color = theme.theme.foreground.closestColor().toUiColor()
    }
}