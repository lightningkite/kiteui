package com.lightningkite.kiteui.views.direct

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Path.PathDrawable
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.github.chrisbanes.photoview.PhotoView
import com.lightningkite.kiteui.views.*
import android.widget.ImageView as AImageView


actual class ZoomableImageView actual constructor(context: RContext): RView(context) {
    override val native = PhotoView(context.activity)
    actual var refreshOnParamChange: Boolean = false
    private var placeholder: Drawable = CircularProgressDrawable(context.activity).apply {
        strokeWidth = 5f
        centerRadius = 30f
        start()
    }
    actual var showLoadingIndicator: Boolean = true
        set(value) {
            field = value
            placeholder = if(value) CircularProgressDrawable(context.activity).apply {
                strokeWidth = 5f
                centerRadius = 30f
                start()
            } else ColorDrawable(Color.TRANSPARENT)
        }

    actual var source: ImageSource? = null
        set(value) {
            if((native.context as? Activity)?.isDestroyed == true) return
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (field as? ImageRemote)?.url) return
            } else if (value == field) return
            fun RequestBuilder<Drawable>.finish() {
                val previous = native.drawable
                println("Loading with existing drawable ${previous}")
                if(previous == null) placeholder(placeholder).transition(withCrossFade(100)).into(native)
                else this.placeholder(previous).transition(withCrossFade(100)).into(native)
            }
            when (value) {
                is ImageLocal -> Glide.with(native).load(value.file.uri).finish()
                is ImageRaw -> Glide.with(native).load(value.data.data).finish()
                is ImageRemote -> Glide.with(native).load(value.url).finish()
                is ImageResource -> Glide.with(native).load(value.resource).into(native)
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

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        (placeholder as? CircularProgressDrawable)?.let {
            it.setColorSchemeColors(
                theme.icon.closestColor().withAlpha(0.5f).colorInt(),
                theme.icon.closestColor().withAlpha(0f).colorInt(),
            )
        }
    }
}
