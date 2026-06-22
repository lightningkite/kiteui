package com.lightningkite.kiteui.views.l2.editorHelpers


import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.views.l2.MarkdownRichTextEditor

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.Text
import org.w3c.dom.asList
import org.w3c.dom.events.Event


fun MarkdownRichTextEditor.isListItemEmpty(listItem: HTMLElement): Boolean {
    // 1. Clone the node so we don't mutate the active editor DOM
    val clone = listItem.cloneNode(true) as HTMLElement

    // 2. Strip out nested lists from the clone so their text isn't counted
    val nestedLists = clone.querySelectorAll("ul, ol")
    for (i in 0 until nestedLists.length) {
        nestedLists.item(i)?.let { it.parentNode?.removeChild(it) }
    }

    // 3. Check for structural elements that mean it isn't truly empty
    val hasVisibleElements = clone.querySelector("img, br") != null
    val text = clone.textContent ?: ""

    return !hasVisibleElements && (text.trim().isEmpty() || text == ZERO_WIDTH_SPACE)
}

fun MarkdownRichTextEditor.handleEnterInCodeBlock(block: HTMLElement?, event: KeyboardEvent) {
    if (block == null) return

    val selection = getValidSelection() ?: return
    val range = selection.getRangeAt(0)

    // Notion/Slack Style: If the user holds Ctrl (Windows) or Cmd/Meta (Mac) while hitting Enter
    if (event.ctrlKey || event.metaKey) {
        exitCodeBlockToNewParagraph(block)
        return
    }

    // Normal Enter: Just insert a physical newline inside the code block
    insertNewlineInCodeBlock(range, selection)
}

fun MarkdownRichTextEditor.exitCodeBlockToNewParagraph(block: HTMLElement?) {
    val newParagraph = createParagraphWithContent()
    block?.parentNode?.insertBefore(newParagraph, block.nextSibling)
    positionCursorInElement(newParagraph, true)
    updateCursorPosition()
    updateEditorState()
}

fun MarkdownRichTextEditor.insertNewlineInCodeBlock(range: dynamic, selection: dynamic) {
    val textNode = document.createTextNode("\n")
    range.insertNode(textNode)
    range.setStartAfter(textNode)
    range.collapse(true)
    selection.removeAllRanges()
    selection.addRange(range)
}

fun MarkdownRichTextEditor.beforeInput(event: Event) {
    if (event.asDynamic().inputType == "insertParagraph") {
        event.preventDefault()
        insertParagraphManually()
    }
}

fun MarkdownRichTextEditor.handleInput(event: Event) {
    val inputType = event.asDynamic().inputType.toString()
    if (inputType in DELETE_INPUT_TYPES) {
        checkAndRemoveEmptyFormattingTags()
    }
    updateEditorState()
}

fun MarkdownRichTextEditor.handleClick(event: Event) {
    val linkElement = (event.target as? Node)?.let { target ->
        traverseUpDOM(target, textArea.element) { it.nodeName == "A" } as? HTMLElement
    }
    if (linkElement != null) {
        openLinkEditor(linkElement)
        return
    }
    updateCursorPosition()
    updateEditorState()
}

fun MarkdownRichTextEditor.handleKeyUp(event: Event) {
    event as KeyboardEvent

    if (isSuggestionActive && event.key in listOf("ArrowUp", "ArrowDown")) {
        return
    }

    if (event.key in listOf("Backspace", "Delete")) {
        checkAndRemoveEmptyFormattingTags()
    }
    updateCursorPosition()
    updateEditorState()
}

fun MarkdownRichTextEditor.updateCursorPosition() {
    cursorIndex = getHtmlAdjustedSelectionRange(textArea.element as HTMLElement)
}

fun MarkdownRichTextEditor.updateEditorState() {
    launch {
        updateSelectedRichTextTag()
    }
}

fun MarkdownRichTextEditor.insertParagraphManually() {
    // Route mobile/virtual keyboard paragraph insertions to the same robust logic
    handleEnterInRegularContext()
}

fun MarkdownRichTextEditor.createParagraphWithContent(): HTMLElement {
    return document.createElement("p").apply {
        appendChild(document.createElement("br"))
    } as HTMLElement
}


fun MarkdownRichTextEditor.positionCursorInElement(element: HTMLElement, atStart: Boolean = true) {
    val selection = getValidSelection() ?: return
    val newRange = document.createRange()
    newRange.selectNodeContents(element)
    newRange.collapse(atStart)

    selection.removeAllRanges()
    selection.addRange(newRange)
}

fun MarkdownRichTextEditor.handleKeyDown(event: Event) {
    event as KeyboardEvent

    if (isSuggestionActive && event.key in listOf("ArrowUp", "ArrowDown", "Escape")) {
        return
    }

    when {
        shouldTriggerAction(event) -> handleActionTrigger(event)
        shouldHandleTab(event) -> handleTabKey(event)
        shouldHandleDelete(event) -> handleDeleteKeys(event)
        shouldHandleEnter(event) -> handleEnterKey(event)
    }
}

fun MarkdownRichTextEditor.shouldTriggerAction(event: KeyboardEvent): Boolean {
    return event.key == "Enter" && !event.shiftKey && action != null && !isSuggestionActive
}

fun MarkdownRichTextEditor.shouldHandleTab(event: KeyboardEvent): Boolean {
    return event.key == "Tab" && !isSuggestionActive
}

fun MarkdownRichTextEditor.shouldHandleDelete(event: KeyboardEvent): Boolean {
    return event.key in listOf("Backspace", "Delete")
}

fun MarkdownRichTextEditor.shouldHandleEnter(event: KeyboardEvent): Boolean {
    return event.key == "Enter" && !isSuggestionActive
}

fun MarkdownRichTextEditor.handleActionTrigger(event: KeyboardEvent) {
    action?.startAction(this)
    event.preventDefault()
    textArea.innerHtmlUnsafe = ""
    event.stopImmediatePropagation()
}

fun MarkdownRichTextEditor.handleTabKey(event: KeyboardEvent) {
    val selection = getValidSelection() ?: return
    val listContext = getListContext(selection) ?: return

    event.preventDefault()
    if (event.shiftKey) {
        outdentListItem(listContext)
    } else {
        createSublist(listContext)
    }
}

fun MarkdownRichTextEditor.handleDeleteKeys(event: KeyboardEvent) {
    val selection = getValidSelection() ?: return
    val range = selection.getRangeAt(0)
    if (range.collapsed) {
        handleFormattingTagDeletion(event, range)
    }
}

fun MarkdownRichTextEditor.handleEnterKey(event: KeyboardEvent) {
    event.preventDefault()
    event.stopPropagation()

    // Move the debounce here to protect ALL Enter key interactions
    if (isProcessingEnter) return
    isProcessingEnter = true
    window.setTimeout({ isProcessingEnter = false }, 50)

    val selection = getValidSelection() ?: return
    val listContext = getListContext(selection)

    when {
        listContext != null -> handleEnterInListContext(listContext)
        isInNodeType(selection, "PRE") -> handleEnterInCodeBlock(findParentNode(selection, "PRE"), event)
        else -> handleEnterInRegularContext()
    }
}

fun MarkdownRichTextEditor.exitListToNewParagraph(listContext: MarkdownRichTextEditor.ListContext) {
    try {
        val listItem = listContext.listItem
        val listElement = listContext.listElement

        val newParagraph = createParagraphWithContent()

        if (listElement.childNodes.length == 1) {
            listElement.parentNode?.replaceChild(newParagraph, listElement)
        } else {
            listElement.removeChild(listItem)
            if (listElement.childNodes.length == 0) {
                listElement.parentNode?.replaceChild(newParagraph, listElement)
            } else {
                listElement.parentNode?.insertBefore(newParagraph, listElement.nextSibling)
            }
        }

        positionCursorInNewParagraph(newParagraph)
        updateCursorPosition()
        updateEditorState()
    } catch (e: Throwable) {
        console.log("Error exiting list to new paragraph: ${e.message}")
    }
}

fun MarkdownRichTextEditor.positionCursorInNewParagraph(paragraph: HTMLElement) {
    try {
        val selection = getValidSelection() ?: return
        val range = document.createRange()
        range.selectNodeContents(paragraph)
        range.collapse(true)

        selection.removeAllRanges()
        selection.addRange(range)
    } catch (e: Throwable) {
        console.log("Error positioning cursor in new paragraph: ${e.message}")
    }
}

fun MarkdownRichTextEditor.handleFormattingTagDeletion(event: KeyboardEvent, range: dynamic) {
    val currentNode = range.startContainer
    if (currentNode.nodeType != Node.TEXT_NODE) return

    val isAtStart = range.startOffset == 0 && event.key == "Backspace"
    val isAtEnd = range.startOffset == currentNode.textContent.length && event.key == "Delete"

    if (isAtStart || isAtEnd) {
        val parentNode = currentNode.parentNode
        if (parentNode != null && parentNode != textArea.element) {
            val tagName = parentNode.nodeName
            if (tagName in FORMATTING_TAGS && isElementEmpty(parentNode as HTMLElement)) {
                event.preventDefault()
                unwrapTag(tagName)
            }
        }
    }
}

fun MarkdownRichTextEditor.handleEnterInRegularContext() {
    val editorRootElement = textArea.element as? HTMLElement ?: return
    val selection = getValidSelection() ?: return
    val range = selection.getRangeAt(0)

    if (!range.collapsed) {
        range.deleteContents()
    }

    val currentBlock = findContainingBlockElement(range.startContainer, editorRootElement)
    val newParagraph = document.createElement("p") as HTMLElement

    if (currentBlock != null) {
        val afterCursorRange = document.createRange()
        afterCursorRange.setStart(range.startContainer, range.startOffset)
        if (currentBlock.lastChild != null) {
            afterCursorRange.setEndAfter(currentBlock.lastChild!!)
        } else {
            afterCursorRange.setEnd(currentBlock, 0)
        }

        try {
            val afterContent = afterCursorRange.extractContents()
            val hasActualContent = afterContent.textContent?.trim()?.isNotEmpty() == true ||
                    afterContent.querySelector("img, br, a") != null

            if (hasActualContent) {
                newParagraph.appendChild(afterContent)
            } else {
                newParagraph.appendChild(document.createElement("br")) // Use <br> instead of \u200B
            }
        } catch (e: Throwable) {
            newParagraph.appendChild(document.createElement("br"))
        }

        // Clean up the original block if extracting the contents left it completely empty
        if (currentBlock.childNodes.length == 0 || currentBlock.textContent?.trim()?.isEmpty() == true) {
            currentBlock.innerHTML = "" // Clear out any lingering \u200B
            currentBlock.appendChild(document.createElement("br"))
        }

        currentBlock.parentNode?.insertBefore(newParagraph, currentBlock.nextSibling)
    } else {
        // FIX: Text is floating directly in the editor root.

        // 1. Extract the "after" content FIRST using the root element to preserve range validity.
        val afterCursorRange = document.createRange()
        afterCursorRange.setStart(range.startContainer, range.startOffset)
        afterCursorRange.setEnd(editorRootElement, editorRootElement.childNodes.length)

        try {
            val afterContent = afterCursorRange.extractContents()
            val hasActualContent = afterContent.textContent?.trim()?.isNotEmpty() == true ||
                    afterContent.querySelector("img, br, a") != null

            if (hasActualContent) {
                newParagraph.appendChild(afterContent)
            } else {
                newParagraph.appendChild(document.createElement("br"))
            }
        } catch (e: Throwable) {
            newParagraph.appendChild(document.createElement("br"))
        }

        // 2. Whatever is LEFT in the editor is the "before" content. Move it to a new block.
        val beforeParagraph = document.createElement("p") as HTMLElement
        while (editorRootElement.firstChild != null) {
            beforeParagraph.appendChild(editorRootElement.firstChild!!)
        }

        if (beforeParagraph.childNodes.length == 0 || beforeParagraph.textContent?.trim()?.isEmpty() == true) {
            beforeParagraph.innerHTML = ""
            beforeParagraph.appendChild(document.createElement("br"))
        }

        // 3. Append both cleanly wrapped blocks back into the editor root
        editorRootElement.appendChild(beforeParagraph)
        editorRootElement.appendChild(newParagraph)
    }

    // Setting the cursor at the start of a paragraph with a <br> perfectly positions the caret
    positionCursorInElement(newParagraph, true)
    updateCursorPosition()
    updateEditorState()
    notifyContentChanged()
}

fun MarkdownRichTextEditor.findContainingBlockElement(node: Node, editorRoot: HTMLElement): HTMLElement? {
    var currentNode = if (node.nodeType == Node.TEXT_NODE) node.parentNode else node

    while (currentNode != null && currentNode != editorRoot) {
        if (currentNode.nodeType == Node.ELEMENT_NODE) {
            val nodeName = currentNode.nodeName
            if (nodeName in BLOCK_ELEMENTS) {
                return currentNode as HTMLElement
            }
        }
        currentNode = currentNode.parentNode
    }
    return null
}

fun MarkdownRichTextEditor.handleEnterInList(listItem: HTMLElement?, isShiftEnter: Boolean = false) {
    if (listItem == null) return

    val selection = getValidSelection() ?: return
    val range = selection.getRangeAt(0)
    val listParent = findListParent(listItem) ?: return

    if (isElementEmpty(listItem) && !isShiftEnter) {
        val listContext = MarkdownRichTextEditor.ListContext(
            listItem = listItem,
            listElement = listParent,
            formattingElements = emptyList(),
            selection = selection
        )
        exitListToNewParagraph(listContext)
    } else {
        createNewListItem(listItem, listParent, range, selection, isShiftEnter)
    }

    updateCursorPosition()
    updateEditorState()
}

fun MarkdownRichTextEditor.findListParent(listItem: HTMLElement): HTMLElement? {
    var node = listItem.parentNode
    while (node != null && node != textArea.element) {
        if (node.nodeName in listOf("UL", "OL")) {
            return node as HTMLElement
        }
        node = node.parentNode
    }
    return null
}

fun MarkdownRichTextEditor.createNewListItem(
    currentItem: HTMLElement,
    listParent: HTMLElement,
    range: dynamic,
    selection: dynamic,
    isShiftEnter: Boolean
) {
    val newListItem = document.createElement("li") as HTMLElement

    if (isShiftEnter) {
        createFormattedListItem(newListItem, currentItem, selection)
    } else {
        createRegularListItem(newListItem, range, currentItem)
    }

    listParent.insertBefore(newListItem, currentItem.nextSibling)
    positionCursorInListItem(newListItem, isShiftEnter, selection)
}

fun MarkdownRichTextEditor.createFormattedListItem(
    newListItem: HTMLElement,
    currentItem: HTMLElement,
    selection: dynamic
) {
    val formatTags = findFormattingElements(selection.anchorNode, currentItem)
    var innerElement = newListItem

    formatTags.reversed().forEach { tag ->
        val formattingElement = document.createElement(tag) as HTMLElement
        innerElement.appendChild(formattingElement)
        innerElement = formattingElement
    }

    innerElement.appendChild(document.createTextNode(ZERO_WIDTH_SPACE))
}

fun MarkdownRichTextEditor.createRegularListItem(newListItem: HTMLElement, range: dynamic, currentItem: HTMLElement) {
    if (!range.collapsed) {
        newListItem.appendChild(range.extractContents())
    } else {
        val afterCursorRange = document.createRange()
        afterCursorRange.setStart(range.endContainer, range.endOffset)
        afterCursorRange.setEndAfter(currentItem.lastChild ?: currentItem)

        val afterContent = afterCursorRange.extractContents()
        if (afterContent.childNodes.length > 0) {
            newListItem.appendChild(afterContent)
        }
    }

    if (isElementEmpty(newListItem)) {
        newListItem.appendChild(document.createTextNode(ZERO_WIDTH_SPACE))
    }
}

fun MarkdownRichTextEditor.findFormattingElements(node: Node, listItem: HTMLElement): List<String> {
    val formatTags = mutableListOf<String>()
    var current: Node? = node

    while (current != null && current != listItem) {
        if (current.nodeType == Node.ELEMENT_NODE) {
            val nodeName = current.nodeName
            if (nodeName in listOf("STRONG", "B", "EM", "I", "CODE")) {
                formatTags.add(nodeName)
            }
        }
        current = current.parentNode
    }

    return formatTags
}

fun MarkdownRichTextEditor.positionCursorInListItem(
    newListItem: HTMLElement,
    isShiftEnter: Boolean,
    selection: dynamic
) {
    try {
        val newRange = document.createRange()

        if (isShiftEnter && newListItem.firstChild?.nodeName in listOf("STRONG", "B", "EM", "I", "CODE")) {
            positionCursorInFormattedListItem(newListItem, newRange)
        } else {
            newRange.selectNodeContents(newListItem)
            newRange.collapse(true)
        }

        selection.removeAllRanges()
        selection.addRange(newRange)
    } catch (e: Throwable) {
        console.log("Error positioning cursor in new list item: ${e.message}")
    }
}

fun MarkdownRichTextEditor.positionCursorInFormattedListItem(newListItem: HTMLElement, newRange: dynamic) {
    var deepestElement = newListItem.firstChild as HTMLElement
    while (deepestElement.firstChild != null && deepestElement.firstChild!!.nodeType == Node.ELEMENT_NODE) {
        deepestElement = deepestElement.firstChild as HTMLElement
    }

    val textNode = deepestElement.firstChild as? Text
        ?: deepestElement.childNodes.asList().find { it.nodeType == Node.TEXT_NODE } as? Text
        ?: document.createTextNode(ZERO_WIDTH_SPACE).also { deepestElement.appendChild(it) }

    newRange.setStart(textNode, 0)
}
