package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ViewDsl
fun ViewWriter.icon(icon: Icon, description: String, setup: IconView.()->Unit = {}): IconView {
    return icon {
        source = icon
        this.description = description
        setup(this)
    }
}

@ViewDsl
fun ViewWriter.lazyExpanding(visible: Readable<Boolean>, sub: ViewWriter.()->Unit) {
    col {
        var noViewCreated = true
        var view: RView? = null
        reactiveScope {
            val v = visible()
            if (v) {
                if (noViewCreated) {
                    noViewCreated = false
                    withoutAnimation {
                        sub()
                        view = children[0]
                        view?.shown = false
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
fun RView.errorText(): ViewModifiable {
    val errors = Property<Set<Exception>>(setOf())
    return shownWhen { errors().isNotEmpty() } - ErrorSemantic.onNext - text {
        this@errorText += object: ExceptionHandler {
            override val priority: Float
                get() = 1f

            override fun handle(view: RView, working: Boolean, exception: Exception): (() -> Unit)? {
                errors.value += exception
                return {
                    errors.value -= exception
                }
            }
        }
        ::content {
            errors().joinToString("\n") {
                exceptionToMessage(it)?.body ?: it.message ?: it.toString()
            }
        }
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.field(label: String, content: ViewWriter.() -> ViewModifiable): ViewModifiable {
    contract { callsInPlace(content, InvocationKind.EXACTLY_ONCE) }
    return col {
        gap = 0.px
        FieldLabelSemantic.onNext - text(label)
        fieldTheme - content()
        SubtextSemantic.onNext - errorText()
    }
}