package com.lightningkite.kiteui.views.direct


import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.Signal

actual class TextView actual constructor(context: RContext) :
    RView(context) {

    val m_content = Signal("")
    val m_align = Signal<Align?>(null)
    val m_ellipsis = Signal<Boolean>(false)
    val m_wraps = Signal<Boolean>(true)
    val m_lineClamp = Signal<Int?>(null)
    val m_htmlContent = Signal<AnnotatedString?>(null)
    val m_wordBreak = Signal(WordBreak.Normal)


    @Composable
    override fun compose() {
        val contentState = m_content.collectAsMutableState()
        val alignState = m_align.collectAsMutableState()
        val ellipsisState = m_ellipsis.collectAsMutableState()
        val wrapsState = m_wraps.collectAsMutableState()
        val lineClampState = m_lineClamp.collectAsMutableState()
        val htmlContentState = m_htmlContent.collectAsMutableState()

        htmlContentState.value?.let {
            Text(
                text = it,
                textAlign = alignState.value?.toComposeAlign(),
                overflow = if (ellipsisState.value) androidx.compose.ui.text.style.TextOverflow.Ellipsis else androidx.compose.ui.text.style.TextOverflow.Clip,
                softWrap = wrapsState.value,
                maxLines = lineClampState.value ?: Int.MAX_VALUE
            )
        } ?: contentState.value.let {
            Text(
                text = it,
                textAlign = alignState.value?.toComposeAlign(),
                overflow = if (ellipsisState.value) androidx.compose.ui.text.style.TextOverflow.Ellipsis else androidx.compose.ui.text.style.TextOverflow.Clip,
                softWrap = wrapsState.value,
                maxLines = lineClampState.value ?: Int.MAX_VALUE
            )
        }
    }


    actual var content: String
        get() {
            return m_content.value
        }
        set(value) {
            m_content.value = value
            m_htmlContent.value = null  // Clear HTML content when setting plain text
        }

    actual var align: Align
        get() = m_align.value ?: Align.Start
        set(value) {
            m_align.value = value
        }
    actual var ellipsis: Boolean
        get() = m_ellipsis.value
        set(value) {
            m_ellipsis.value = value
        }
    actual var wraps: Boolean
        get() = m_wraps.value
        set(value) {
            m_wraps.value = value
        }
    actual var wordBreak: WordBreak
        get() = m_wordBreak.value
        set(value) {
            m_wordBreak.value = value
        }
    actual var lineClamp: Int?
        get() = m_lineClamp.value
        set(value) {
            m_lineClamp.value = value
        }

    actual fun setBasicHtmlContent(html: String) {
        m_htmlContent.value = parseBasicHtml(html)
    }

    private fun parseBasicHtml(html: String): AnnotatedString {
        return buildAnnotatedString {
            var currentIndex = 0
            val tagPattern = Regex("<(/?)([a-z]+)>", RegexOption.IGNORE_CASE)

            val activeStyles = mutableListOf<SpanStyle>()

            while (currentIndex < html.length) {
                val match = tagPattern.find(html, currentIndex)

                if (match == null) {
                    // No more tags, append remaining text
                    append(html.substring(currentIndex))
                    break
                }

                // Append text before the tag
                if (match.range.first > currentIndex) {
                    val text = html.substring(currentIndex, match.range.first)
                    if (activeStyles.isNotEmpty()) {
                        activeStyles.forEach { style ->
                            withStyle(style) { append(text) }
                        }
                    } else {
                        append(text)
                    }
                }

                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2].lowercase()

                when (tagName) {
                    "b", "strong" -> {
                        if (!isClosing) {
                            activeStyles.add(SpanStyle(fontWeight = FontWeight.Bold))
                        } else {
                            activeStyles.removeAll { it.fontWeight == FontWeight.Bold }
                        }
                    }
                    "i", "em" -> {
                        if (!isClosing) {
                            activeStyles.add(SpanStyle(fontStyle = FontStyle.Italic))
                        } else {
                            activeStyles.removeAll { it.fontStyle == FontStyle.Italic }
                        }
                    }
                    "u" -> {
                        if (!isClosing) {
                            activeStyles.add(SpanStyle(textDecoration = TextDecoration.Underline))
                        } else {
                            activeStyles.removeAll { it.textDecoration == TextDecoration.Underline }
                        }
                    }
                    "br" -> {
                        append("\n")
                    }
                }

                currentIndex = match.range.last + 1
            }
        }
    }
}
