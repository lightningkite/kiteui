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

public fun ElementWriter.lazyExpanding(visible: Reactive<Boolean>, sub: ViewWriter.() -> Unit) {
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

/**
 * Shows the errors raised anywhere in this writer's [ElementContext] (button actions, reactive
 * blocks, ...) while they're unresolved.  A context is shared by everything written under it,
 * nested containers included, until [split] starts a new one, so an errorText() catches errors
 * from its siblings' whole subtrees, not just its neighbor.  [field] splits for you; give other
 * actions their own scope:
 * ```kotlin
 * split().col {
 *     button { text("Save"); onClick { save() } }
 *     errorText()
 * }
 * ```
 * Stops catching when it is removed.
 */
public fun ElementWriter.CanAddShownWhen.errorText() {
    val errors = ReactiveMutableSet<Exception>()
    val errorView = shownWhen { errors().isNotEmpty() }.themed(SubtextSemantic).themed(ErrorSemantic).text {
        val handler = ExceptionHandler(1f) {
            errors.add(it);
            { errors.remove(it) }
        }
        this@errorText.context.exceptionHandlers += handler
        onRemove { this@errorText.context.exceptionHandlers.remove(handler) }

        ::content {
            errors().joinToString("\n") {
                context.exceptionMessage(it)?.body ?: it.message ?: it.toString()
            }
        }
    }
    // Link the error text to its sibling interactive element for screen readers
    errorView.accessibleLiveRegion = LiveRegionMode.Assertive
    // Walk up the parent chain to find a container with an interactive element as a sibling
    generateSequence(errorView.parent) { it.parent }
        .firstNotNullOfOrNull { it.findFirstInteractiveDescendant() }
        ?.let { it.describedBy = errorView }
}
