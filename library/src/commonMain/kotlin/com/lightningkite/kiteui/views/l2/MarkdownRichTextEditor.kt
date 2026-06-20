package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A rich text editor that supports Markdown serialization and deserialization.
 * Supports basic formatting like bold, italic, headers, and lists.
 */
expect class MarkdownRichTextEditor(context: ElementContext) : NativeContainerElement, ElementWithAction {
    var hint: String
    val content: MutableReactiveValue<String>
    var suggestionHandler: SuggestionHandler?
    override var action: Action?
    override var enabled: Boolean
}

/**
 * Handler for suggestions (e.g., mentions, hashtags) in the editor.
 */
interface SuggestionHandler {
    // Currently a stub for future extension
}

/**
 * Enumeration of supported rich text formatting tags.
 */
enum class RichTextTags {
    HEADER1,
    HEADER2,
    HEADER3,
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    SUPERSCRIPT,
    SUBSCRIPT,
    LINK,
    UNORDERED_LIST,
    ORDERED_LIST,
    QOUTE,
    CODE,
    CODE_BLOCK
}

val Icon.Companion.header1: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M200-280v-400h80v160h160v-160h80v400h-80v-160H280v160h-80Zm480 0v-320h-80v-80h160v400h-80Z")
    )

val Icon.Companion.header2: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M120-280v-400h80v160h160v-160h80v400h-80v-160H200v160h-80Zm400 0v-160q0-33 23.5-56.5T600-520h160v-80H520v-80h240q33 0 56.5 23.5T840-600v80q0 33-23.5 56.5T760-440H600v80h240v80H520Z")
    )

val Icon.Companion.header3: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M120-280v-400h80v160h160v-160h80v400h-80v-160H200v160h-80Zm400 0v-80h240v-80H600v-80h160v-80H520v-80h240q33 0 56.5 23.5T840-600v240q0 33-23.5 56.5T760-280H520Z")
    )

val Icon.Companion.italic: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M200-200v-100h160l120-360H320v-100h400v100H580L460-300h140v100H200Z")
    )

val Icon.Companion.orderedList: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M120-80v-60h100v-30h-60v-60h60v-30H120v-60h120q17 0 28.5 11.5T280-280v40q0 17-11.5 28.5T240-200q17 0 28.5 11.5T280-160v40q0 17-11.5 28.5T240-80H120Zm0-280v-110q0-17 11.5-28.5T160-510h60v-30H120v-60h120q17 0 28.5 11.5T280-560v70q0 17-11.5 28.5T240-450h-60v30h100v60H120Zm60-280v-180h-60v-60h120v240h-60Zm180 440v-80h480v80H360Zm0-240v-80h480v80H360Zm0-240v-80h480v80H360Z")
    )

val Icon.Companion.orderList: Icon get() = orderedList

val Icon.Companion.qoute: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("m228-240 92-160q-66 0-113-47t-47-113q0-66 47-113t113-47q66 0 113 47t47 113q0 23-5.5 42.5T458-480L320-240h-92Zm360 0 92-160q-66 0-113-47t-47-113q0-66 47-113t113-47q66 0 113 47t47 113q0 23-5.5 42.5T818-480L680-240h-92ZM320-500q25 0 42.5-17.5T380-560q0-25-17.5-42.5T320-620q-25 0-42.5 17.5T260-560q0 25 17.5 42.5T320-500Zm360 0q25 0 42.5-17.5T740-560q0-25-17.5-42.5T680-620q-25 0-42.5 17.5T620-560q0 25 17.5 42.5T680-500Zm0-60Zm-360 0Z")
    )

val Icon.Companion.code: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M320-240 80-480l240-240 57 57-184 184 183 183-56 56Zm320 0-57-57 184-184-183-183 56-56 240 240-240 240Z")
    )

val Icon.Companion.link: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M440-280H280q-83 0-141.5-58.5T80-480q0-83 58.5-141.5T280-680h160v80H280q-50 0-85 35t-35 85q0 50 35 85t85 35h160v80ZM320-440v-80h320v80H320Zm200 160v-80h160q50 0 85-35t35-85q0-50-35-85t-85-35H520v-80h160q83 0 141.5 58.5T880-480q0 83-58.5 141.5T680-280H520Z")
    )

val Icon.Companion.codeBlock: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("m384-336 56-57-87-87 87-87-56-57-144 144 144 144Zm192 0 144-144-144-144-56 57 87 87-87 87 56 57ZM200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Zm0-80h560v-560H200v560Zm0-560v560-560Z")
    )

val Icon.Companion.bold: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M272-200v-560h221q65 0 120 40t55 111q0 51-23 78.5T602-491q25 11 55.5 41t30.5 90q0 89-65 124.5T501-200H272Zm121-112h104q48 0 58.5-24.5T566-372q0-11-10.5-35.5T494-432H393v120Zm0-228h93q33 0 48-17t15-38q0-24-17-39t-44-15h-95v109Z")
    )

val Icon.Companion.strikeThrough: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M80-400v-80h800v80H80Zm340-160v-120H200v-120h560v120H540v120H420Zm0 400v-160h120v160H420Z")
    )

val Icon.Companion.unorderedList: Icon
    get() = Icon(
        width = 1.5.rem,
        height = 1.5.rem,
        viewBoxMinX = 0,
        viewBoxMinY = -960,
        viewBoxWidth = 960,
        viewBoxHeight = 960,
        pathDatas = listOf("M360-200v-80h480v80H360Zm0-240v-80h480v80H360Zm0-240v-80h480v80H360ZM200-160q-33 0-56.5-23.5T120-240q0-33 23.5-56.5T200-320q33 0 56.5 23.5T280-240q0 33-23.5 56.5T200-160Zm0-240q-33 0-56.5-23.5T120-480q0-33 23.5-56.5T200-560q33 0 56.5 23.5T280-480q0 33-23.5 56.5T200-400Zm-56.5-263.5Q120-687 120-720t23.5-56.5Q167-800 200-800t56.5 23.5Q280-753 280-720t-23.5 56.5Q233-640 200-640t-56.5-23.5Z")
    )
