package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@ViewDsl
fun ViewWriter.icon(icon: Icon, description: String, setup: IconView.()->Unit = {}) {
    icon {
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
                        view?.exists = false
                    }
                    view?.exists = true
                } else {
                    view?.exists = true
                }
            } else {
                view?.exists = false
            }
        }
    }
}

@ViewDsl
fun RView.errorText() {
    val errors = Property<Set<Exception>>(setOf())
    onlyWhen { errors().isNotEmpty() } - ErrorSemantic.onNext - text {
        this@errorText += object: ExceptionHandler {
            override val priority: Float
                get() = 1f

            override fun handle(view: RView, exception: Exception): (() -> Unit)? {
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
fun ViewWriter.field(label: String, content: ViewWriter.() -> Unit) {
    col {
        spacing = 0.px
        subtext(label)
        fieldTheme - content()
        SubtextSemantic.onNext - errorText()
    }
}
