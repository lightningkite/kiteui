package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement

public expect class TextView(context: ElementContext) : NativeElement {
    public var content: String
    public var align: Align?
    public var ellipsis: Boolean
    public var wraps: Boolean
    public var wordBreak: WordBreak
    public var lineClamp: Int?
    public fun setBasicHtmlContent(html: String)
}