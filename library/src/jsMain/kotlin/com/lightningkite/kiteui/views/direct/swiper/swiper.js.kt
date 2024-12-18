@file:JsModule("swiper/bundle")

package com.lightningkite.kiteui.views.direct.swiper

import org.w3c.dom.HTMLElement

external class Swiper(container: HTMLElement, options: dynamic)  {
    fun slideNext(duration:Int,runCallBacks: Boolean)
    fun slidePrev(duration:Int, runCallBacks: Boolean)
    fun appendSlide(slide: String)
    fun on(event: String, callback: (dynamic) -> Unit)
    fun slideTo(index: Int, duration: Int, runCallBacks: Boolean)
    var realIndex: Int
    var previousIndex: Int
}

