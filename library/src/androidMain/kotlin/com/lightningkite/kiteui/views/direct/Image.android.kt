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
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.views.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import android.widget.ImageView as AImageView

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NImageView = TransitionImageView

actual var ImageView.source: ImageSource?
    get() = TODO()
    set(value) {
        println("$this: Cleared")
        native.tag = value
        if(!animationsEnabled) native.removeAllViews()
        if(!animationsEnabled) native.ignoreAnimationsUntil = System.currentTimeMillis() + 16
        val h = native.lparams.height.coerceAtLeast(0).let { if(it == 0) WindowInfo.value.height.value.toInt() else it }
        when (value) {
            null -> {
                native.transition(null)
            }
            is ImageRaw -> {
                val imageData = value.data.data
                native.transition {
                    try {
                        setImageDrawable(
                            BitmapDrawable(
                                native.resources,
                                BitmapFactory.decodeByteArray(
                                    /* data = */ imageData,
                                    /* offset = */ 0,
                                    /* length = */ imageData.size
                                )
                            )
                        )
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        setImageResource(R.drawable.baseline_broken_image_24)
                    }
                }
            }

            is ImageRemote -> {
                Picasso.get().load(value.url).resize(0, h).onlyScaleDown().into(native)
            }

            is ImageResource -> {
                native.transition {
                    setImageResource(value.resource)
                }
            }

            is ImageVector -> {
                native.transition {
                    setImageDrawable(PathDrawable(value))
                }
            }

            is ImageLocal -> {
                Picasso.get().load(value.file.uri).resize(0, h).onlyScaleDown().into(native)
            }

            else -> {
                throw RuntimeException("Android View Tag is not an Image Source")
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

@ViewDsl
actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit) {
    return viewElement(factory = ::TransitionImageView, wrapper = ::ImageView) {
        native.clipToOutline = true
        handleTheme(native, viewDraws = true, viewLoads = true) {
            setup(this)
        }
    }
}


@ViewDsl
actual inline fun ViewWriter.zoomableImageActual(crossinline setup: ImageView.() -> Unit) {
    return viewElement(::ZoomClass, wrapper = ::ImageView){
        native.clipToOutline = true
        handleTheme(native, viewDraws = true) {
            setup(this)
        }
    }
}

open class TransitionImageView(context: Context): FrameLayout(context), HasSpacingMultiplier, Target {
    var ignoreAnimationsUntil = 0L
    override val spacingOverride: Property<Dimension?> = Property(null)
    var scaleType: AImageView.ScaleType = AImageView.ScaleType.CENTER_INSIDE
        set(value) {
            field = value
            children.filterIsInstance<AImageView>().forEach { it.scaleType = value }
        }
    open fun transition(setter: (AImageView.()->Unit)?) {
        children.forEach { it.animate().alpha(0f).setDuration(150L).withEndAction { removeView(it) }.start() }
        if(setter != null) {
            addView(AImageView(context).apply(setter).also {
                it.scaleType = this@TransitionImageView.scaleType
                if(animationsEnabled && System.currentTimeMillis() > ignoreAnimationsUntil) {
                    it.alpha = 0f
                    it.animate().alpha(1f).setDuration(150L).start()
                }
            })
        }
    }
    override fun onBitmapFailed(p0: java.lang.Exception?, p1: Drawable?) {
        Handler(Looper.getMainLooper()).post {
            transition {
                p0?.printStackTrace()
                setImageResource(R.drawable.baseline_broken_image_24)
            }
        }
    }
    override fun onBitmapLoaded(p0: Bitmap?, p1: Picasso.LoadedFrom?) {
        Handler(Looper.getMainLooper()).post {
            transition {
                setImageDrawable(BitmapDrawable(resources, p0))
            }
        }
    }
    override fun onPrepareLoad(p0: Drawable?) {
    }
}


class ZoomClass : TransitionImageView, View.OnTouchListener,
    GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    //shared constructing
    private var mContext: Context? = null
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    var mMatrix: Matrix? = null
    private var mMatrixValues: FloatArray? = null
    var mode = NONE

    // Scales
    var mSaveScale = 1f
    var mMinScale = 1f
    var mMaxScale = 4f

    // view dimensions
    var origWidth = 0f
    var origHeight = 0f
    var viewWidth = 0
    var viewHeight = 0
    private var mLast = PointF()
    private var mStart = PointF()

    constructor(context: Context) : super(context) {}

    val identity = Matrix()
    val imageView: AImageView? get() = children.filterIsInstance<AImageView>().lastOrNull()

    private fun resetZoom(){
        mSaveScale = 1f
        mMinScale = 1f
        mMaxScale = 4f

        // view dimensions
        origWidth = 0f
        origHeight = 0f
        viewWidth = 0
        viewHeight = 0
        mLast = PointF()
        mStart = PointF()

        mMatrix = Matrix()
        mMatrixValues = FloatArray(9)
        imageView?.imageMatrix = mMatrix
    }

    override fun transition(setter: (android.widget.ImageView.() -> Unit)?) {
        resetZoom()
        super.transition(setter)
    }

    init{
        super.setClickable(true)
        mContext = context
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mMatrix = Matrix()
        mMatrixValues = FloatArray(9)
        imageView?.imageMatrix = mMatrix
        scaleType = AImageView.ScaleType.MATRIX
        mGestureDetector = GestureDetector(context, this)
        setOnTouchListener(this)
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            mode = ZOOM
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val prevScale = mSaveScale
            mSaveScale *= mScaleFactor
            if (mSaveScale > mMaxScale) {
                mSaveScale = mMaxScale
                mScaleFactor = mMaxScale / prevScale
            } else if (mSaveScale < mMinScale) {
                mSaveScale = mMinScale
                mScaleFactor = mMinScale / prevScale
            }
            if (origWidth * mSaveScale <= viewWidth
                || origHeight * mSaveScale <= viewHeight) {
                mMatrix!!.postScale(mScaleFactor, mScaleFactor, viewWidth / 2.toFloat(),
                    viewHeight / 2.toFloat())
            } else {
                mMatrix!!.postScale(mScaleFactor, mScaleFactor,
                    detector.focusX, detector.focusY)
            }
            fixTranslation()
            return true
        }
    }

    private fun fitToScreen() {
        mSaveScale = 1f
        val scale: Float
        val drawable = imageView?.drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return
        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight
        val scaleX = viewWidth.toFloat() / imageWidth.toFloat()
        val scaleY = viewHeight.toFloat() / imageHeight.toFloat()
        scale = scaleX.coerceAtMost(scaleY)
        mMatrix!!.setScale(scale, scale)

        // Center the image
        var redundantYSpace: Float = (viewHeight.toFloat() - scale * imageHeight.toFloat())
        var redundantXSpace: Float = (viewWidth.toFloat() - scale * imageWidth.toFloat())
        redundantYSpace /= 2f
        redundantXSpace /= 2f
        mMatrix!!.postTranslate(redundantXSpace, redundantYSpace)
        origWidth = viewWidth - 2 * redundantXSpace
        origHeight = viewHeight - 2 * redundantYSpace
        imageView?.imageMatrix = mMatrix
    }

    fun fixTranslation() {
        mMatrix!!.getValues(mMatrixValues) //put matrix values into a float array so we can analyze
        val transX = mMatrixValues!![Matrix.MTRANS_X] //get the most recent translation in x direction
        val transY = mMatrixValues!![Matrix.MTRANS_Y] //get the most recent translation in y direction
        val fixTransX = getFixTranslation(transX, viewWidth.toFloat(), origWidth * mSaveScale)
        val fixTransY = getFixTranslation(transY, viewHeight.toFloat(), origHeight * mSaveScale)
        if (fixTransX != 0f || fixTransY != 0f) mMatrix!!.postTranslate(fixTransX, fixTransY)
    }

    private fun getFixTranslation(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float
        if (contentSize <= viewSize) { // case: NOT ZOOMED
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else { //CASE: ZOOMED
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }
        if (trans < minTrans) { // negative x or y translation (down or to the right)
            return -trans + minTrans
        }
        if (trans > maxTrans) { // positive x or y translation (up or to the left)
            return -trans + maxTrans
        }
        return 0F
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        return if (contentSize <= viewSize) {
            0F
        } else delta
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        if (mSaveScale == 1f) {

            // Fit to screen.
            fitToScreen()
        }
    }

    /*
        Ontouch
     */
    override fun onTouch(view: View?, event: MotionEvent): Boolean {
        mScaleDetector!!.onTouchEvent(event)
        mGestureDetector!!.onTouchEvent(event)
        val currentPoint = PointF(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mLast.set(currentPoint)
                mStart.set(mLast)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> if (mode == DRAG) {
                val dx = currentPoint.x - mLast.x
                val dy = currentPoint.y - mLast.y
                val fixTransX = getFixDragTrans(dx, viewWidth.toFloat(), origWidth * mSaveScale)
                val fixTransY = getFixDragTrans(dy, viewHeight.toFloat(), origHeight * mSaveScale)
                mMatrix!!.postTranslate(fixTransX, fixTransY)
                fixTranslation()
                mLast[currentPoint.x] = currentPoint.y
            }
            MotionEvent.ACTION_POINTER_UP -> mode = NONE
        }
        imageView?.imageMatrix = mMatrix
        return false
    }

    /*
        GestureListener
     */
    override fun onDown(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(motionEvent: MotionEvent) {}
    override fun onSingleTapUp(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(motionEvent: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    /*
        onDoubleTap
     */
    override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(motionEvent: MotionEvent): Boolean {
        fitToScreen()
        return false
    }

    override fun onDoubleTapEvent(motionEvent: MotionEvent): Boolean {
        return false
    }

    companion object {

        // Image States
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
    }
}