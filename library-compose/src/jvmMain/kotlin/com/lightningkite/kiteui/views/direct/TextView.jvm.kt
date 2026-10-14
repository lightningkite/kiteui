package com.lightningkite.kiteui.views.direct

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import java.io.StringReader
import javax.swing.text.MutableAttributeSet
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.parser.ParserDelegator

/** The inline tags Android's TextView supports, via the JDK's HTML parser. */
internal actual fun parseBasicHtml(html: String): AnnotatedString = buildAnnotatedString {
    // Tag -> stack of indices from push*, so an end tag pops what its start tag pushed.
    val pushed = HashMap<HTML.Tag, ArrayDeque<Int>>()
    val callback = object : HTMLEditorKit.ParserCallback() {
        override fun handleText(data: CharArray, pos: Int) {
            append(String(data))
        }

        override fun handleSimpleTag(t: HTML.Tag, a: MutableAttributeSet, pos: Int) {
            if (t == HTML.Tag.BR) append('\n')
        }

        override fun handleStartTag(t: HTML.Tag, a: MutableAttributeSet, pos: Int) {
            val index = when (t) {
                HTML.Tag.B, HTML.Tag.STRONG -> pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                HTML.Tag.I, HTML.Tag.EM -> pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                HTML.Tag.U -> pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                HTML.Tag.S, HTML.Tag.STRIKE -> pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                HTML.Tag.TT, HTML.Tag.CODE -> pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                HTML.Tag.A -> (a.getAttribute(HTML.Attribute.HREF) as? String)?.let { pushLink(LinkAnnotation.Url(it)) }
                else -> null
            } ?: return
            pushed.getOrPut(t) { ArrayDeque() }.addLast(index)
        }

        override fun handleEndTag(t: HTML.Tag, pos: Int) {
            pushed[t]?.removeLastOrNull()?.let { pop(it) }
        }
    }
    ParserDelegator().parse(StringReader(html), callback, true)
}
