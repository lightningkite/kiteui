package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.parseMPNodes
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.*



actual class TextView actual constructor(context: RContext) : RView(context) {
    override val driverValue: String? get() = content
    init {
        native.tag = "p"
        native.content = Typography.nbsp.toString()
    }
    actual inline var content: String
        get() = native.content ?: ""
        set(value) {
            native.content = if(value.isEmpty()) Typography.nbsp.toString() else value
        }

    private var _align: Align? = null

    actual var align: Align?
        get() = _align
        set(value) {
            _align = value
            applyAlign(value ?: theme.font.align)
        }

    private fun applyAlign(value: Align) {
        native.style.textAlign = when (value) {
            Align.Start -> "start"
            Align.Center -> "center"
            Align.End -> "end"
            Align.Stretch -> "justify"
        }
    }
    actual var ellipsis: Boolean = true
        set(value) {
            field = value
            native.style.textOverflow = if(value) "ellipsis" else "clip"
            if(value)
                native.setStyleProperty("overflow", "hidden")
            else
                native.setStyleProperty("overflow", null)
        }
    actual var wraps: Boolean = true
        set(value) {
            field = value
            native.setStyleProperty("text-wrap", if(value) "wrap" else "nowrap")
            native.setStyleProperty("text-wrap-mode", if(value) "wrap" else "nowrap")
        }
    actual var wordBreak: WordBreak
        get() = TODO("Not yet implemented")
        set(value) {
            native.setStyleProperty("word-break", if(value == WordBreak.BreakAll) "break-all" else "normal")
        }
    actual var lineClamp: Int? = null
        set(value) {
            field = value
            value?.let {
                native.setStyleProperty("display", "-webkit-box")
                native.setStyleProperty("line-clamp", "$it")
                native.setStyleProperty("-webkit-line-clamp", "$it")
                native.setStyleProperty("-webkit-box-orient", "vertical")
                native.setStyleProperty("overflow", "hidden")
                native.setStyleProperty("text-overflow", "ellipsis")
            }
        }

    override fun applyTheme(theme: ThemeAndBack) {
        super.applyTheme(theme)
        applyAlign(_align ?: theme.theme.font.align)
    }

    actual fun setBasicHtmlContent(html: String) {
        native.style.whiteSpace = "pre-line"
        native.innerHtmlUnsafe = html.parseMPNodes().onEach { it.secure() }.joinToString(" ")
    }
}

