package com.lightningkite.kiteui.views.direct

import android.app.Activity
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
import androidx.core.view.children
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.views.*
import android.widget.ImageView as AImageView


actual class ZoomableImageView actual constructor(context: RContext): RView(context) {
    override val native = PhotoView(context.activity)
    var placeholder = CircularProgressDrawable(context.activity)
    actual var refreshOnParamChange: Boolean = false

    actual var source: ImageSource? = null
        set(value) {
            if((native.context as? Activity)?.isDestroyed == true) return
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (field as? ImageRemote)?.url) return
            } else if (value == field) return
            println("Loading $value")
            when (value) {
                is ImageLocal -> Glide.with(native).load(value.file.uri).placeholder(placeholder).into(native)
                is ImageRaw -> Glide.with(native).load(value.data).placeholder(placeholder).into(native)
                is ImageRemote -> Glide.with(native).load(value.url).placeholder(placeholder).into(native)
                is ImageResource -> Glide.with(native).load(value.resource).placeholder(placeholder).into(native)
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

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        (placeholder as? CircularProgressDrawable)?.let {
            it.setColorSchemeColors(
                theme.icon.closestColor().withAlpha(0.5f).colorInt(),
                theme.icon.closestColor().withAlpha(0f).colorInt(),
            )
        }
    }
}
