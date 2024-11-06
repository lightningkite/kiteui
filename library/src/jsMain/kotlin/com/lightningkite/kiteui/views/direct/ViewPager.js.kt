package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement


@JsModule("glider-js")
@JsNonModule
external val glider: dynamic

actual class ViewPager actual constructor(context: RContext): RView(context) {
//    private var controller: RecyclerController2? = null
//    private var onController = ArrayList<(RecyclerController2)->Unit>()
//    private fun onController(action: (RecyclerController2)->Unit) {
//        controller?.let(action) ?: onController.add(action)
//    }
//    private val newViews = NewViewWriter(this, context)



    private val buttons = ArrayList<RView>()
    init {
        native.tag = "div"
        native.classes.add("viewPager")
        glider
        native.onElement {
//            js("")
        }

        with(object: ViewWriter(), CalculationContext by this {
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
                }
                icon {
                    source = Icon.chevronLeft
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
                    right = "0"
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
//        onController { controller ->
//            controller.renderer = ItemRenderer<T>(
//                create = { value ->
//                    val prop = Property(value)
//                    render(newViews, prop)
//                    val new = newViews.newView!!
//                    addChild(new)
//                    new.asDynamic().__ROCK__prop = prop
//                    new.native.create() as HTMLElement
//                },
//                update = { element, value ->
//                    @Suppress("UNCHECKED_CAST")
//                    (children.find { it.native.element === element }?.asDynamic().__ROCK__prop as? Property<T>)?.value = value
//                },
//                shutdown = { element ->
//                    removeChild(children.indexOfFirst { it.native.element === element })
//                }
//            )
//            reactiveScope {
//                controller.data = items().asIndexed()
//            }
//        }
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