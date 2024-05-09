package com.lightningkite.kiteui.views.direct

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Path.PathDrawable
import timber.log.Timber
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.widget.AppCompatImageView
import com.lightningkite.kiteui.views.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NIconView(context: Context) : AppCompatImageView(context) {
    init {
        scaleType = ScaleType.CENTER_INSIDE
    }
    var icon: Icon? = null
        set(value) {
            field = value
            updateIcon()
        }
    var iconPaint: Paint = Color.black
        set(value) {
            field = value
            updateIcon()
        }
    private fun updateIcon() {
        setImageDrawable(icon?.let { PathDrawable(it.toImageSource(iconPaint)) })
    }
}

actual class IconView actual constructor(context: RContext): RView(context) {
    override val native = NIconView(context.activity)
    actual var source: Icon?
        get() = native.icon
        set(value) {
            native.icon = value
        }
    actual var description: String?
        get() {
            return native.contentDescription.toString()
        }
        set(value) {
            native.contentDescription = value
        }

    override fun applyForeground(theme: Theme) {
        native.iconPaint = theme.icon
    }
}