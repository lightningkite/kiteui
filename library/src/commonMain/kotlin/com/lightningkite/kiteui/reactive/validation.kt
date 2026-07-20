@file:Suppress("DSL_MARKER_APPLIED_TO_WRONG_TARGET")

package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.UnsafeModifier
import com.lightningkite.kiteui.models.ErrorSemantic
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.models.LiveRegionMode
import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.subtext
import com.lightningkite.kiteui.views.l2.findFirstInteractiveDescendant
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.lensing.validation.Issue
import com.lightningkite.reactive.lensing.validation.IssueTracking
import com.lightningkite.reactive.lensing.validation.Validated
import com.lightningkite.reactive.lensing.validation.issues

private val ValidationTheming = NativeElementCommonCode.ThemePipeline.Step(0.5f) // between dynamicTheme and elementStatus

@UnsafeModifier
public fun Element.applyValidationTheming(validates: Array<out IssueTracking>, appliedWhen: ReactiveContext.() -> Boolean = { true }) {
    var invalid = false
    val e = underlyingNativeElement

    @OptIn(ExperimentalKiteUi::class)
    e.themePipeline.add(ValidationTheming) {
        if (invalid) InvalidSemantic else ThemeDerivation.None
    }

    reactive {
        val newState = (appliedWhen() and validates.any { it.issues().isNotEmpty() })
        if (newState != invalid) {
            invalid = newState
            e.refreshTheming()
        }
    }
}

@UnsafeModifier
@Deprecated("Use new modifier syntax")
public fun Element.validates(vararg validates: Validated<*>, validatesWhen: ReactiveContext.() -> Boolean = { true }): Unit = applyValidationTheming(validates, validatesWhen)

public fun ElementWriter.CanAddTheme.validate(vararg validates: IssueTracking, appliedWhen: ReactiveContext.() -> Boolean = { true }): ElementWriter.CanAddTheme =
    beforeSetup {
        @OptIn(UnsafeModifier::class)
        applyValidationTheming(validates, appliedWhen)
    }

@ViewDsl
public fun ElementWriter.CanAddShownWhen.issueText(
    issues: Reactive<List<Issue>>,
    transform: (Issue) -> String = { it.summary },
    shownWhen: ReactiveContext.() -> Boolean = { true }
) {
    val issueView = this.shownWhen { shownWhen() && issues().isNotEmpty() }.themed(ErrorSemantic).subtext {
        ::content {
            issues().joinToString("\n", transform = transform)
        }
    }
    // Link the issue text to its sibling interactive element for screen readers
    issueView.accessibleLiveRegion = LiveRegionMode.Assertive
    // Walk up the parent chain to find a container with an interactive element as a sibling
    generateSequence(issueView.parent) { it.parent }
        .firstNotNullOfOrNull { it.findFirstInteractiveDescendant() }
        ?.let { it.describedBy = issueView }
}

//private class ValidatedTextInput(private val wraps: TextInput) : Element by wraps {
//    var enabled: Boolean by wraps::enabled
//    var keyboardHints: KeyboardHints by wraps::keyboardHints
//    var hint: String by wraps::hint
//    var align: Align? by wraps::align
//
//    // Intentionally obscuring the 'MutableReactive` interface so that the normal 'bind' function does not work.
//    interface Data : MutableValue<String>, ReactiveValue<String> {
//        infix fun bind(master: MutableValidated<String>)
//    }
//
//    private inner class DataImpl: Data, ReactiveValue<String> by wraps.content {
//        override fun valueSet(value: String) = wraps.content.valueSet(value)
//
//        override fun bind(master: MutableValidated<String>) {
//            wraps.content bind master // normal binding
//            boundNode.value = master.node
//        }
//    }
//
//    val content: Data = DataImpl()
//
//    private val boundNode = Signal<IssueNode?>(null)
//
//    val issues = remember { boundNode()?.issues() ?: emptyList() }
//}
//
//private inline fun ElementWriter.validatedTextInput(setup: ValidatedTextInput.() -> Unit): ValidatedTextInput {
//    return write(ValidatedTextInput(TextInput(context)), setup)
//}
//
//private fun ViewWriter.test() {
//    val wrong = Signal("")
//    val right = wrong.validated()
//
//    validatedTextInput {
//        content bind right
//
//
//    }
//}