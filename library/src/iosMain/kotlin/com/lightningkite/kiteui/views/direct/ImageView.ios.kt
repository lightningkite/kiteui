package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSCache
import platform.UIKit.*
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.loadDataRepresentationForContentType
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.objc.sel_registerName

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NImageView = MyImageView

@ViewDsl
actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit): Unit = element(NImageView()) {
    handleTheme(this, viewDraws = true, viewLoads = false,) {
        clipsToBounds = true
        this.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        setup(ImageView(this))
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
}

actual inline var ImageView.source: ImageSource?
    get() = native.imageSource
    set(value) {
        if (!animationsEnabled) {
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
                animateIfAllowed { native.image = UIImage(data = value.data.data) }
                native.informParentOfSizeChange()
            }

            is ImageRemote -> {
                native.startLoad()
                launch {
                    if (native.imageSource != value) return@launch
                    val image = ImageCache.get(value) { UIImage(data = fetch(value.url).blob().data) }
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
                ImageCache.get(value)?.let {
                    animateIfAllowed { native.image = it }
                    native.informParentOfSizeChange()
                } ?: run {
                    native.startLoad()
                    value.file.provider.loadDataRepresentationForContentType(
                        value.file.suggestedType ?: UTTypeImage
                    ) { data, err ->
                        if (data != null) {
                            dispatch_async(queue = dispatch_get_main_queue(), block = {
                                native.endLoad()
                                val image = UIImage(data = data)
                                ImageCache.set(value, image)
                                if (native.imageSource != value) return@dispatch_async
                                animateIfAllowed { native.image = image }
                                native.informParentOfSizeChange()
                            })
                        }
                    }
                }
            }

            else -> {}
        }
    }
actual inline var ImageView.scaleType: ImageScaleType
    get() = TODO()
    set(value) {
        native.contentMode = when (value) {
            ImageScaleType.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
            ImageScaleType.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
            ImageScaleType.Stretch -> UIViewContentMode.UIViewContentModeScaleToFill
            ImageScaleType.NoScale -> UIViewContentMode.UIViewContentModeCenter
        }
    }
actual inline var ImageView.description: String?
    get() = TODO()
    set(value) {
        native.accessibilityLabel = value
    }

@ViewDsl
actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) =
    element(PanZoomImageView()) {
        handleTheme(this, viewDraws = true,) {
            setup(ImageView(imageView))
            imageView.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFit
        }
    }

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
                loadingIndicator.setFrame(
                    CGRectMake(
                        outerSize.width / 2 - mySize.width / 2,
                        outerSize.height / 2 - mySize.height / 2,
                        mySize.width,
                        mySize.height
                    )
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
        calculationContext.onRemove { imageView.onImageChange = null }
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
