@file:OptIn(ExperimentalContracts::class)

package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
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
public data object LabelSemantic : Semantic("label") {
    override fun default(theme: Theme): ThemeAndBack = theme[SubtextSemantic]
}

/**
 * Semantic for sections with a label, used to control the space between labels and main content
 *
 * Automatically applied to [labels][label]
 * */
public data object LabelGapSemantic : Semantic("labelgp") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        cascading = false,
        gap = theme.gap * 0.1
    )
}

public class LabeledView(private val container: RowOrCol): LinearLayoutElement by container {
    public constructor(context: ElementContext) : this(RowOrCol(context))

    private val label = atStart.themed(LabelSemantic).text()
    public var content: String by label::content

    init {
        themeChoice += LabelGapSemantic
        container.vertical = true
    }

    @OverrideOnly
    override fun onStartup() {
        container.onStartup()
        val target = findFirstInteractiveDescendant()
        if (target != null) {
            label.labelFor = target
        }
    }
}

internal fun Element.findFirstInteractiveDescendant(): InteractiveElement? {
    if (this is InteractiveElement) return this
    if (this is ElementWithChildren) {
        for (child in children) {
            val found = child.findFirstInteractiveDescendant()
            if (found != null) return found
        }
    }
    return null
}

public inline fun ElementWriter.label(setup: LabeledView.() -> Unit = {}): LabeledView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LabeledView(context), setup)
}

public inline fun ElementWriter.label(label: String, setup: LinearLayoutElement.() -> Unit): LabeledView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return label {
        content = label
        setup()
    }
}

public inline fun ElementWriter.field(label: String, content: ElementWriter.CanAddTheme.() -> Unit): LabeledView {
    contract { callsInPlace(content, InvocationKind.EXACTLY_ONCE) }
    return label(label) {
        fieldTheme.content()
        errorText()
    }
}