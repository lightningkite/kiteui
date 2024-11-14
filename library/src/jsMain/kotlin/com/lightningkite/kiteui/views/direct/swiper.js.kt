@file:JsModule("swiper")

package com.lightningkite.kiteui.views.direct

import org.w3c.dom.HTMLElement

external class Swiper(container: HTMLElement, options: dynamic) {
    fun slideNext()
    fun slidePrev()
    fun update()
}



//external interface SwiperOptions {
//    val modules: Array
//}