@file:OptIn(ExperimentalContracts::class)

package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.LinearLayoutElement
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.text
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Semantic for label text (usually smaller)
 *
 * Automatically applied to [labels][label]
 * */
data object LabelSemantic : Semantic("label") {
    override fun default(theme: Theme): ThemeAndBack = theme[SubtextSemantic]
}

/**
 * Semantic for decreasing the space between labels and main content
 *
 * Automatically applied to [labels][label]
 * */
data object LabelGapSemantic : Semantic("labelgp") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        cascading = false,
        gap = theme.gap * 0.1
    )
}

class LabelView(private val container: RowOrCol): LinearLayoutElement by container {
    constructor(context: ElementContext) : this(RowOrCol(context))

    private val label = atStart.themed(LabelSemantic).text()
    var content: String by label::content

    init {
        @OptIn(ExperimentalKiteUi::class)
        container.elementSpecificTheming += LabelGapSemantic    // apply label gap to column
    }
}

inline fun ElementWriter.label(setup: LabelView.() -> Unit = {}): LabelView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LabelView(context), setup)
}

inline fun ElementWriter.label(label: String, setup: LinearLayoutElement.() -> Unit): LabelView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return label {
        content = label
        setup()
    }
}

inline fun ElementWriter.field(label: String, content: ElementWriter.CanAddTheme.() -> Unit): LabelView {
    contract { callsInPlace(content, InvocationKind.EXACTLY_ONCE) }
    return label(label) {
        fieldTheme.content()
        errorText()
    }
}