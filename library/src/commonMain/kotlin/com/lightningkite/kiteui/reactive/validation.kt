package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.ErrorSemantic
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.TextInput
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.subtext
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.lensing.validation.*

fun Element.validates(vararg validates: Validated<*>, validatesWhen: ReactiveContext.() -> Boolean = { true }) {
    dynamicTheme {
        if (validatesWhen() && validates.any { it.issues().isNotEmpty() }) InvalidSemantic
        else null
    }
}

@ViewDsl
fun ViewWriter.issueText(
    issues: Reactive<List<Issue>>,
    transform: (Issue) -> String = { it.summary },
    shownWhen: ReactiveContext.() -> Boolean = { true }
): Unit {
    this.shownWhen { shownWhen() && issues().isNotEmpty() }.themed(ErrorSemantic).subtext {
        ::content {
            issues().joinToString("\n", transform = transform)
        }
    }
}

private class ValidatedTextInput(private val wraps: TextInput) : Element by wraps {
    var enabled: Boolean by wraps::enabled
    var keyboardHints: KeyboardHints by wraps::keyboardHints
    var hint: String by wraps::hint
    var align: Align? by wraps::align

    // Intentionally obscuring the 'MutableReactive` interface so that the normal 'bind' function does not work.
    interface Data : MutableValue<String>, ReactiveValue<String> {
        infix fun bind(master: MutableValidated<String>)
    }

    private inner class DataImpl: Data, ReactiveValue<String> by wraps.content {
        override fun valueSet(value: String) = wraps.content.valueSet(value)

        override fun bind(master: MutableValidated<String>) {
            wraps.content bind master // normal binding
            boundNode.value = master.node
        }
    }

    val content: Data = DataImpl()

    private val boundNode = Signal<IssueNode?>(null)

    val issues = remember { boundNode()?.issues() ?: emptyList() }
}

private inline fun ElementWriter.validatedTextInput(setup: ValidatedTextInput.() -> Unit): ValidatedTextInput {
    return write(ValidatedTextInput(TextInput(context)), setup)
}

private fun ViewWriter.test() {
    val wrong = Signal("")
    val right = wrong.validated()

    validatedTextInput {
        content bind right


    }
}