package com.lightningkite.kiteui.views.direct

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.LoadRemoteImageScope
import com.lightningkite.kiteui.views.Path.PathDrawable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import timber.log.Timber
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

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

actual var IconView.source: Icon?
    get() = native.icon
    set(value) {
        native.icon = value
    }
actual var IconView.description: String?
    get() {
        return native.contentDescription.toString()
    }
    set(value) {
        native.contentDescription = value
    }

@ViewDsl
actual inline fun ViewWriter.iconActual(crossinline setup: IconView.() -> Unit) {
    return viewElement(factory = ::NIconView, wrapper = ::IconView) {
        handleTheme(native, viewDraws = true, viewLoads = true, foreground = { t, v ->
            v.iconPaint = t.icon
        }) {
            setup(this)
        }
    }
}