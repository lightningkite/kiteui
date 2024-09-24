package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.launchGlobal
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

fun ViewWriter.toast(duration: Duration = 3.seconds, content: ViewWriter.()->Unit) {
    overlayStack?.run {
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
            spacing = 2.rem
            dialog - content()
            space()
        }
    }
}

fun ViewWriter.dialog(dismissable: Boolean = true, content: ViewWriter.()->Unit) {
    var willRemove: RView? = null
    this.overlayStack!!.popoverWriter {
        willRemove?.let {
            launch {
                it.opacity = 0.0
                delay(it.theme.transitionDuration)
                overlayStack!!.removeChild(it)
            }
        }
    }.run {
        beforeNextElementSetup {
            opacity = 0.0
            launch {
                delay(1)
                opacity = 1.0
            }
        }
        willRemove = dismissBackground {
            onClick { if(dismissable) closePopovers() }
            centered - dialog - stack {
                content()
            }
        }
    }
}
