package com.lightningkite.kiteui.views.direct

import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Path.PathDrawable
import timber.log.Timber
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.Window
import android.widget.FrameLayout
import androidx.annotation.Nullable
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.children
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.views.*
import com.ortiz.touchview.TouchImageView
import android.widget.ImageView as AImageView

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NImageView = AppCompatImageView

actual var ImageView.source: ImageSource?
    get() = native.tag as? ImageSource
    set(value) {
        if(value == native.tag) {
            println("$this: Skip loading of $value vs ${native.tag}")
            return
        }
        println("$this: Loading $value vs ${native.tag}")
        native.tag = value
        @Suppress("KotlinConstantConditions")
        if(native is TouchImageView) {
            fun target() = object: SimpleTarget<Drawable>() {
                override fun onResourceReady(p0: Drawable, p1: Transition<in Drawable>?) {
                    native.setImageDrawable(p0)
                }
            }
            when(value) {
                is ImageLocal -> Glide.with(native).load(value.file.uri).into(target())
                is ImageRaw -> Glide.with(native).load(value.data).into(target())
                is ImageRemote -> Glide.with(native).load(value.url).into(target())
                is ImageResource -> Glide.with(native).load(value.resource).into(target())
                is ImageVector -> native.setImageDrawable(PathDrawable(value))
                null -> native.setImageDrawable(null)
                else -> TODO()
            }
        } else {
            when(value) {
                is ImageLocal -> Glide.with(native).load(value.file.uri).into(native)
                is ImageRaw -> Glide.with(native).load(value.data).into(native)
                is ImageRemote -> Glide.with(native).load(value.url).into(native)
                is ImageResource -> Glide.with(native).load(value.resource).into(native)
                is ImageVector -> native.setImageDrawable(PathDrawable(value))
                null -> native.setImageDrawable(null)
                else -> TODO()
            }
        }
    }
actual var ImageView.scaleType: ImageScaleType
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
actual var ImageView.description: String?
    get() {
        return native.contentDescription.toString()
    }
    set(value) {
        native.contentDescription = value
    }
actual var ImageView.naturalSize: Boolean
    get() = false
    set(value) {}

@ViewDsl
actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit) {
    return viewElement(factory = ::NImageView, wrapper = ::ImageView) {
        native.clipToOutline = true
        handleTheme(native, viewDraws = true, viewLoads = true) {
            setup(this)
        }
    }
}


@ViewDsl
actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) {
    return viewElement(::TouchImageView, wrapper = ::ImageView){
        native.clipToOutline = true
        handleTheme(native, viewDraws = true) {
            setup(this)
        }
    }
}
