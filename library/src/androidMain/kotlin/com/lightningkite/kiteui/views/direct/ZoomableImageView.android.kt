package com.lightningkite.kiteui.views.direct

import android.app.Activity
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Path.PathDrawable
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
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
