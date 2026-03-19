package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

expect class TextView(context: ElementContext) : NativeElement {
    var content: String
    var align: Align?
    var ellipsis: Boolean
    var wraps: Boolean
    var wordBreak: WordBreak
    var lineClamp: Int?
    fun setBasicHtmlContent(html: String)
}