package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.parseMinimalHtmlNodes
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.*



public actual class TextView actual constructor(context: ElementContext) : NativeElement(context) {
    override val driverValue: String? get() = content
    init {
        native.tag = "p"
        native.content = Typography.nbsp.toString()
    }
    public actual inline var content: String
        get() = native.content ?: ""
        set(value) {
            native.content = if(value.isEmpty()) Typography.nbsp.toString() else value
        }

    private var _align: Align? = null

    public actual var align: Align?
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
    public actual var ellipsis: Boolean = true
        set(value) {
            field = value
            native.style.textOverflow = if(value) "ellipsis" else "clip"
            if(value)
                native.setStyleProperty("overflow", "hidden")
            else
                native.setStyleProperty("overflow", null)
        }
    public actual var wraps: Boolean = true
        set(value) {
            field = value
            native.setStyleProperty("text-wrap", if(value) "wrap" else "nowrap")
            native.setStyleProperty("text-wrap-mode", if(value) "wrap" else "nowrap")
        }
    public actual var wordBreak: WordBreak = WordBreak.Normal
        set(value) {
            field = value
            native.setStyleProperty("word-break", if(value == WordBreak.BreakAll) "break-all" else "normal")
        }
    public actual var lineClamp: Int? = null
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

    override fun nativeApplyTheme(theme: ThemeAndBack) {
        super.nativeApplyTheme(theme)
        applyAlign(_align ?: theme.theme.font.align)
    }

    public actual fun setBasicHtmlContent(html: String) {
        native.style.whiteSpace = "pre-line"
        native.classes.add("kui-basic-html-content")
        native.innerHtmlUnsafe = html.parseMinimalHtmlNodes().onEach { it.secure() }.joinToString(" ")
    }
}

