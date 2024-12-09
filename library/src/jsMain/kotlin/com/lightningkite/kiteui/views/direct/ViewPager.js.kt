package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.direct.swiper.SwiperCSS
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.swiper.Swiper
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.launch
import org.w3c.dom.*
import kotlin.js.json


actual class ViewPager actual constructor(context: RContext) :
    RView(context) {
    private val buttons = ArrayList<RView>()
    private val slides: MutableList<Any> = mutableListOf()
    private lateinit var childrenRender: ( ViewWriter.(value: Readable<Any>) -> Unit)
    private val newView = NewViewWriter(this, context)
    private val transitionDuration:Int = 300 //in ms
    private var swiper: Swiper? = null

    init {
        native.tag = "div"
        native.classes.add("viewPager")
        native.setStyleProperty("height", "100vh !important")
        native.setStyleProperty("width", "auto !important")
        native.setStyleProperty("--spacing", "0.0rem !important")
        native.setStyleProperty("display", "flex !important")
        native.setStyleProperty("overflow", "hidden !important")
        native.setStyleProperty("align-items", "center !important")
        SwiperCSS
        native.innerHtmlUnsafe = """
            <style>
             .swiper-slide {
                    flex-shrink: 0 !important;
                    width: 100% !important;
                    height: 100% !important;
                    position: relative !important;
                    transition-property: transform !important;
                    display: flex !important;
                    justify-content: center !important;
                }
            </style>
            <div class="swiper-wrapper" style="display:flex !important; width: 100% !important; height:100% !important;" >
            </div>
            <div class="swiper-pagination"></div>
            <div class="swiper-scrollbar"></div>
            """.trimIndent()
        native.onElement {
            val json = json(
                        "direction" to "horizontal",
                        "spaceBetween" to 0,
                        "slidesPerView" to 1,
                        "centeredSlides" to true,
                        "autoHeight" to false,
                        "initialSlide" to _index.value ,
                        "speed" to transitionDuration,
                        "keyboardControl" to true,
                        "virtual" to json (
                            "slides" to slides.map { it }.toTypedArray(),
                            "renderSlide" to renderSlide,
                            "addSlidesAfter" to 2,
                            "addSlidesBefore" to 2,
                        )
                    )
                    if(_index.value != 0) {
                        native.setStyleProperty("justify-content", "center")
                    }
                    swiper = Swiper(it as HTMLElement, json)
                    swiper?.on("slideChange", {
                        // not sure why but if it is the first element then it needs to have justify-content center or
                        // else it will have space on the right side but if it not then it needs to left justified or there will be space on the left side
                        _index.value =  swiper?.realIndex ?: 0
                        if(swiper?.previousIndex == 0 && swiper?.realIndex != 0){
                            native.setStyleProperty("justify-content", "center")
                        }
                        if (swiper?.realIndex == 0) {
                            native.setStyleProperty("justify-content", "left")
                        }
                    })
                }
        with(object : ViewWriter(), CalculationContext by this {
            override val context: RContext = context
            override fun willAddChild(view: RView) {
                super.willAddChild(view)
                view.parent = this@ViewPager
            }

            override fun addChild(view: RView) {
                native.appendChild(view.native)
            }
        }) {


            buttonTheme - button {
                buttons += this
                native.classes.add("touchscreenOnly")
                native.style.run {
                    position = "absolute"
                    left = "0"
                    top = "50%"
                    transform = "translateY(-50%)"
                    zIndex = "99"
                    width="2.7rem"
                    height="2.7rem"
                }

                icon {

                    source = Icon.chevronLeft
                }
                onClick {
                    if (swiper != null) {
                        swiper?.slidePrev(transitionDuration,true)
                    }
                }
            }

            buttonTheme - button {
                buttons += this
                native.classes.add("touchscreenOnly")
                icon {
                    source = Icon.chevronRight

                }
                native.style.run {
                    position = "absolute"
                    right = "0"
                    top = "50%"
                    transform = "translateY(-50%)"
                    zIndex = "99"
                    width="2.7rem"
                    height="2.7rem"
                }
                onClick {
                    if (swiper != null) {
                        swiper?.slideNext(transitionDuration,true)
                    }
                }
            }

        }
    }

    val renderSlide: (dynamic, dynamic) -> dynamic = { slide, index ->
        val prop = Property(slide)

        this.childrenRender?.let { it(newView, prop) }
        val new = newView.newView


        new.asDynamic().__ROCK__PROP=prop
        val wrapper = document.createElement("div") as HTMLElement
        wrapper.classList.add("swiper-slide")
        val htmlViewElement = new?.native?.create() as HTMLElement
        wrapper.appendChild(htmlViewElement)
        wrapper
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
        launch {
            childrenRender = render as (ViewWriter.(value: Readable<Any>) -> Unit)
            slides.addAll(items().map {it as Any})
        }
        slides
    }

    private val _index = Property<Int>(0)
    actual val index: Writable<Int> = _index.withWrite { index ->
        _index.value = index
        if (swiper != null ) {
            swiper?.slideTo(index,transitionDuration,false)
        }
    }
}