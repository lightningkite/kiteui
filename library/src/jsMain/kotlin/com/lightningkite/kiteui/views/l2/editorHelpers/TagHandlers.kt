package com.lightningkite.kiteui.views.l2.editorHelpers


import com.lightningkite.reactive.context.invoke
import com.lightningkite.kiteui.views.l2.RichTextTags
import com.lightningkite.kiteui.views.l2.MarkdownRichTextEditor
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.Range
import org.w3c.dom.Text


private fun MarkdownRichTextEditor.safeExecute(operation: String, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        console.log("Error in $operation: ${e.message}")
    }
}


fun MarkdownRichTextEditor.unwrapTag(tagName: String) {
    val selection = getValidSelection() ?: return
    val currentNode = getEffectiveNode(selection.anchorNode)

    val targetElement = traverseUpDOM(currentNode, textArea.element) { node ->
        node.nodeName == tagName
    } as? HTMLElement ?: return

    val range = selection.getRangeAt(0)
    if (range != null && !range.collapsed) {
        // Handle character-level granular unformatting by splitting the tag
        unwrapPartialElement(targetElement, tagName, range)
    } else {
        // Fallback to full unwrap if selection is collapsed (just a cursor)
        unwrapElement(targetElement, selection)
    }
}

fun MarkdownRichTextEditor.unwrapPartialElement(targetElement: HTMLElement, tagName: String, range: dynamic) {
    val parent = targetElement.parentNode ?: return

    try {
        // 1. Extract content after the selection boundary
        val afterRange = document.createRange()
        afterRange.setStart(range.endContainer, range.endOffset)
        if (targetElement.lastChild != null) {
            afterRange.setEndAfter(targetElement.lastChild!!)
        }
        val afterContent = afterRange.extractContents()

        // 2. Extract content before the selection boundary
        val beforeRange = document.createRange()
        if (targetElement.firstChild != null) {
            beforeRange.setStartBefore(targetElement.firstChild!!)
        }
        beforeRange.setEnd(range.startContainer, range.startOffset)
        val beforeContent = beforeRange.extractContents()

        // At this point, targetElement contains ONLY the user's selected nodes

        // 3. Re-wrap and insert leading content if it exists
        if (beforeContent.childNodes.length > 0) {
            val beforeElement = document.createElement(tagName) as HTMLElement
            beforeElement.appendChild(beforeContent)
            parent.insertBefore(beforeElement, targetElement)
        }

        // 4. Re-wrap and insert trailing content if it exists
        if (afterContent.childNodes.length > 0) {
            val afterElement = document.createElement(tagName) as HTMLElement
            afterElement.appendChild(afterContent)
            parent.insertBefore(afterElement, targetElement.nextSibling)
        }

        // 5. Move the remaining selected nodes out of the tag container
        val unwrappedFragment = document.createDocumentFragment()
        while (targetElement.firstChild != null) {
            unwrappedFragment.appendChild(targetElement.firstChild!!)
        }

        val firstUnwrappedNode = unwrappedFragment.firstChild
        val lastUnwrappedNode = unwrappedFragment.lastChild

        parent.insertBefore(unwrappedFragment, targetElement)
        parent.removeChild(targetElement)

        notifyContentChanged()

        // 6. Seamlessly restore the user's highlighted selection range on the unwrapped letters
        if (firstUnwrappedNode != null && lastUnwrappedNode != null) {
            val newRange = document.createRange()
            newRange.setStartBefore(firstUnwrappedNode)
            newRange.setEndAfter(lastUnwrappedNode)

            val sel = window.asDynamic().getSelection()
            sel?.removeAllRanges()
            sel?.addRange(newRange)
        }
    } catch (e: Throwable) {
        console.log("Error during precise tag split: ${e.message}")
        // Safe fallback to full unwrap if a DOM mutation boundary calculation fails
        unwrapElement(targetElement, window.asDynamic().getSelection())
    }
}

fun MarkdownRichTextEditor.unwrapTagForSelection(tagName: String, selectionRange: dynamic) {
    val selection = getValidSelection() ?: return
    val selectedText = selection.toString()

    val editorElement = textArea.element as? HTMLElement ?: return
    val targetElements = editorElement.querySelectorAll(tagName.lowercase())

    var elementToUnwrap: HTMLElement? = null

    for (i in 0 until targetElements.length) {
        val element = targetElements.item(i) as? HTMLElement ?: continue
        val elementText = element.textContent ?: ""

        if (elementText == selectedText && selectedText.isNotEmpty()) {
            elementToUnwrap = element
            break
        }
    }

    if (elementToUnwrap != null) {
        unwrapElement(elementToUnwrap, selection)
    } else {
        unwrapTag(tagName)
    }
}

fun MarkdownRichTextEditor.unwrapElement(element: HTMLElement, selection: dynamic) {
    val parent = element.parentNode ?: return
    val fragment = document.createDocumentFragment()
    val originalText = element.textContent ?: ""

    while (element.firstChild != null) {
        fragment.appendChild(element.firstChild!!)
    }

    if (fragment.childNodes.length == 0) {
        fragment.appendChild(document.createTextNode(ZERO_WIDTH_SPACE))
    }

    // FIX: Track the boundary nodes before they are added to the live DOM tree
    val firstUnwrappedNode = fragment.firstChild
    val lastUnwrappedNode = fragment.lastChild

    parent.insertBefore(fragment, element)
    parent.removeChild(element)

    notifyContentChanged()

    if (originalText.isNotEmpty() && firstUnwrappedNode != null && lastUnwrappedNode != null) {
        safeExecute("restoring selection after unwrap") {
            val newRange = document.createRange()
            newRange.setStartBefore(firstUnwrappedNode)
            newRange.setEndAfter(lastUnwrappedNode)

            val sel = window.asDynamic().getSelection()
            sel?.removeAllRanges()
            sel?.addRange(newRange)
        }
    } else {
        positionCursorAfterUnwrap(parent, element, selection)
    }

    launch { updateSelectedRichTextTag() }
}


fun MarkdownRichTextEditor.positionCursorAfterUnwrap(parent: Node, element: HTMLElement, selection: dynamic) {
    safeExecute("positioning cursor after unwrap") {
        val range = document.createRange()

        if (parent.childNodes.length > 0) {
            var targetNode: Node? = null
            for (i in 0 until parent.childNodes.length) {
                val child = parent.childNodes.item(i)
                if (child?.nodeType == Node.TEXT_NODE && child.textContent?.isNotEmpty() == true) {
                    targetNode = child
                    break
                }
            }

            if (targetNode != null) {
                range.setStart(targetNode, 0)
                range.collapse(true)
            } else {
                range.selectNodeContents(parent)
                range.collapse(false)
            }
        } else {
            range.selectNodeContents(parent)
            range.collapse(false)
        }

        val sel = window.asDynamic().getSelection()
        sel?.removeAllRanges()
        sel?.addRange(range)
    }
}

suspend fun MarkdownRichTextEditor.updateSelectedRichTextTag() {
    val selection = getValidSelection() ?: return
    val range = selection.getRangeAt(0)
    val detectedTags = mutableSetOf<RichTextTags>()

    if (range.collapsed) {
        detectTagsForCollapsedSelection(selection.anchorNode, detectedTags)
    } else {
        detectTagsForRangeSelection(range, detectedTags)
    }

    // FIX 1: Code vs Code Block mutual exclusivity
    if (detectedTags.contains(RichTextTags.CODE_BLOCK)) {
        detectedTags.remove(RichTextTags.CODE)
    }

    // FIX 2: Header mutual exclusivity
    val headerTags = listOf(RichTextTags.HEADER1, RichTextTags.HEADER2, RichTextTags.HEADER3)
    val foundHeaders = detectedTags.filter { it in headerTags }

    if (foundHeaders.size > 1) {
        // Keep the first detected header (the innermost one closest to the cursor)
        // and safely remove the rest from the active UI state.
        val headerToKeep = foundHeaders.first()
        headerTags.forEach { header ->
            if (header != headerToKeep) {
                detectedTags.remove(header)
            }
        }
    }

    updateTagsIfChanged(detectedTags)
}

fun MarkdownRichTextEditor.detectTagsForCollapsedSelection(anchorNode: Node?, detectedTags: MutableSet<RichTextTags>) {
    traverseUpDOM(anchorNode, textArea.element) { node ->
        getTagForNode(node)?.let { tag ->
            detectedTags.add(tag)
        }
        false
    }
}

fun MarkdownRichTextEditor.detectTagsForRangeSelection(range: dynamic, detectedTags: MutableSet<RichTextTags>) {
    val commonAncestor = getEffectiveNode(range.commonAncestorContainer)

    traverseUpDOM(commonAncestor, textArea.element) { ancestor ->
        getTagForNode(ancestor)?.let { tag ->
            if (containsEntireSelection(ancestor, range)) {
                detectedTags.add(tag)
            }
        }
        false
    }

    detectTagsForPartialSelection(range, commonAncestor, detectedTags)
    detectTagsForIntersectingSelection(range, detectedTags)
}

fun MarkdownRichTextEditor.detectTagsForPartialSelection(
    range: dynamic,
    commonAncestor: Node?,
    detectedTags: MutableSet<RichTextTags>
) {
    if (range.startContainer.nodeType == Node.TEXT_NODE) {
        val immediateParent = range.startContainer.parentNode
        getTagForNode(immediateParent)?.let { tag ->
            detectedTags.add(tag)
        }
    }

    val startNode = if (range.startContainer.nodeType == Node.TEXT_NODE) {
        range.startContainer.parentNode
    } else {
        range.startContainer
    }

    traverseUpDOM(startNode, textArea.element) { node ->
        getTagForNode(node)?.let { tag ->
            detectedTags.add(tag)
        }
        false
    }

    val endNode = if (range.endContainer.nodeType == Node.TEXT_NODE) {
        range.endContainer.parentNode
    } else {
        range.endContainer
    }

    traverseUpDOM(endNode, textArea.element) { node ->
        getTagForNode(node)?.let { tag ->
            detectedTags.add(tag)
        }
        false
    }
}

fun MarkdownRichTextEditor.detectTagsForIntersectingSelection(
    range: dynamic,
    detectedTags: MutableSet<RichTextTags>
) {
    val editorElement = textArea.element as? HTMLElement ?: return
    val formattingElements =
        editorElement.querySelectorAll("strong, b, em, i, code, h1, h2, h3, blockquote, pre, u, s, del, sup, sub, a")

    for (i in 0 until formattingElements.length) {
        val element = formattingElements.item(i) as? HTMLElement ?: continue

        if (doesElementIntersectWithSelection(element, range)) {
            getTagForNode(element)?.let { tag ->
                detectedTags.add(tag)
            }
        }
    }
}

fun MarkdownRichTextEditor.doesElementIntersectWithSelection(element: HTMLElement, selectionRange: dynamic): Boolean {
    try {
        val elementRange = document.createRange()
        elementRange.selectNodeContents(element)

        val selection = window.asDynamic().getSelection()
        val selectedText = selection.toString()
        val elementText = element.textContent

        if (selectedText == elementText && selectedText.isNotEmpty()) {
            return true
        }

        val startComparison = selectionRange.compareBoundaryPoints(Range.START_TO_END, elementRange)
        val endComparison = selectionRange.compareBoundaryPoints(Range.END_TO_START, elementRange)

        return startComparison <= 0 && endComparison >= 0
    } catch (e: Throwable) {
        return false
    }
}

suspend fun MarkdownRichTextEditor.updateTagsIfChanged(detectedTags: Set<RichTextTags>) {
    if (currentSelectedRichTextTags.invoke() != detectedTags) {
        currentSelectedRichTextTags.set(detectedTags)
    }
}

fun MarkdownRichTextEditor.getTagForNode(node: Node?): RichTextTags? {
    return when (node?.nodeName) {
        "H1" -> RichTextTags.HEADER1
        "H2" -> RichTextTags.HEADER2
        "H3" -> RichTextTags.HEADER3
        "STRONG", "B" -> RichTextTags.BOLD
        "EM", "I" -> RichTextTags.ITALIC
        "U" -> RichTextTags.UNDERLINE
        "S", "DEL", "STRIKE" -> RichTextTags.STRIKETHROUGH
        "SUP" -> RichTextTags.SUPERSCRIPT
        "SUB" -> RichTextTags.SUBSCRIPT
        "A" -> RichTextTags.LINK
        "CODE" -> {
            // FIX: Ignore the CODE tag if it is just the inner wrapper of a PRE code block
            if (node.parentNode?.nodeName == "PRE") {
                null
            } else {
                RichTextTags.CODE
            }
        }

        "PRE" -> RichTextTags.CODE_BLOCK
        "BLOCKQUOTE" -> RichTextTags.QOUTE
        "LI" -> getListItemTag(node)
        else -> null
    }
}

fun MarkdownRichTextEditor.getListItemTag(listItemNode: Node): RichTextTags? {
    return when (listItemNode.parentNode?.nodeName) {
        "UL" -> RichTextTags.UNORDERED_LIST
        "OL" -> RichTextTags.ORDERED_LIST
        else -> null
    }
}

fun MarkdownRichTextEditor.findParentNode(selection: dynamic, nodeType: String): HTMLElement? {
    val startNode = getEffectiveNode(selection?.anchorNode)
    return traverseUpDOM(startNode, textArea.element) { node ->
        node.nodeName == nodeType
    } as? HTMLElement
}

fun MarkdownRichTextEditor.insertHtmlElement(tagName: String) {
    val editorRootElement = textArea.element as? HTMLElement ?: return

    val selection = getValidSelection() ?: return
    val anchorDomNode = selection.anchorNode

    if (!isNodeInsideEditor(anchorDomNode, editorRootElement)) return

    val selectionRange = selection.getRangeAt(0)
    val upperCaseTagName = tagName.uppercase()

    // --- NEW LOGIC: Prevent Header Nesting ---
    val headerTags = setOf("H1", "H2", "H3")
    if (upperCaseTagName in headerTags) {
        val currentHeader = traverseUpDOM(anchorDomNode, editorRootElement) {
            it.nodeName in headerTags
        } as? HTMLElement

        if (currentHeader != null) {
            if (currentHeader.nodeName == upperCaseTagName) {
                // Toggle OFF: If they clicked the exact same header, convert it back to a normal paragraph
                replaceTagWith(currentHeader, "P")
            } else {
                // SWAP: If they clicked a different header, change the tag entirely instead of nesting
                replaceTagWith(currentHeader, upperCaseTagName)
            }
            updateCursorAndTags(editorRootElement)
            return
        }
    }
    // ----------------------------------------

    val isInsideTag = isSelectionInsideTag(upperCaseTagName)

    if (isInsideTag) {
        handleExistingTag(tagName, upperCaseTagName, selectionRange)
        return
    }

    insertNewHtmlElement(tagName, selectionRange, editorRootElement)
}

fun MarkdownRichTextEditor.handleExistingTag(tagName: String, upperCaseTagName: String, selectionRange: dynamic) {
    val isInlineFormattingTag = tagName.lowercase() in setOf("strong", "b", "em", "i", "u", "s", "del", "sup", "sub")

    if (isInlineFormattingTag && selectionRange.collapsed) {
        moveCursorOutsideTag(upperCaseTagName)
        launch { updateSelectedRichTextTag() }
    } else {
        unwrapTagForSelection(upperCaseTagName, selectionRange)
    }
}

fun MarkdownRichTextEditor.insertNewHtmlElement(
    tagName: String,
    selectionRange: dynamic,
    editorRootElement: HTMLElement
) {
    safeExecute("inserting HTML element") {
        val newHtmlElement = document.createElement(tagName) as HTMLElement

        if (selectionRange.collapsed) {
            insertElementForCollapsedSelection(newHtmlElement, selectionRange)
        } else {
            insertElementForRangeSelection(newHtmlElement, selectionRange)
        }

        notifyContentChanged()
        updateCursorAndTags(editorRootElement)
    }
}

fun MarkdownRichTextEditor.insertElementForCollapsedSelection(element: HTMLElement, range: dynamic) {
    element.appendChild(document.createTextNode(ZERO_WIDTH_SPACE))
    range.insertNode(element)
    placeCursorInsideElement(element)
}

fun MarkdownRichTextEditor.insertElementForRangeSelection(element: HTMLElement, range: dynamic) {
    if (range.collapsed) return
    val content = range.extractContents()
    element.appendChild(content)
    range.insertNode(element)


    val selectionRange = document.createRange()
    selectionRange.selectNodeContents(element)
    val sel = window.asDynamic().getSelection()
    sel?.removeAllRanges()
    sel?.addRange(selectionRange)
}

fun MarkdownRichTextEditor.updateCursorAndTags(editorRootElement: HTMLElement) {
    window.setTimeout({
        cursorIndex = getHtmlAdjustedSelectionRange(editorRootElement)
        launch { updateSelectedRichTextTag() }
    }, 10)
}


// FIXED: Changed childNodes[i] syntax to use native browser DOM .item(i)
fun MarkdownRichTextEditor.findNestedList(listItem: HTMLElement): HTMLElement? {
    for (i in 0 until listItem.childNodes.length) {
        val child = listItem.childNodes.item(i)
        if (child?.nodeType == Node.ELEMENT_NODE && (child.nodeName == "UL" || child.nodeName == "OL")) {
            return child as HTMLElement
        }
    }
    return null
}

fun MarkdownRichTextEditor.createEmptyListItem(): HTMLElement {
    val listItem = document.createElement("li") as HTMLElement
    listItem.appendChild(document.createTextNode("\u200B"))
    return listItem
}

fun MarkdownRichTextEditor.ensureElementHasContent(element: HTMLElement) {
    if (element.childNodes.length == 0 || element.textContent?.trim()?.isEmpty() == true) {
        element.appendChild(document.createTextNode("\u200B"))
    }
}

fun MarkdownRichTextEditor.moveAllChildren(source: HTMLElement, target: HTMLElement) {
    while (source.firstChild != null) {
        target.appendChild(source.firstChild!!)
    }
}

fun MarkdownRichTextEditor.getOrCreateNestedList(listItem: HTMLElement, listType: String): HTMLElement {
    return findNestedList(listItem) ?: run {
        val nestedList = document.createElement(listType) as HTMLElement
        listItem.appendChild(nestedList)
        nestedList
    }
}

fun MarkdownRichTextEditor.removeListIfEmpty(listElement: HTMLElement, parentElement: HTMLElement) {
    if (listElement.childNodes.length == 0) {
        parentElement.removeChild(listElement)
    }
}

fun MarkdownRichTextEditor.updateCursorTracking() {
    val element = textArea.element as? HTMLElement
    if (element != null) {
        cursorIndex = getHtmlAdjustedSelectionRange(element)
        launch {
            updateSelectedRichTextTag()
        }
    }
}

fun MarkdownRichTextEditor.setCursorPosition(selection: dynamic, range: Range) {
    try {
        selection.removeAllRanges()
        selection.addRange(range)
    } catch (e: Throwable) {
        console.log("Error setting cursor position: ${e.message}")
    }
}


fun MarkdownRichTextEditor.isSelectionInsideElement(selection: dynamic, element: HTMLElement): Boolean {
    val anchorNode = selection.anchorNode
    var node: dynamic = anchorNode
    while (node != null) {
        if (node == element) return true
        node = node.parentNode
    }
    return false
}

fun MarkdownRichTextEditor.outdentListItem(context: MarkdownRichTextEditor.ListContext) {
    val listItem = context.listItem
    val listElement = context.listElement

    val parentListItem = listElement.parentNode as? HTMLElement
    if (parentListItem?.nodeName == "LI") {

        val parentList = parentListItem.parentNode as? HTMLElement
        if (parentList?.nodeName == "UL" || parentList?.nodeName == "OL") {
            listElement.removeChild(listItem)
            parentList.insertBefore(listItem, parentListItem.nextSibling)
            removeListIfEmpty(listElement, parentListItem)
            focusAndPositionCursor(listItem)
        }
    } else {
        console.log("Cannot outdent: not in a nested list")
    }
}


fun MarkdownRichTextEditor.createSublist(context: MarkdownRichTextEditor.ListContext) {
    val listItem = context.listItem
    val listElement = context.listElement

    val previousListItem = listItem.previousElementSibling as? HTMLElement

    //  If there is no previous list item, do nothing.
    if (previousListItem == null || previousListItem.nodeName != "LI") {
        return
    }

    if (previousListItem.nodeName == "LI") {
        val nestedList = getOrCreateNestedList(previousListItem, listElement.nodeName)

        listElement.removeChild(listItem)
        nestedList.appendChild(listItem)

        focusAndPositionCursor(listItem)
    } else {
        if (listElement.firstChild == listItem) {
            val newListItem = createEmptyListItem()
            listElement.insertBefore(newListItem, listItem)

            val nestedList = document.createElement(listElement.nodeName) as HTMLElement

            listElement.removeChild(listItem)
            nestedList.appendChild(listItem)

            newListItem.appendChild(nestedList)
            focusAndPositionCursor(listItem)
        } else {
            console.log("Cannot create sublist: unexpected list structure")
        }
    }
}

fun MarkdownRichTextEditor.getListContext(selection: dynamic): MarkdownRichTextEditor.ListContext? {
    if (selection == null || selection.rangeCount == 0) return null

    var currentNode = selection.anchorNode
    if (currentNode?.nodeType == Node.TEXT_NODE) {
        currentNode = currentNode.parentNode
    }

    val formattingElements = mutableListOf<HTMLElement>()
    var listItem: HTMLElement? = null
    var listElement: HTMLElement? = null

    var node: Node? = currentNode
    while (node != null && node != textArea.element) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            val nodeName = node.nodeName.uppercase()

            if (nodeName == "LI" && listItem == null) {
                listItem = node as HTMLElement
            } else if ((nodeName == "UL" || nodeName == "OL") && listElement == null) {
                listElement = node as HTMLElement
            } else if (nodeName in listOf("STRONG", "B", "EM", "I", "CODE")) {
                formattingElements.add(0, node as HTMLElement)
            }
        }
        node = node.parentNode
    }

    if (listItem != null && listElement != null) {
        return MarkdownRichTextEditor.ListContext(listItem, listElement, formattingElements, selection)
    }

    return null
}


fun MarkdownRichTextEditor.handleExitList(listItem: HTMLElement, listElement: HTMLElement) {
    val newParagraph = document.createElement("p")
    newParagraph.appendChild(document.createElement("br"))

    if (listElement.childNodes.length == 1) {
        listElement.parentNode?.replaceChild(newParagraph, listElement)
    } else {
        listElement.parentNode?.insertBefore(newParagraph, listElement.nextSibling)
        listElement.removeChild(listItem)
    }

    focusAndPositionCursor(newParagraph as HTMLElement)
}

// FIXED: Changed two instances of childNodes[i] syntax to use .item(i)
fun MarkdownRichTextEditor.handleFormattedCursorPositioning(
    newListItem: HTMLElement,
    formattingElements: List<HTMLElement>,
    selection: dynamic
) {
    var targetElement: Node = newListItem
    var formatIndex = 0

    while (formatIndex < formattingElements.size) {
        val expectedTagName = formattingElements[formatIndex].tagName

        var matchingChild: Node? = null
        for (i in 0 until targetElement.childNodes.length) {
            val child = targetElement.childNodes.item(i)
            if (child?.nodeType == Node.ELEMENT_NODE && child.nodeName == expectedTagName) {
                matchingChild = child
                break
            }
        }

        targetElement = matchingChild ?: break
        formatIndex++
    }

    var textNode: Text? = null
    for (i in 0 until targetElement.childNodes.length) {
        val child = targetElement.childNodes.item(i)
        if (child?.nodeType == Node.TEXT_NODE) {
            textNode = child as Text
            break
        }
    }

    if (textNode == null) {
        textNode = document.createTextNode(ZERO_WIDTH_SPACE)
        targetElement.appendChild(textNode)
    }

    val newRange = document.createRange()
    newRange.setStart(textNode, 0)
    newRange.collapse(true)

    setCursorPosition(selection, newRange)
}

fun MarkdownRichTextEditor.handleEnterInListContext(context: MarkdownRichTextEditor.ListContext) {
    val listItem = context.listItem
    val listElement = context.listElement
    val formattingElements = context.formattingElements
    val selection = context.selection

    // If the current list item is empty, determine whether to outdent or exit completely
    if (isListItemEmpty(listItem)) {
        val parentListItem = listElement.parentNode as? HTMLElement
        if (parentListItem?.nodeName == "LI") {
            // We are inside a nested list -> Move up one indentation level
            outdentListItem(context)
            updateCursorTracking()
        } else {
            // We are already at the root list level -> Exit to a regular paragraph
            handleExitList(listItem, listElement)
        }
        return
    }

    val newListItem = document.createElement("li")
    val range = selection.getRangeAt(0)

    if (!range.collapsed) {
        range.extractContents()
    }

    val afterCursorRange = document.createRange()
    afterCursorRange.setStart(range.endContainer, range.endOffset)

    if (listItem.lastChild != null) {
        afterCursorRange.setEndAfter(listItem.lastChild!!)
    } else {
        afterCursorRange.setEnd(listItem, 0)
    }

    try {
        val afterContent = afterCursorRange.extractContents()

        if (formattingElements.isNotEmpty() && afterContent.childNodes.length > 0) {
            applyFormattingToFragment(afterContent, formattingElements)
        }

        if (afterContent.childNodes.length > 0) {
            newListItem.appendChild(afterContent)
        } else {
            newListItem.appendChild(document.createElement("br"))
        }
    } catch (e: Throwable) {
        console.log("Error extracting content after cursor: ${e.message}")
        newListItem.appendChild(document.createElement("br"))
    }

    ensureElementHasContent(newListItem as HTMLElement)
    listElement.insertBefore(newListItem, listItem.nextSibling)

    if (formattingElements.isNotEmpty()) {
        handleFormattedCursorPositioning(newListItem as HTMLElement, formattingElements, selection)
    } else {
        focusAndPositionCursor(newListItem as HTMLElement)
    }

    updateCursorTracking()
}


fun MarkdownRichTextEditor.findListElements(selection: dynamic): Pair<HTMLElement?, HTMLElement?> {
    var currentNode = selection.anchorNode

    if (currentNode?.nodeType == Node.TEXT_NODE) {
        currentNode = currentNode.parentNode
    }

    var listItem: HTMLElement? = null
    var listContainer: HTMLElement? = null

    while (currentNode != null && currentNode != textArea.element) {
        if (currentNode.nodeName == "LI") {
            listItem = currentNode as HTMLElement
        } else if (currentNode.nodeName == "UL" || currentNode.nodeName == "OL") {
            listContainer = currentNode as HTMLElement
            break
        }
        currentNode = currentNode.parentNode
    }

    return Pair(listItem, listContainer)
}

fun MarkdownRichTextEditor.unwrapList() {
    val selection = getValidSelection() ?: return
    val (listItem, listContainer) = findListElements(selection)

    if (listItem == null || listContainer == null) return

    val paragraph = document.createElement("p") as HTMLElement
    moveAllChildren(listItem, paragraph)
    ensureElementHasContent(paragraph)

    val parent = listContainer.parentNode

    if (listContainer.childNodes.length == 1) {
        parent?.replaceChild(paragraph, listContainer)
    } else {
        val afterNode = listContainer.nextSibling
        listContainer.removeChild(listItem)
        if (afterNode != null) {
            parent?.insertBefore(paragraph, afterNode)
        } else {
            parent?.appendChild(paragraph)
        }
        removeListIfEmpty(listContainer, parent as HTMLElement)
    }

    try {
        val newRange = document.createRange()
        newRange.selectNodeContents(paragraph)
        newRange.collapse(false)

        setCursorPosition(selection, newRange)
        updateCursorTracking()
    } catch (e: Throwable) {
        console.log("Error setting range after unwrapping list: ${e.message}")
    }
}

fun MarkdownRichTextEditor.createAndInsertList(
    ordered: Boolean,
    range: Range,
    blockElement: HTMLElement?,
    listParentElement: HTMLElement,
    liElement: HTMLElement
) {
    when {
        blockElement != null -> {
            val content = blockElement.innerHTML
            liElement.innerHTML = content
            listParentElement.appendChild(liElement)
            blockElement.parentNode?.replaceChild(listParentElement, blockElement)
        }

        !range.collapsed -> {
            val content = range.extractContents()
            liElement.appendChild(content)
            listParentElement.appendChild(liElement)
            range.insertNode(listParentElement)
        }

        else -> {
            liElement.appendChild(document.createElement("br"))
            listParentElement.appendChild(liElement)
            range.insertNode(listParentElement)
        }
    }
}

fun MarkdownRichTextEditor.insertList(ordered: Boolean = false) {
    val element = textArea.element as? HTMLElement ?: return
    val selection = getValidSelection() ?: return

    if (!isSelectionInsideElement(selection, element)) return

    val range = selection.getRangeAt(0)

    if (isSelectionInsideTag("LI") && (isSelectionInsideTag("UL") || isSelectionInsideTag("OL"))) {
        unwrapList()
        return
    }

    var currentNode = range.startContainer
    if (currentNode.nodeType == Node.TEXT_NODE) {
        currentNode = currentNode.parentNode
    }

    val blockElement = findContainingBlockElement(currentNode, element)

    val listParentElement = if (ordered) {
        document.createElement("ol") as HTMLElement
    } else {
        document.createElement("ul") as HTMLElement
    }
    val liElement = document.createElement("li") as HTMLElement

    createAndInsertList(ordered, range, blockElement, listParentElement, liElement)

    try {
        val newRange = document.createRange()
        newRange.selectNodeContents(liElement)
        newRange.collapse(false)

        setCursorPosition(selection, newRange)
        updateCursorTracking()
    } catch (e: Throwable) {
        console.log("Error positioning cursor in list: ${e.message}")
    }
}


fun MarkdownRichTextEditor.switchListRoot(toOrdered: Boolean) {
    val editorRoot = textArea.element as? HTMLElement ?: return
    var listContainer: HTMLElement? = null

    val selection = window.asDynamic().getSelection()
    if (selection != null && selection.rangeCount > 0) {
        var currentNode = selection.anchorNode
        if (currentNode?.nodeType == Node.TEXT_NODE) currentNode = currentNode.parentNode

        while (currentNode != null && currentNode != editorRoot && currentNode != document.body) {
            if (currentNode.nodeName == "UL" || currentNode.nodeName == "OL") {
                listContainer = currentNode as HTMLElement
                break
            }
            currentNode = currentNode.parentNode
        }
    }

    if (listContainer == null) {
        listContainer = editorRoot.querySelector("ul, ol") as? HTMLElement
    }

    if (listContainer == null) return

    val currentTag = listContainer.tagName.uppercase()
    val targetTag = if (toOrdered) "OL" else "UL"

    if (currentTag == targetTag) return

    val newList = document.createElement(targetTag) as HTMLElement

    while (listContainer.firstChild != null) {
        newList.appendChild(listContainer.firstChild!!)
    }

    listContainer.parentNode?.replaceChild(newList, listContainer)

    notifyContentChanged()
    launch { updateSelectedRichTextTag() }
}

fun MarkdownRichTextEditor.replaceTagWith(element: HTMLElement, newTagName: String) {
    safeExecute("replacing header tag") {
        val newElement = document.createElement(newTagName) as HTMLElement

        // Move all inner content (text, bold tags, etc.) into the new wrapper
        while (element.firstChild != null) {
            newElement.appendChild(element.firstChild!!)
        }

        // Swap them in the DOM
        element.parentNode?.replaceChild(newElement, element)
        notifyContentChanged()

        // Restore the cursor inside the newly created block
        val selectionRange = document.createRange()
        selectionRange.selectNodeContents(newElement)
        selectionRange.collapse(false) // Collapse to the end of the text
        applyRangeToSelection(selectionRange)
    }
}
