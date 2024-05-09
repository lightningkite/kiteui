package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

//fun RView.toast(text: String, duration: Duration = 3.seconds) {
//    toast(duration) { text(text) }
//}
//
//fun RView.toast(duration: Duration = 3.seconds, inner: RView.()->Unit) {
//    baseStackWriter?.let {
//        baseStack?.native?.let { s ->
//            var toast: NView? = null
//            s.withoutAnimation {
//                atBottomCenter - stack {
//                    spacing = 1.rem
//                    beforeNextElementSetup { toast = this }
//                    dialog - inner()
//                    toast?.opacity = 0.0
//                }
//            }
//            val theme = currentTheme
//            launchGlobal {
//                val toast = toast ?: return@launchGlobal
//                val t = theme()
//                delay(t.transitionDuration.inWholeMilliseconds)
//                toast.opacity = 1.0
//                delay(duration.inWholeMilliseconds)
//                toast.opacity = 0.0
//                delay(t.transitionDuration.inWholeMilliseconds)
//                s.removeNView(toast)
//            }
//        }
//    }
//}
