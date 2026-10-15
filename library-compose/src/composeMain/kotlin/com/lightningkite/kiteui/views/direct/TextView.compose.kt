package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import com.lightningkite.kiteui.externalServices
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.WordBreak
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElement
import com.lightningkite.kiteui.views.toCompose
import com.lightningkite.kiteui.views.toTextStyle

internal expect fun parseBasicHtml(html: String, openLink: (url: String) -> Unit): AnnotatedString

public actual class TextView actual constructor(context: ElementContext) : NativeElement(context) {
    override val driverValue: String? get() = content
    private var text: AnnotatedString by mutableStateOf(AnnotatedString(""))

    public actual var content: String
        get() = text.text
        set(value) {
            text = AnnotatedString(value)
        }
    public actual var align: Align? by mutableStateOf(null)
    public actual var ellipsis: Boolean by mutableStateOf(true)
    public actual var wraps: Boolean by mutableStateOf(true)
    public actual var wordBreak: WordBreak by mutableStateOf(WordBreak.Normal)
    public actual var lineClamp: Int? by mutableStateOf(null)

    public actual fun setBasicHtmlContent(html: String) {
        text = parseBasicHtml(html) { context.externalServices.openLink(it) }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val theme = currentTheme.theme
        val font = theme.font
        val style = font.toTextStyle(theme.foreground.closestColor().toCompose(), align ?: font.align).let {
            if (wordBreak == WordBreak.BreakAll) it.copy(lineBreak = LineBreak.Simple) else it
        }
        BasicText(
            text = if (font.allCaps) text.toUpperCase() else text,
            modifier = modifier,
            style = style,
            overflow = if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
            softWrap = wraps,
            maxLines = if (!wraps) 1 else lineClamp ?: Int.MAX_VALUE,
            // Read while drawing, so a theme transition doesn't recompose or re-measure the text.
            color = { foreground },
        )
    }
}
