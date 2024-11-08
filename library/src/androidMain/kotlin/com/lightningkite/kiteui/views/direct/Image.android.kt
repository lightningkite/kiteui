package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.Path.PathDrawable
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import android.widget.ImageView as AImageView


actual class ImageView actual constructor(context: RContext) : RView(context) {
    override val native = Custom(context.activity)
    class Custom(context: Context): androidx.appcompat.widget.AppCompatImageView(context) {
        init {
            this.adjustViewBounds = true
            this.clipToOutline = true
        }
        var widthMeasureSpecLast = 0
        var heightMeasureSpecLast = 0
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            if(this !in animatingSize) {
                widthMeasureSpecLast = widthMeasureSpec
                heightMeasureSpecLast = heightMeasureSpec
//                if(this ==  viewDebugTarget?.native){
//                    println("widthMeasureSpec ${widthMeasureSpec},  MeasureSpec.getMode( ${
//                        MeasureSpec.getMode(heightMeasureSpec)}")
//                }

                    if (callbacks.isNotEmpty()) {
                    val width = if (MeasureSpec.getMode(widthMeasureSpecLast) > 0) MeasureSpec.getSize(widthMeasureSpecLast) else AppState.windowInfo.value.width.value.toInt()
                    val height = if (MeasureSpec.getMode(heightMeasureSpecLast) > 0) MeasureSpec.getSize(heightMeasureSpecLast) else AppState.windowInfo.value.height.value.toInt()
//                    if(this ==  viewDebugTarget?.native){
//                        println(" MeasureSpec.getMode(widthMeasureSpecLast): ${MeasureSpec.getMode(widthMeasureSpecLast)}, MeasureSpec.getMode(heightMeasureSpecLast): ${MeasureSpec.getMode(heightMeasureSpecLast)}")
//                    }

                        if (width != 0 && height != 0) {
                        callbacks.toList().forEach { cb -> cb.onSizeReady(width, height) }
                        callbacks.clear()
                    }
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
        val callbacks = ArrayList<SizeReadyCallback>()

        val target = object: ImageViewTarget<Drawable>(this) {
            override fun setResource(resource: Drawable?) {
                this@Custom.setImageDrawable(resource)
            }

            @SuppressLint("MissingSuperCall")
            override fun getSize(cb: SizeReadyCallback) {
                if(widthMeasureSpecLast != 0) {
                    val width = if (MeasureSpec.getMode(widthMeasureSpecLast) > 0 ) MeasureSpec.getSize(widthMeasureSpecLast) else AppState.windowInfo.value.width.value.toInt()
                    val height = if (MeasureSpec.getMode(heightMeasureSpecLast) > 0 ) MeasureSpec.getSize(heightMeasureSpecLast) else AppState.windowInfo.value.height.value.toInt()
                    if(width != 0 && height != 0) {
                        cb.onSizeReady(width, height)
                    }
                } else {
                    callbacks.add(cb)
                }
            }

            @SuppressLint("MissingSuperCall")
            override fun removeCallback(cb: SizeReadyCallback) {
                callbacks.remove(cb)
            }

            @SuppressLint("MissingSuperCall")
            override fun onLoadCleared(placeholder: Drawable?) {
                callbacks.clear()
            }
        }
    }

    //    var placeholder = ColorDrawable(0xFFFF0000.toInt())
    var placeholder = CircularProgressDrawable(context.activity).apply {
        strokeWidth = 5f
        centerRadius = 30f
        start()
    }
    actual var useLoadingSpinners: Boolean = true

    private fun RequestBuilder<Drawable>.placeholderIf(drawable: Drawable, predicate: Boolean) =
        if (predicate) placeholder(drawable) else this

    actual var source: ImageSource? = null
        set(value) {
            if((native.context as? Activity)?.isDestroyed == true) return
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (field as? ImageRemote)?.url) return
            } else if (value == field) return
            field = value
            reload()
        }

    private fun reload() {
        val value = source
        fun RequestBuilder<Drawable>.load() {
            var requestOptions = this
            if ((!requestOptions.isTransformationSet
                        && requestOptions.isTransformationAllowed) && native.getScaleType() != null
            ) {
                when (native.getScaleType()) {
                    ImageView.ScaleType.CENTER_CROP -> requestOptions = requestOptions.clone().optionalCenterCrop()
                    ImageView.ScaleType.CENTER_INSIDE -> requestOptions = requestOptions.clone().optionalCenterInside()
                    ImageView.ScaleType.FIT_CENTER, android.widget.ImageView.ScaleType.FIT_START, android.widget.ImageView.ScaleType.FIT_END -> requestOptions =
                        requestOptions.clone().optionalFitCenter()

                    ImageView.ScaleType.FIT_XY -> requestOptions = requestOptions.clone().optionalCenterInside()
                    ImageView.ScaleType.CENTER, android.widget.ImageView.ScaleType.MATRIX -> {}
                    else -> {}
                }
            }
            requestOptions.into(native.target)
        }
        when (value) {
            is ImageLocal -> Glide.with(native).load(value.file.uri).placeholderIf(placeholder, useLoadingSpinners).load()
            is ImageRaw -> Glide.with(native).load(value.data.data).placeholderIf(placeholder, useLoadingSpinners).load()
            is ImageRemote -> Glide.with(native).load(value.url).placeholderIf(placeholder, useLoadingSpinners).load()
            is ImageResource -> Glide.with(native).load(value.resource).load()
            is ImageVector -> native.setImageDrawable(PathDrawable(value))
            null -> native.setImageDrawable(null)
            else -> TODO()
        }
    }

    actual var scaleType: ImageScaleType
        get() {
            return when (this.native.scaleType) {
                AImageView.ScaleType.MATRIX -> ImageScaleType.NoScale
                AImageView.ScaleType.FIT_XY -> ImageScaleType.Stretch
                AImageView.ScaleType.FIT_START -> ImageScaleType.Fit
                AImageView.ScaleType.FIT_CENTER -> ImageScaleType.Fit
                AImageView.ScaleType.FIT_END -> ImageScaleType.Fit
                AImageView.ScaleType.CENTER -> ImageScaleType.Fit
                AImageView.ScaleType.CENTER_CROP -> ImageScaleType.Crop
                AImageView.ScaleType.CENTER_INSIDE -> ImageScaleType.NoScale
                else -> ImageScaleType.Fit
            }
        }
        set(value) {
            val scaleType: AImageView.ScaleType = when (value) {
                ImageScaleType.Fit -> AImageView.ScaleType.FIT_CENTER
                ImageScaleType.Crop -> AImageView.ScaleType.CENTER_CROP
                ImageScaleType.Stretch -> AImageView.ScaleType.FIT_XY
                ImageScaleType.NoScale -> AImageView.ScaleType.CENTER_INSIDE
            }
            this.native.scaleType = scaleType
        }
    actual var description: String?
        get() {
            return native.contentDescription.toString()
        }
        set(value) {
            native.contentDescription = value
        }

    actual var refreshOnParamChange: Boolean = false

    /**
     * When true, images are dimensioned according to the platform logical coordinate space as opposed to the physical
     * coordinate space. This will cause images to appear closer to their natural size on supported platforms with high
     * density screens.
     */
    actual var naturalSize: Boolean = true

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        (placeholder as? CircularProgressDrawable)?.let {
            it.setColorSchemeColors(
                theme.icon.closestColor().colorInt(),
//                theme.icon.closestColor().withAlpha(0.5f).colorInt(),
//                theme.icon.closestColor().withAlpha(0f).colorInt(),
            )
        }
    }
    override fun applyBackground(theme: Theme, fullyApply: Boolean) = applyBackgroundWithClipping(theme, fullyApply)
}

//@ViewDsl
//actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit) {
//    return viewElement(factory = ::NImageView, wrapper = ::ImageView) {
//        native.clipToOutline = true
//        handleTheme(native, viewDraws = true, viewLoads = true) {
//            setup(this)
//        }
//    }
//}
//
//
//@ViewDsl
//actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) {
//    return viewElement(::TouchImageView, wrapper = ::ImageView){
//        native.clipToOutline = true
//        handleTheme(native, viewDraws = true) {
//            setup(this)
//        }
//    }
//}
