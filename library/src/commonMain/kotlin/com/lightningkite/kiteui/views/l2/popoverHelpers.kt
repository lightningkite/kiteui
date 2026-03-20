package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun ViewWriter.toast(text: String, duration: Duration = 3.seconds) {
    toast(duration) { text(text) }
}

fun ViewWriter.toast(duration: Duration = 3.seconds, content: ElementWriter.CanAddScrolling.() -> Unit) {
    overlayWriter(false) {
        withoutAnimation {
            atBottomCenter.col {
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
                themed(DialogSemantic).content()
                space()
            }
        }
    }
}

fun ViewWriter.dialog(dismissable: Boolean = true, content: ViewWriter.(close: ()->Unit) -> Unit) {
    overlayWriter(modal = true) { close ->
        dismissBackground {
            onClick { if (dismissable) close() }
            centered.themed(DialogSemantic).frame {
                content { close() }
            }
        }
    }
}

fun ViewWriter.rawPopover(transition: ScreenTransitions, content: ViewWriter.() -> Unit) {
    var willRemove: RView? = null
    overlayWriter {
        representsView!!.withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        this@overlayWriter.representsView!!.removeChild(it)
                    }
                }
            }.run {
                willRemove = beforeNextElementSetup {
                    animateIn(transition.forward)
                }.produceOne(content)
            }
        }
    }
}
