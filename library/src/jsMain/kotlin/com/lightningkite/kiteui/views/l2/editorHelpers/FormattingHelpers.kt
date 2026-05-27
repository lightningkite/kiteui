package com.lightningkite.kiteui.views.l2.editorHelpers


import com.lightningkite.kiteui.views.l2.MarkdownRichTextEditor
import org.w3c.dom.*
import kotlinx.browser.document
import kotlinx.browser.window

fun MarkdownRichTextEditor.isElementEmpty(element: HTMLElement): Boolean {
    return element.textContent?.trim()?.isEmpty() == true ||
            element.textContent == ZERO_WIDTH_SPACE ||
            element.innerHTML.trim().isEmpty()
}

/**
 * Applies nested formatting elements to a document fragment
 */
fun MarkdownRichTextEditor.applyFormattingToFragment(fragment: dynamic, formattingElements: List<HTMLElement>) {
    if (formattingElements.isEmpty()) return

    val (outerElement, innermostElement) = createNestedFormattingElements(formattingElements)
    moveFragmentContent(fragment, outerElement, innermostElement)
}

/**
 * Removes empty formatting tags from the editor and positions the cursor appropriately
 */
fun MarkdownRichTextEditor.checkAndRemoveEmptyFormattingTags() {
    val selection = getValidSelection() ?: return
    val range = selection.getRangeAt(0)

    // Start from where the cursor is, not the whole editor
    var current: Node? = range.commonAncestorContainer

    // Traverse up the tree until we hit the editor boundary
    while (current != null && current != textArea.element) {
        val parent = current.parentNode

        if (current.nodeType == Node.ELEMENT_NODE) {
            val element = current as HTMLElement
            if (element.tagName.uppercase() in EMPTY_FORMATTING_TAGS && isElementEmpty(element)) {
                removeEmptyTag(element, element.tagName)
            }
        }
        current = parent
    }
}

private fun MarkdownRichTextEditor.createNestedFormattingElements(formattingElements: List<HTMLElement>): Pair<HTMLElement?, HTMLElement?> {
    if (formattingElements.isEmpty()) return Pair(null, null)

    return formattingElements.fold(
        Pair<HTMLElement?, HTMLElement?>(
            null,
            null
        )
    ) { (outerElement, currentElement), formatElem ->
        val newFormatElem = document.createElement(formatElem.tagName) as HTMLElement

        if (outerElement == null) {
            // First element becomes both outer and current tracking element
            Pair(newFormatElem, newFormatElem)
        } else {
            // Append to current element and step down the hierarchy chain
            currentElement?.appendChild(newFormatElem)
            Pair(outerElement, newFormatElem)
        }
    }
}

private fun MarkdownRichTextEditor.moveFragmentContent(
    fragment: dynamic,
    outerElement: HTMLElement?,
    innermostElement: HTMLElement?
) {
    if (outerElement == null || innermostElement == null) return

    try {
        val tempDiv = document.createElement("div").apply {
            appendChild(fragment.cloneNode(true))
        }
        innermostElement.innerHTML = tempDiv.innerHTML
        fragment.appendChild(outerElement)
    } catch (e: Throwable) {
        console.log("Error moving fragment content: ${e.message}")
    }
}

private fun MarkdownRichTextEditor.removeEmptyTag(tag: HTMLElement, tagName: String) {
    try {
        tag.parentNode?.let { parent ->
            val textNode = document.createTextNode(ZERO_WIDTH_SPACE)
            parent.replaceChild(textNode, tag)
            setCursorToNode(textNode)
        }
    } catch (e: Throwable) {
        console.log("Error removing empty tag $tagName: ${e.message}")
    }
}

private fun MarkdownRichTextEditor.setCursorToNode(node: Node) {
    try {
        val range = document.createRange().apply {
            setStart(node, 0)
            collapse(true)
        }
        val selection = window.asDynamic().getSelection()
        selection?.removeAllRanges()
        selection?.addRange(range)
    } catch (e: Throwable) {
        console.log("Error setting cursor position: ${e.message}")
    }
}
