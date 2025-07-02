package com.lightningkite.kiteui.views.direct

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.View.MeasureSpec
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.github.chrisbanes.photoview.PhotoView
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.Path.PathDrawable
import com.lightningkite.readable.*
import android.widget.ImageView as AImageView

actual abstract class RawImageViewLike constructor(
    context: RContext,
    actual val source: ImageSource,
    actual val description: String,
    actual val scaleType: ImageScaleType,
) : RView(context){
    actual abstract val state: Readable<Unit>
}



actual class RawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    private val _state = RawReadable<Unit>()
    actual override val state: Readable<Unit> = _state
    override val native: GlideImageView = GlideImageView(context.activity)
    init {
        native.scaleType = when (scaleType) {
            ImageScaleType.Fit -> AImageView.ScaleType.FIT_CENTER
            ImageScaleType.Crop -> AImageView.ScaleType.CENTER_CROP
            ImageScaleType.Stretch -> AImageView.ScaleType.FIT_XY
            ImageScaleType.NoScale -> AImageView.ScaleType.CENTER_INSIDE
        }
        native.contentDescription = description
    }
    init {
        fun RequestBuilder<Drawable>.finish() {
            var requestOptions = this
            if ((!requestOptions.isTransformationSet
                        && requestOptions.isTransformationAllowed) && native.getScaleType() != null
            ) {
                when (native.getScaleType()) {
                    ImageView.ScaleType.CENTER_CROP -> requestOptions = requestOptions.clone().optionalCenterCrop()
                    ImageView.ScaleType.CENTER_INSIDE -> requestOptions =
                        requestOptions.clone().optionalCenterInside()

                    ImageView.ScaleType.FIT_CENTER, android.widget.ImageView.ScaleType.FIT_START, android.widget.ImageView.ScaleType.FIT_END -> requestOptions =
                        requestOptions.clone().optionalFitCenter()

                    ImageView.ScaleType.FIT_XY -> requestOptions = requestOptions.clone().optionalCenterInside()
                    ImageView.ScaleType.CENTER, android.widget.ImageView.ScaleType.MATRIX -> {}
                    else -> {}
                }
            }
            requestOptions.listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    p0: GlideException?,
                    p1: Any?,
                    p2: Target<Drawable?>,
                    p3: Boolean
                ): Boolean {
                    _state.state = ReadableState.exception(p0 ?: Exception("Unknown error"))
                    return false
                }

                override fun onResourceReady(
                    p0: Drawable,
                    p1: Any,
                    p2: Target<Drawable?>?,
                    p3: DataSource,
                    p4: Boolean
                ): Boolean {
                    _state.state = ReadableState(Unit)
                    return false
                }
            }).into(native.target)
        }
        when (val value = source) {
            is ImageLocal -> Glide.with(native).load(value.file.uri).finish()
            is ImageRaw -> Glide.with(native).load(value.data.data).finish()
            is ImageRemote -> Glide.with(native).load(value.url).finish()
            is ImageResource -> Glide.with(native).load(value.resource).finish()
            is ImageVector -> {
                native.setImageDrawable(PathDrawable(value))
                _state.state = ReadableState(Unit)
            }
            else -> TODO()
        }
    }


    class GlideImageView(context: Context) : AppCompatImageView(context) {
        var ignoreNaturalSize: Boolean = false
            set(value) {
                field = value
                requestLayout()
            }
        init {
            this.adjustViewBounds = true
            this.clipToOutline = true
        }

        var widthMeasureSpecLast = 0
        var heightMeasureSpecLast = 0
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            if (this !in animatingSize) {
                widthMeasureSpecLast = widthMeasureSpec
                heightMeasureSpecLast = heightMeasureSpec
                if (callbacks.isNotEmpty()) {
                    val width =
                        if (MeasureSpec.getMode(widthMeasureSpecLast) > 0) MeasureSpec.getSize(widthMeasureSpecLast) else AppState.windowInfo.value.width.value.toInt()
                    val height =
                        if (MeasureSpec.getMode(heightMeasureSpecLast) > 0) MeasureSpec.getSize(heightMeasureSpecLast) else AppState.windowInfo.value.height.value.toInt()
                    if (width != 0 && height != 0) {
                        callbacks.toList().forEach { cb -> cb.onSizeReady(width, height) }
                        callbacks.clear()
                    }
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            if(ignoreNaturalSize) setMeasuredDimension(0, 0)
        }

        val callbacks = ArrayList<SizeReadyCallback>()

        val target = object : ImageViewTarget<Drawable>(this) {
            override fun setResource(resource: Drawable?) {
                this@GlideImageView.setImageDrawable(resource)
            }

            @SuppressLint("MissingSuperCall")
            override fun getSize(cb: SizeReadyCallback) {
                if (widthMeasureSpecLast != 0) {
                    val width =
                        if (MeasureSpec.getMode(widthMeasureSpecLast) > 0) MeasureSpec.getSize(widthMeasureSpecLast) else AppState.windowInfo.value.width.value.toInt()
                    val height =
                        if (MeasureSpec.getMode(heightMeasureSpecLast) > 0) MeasureSpec.getSize(heightMeasureSpecLast) else AppState.windowInfo.value.height.value.toInt()
                    if (width != 0 && height != 0) {
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
}


actual class SizelessRawImageView actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    private val _state = RawReadable<Unit>()
    actual override val state: Readable<Unit> = _state
    override val native: GlideImageView = GlideImageView(context.activity)
    init {
        native.scaleType = when (scaleType) {
            ImageScaleType.Fit -> AImageView.ScaleType.FIT_CENTER
            ImageScaleType.Crop -> AImageView.ScaleType.CENTER_CROP
            ImageScaleType.Stretch -> AImageView.ScaleType.FIT_XY
            ImageScaleType.NoScale -> AImageView.ScaleType.CENTER_INSIDE
        }
        native.contentDescription = description
    }
    init {
        fun RequestBuilder<Drawable>.finish() {
            var requestOptions = this
            if ((!requestOptions.isTransformationSet
                        && requestOptions.isTransformationAllowed) && native.getScaleType() != null
            ) {
                when (native.getScaleType()) {
                    ImageView.ScaleType.CENTER_CROP -> requestOptions = requestOptions.clone().optionalCenterCrop()
                    ImageView.ScaleType.CENTER_INSIDE -> requestOptions =
                        requestOptions.clone().optionalCenterInside()

                    ImageView.ScaleType.FIT_CENTER, android.widget.ImageView.ScaleType.FIT_START, android.widget.ImageView.ScaleType.FIT_END -> requestOptions =
                        requestOptions.clone().optionalFitCenter()

                    ImageView.ScaleType.FIT_XY -> requestOptions = requestOptions.clone().optionalCenterInside()
                    ImageView.ScaleType.CENTER, android.widget.ImageView.ScaleType.MATRIX -> {}
                    else -> {}
                }
            }
            requestOptions.listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    p0: GlideException?,
                    p1: Any?,
                    p2: Target<Drawable?>,
                    p3: Boolean
                ): Boolean {
                    _state.state = ReadableState.exception(p0 ?: Exception("Unknown error"))
                    return false
                }

                override fun onResourceReady(
                    p0: Drawable,
                    p1: Any,
                    p2: Target<Drawable?>?,
                    p3: DataSource,
                    p4: Boolean
                ): Boolean {
                    _state.state = ReadableState(Unit)
                    return false
                }
            }).into(native.target)
        }
        when (val value = source) {
            is ImageLocal -> Glide.with(native).load(value.file.uri).finish()
            is ImageRaw -> Glide.with(native).load(value.data.data).finish()
            is ImageRemote -> Glide.with(native).load(value.url).finish()
            is ImageResource -> Glide.with(native).load(value.resource).finish()
            is ImageVector -> native.setImageDrawable(PathDrawable(value))
            else -> TODO()
        }
    }


    class GlideImageView(context: Context) : AppCompatImageView(context) {
        init {
            this.adjustViewBounds = true
            this.clipToOutline = true
        }


        var widthMeasureSpecLast = 0
        var heightMeasureSpecLast = 0
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            if (this !in animatingSize) {
                widthMeasureSpecLast = widthMeasureSpec
                heightMeasureSpecLast = heightMeasureSpec
                if (callbacks.isNotEmpty()) {
                    val width =
                        if (MeasureSpec.getMode(widthMeasureSpecLast) > 0) MeasureSpec.getSize(widthMeasureSpecLast) else AppState.windowInfo.value.width.value.toInt()
                    val height =
                        if (MeasureSpec.getMode(heightMeasureSpecLast) > 0) MeasureSpec.getSize(heightMeasureSpecLast) else AppState.windowInfo.value.height.value.toInt()
                    if (width != 0 && height != 0) {
                        callbacks.toList().forEach { cb -> cb.onSizeReady(width, height) }
                        callbacks.clear()
                    }
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            setMeasuredDimension(
                when(View.MeasureSpec.getMode(widthMeasureSpec)) {
                    MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(widthMeasureSpec)
                    else -> 0
                },
                when(View.MeasureSpec.getMode(heightMeasureSpec)) {
                    MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(heightMeasureSpec)
                    else -> 0
                }
            )
        }

        val callbacks = ArrayList<SizeReadyCallback>()

        val target = object : ImageViewTarget<Drawable>(this) {
            override fun setResource(resource: Drawable?) {
                this@GlideImageView.setImageDrawable(resource)
            }

            @SuppressLint("MissingSuperCall")
            override fun getSize(cb: SizeReadyCallback) {
                if (widthMeasureSpecLast != 0) {
                    val width =
                        if (MeasureSpec.getMode(widthMeasureSpecLast) > 0) MeasureSpec.getSize(widthMeasureSpecLast) else AppState.windowInfo.value.width.value.toInt()
                    val height =
                        if (MeasureSpec.getMode(heightMeasureSpecLast) > 0) MeasureSpec.getSize(heightMeasureSpecLast) else AppState.windowInfo.value.height.value.toInt()
                    if (width != 0 && height != 0) {
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
}


actual class RawImageViewZoomable actual constructor(
    context: RContext,
    source: ImageSource,
    description: String,
    scaleType: ImageScaleType,
) : RawImageViewLike(context, source, description, scaleType) {
    private val _state = RawReadable<Unit>()
    actual override val state: Readable<Unit> = _state
    override val native: PhotoView = PhotoView(context.activity)
    private val _zoomState = Property<ZoomState>(native.imageMatrix)
    actual val zoomState: ImmediateWritable<ZoomState> = _zoomState
    init {
        native.setOnScaleChangeListener { _, _, _ -> zoomState.value = native.imageMatrix }
        native.setOnViewDragListener { _, _ -> zoomState.value = native.imageMatrix }
    }
    init {
        native.scaleType = when (scaleType) {
            ImageScaleType.Fit -> AImageView.ScaleType.FIT_CENTER
            ImageScaleType.Crop -> AImageView.ScaleType.CENTER_CROP
            ImageScaleType.Stretch -> AImageView.ScaleType.FIT_XY
            ImageScaleType.NoScale -> AImageView.ScaleType.CENTER_INSIDE
        }
        native.contentDescription = description
    }
    init {
        fun RequestBuilder<Drawable>.finish() {
            listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    p0: GlideException?,
                    p1: Any?,
                    p2: Target<Drawable?>,
                    p3: Boolean
                ): Boolean {
                    _state.state = ReadableState.exception(p0 ?: Exception("Unknown error"))
                    return false
                }

                override fun onResourceReady(
                    p0: Drawable,
                    p1: Any,
                    p2: Target<Drawable?>?,
                    p3: DataSource,
                    p4: Boolean
                ): Boolean {
                    _state.state = ReadableState(Unit)
                    return false
                }
            }).into(native)
        }
        when (val value = source) {
            is ImageLocal -> Glide.with(native).load(value.file.uri).finish()
            is ImageRaw -> Glide.with(native).load(value.data.data).finish()
            is ImageRemote -> Glide.with(native).load(value.url).finish()
            is ImageResource -> Glide.with(native).load(value.resource).finish()
            is ImageVector -> {
                native.setImageDrawable(PathDrawable(value))
                _state.state = ReadableState(Unit)
            }
            else -> TODO()
        }
    }
}

actual typealias ZoomState = Matrix