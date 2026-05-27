package com.lightningkite.kiteui.views.l2.editorHelpers

import com.lightningkite.kiteui.views.l2.MarkdownRichTextEditor
import org.w3c.dom.*
import org.w3c.dom.Range
import kotlinx.browser.window
import kotlinx.browser.document

fun MarkdownRichTextEditor.getValidSelection(): dynamic? {
    val selection = window.asDynamic().getSelection()
    return if (selection != null && selection.rangeCount > 0) selection else null
}

fun MarkdownRichTextEditor.getValidSelectionRange(): dynamic? {
    val selection = getValidSelection() ?: return null
    return selection.getRangeAt(0)
}

fun MarkdownRichTextEditor.applyRangeToSelection(range: Range) {
    val selection = window.asDynamic().getSelection()
    selection.removeAllRanges()
    selection.addRange(range)
}

fun MarkdownRichTextEditor.traverseUpDOM(startNode: Node?, root: Node?, condition: (Node) -> Boolean): Node? {
    var currentNode = startNode
    while (currentNode != null && currentNode != root) {
        if (condition(currentNode)) {
            return currentNode
        }
        currentNode = currentNode.parentNode
    }
    return null
}

fun MarkdownRichTextEditor.getEffectiveNode(node: Node?): Node? {
    return if (node?.nodeType == Node.TEXT_NODE) {
        node.parentNode
    } else {
        node
    }
}

fun MarkdownRichTextEditor.createAndSetupRange(setup: Range.() -> Unit): Range {
    return document.createRange().apply(setup)
}

fun findFirstTextNode(node: Node?): Text? {
    if (node?.nodeType == Node.TEXT_NODE) {
        return node as Text
    } else if (node?.nodeType == Node.ELEMENT_NODE) {
        for (i in 0 until node.childNodes.length) {
            val child = node.childNodes[i]
            val result = findFirstTextNode(child)
            if (result != null) {
                return result
            }
        }
    }
    return null
}


fun MarkdownRichTextEditor.isNodeInsideEditor(domNode: dynamic, editorRoot: HTMLElement): Boolean {
    var currentDomNode = domNode
    while (currentDomNode != null) {
        if (currentDomNode == editorRoot) return true
        currentDomNode = currentDomNode.parentNode
    }
    return false
}

fun MarkdownRichTextEditor.placeCursorAfterElement(element: HTMLElement) {
    val range = createAndSetupRange {
        setStartAfter(element)
        collapse(true)
    }
    applyRangeToSelection(range)
}

fun MarkdownRichTextEditor.placeCursorInsideElement(element: HTMLElement) {
    val range = createAndSetupRange {
        selectNodeContents(element)
        collapse(false)
    }
    applyRangeToSelection(range)
}

fun MarkdownRichTextEditor.getTagPositions(html: String): List<Pair<Int, Int>> {
    val tagRegex = Regex("""<[^>]+>""")
    val matches = tagRegex.findAll(html)
    return matches.map { Pair(it.range.first, it.range.last) }.toList()
}

fun MarkdownRichTextEditor.getHtmlAdjustedSelectionRange(element: HTMLElement): Pair<Int, Int>? {
    val selection = getValidSelection() ?: return null
    val range = selection.getRangeAt(0)
    val html = element.innerHTML
    val tagPositions = getTagPositions(html)

    val textStart = getTextIndex(range.startContainer, range.startOffset, element)
    val textEnd = getTextIndex(range.endContainer, range.endOffset, element)

    val htmlStart = adjustIndexForTags(textStart, tagPositions)
    val htmlEnd = adjustIndexForTags(textEnd, tagPositions)

    return Pair(htmlStart, htmlEnd)
}

fun MarkdownRichTextEditor.getTextIndex(node: Node, offset: Int, root: HTMLElement): Int {
    val range = createAndSetupRange {
        selectNodeContents(root)
        setEnd(node, offset)
    }
    return range.toString().length
}

fun MarkdownRichTextEditor.positionAtEnd(node: Node) {
    try {
        val range = when (node.nodeType) {
            Node.TEXT_NODE -> {
                val textNode = node as Text
                createAndSetupRange {
                    setStart(textNode, textNode.length)
                    collapse(true)
                }
            }

            Node.ELEMENT_NODE -> {
                val element = node as HTMLElement
                if (element.lastChild != null) {
                    positionAtEnd(element.lastChild!!)
                    return
                } else {
                    createAndSetupRange {
                        selectNodeContents(element)
                        collapse(false)
                    }
                }
            }

            else -> return
        }

        applyRangeToSelection(range)
    } catch (e: Throwable) {
        println("DEBUG Error positioning at end: ${e.message}")
    }
}

fun MarkdownRichTextEditor.getCleanCursorOffset(): Int {
    val element = textArea.element as? HTMLElement ?: return 0
    val selection = window.asDynamic().getSelection() ?: return 0
    if (selection.rangeCount == 0) return 0

    val range = selection.getRangeAt(0)
    val preCaretRange = document.createRange()
    preCaretRange.selectNodeContents(element)
    preCaretRange.setEnd(range.startContainer, range.startOffset)

    val textBeforeCursor = preCaretRange.toString()
    return textBeforeCursor.replace("\u200B", "").replace("\n", "").length
}

fun MarkdownRichTextEditor.restoreCursorPosition(targetOffset: Int) {
    val element = textArea.element as? HTMLElement ?: return
    val selection = window.asDynamic().getSelection() ?: return
    val range = document.createRange()

    var currentOffset = 0
    var nodeFound = false

    fun traverse(node: Node) {
        if (nodeFound) return

        if (node.nodeType == Node.TEXT_NODE) {
            val textLength = node.textContent?.length ?: 0
            if (currentOffset + textLength >= targetOffset) {
                val offsetInNode = targetOffset - currentOffset
                range.setStart(node, offsetInNode)
                range.collapse(true)
                nodeFound = true
            } else {
                currentOffset += textLength
            }
        } else {
            for (i in 0 until node.childNodes.length) {
                node.childNodes.item(i)?.let { traverse(it) }
            }
        }
    }

    traverse(element)

    if (nodeFound) {
        selection.removeAllRanges()
        selection.addRange(range)
    } else {
        positionAtEnd(element)
    }
}

fun MarkdownRichTextEditor.restoreCleanCursorOffset(targetOffset: Int) {
    val element = textArea.element as? HTMLElement ?: return
    val selection = window.asDynamic().getSelection() ?: return
    val range = document.createRange()

    var currentOffset = 0
    var nodeFound = false

    fun traverse(node: Node) {
        if (nodeFound) return

        if (node.nodeType == Node.TEXT_NODE) {
            val text = node.textContent ?: ""
            var charIndex = 0

            while (charIndex < text.length) {
                if (currentOffset == targetOffset) {
                    range.setStart(node, charIndex)
                    range.collapse(true)
                    nodeFound = true
                    return
                }
                if (text[charIndex] != '\u200B' && text[charIndex] != '\n') {
                    currentOffset++
                }
                charIndex++
            }

            if (currentOffset == targetOffset) {
                range.setStart(node, text.length)
                range.collapse(true)
                nodeFound = true
            }
        } else {
            for (i in 0 until node.childNodes.length) {
                node.childNodes.item(i)?.let { traverse(it) }
            }
        }
    }

    traverse(element)

    if (nodeFound) {
        selection.removeAllRanges()
        selection.addRange(range)
    } else {
        positionAtEnd(element)
    }
}

fun MarkdownRichTextEditor.isSelectionInsideTag(tagName: String): Boolean {
    val selection = getValidSelection() ?: return false

    if (isInNodeType(selection, tagName)) return true

    val selectedText = selection.toString()
    if (selectedText.isNotEmpty()) {
        val editorElement = textArea.element as? HTMLElement ?: return false
        val targetElements = editorElement.querySelectorAll(tagName.lowercase())
        for (i in 0 until targetElements.length) {
            val element = targetElements.item(i) as? HTMLElement ?: continue
            if ((element.textContent ?: "") == selectedText) return true
        }
    }

    return false
}

fun MarkdownRichTextEditor.focusAndPositionCursor(element: HTMLElement) {
    try {
        var textNode = findFirstTextNode(element)

        if (textNode == null) {
            textNode = document.createTextNode("\u200B")
            element.appendChild(textNode)
        }

        val range = createAndSetupRange {
            setStart(textNode, 0)
            collapse(true)
        }
        applyRangeToSelection(range)
    } catch (e: Throwable) {
        console.log("Error positioning cursor: ${e.message}")
    }
}

fun MarkdownRichTextEditor.isInNodeType(selection: dynamic, nodeType: String): Boolean {
    val startNode = getEffectiveNode(selection?.anchorNode)
    return traverseUpDOM(startNode, textArea.element) { node ->
        node.nodeName == nodeType
    } != null
}

fun MarkdownRichTextEditor.moveCursorOutsideTag(tagName: String) {
    val selection = getValidSelection() ?: return
    val targetNode = traverseUpDOM(selection.anchorNode, textArea.element) { node ->
        node.nodeName == tagName
    } ?: return

    val zwsNode = document.createTextNode("\u200B")
    targetNode.parentNode?.insertBefore(zwsNode, targetNode.nextSibling)

    val range = createAndSetupRange {
        setStart(zwsNode, 1)
        collapse(true)
    }

    applyRangeToSelection(range)
}

fun MarkdownRichTextEditor.containsEntireSelection(node: Node, range: dynamic): Boolean {
    val nodeRange = createAndSetupRange {
        selectNodeContents(node)
    }

    return nodeRange.compareBoundaryPoints(Range.START_TO_START, range) <= 0 &&
            nodeRange.compareBoundaryPoints(Range.END_TO_END, range) >= 0
}

fun MarkdownRichTextEditor.adjustIndexForTags(
    textIndex: Int,
    tagPositions: List<Pair<Int, Int>>
): Int {
    var adjustedIndex = textIndex
    var currentTextPos = 0
    var currentHtmlPos = 0

    for ((tagStart, tagEnd) in tagPositions) {
        while (currentHtmlPos < tagStart && currentTextPos < textIndex) {
            currentTextPos++
            currentHtmlPos++
        }
        if (currentTextPos >= textIndex) break
        adjustedIndex += (tagEnd - tagStart + 1)
        currentHtmlPos = tagEnd + 1
    }

    return adjustedIndex
}
