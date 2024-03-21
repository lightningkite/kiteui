package com.lightningkite.mppexampleapp

import com.lightningkite.rock.contains
import com.lightningkite.rock.navigation.PlatformNavigator
import com.lightningkite.rock.reactive.Property
import com.lightningkite.rock.reactive.await
import com.lightningkite.rock.views.*
import com.lightningkite.rock.views.direct.*
import com.lightningkite.rock.views.l2.appBase
import com.lightningkite.rock.views.l2.navigatorViewDialog
import kotlinx.browser.document

fun main() {
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
//        stack {
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
////            stack {
////                text("D")
////            } in tweakTheme { it.dialog() }
//        }
//    }
}
