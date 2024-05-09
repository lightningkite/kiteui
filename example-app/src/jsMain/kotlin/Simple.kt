package com.lightningkite.mppexampleapp

import ViewWriter
import kotlinx.browser.document

fun main() {
    val context = ViewWriter(document.body!!)
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
}
