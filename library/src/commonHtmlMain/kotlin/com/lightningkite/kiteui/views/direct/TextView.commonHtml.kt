package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*



actual class TextView actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "p"
    }
    actual inline var content: String
        get() = native.content ?: ""
        set(value) {
            native.content = value
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
}

