package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.navigation.PlatformNavigator
import com.lightningkite.signal.Property
import com.lightningkite.signal.await
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.appBase
import com.lightningkite.kiteui.views.l2.navigatorViewDialog
import kotlinx.browser.document

public fun main() {
    println("TEST")
    val context = ViewWriter(NView3(document.body!!))
    context.app()
//    with(context) {
//        col {
//            repeat(5) {
//                onlyWhen { true }
//                hasPopover { text("POPOVER") }
//                text("TEST")
//            }
//        }
//    }
//    with(context) {
//        frame {
//            rootTheme = lastTheme
//            val navigator = PlatformNavigator
//            PlatformNavigator.routes = AutoRoutes
//            context.navigator = navigator
//            val counter = Property<Int>(0)
//            swapView {
//                swapping(current = { counter.await() }, views = {
//                    text("Value is now ${it}")
//                })
//            }
//            col {
//
//                counter.value = 0
//
//                text {
//                    reactiveScope { content = "The current counter value is ${counter.await()}" }
//                }
//
//                important - button {
//                    text("Increment the counter")
//                    onClick { counter.value++ }
//                }
//            }
////            frame {
////                text("D")
////            } in tweakTheme { it.dialog() }
//        }
//    }
}
