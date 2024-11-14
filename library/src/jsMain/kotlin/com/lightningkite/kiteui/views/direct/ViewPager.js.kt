
package com.lightningkite.kiteui.views.direct

import SwiperCSS
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import org.w3c.dom.*



actual class ViewPager actual constructor(context: RContext):
    RView(context) {
    private val buttons = ArrayList<RView>()
    private val slides:MutableList<HTMLElement> = mutableListOf()
    private var loaded = false
    init {
        native.tag = "div"
        native.classes.add("viewPager")
        val swiperViewPagerContainer = FutureElement().apply {
            tag = "div"
            classes.add("swiper-viewpager-container")
            setStyleProperty("height", "100%")
            setStyleProperty("width", "100%")
            setStyleProperty("overflow", "hidden")
        }

        val swiperViewPager = FutureElement().apply {
            tag = "div"
            classes.add("swiper-viewpager")
            SwiperCSS
            innerHtmlUnsafe = """
                        <div class="swiper-wrapper">
                                <div class="swiper-slide">Slide 1</div>
                                <div class="swiper-slide">Slide 2</div>
                                <div class="swiper-slide" >Slide 3</div>
                                                                <div class="swiper-slide">Slide 3</div>
                                                                <div class="swiper-slide">Slide 3</div>
                                                                <div class="swiper-slide">Slide 3</div>
                                                                <div class="swiper-slide">Slide 3</div>
                                                                <div class="swiper-slide">Slide 3</div>
                                                                <div class="swiper-slide">Slide 3</div>


                        </div>
                        <div class="swiper-pagination"></div>
                        <div class="swiper-scrollbar"></div>
        """.trimIndent()

        }


        swiperViewPager.onElement {
//            js("console.log(getWindow().getComputedStyle(it))")
            val test:HTMLElement = it as HTMLElement
            println(test)
            val options = buildJsonObject {
                put("direction","horizontal")
                put("slidesPerView", 1)
                put("centeredSlides", true)
                put("freeMode", true)
                put("freeModeSticky", true)
                put("mousewheel", true)
                put("slideToClickedSlide",true)
                put("scrollbar", buildJsonObject {
                    put("el", "swiper-scrollbar")
                    put("hide", true)
                })
                put("keyboard", buildJsonObject { put("enabled", true) })
                put("virtual", buildJsonObject { put("slides", buildJsonArray { slides.forEach { add(it.toString()) } }) })
            }
            js("console.log(it.parent)")
                val testSwiper = Swiper(it as HTMLElement, options)
                println(testSwiper.update())
                js("console.log(testSwiper)")
            testSwiper.update()
//
        }

        swiperViewPagerContainer.appendChild(swiperViewPager)
        native.appendChild(swiperViewPagerContainer)



        with(object: ViewWriter(), CalculationContext by this {
            override val context: RContext = context
            override fun willAddChild(view: RView) {
                super.willAddChild(view)
                view.parent = this@ViewPager
            }
            override fun addChild(view: RView) {
                native .appendChild(view.native)
            }
        }) {

            buttonTheme - button {
                buttons += this
                native.classes.add("touchscreenOnly")
                icon {
                    source = Icon.chevronLeft
                }
                native.style.run {
                    position = "absolute"
                    right = "0"
                    top = "50%"
                    transform = "translateY(-50%)"
                    zIndex = "99"
                }
                onClick {
//                    onController { rc -> rc.jump(rc.centerVisible.value - 1, Align.Center, true) }
                }
            }
            buttonTheme - button {
                buttons += this
                native.classes.add("touchscreenOnly")
                native.style.run {
                    position = "absolute"
                    left = "0"
                    top = "50%"
                    transform = "translateY(-50%)"
                    zIndex = "99"
                }

                icon {
                    source = Icon.chevronRight
                }
                onClick {
//                    onController { rc -> rc.jump(rc.centerVisible.value + 1, Align.Center, true) }
                }
            }

        }
    }


    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        buttons.forEach { it.refreshTheming() }
    }

    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }


    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ): Unit {
        reactiveScope {
            items().map {
//                render(it)
            }
        }
        slides
    }

    init {
        onRemove {
//            controller?.shutdown()
        }
        native.onElement {
            it as HTMLElement
            ResizeObserver { entries, obs ->
                it.style.setProperty("--pager-width", "calc(${it.clientWidth}px")
                it.style.setProperty("--pager-height", "calc(${it.clientHeight}px")
            }.observe(it)
            it.style.setProperty("--pager-width", "calc(${it.clientWidth}px")
            it.style.setProperty("--pager-height", "calc(${it.clientHeight}px")
        }
    }

    private val _index = Property<Int>(0)
    actual val index: Writable<Int> = _index.withWrite { index ->
//        onController { it.jump(index, Align.Center, animationsEnabled) }
    }
//    init { onController { it.centerVisible.addListener { _index.value = it.centerVisible.value } } }
}


//}