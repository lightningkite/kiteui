@file:JsModule("swiper/bundle")

package com.lightningkite.kiteui.views.direct

import org.w3c.dom.HTMLElement

//external interface ManipulationMethods {
//    fun appendSlide(slides: dynamic) // Accepts HTMLElement | string | string[] | HTMLElement[]
//    fun prependSlide(slides: dynamic)
//    fun addSlide(index: Int, slides: dynamic)
//    fun removeSlide(slideIndex: dynamic) // Accepts number | number[]
//    fun removeAllSlides()
//}


external class Swiper(container: HTMLElement, options: dynamic)  {
    fun update()
    fun appendSlide(slide: String)
//    override fun appendSlide(slides: dynamic)
//    override fun prependSlide(slides: dynamic)
//    override fun addSlide(index: Int, slides: dynamic)
//    override fun removeSlide(slideIndex: dynamic)
//    override fun removeAllSlides()
}


external interface SwiperOptions {
    val direction:String?
    val slidesPerView:Int?
    val centeredSlides:Boolean?
    val freeMode:Boolean?
}

