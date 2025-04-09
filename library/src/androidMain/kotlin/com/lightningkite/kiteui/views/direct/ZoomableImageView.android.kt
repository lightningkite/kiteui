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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.target.ImageViewTarget
import com.github.chrisbanes.photoview.PhotoView
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.ImageView
import android.widget.ImageView as AImageView


actual class ZoomableImageView actual constructor(context: RContext): RView(context) {
    override val cannotBeCovered: Boolean get() = false
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


    private var previousRequestBuilder: RequestBuilder<Drawable>? = null
    actual var source: ImageSource? = null
        set(value) {
            if((native.context as? Activity)?.isDestroyed == true) return
            if(!animationsEnabled) {
                native.setImageDrawable(null)
                previousRequestBuilder = null
            }
            if (refreshOnParamChange && value is ImageRemote) {
                if (value.url == (field as? ImageRemote)?.url) return
            } else if (value == field) return
            field = value
            val value = source
            fun RequestBuilder<Drawable>.load() {
                var requestOptions = this
                if ((!requestOptions.isTransformationSet
                            && requestOptions.isTransformationAllowed) && native.getScaleType() != null
                ) {
                    when (native.getScaleType()) {
                        AImageView.ScaleType.CENTER_CROP -> requestOptions = requestOptions.clone().optionalCenterCrop()
                        AImageView.ScaleType.CENTER_INSIDE -> requestOptions = requestOptions.clone().optionalCenterInside()
                        AImageView.ScaleType.FIT_CENTER, android.widget.ImageView.ScaleType.FIT_START, android.widget.ImageView.ScaleType.FIT_END -> requestOptions =
                            requestOptions.clone().optionalFitCenter()

                        AImageView.ScaleType.FIT_XY -> requestOptions = requestOptions.clone().optionalCenterInside()
                        AImageView.ScaleType.CENTER, android.widget.ImageView.ScaleType.MATRIX -> {}
                        else -> {}
                    }
                }
                previousRequestBuilder = requestOptions
                requestOptions.into(native)
            }
            fun RequestBuilder<Drawable>.finish() {
                // Instead of directly pulling the old ImageView drawable and using it in the fade, access the drawable via
                // glide using the previous request so as not to circumvent the glide cache and cause resources to be prematurely freed
                val withThumbnailOrPlaceholder = previousRequestBuilder?.let(::thumbnail) ?: placeholder(placeholder)
                withThumbnailOrPlaceholder.transition(withCrossFade(100)).load()
            }
            when (value) {
                is ImageLocal -> Glide.with(native).load(value.file.uri).finish()
                is ImageRaw -> Glide.with(native).load(value.data.data).finish()
                is ImageRemote -> Glide.with(native).load(value.url).finish()
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

    override fun applyTheme(theme: ThemeAndBack) { super.applyTheme(theme); val theme = theme.theme
        (placeholder as? CircularProgressDrawable)?.let {
            it.setColorSchemeColors(
                theme.icon.closestColor().withAlpha(0.5f).colorInt(),
                theme.icon.closestColor().withAlpha(0f).colorInt(),
            )
        }
    }
}
