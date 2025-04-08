package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun ViewWriter.toast(text: String, duration: Duration = 3.seconds) {
    toast(duration) { text(text) }
}

fun ViewWriter.toast(duration: Duration = 3.seconds, content: ViewWriter.() -> ViewModifiable) {
    overlayFrame?.run {
        withoutAnimation {

            beforeNextElementSetup {
                opacity = 0.0
                launch {
                    val t = theme
                    delay(1)
                    opacity = 1.0
                    delay(duration.inWholeMilliseconds)
                    opacity = 0.0
                    delay(t.transitionDuration)
                    this@run.removeChild(this@beforeNextElementSetup)
                }
            }
            atBottomCenter - col {
                gap = 2.rem
                DialogSemantic.onNext - content()
                space()
            }
        }
    }
}

fun ViewWriter.dialog(dismissable: Boolean = true, content: ViewWriter.() -> Unit) {
    var willRemove: RView? = null
    this.overlayFrame!!.run {
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    launch {
                        it.opacity = 0.0
                        delay(it.theme.transitionDuration)
                        overlayFrame!!.removeChild(it)
                    }
                }
            }.run {
                beforeNextElementSetup {
                    opacity = 0.0
                    launch {
                        delay(110)
                        opacity = 1.0
                    }
                }
                willRemove = dismissBackground {
                    onClick { if (dismissable) closePopovers() }
                    centered - DialogSemantic.onNext - frame {
                        content()
                    }
                }
            }
        }
    }
}

fun ViewWriter.rawPopover(transition: ScreenTransitions, content: ViewWriter.() -> ViewModifiable) {
    var willRemove: RView? = null
    this.overlayFrame!!.run {
        withoutAnimation {
            popoverWriter {
                willRemove?.let {
                    it.animateOut(transition.reverse) {
                        overlayFrame!!.removeChild(it)
                    }
                }
            }.run {
                beforeNextElementSetup {
                    animateIn(transition.forward)
                }
                willRemove = content().rView
            }
        }
    }
}
