package com.lightningkite.kiteui.dom

sealed interface MPNode {
    fun secure()

    companion object {
        val okTags = setOf(
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
        val okAttrs = setOf(
            "href"
        )
    }

    data class Element(
        var tagName: String,
        val attributes: MutableMap<String, String> = HashMap(),
        val children: MutableList<MPNode> = ArrayList()
    ) : MPNode {
        override fun toString(): String {
            if(tagName == "br") return "<br>"
            return "<${tagName} ${attributes.entries.joinToString(" ") { "${it.key}=\"${it.value}\"" }}>${
                children.joinToString("")
            }</${tagName}>"
        }

        override fun secure() {
            if (tagName !in okTags) tagName = "span"
            attributes.keys.retainAll(okAttrs)
            children.forEach { it.secure() }
        }
    }

    data class Text(val content: String) : MPNode {
        override fun toString(): String = content
        override fun secure() {}
    }
}

fun String.parseMPNodes(): List<MPNode> {
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
                    repeat(
                        stack.size - stack.indexOfLast { it.tagName == tagName }
                    ) {
                        stack.removeLast()
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
        onTag(substring(nextStart + 1, nextEnd - 1))
        current = nextEnd
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
