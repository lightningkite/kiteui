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

fun ElementContext.toast(text: String, duration: Duration = 3.seconds) {
    toast(duration) { text(text) }
}

fun ElementContext.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddTheme.() -> Unit) {
    overlayWriter(false) {
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
                    this@overlayWriter.removeChild(this@col)
                }
                gap = 2.rem
                themed(PopoverSemantic).content()
                space()
            }
        }
    }
}

fun ElementContext.dialog(dismissable: Boolean = true, content: ElementWriter.CanAddTheme.(close: ()->Unit) -> Unit) {
    overlayWriter(modal = true) { close ->
        dismissBackground {
            onClick { if (dismissable) close() }
            centered.themed(DialogSemantic).content(close)
        }
    }
}

fun ElementContext.rawPopover(transition: ScreenTransitions, content: ElementWriter.() -> Unit) {
    var willRemove: Element? = null
    overlayWriter {
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        this@overlayWriter.removeChild(it)
                    }
                }
            }.run {
                willRemove = beforeSetup { animateIn(transition.forward) }.produceExactlyOne(content)
            }
        }
    }
}