package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.models.ErrorSemantic
import com.lightningkite.kiteui.models.InvalidSemantic
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.subtext
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.lensing.validation.Issue
import com.lightningkite.reactive.lensing.validation.Validated
import com.lightningkite.reactive.lensing.validation.issues
import kotlin.collections.minus
import kotlin.collections.plus

fun RView.validates(vararg validates: Validated<*>, validatesWhen: ReactiveContext.() -> Boolean = { true }) {
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
): ViewModifiable {
    this.shownWhen { shownWhen() && issues().isNotEmpty() }.apply(ErrorSemantic).subtext {
        ::content {
            issues().joinToString("\n", transform = transform)
        }
    }
}