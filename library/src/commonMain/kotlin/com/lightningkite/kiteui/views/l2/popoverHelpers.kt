package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.PopoverSemantic
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.beforeSetup
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@ViewDsl
fun ElementContext.toast(text: String, duration: Duration = 3.seconds) {
    toast(duration) { text(text) }
}

fun ElementContext.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit) {
    overlay(false) {
        atBottomCenter.col {
            withoutAnimation {
                opacity = 0.0
                launch {
                    val t = theme
                    delay(1)
                    opacity = 1.0
                    delay(duration.inWholeMilliseconds)
                    opacity = 0.0
                    delay(t.transitionDuration)
                    this@overlay.removeChild(this@col)
                }
                gap = 2.rem
                themed(PopoverSemantic).content()
                space()
            }
        }
    }
}

fun ElementContext.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddSizing.(close: ()->Unit) -> Unit) {
    overlay(modal = true) { close ->
        dismissBackground {
            debugName = "dialog-bg"
            onClick { if (dismissable) close() }

            centered.beforeSetup {
                debugName = "dialog"
                themeChoice += DialogSemantic
            }.content(close)
        }
    }
}

fun ElementContext.rawPopover(transition: ScreenTransitions, content: ViewWriter.() -> Unit) {
    var willRemove: Element? = null
    overlay {
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        this@overlay.removeChild(it)
                    }
                }
            }.run {
                willRemove = beforeSetup { animateIn(transition.forward) }.produceAtMostOneView(content)
            }
        }
    }
}