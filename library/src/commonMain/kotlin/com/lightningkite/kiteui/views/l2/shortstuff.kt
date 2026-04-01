package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ViewDsl
fun ElementWriter.lazyExpanding(visible: Reactive<Boolean>, sub: ViewWriter.() -> Unit) {
    col {
        var noViewCreated = true
        var view: Element? = null
        reactive {
            val v = visible()
            if (v) {
                if (noViewCreated) {
                    noViewCreated = false
                    withoutAnimation {
                        sub()
                        view = children[0]
                        view.shown = false
                    }
                    view?.shown = true
                } else {
                    view?.shown = true
                }
            } else {
                view?.shown = false
            }
        }
    }
}

@ViewDsl
fun ElementWriter.CanAddShownWhen.errorText() {
    val errors = ReactiveMutableSet<Exception>()
    shownWhen { errors().isNotEmpty() }.themed(SubtextSemantic).themed(ErrorSemantic).text {
        this@errorText.context.exceptionHandlers += ExceptionHandler(1f) {
            errors.add(it);
            { errors.remove(it) }
        }

        ::content {
            errors().joinToString("\n") {
                context.exceptionMessage(it)?.body ?: it.message ?: it.toString()
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun ElementWriter.field(label: String, content: ElementWriter.CanAddTheme.() -> Unit) {
    contract { callsInPlace(content, InvocationKind.EXACTLY_ONCE) }
    col {
        gap = 0.px
        themed(FieldLabelSemantic).text(label)
        fieldTheme.content()
        errorText()
    }
}