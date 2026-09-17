package com.lightningkite.kiteui.dom

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.utils.isSafeLinkUrl

// Depth past which nested markup is discarded rather than walked. secure() and toString() both
// recurse per level, so untrusted HTML nested thousands deep would overflow the stack before any
// of the tag/attribute filtering below got a chance to run. Matches the equivalent guard in
// MarkdownParser, which takes the same kind of input.
private const val MAX_NESTING_DEPTH = 100

// Public so consumers can sanitize untrusted HTML before inserting it into the DOM; the only
// other route is `innerHtmlUnsafe`, which by definition does no sanitization.
@ExperimentalKiteUi
public sealed interface MinimalHtmlNode {
    public fun secure()

    public companion object {
        internal val okTags: Set<String> = setOf(
            "p",
            "ul",
            "li",
            "div",
            "span",
            "strong",
            "b",
            "em",
            "cite",
            "dfn",
            "i",
            "big",
            "small",
            "font",
            "blockquote",
            "tt",
            "a",
            "u",
            "del",
            "s",
            "strike",
            "sup",
            "sub",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "br",
        )
        internal val okAttrs: Set<String> = setOf(
            "href",
            "target",
        )

        /**
         * Escapes an attribute value for emission inside double quotes.
         *
         * `&` is deliberately left alone: it cannot terminate a quoted attribute, and
         * escaping it would corrupt query strings that already contain entities.
         */
        internal fun escapeAttribute(value: String): String = value
            .replace("\"", "&quot;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
    }

    public data class Element(
        var tagName: String,
        val attributes: MutableMap<String, String> = HashMap(),
        val children: MutableList<MinimalHtmlNode> = ArrayList()
    ) : MinimalHtmlNode {
        override fun toString(): String {
            if(tagName == "br") return "<br>"
            return "<${tagName} ${attributes.entries.joinToString(" ") { "${it.key}=\"${escapeAttribute(it.value)}\"" }}>${
                children.joinToString("")
            }</${tagName}>"
        }

        override fun secure() {
            // HTML tag and attribute names are case-insensitive, but the allow-lists are lowercase.
            // Matching them as-written failed closed - safe, but it silently turned every
            // `<A HREF="...">` into a span and threw the link away - so both are normalised first.
            // The parser already lowercases tag names; repeated here so this stays a complete
            // security boundary on its own rather than one that assumes a particular producer.
            tagName = tagName.lowercase()
            if (tagName !in okTags) tagName = "span"

            val lowercased = attributes.entries.associate { it.key.lowercase() to it.value }
            attributes.clear()
            attributes.putAll(lowercased)
            attributes.keys.retainAll(okAttrs)

            // An allowed attribute name is not enough: href values carry their own scheme,
            // so a permitted attribute can still smuggle in executable content.
            if (attributes["href"]?.let { !isSafeLinkUrl(it) } == true) attributes.remove("href")
            children.forEach { it.secure() }
        }
    }

    public data class Text(val content: String) : MinimalHtmlNode {
        override fun toString(): String = content
        override fun secure() {}
    }
}

@ExperimentalKiteUi
public fun String.parseMinimalHtmlNodes(): List<MinimalHtmlNode> {
    val stack = arrayListOf(MinimalHtmlNode.Element("*"))
    starts(
        onTag = {
            it.analyzeTagInside { rawTagName, start, end, kvs ->
                // Tag names are case-insensitive in HTML, so normalising here is what lets
                // `</DIV>` close the `<div>` it was written to close.
                val tagName = rawTagName.lowercase()
                // Capped here rather than in secure()/toString() because this is the only place that
                // can stop the deep tree from being built at all. An element past the cap is dropped
                // outright and nothing is pushed, so its content reattaches to the nearest surviving
                // ancestor - and because the matching close tag still pops a real ancestor, content
                // after it shifts up a level too. Markup nested past 100 deep therefore comes out
                // flattened and reshuffled, which is the intended trade against a stack overflow.
                if (start && stack.size <= MAX_NESTING_DEPTH) {
                    val newElement = MinimalHtmlNode.Element(tagName, attributes = kvs)
                    stack.last().children.add(newElement)
                    stack.add(newElement)
                }

                if (end || tagName == "br") {
                    if(stack.any { it.tagName == tagName }) {
                        repeat(
                            stack.size - stack.indexOfLast { it.tagName == tagName }
                        ) {
                            stack.removeLast()
                        }
                    }
                }
            }
        },
        onContent = {
            if (it.isNotBlank()) stack.last().children.add(MinimalHtmlNode.Text(it))
        }
    )
    return stack.first().children
}

internal inline fun String.starts(
    onTag: (String) -> Unit,
    onContent: (String) -> Unit
) {
    var current = 0
    while (true) {
        val nextStart = this.indexOf('<', current)
        if (nextStart == -1) {
            if(current < length) onContent(substring(current))
            break
        }
        if (current < nextStart) {
            onContent(substring(current, nextStart))
        }
        val nextEnd = this.indexOf('>', nextStart + 1) + 1
        if (nextEnd == 0) {
            onTag(substring(nextStart + 1))
            break
        } else {
            onTag(substring(nextStart + 1, nextEnd - 1))
            current = nextEnd
        }
    }
}

internal inline fun String.analyzeTagInside(out: (tagName: String, start: Boolean, end: Boolean, kvs: MutableMap<String, String>) -> Unit) {

    var isStart = true
    var hasEnd = false
    var lastBuilt = ""
    var tagName: String? = null
    val kvs = HashMap<String, String>()

    var textEncountered = false
    var inQuotes = false
    var quoteChar = '"'
    val builder = StringBuilder()
    var nextIsValue = false
    var currentIsValue = false
    var runFinish = false

    for (char in this) {
        if (inQuotes) {
            if (char == quoteChar) {
                inQuotes = false
                runFinish = true
            } else {
                builder.append(char)
            }
        } else when (char) {
            '/' -> {
                runFinish = true
                hasEnd = true
                if (!textEncountered) {
                    isStart = false
                }
            }

            ' ' -> {
                runFinish = true
            }

            '=' -> {
                nextIsValue = true
                runFinish = true
            }

            '\'', '"' -> {
                runFinish = true
                inQuotes = true
                quoteChar = char
            }

            else -> {
                textEncountered = true
                builder.append(char)
            }
        }
        if (runFinish) {
            runFinish = false
            if (builder.length != 0) {
                val built = builder.toString()
                if (tagName == null) {
                    tagName = built
                } else if (currentIsValue) {
                    kvs[lastBuilt] = built
                } else {
                    kvs[built] = ""
                }
                if (nextIsValue) {
                    nextIsValue = false
                    currentIsValue = true
                } else {
                    currentIsValue = false
                }
                lastBuilt = built
                builder.clear()
            }
        }
    }
    if (builder.length != 0) {
        val built = builder.toString()
        if (tagName == null) {
            tagName = built
        } else if (currentIsValue) {
            kvs[lastBuilt] = built
        } else {
            kvs[built] = ""
        }
        builder.clear()
    }

    out(tagName.toString(), isStart, hasEnd, kvs)
}
