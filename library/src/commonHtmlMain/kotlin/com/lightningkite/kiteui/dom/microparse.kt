package com.lightningkite.kiteui.dom

internal sealed interface MPNode {
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
         * URL schemes permitted in [okAttrs] values. Anything else — notably `javascript:`
         * and `data:` — executes script or renders attacker-controlled documents when the
         * browser follows the link.
         */
        internal val okUrlSchemes: Set<String> = setOf("http", "https", "mailto", "tel")

        /**
         * True if [url] is safe to emit as a link target: either scheme-relative/relative,
         * or carrying one of [okUrlSchemes].
         *
         * Browsers ignore ASCII whitespace and C0 control characters inside URLs, so
         * `java\tscript:` reaches the same handler as `javascript:`. Those characters are
         * removed before the scheme is examined rather than trusted as separators.
         */
        internal fun urlAllowed(url: String): Boolean {
            val cleaned = url.filter { it.code > 0x20 }
            val colon = cleaned.indexOf(':')
            if (colon < 0) return true
            // A delimiter before the colon means the colon belongs to a path, query or
            // fragment rather than to a scheme, e.g. "/a:b" or "?x=1:2".
            val delimiter = cleaned.indexOfFirst { it == '/' || it == '?' || it == '#' }
            if (delimiter in 0 until colon) return true
            return cleaned.substring(0, colon).lowercase() in okUrlSchemes
        }

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
        val children: MutableList<MPNode> = ArrayList()
    ) : MPNode {
        override fun toString(): String {
            if(tagName == "br") return "<br>"
            return "<${tagName} ${attributes.entries.joinToString(" ") { "${it.key}=\"${escapeAttribute(it.value)}\"" }}>${
                children.joinToString("")
            }</${tagName}>"
        }

        override fun secure() {
            if (tagName !in okTags) tagName = "span"
            attributes.keys.retainAll(okAttrs)
            // An allowed attribute name is not enough: href values carry their own scheme,
            // so a permitted attribute can still smuggle in executable content.
            if (attributes["href"]?.let { !urlAllowed(it) } == true) attributes.remove("href")
            children.forEach { it.secure() }
        }
    }

    public data class Text(val content: String) : MPNode {
        override fun toString(): String = content
        override fun secure() {}
    }
}

internal fun String.parseMPNodes(): List<MPNode> {
    val stack = arrayListOf(MPNode.Element("*"))
    starts(
        onTag = {
            it.analyzeTagInside { tagName, start, end, kvs ->
                if (start) {
                    val newElement = MPNode.Element(tagName, attributes = kvs)
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
            if (it.isNotBlank()) stack.last().children.add(MPNode.Text(it))
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
