package com.lightningkite.kiteui.views.direct

import androidx.appcompat.widget.AppCompatImageView
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.Path.PathDrawable
import android.content.Context
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

actual class IconView actual constructor(context: ElementContext): NativeElement(context) {
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

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        native.iconPaint = theme.theme.icon
    }
}