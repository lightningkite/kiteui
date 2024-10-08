package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.parseMPNodes
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.*



actual class TextView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "p"
        native.content = Typography.nbsp.toString()
    }
    actual inline var content: String
        get() = native.content ?: ""
        set(value) {
            native.content = if(value.isEmpty()) Typography.nbsp.toString() else value
        }
    actual var align: Align = Align.Start
        set(value) {
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
    actual fun setBasicHtmlContent(html: String) {
        native.innerHtmlUnsafe = html.parseMPNodes().onEach { it.secure() }.joinToString(" ")
    }
}

