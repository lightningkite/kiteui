package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement

actual class ViewPager actual constructor(context: RContext): RView(context) {
    private var controller: RecyclerController2? = null
    private var onController = ArrayList<(RecyclerController2)->Unit>()
    private fun onController(action: (RecyclerController2)->Unit) {
        controller?.let(action) ?: onController.add(action)
    }
    private val newViews = NewViewWriter(context)
    init {
        native.tag = "div"
        native.classes.add("recyclerView")
        native.onElement {
            val controller = RecyclerController2(it as HTMLDivElement, false)
            controller.forceCentering = true
            controller.contentHolder.classList.add("viewPager")
            this.controller = controller
            onController.forEach { it(controller) }
            onController.clear()
        }


        with(object: ViewWriter() {
            override val context: RContext = context
            override fun addChild(view: RView) {
                native.appendChild(view.native)
            }
        }) {
            button {
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
                    onController { rc -> rc.jump(rc.centerVisible.value - 1, Align.Center, true) }
                }
            }
            button {
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
                    onController { rc -> rc.jump(rc.centerVisible.value + 1, Align.Center, true) }
                }
            }
        }
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
        onController { controller ->
            controller.renderer = ItemRenderer<T>(
                create = { value ->
                    val prop = Property(value)
                    render(newViews, prop)
                    val new = newViews.newView!!
                    addChild(new)
                    new.asDynamic().__ROCK__prop = prop
                    new.native.create() as HTMLElement
                },
                update = { element, value ->
                    @Suppress("UNCHECKED_CAST")
                    (children.find { it.native.element === element }?.asDynamic().__ROCK__prop as? Property<T>)?.value = value
                },
                shutdown = { element ->
                    removeChild(children.indexOfFirst { it.native.element === element })
                }
            )
            reactiveScope {
                controller.data = items.await().asIndexed()
            }
        }
    }

    init {
        onRemove {
            controller?.shutdown()
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

//    private val _lastVisibleIndex = Property(0)
//    actual val lastVisibleIndex: Readable<Int> = _lastVisibleIndex
//    init { onController { it.lastVisible.addListener { _lastVisibleIndex.value = it.lastVisible.value } } }

    private val _index = Property<Int>(0)
    actual val index: Writable<Int> = _index.withWrite { index ->
        onController { it.jump(index, Align.Center, animationsEnabled) }
    }
    init { onController { it.centerVisible.addListener { _index.value = it.centerVisible.value } } }
}

//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NViewPager = HTMLDivElement
//
//@ViewDsl
//actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) {
//    themedElement<HTMLDivElement>("div", viewDraws = false) {
//        classList.add("recyclerView")
//        val newViews: ViewWriter = newViews()
//        ResizeObserver { entries, obs ->
//            style.setProperty("--pager-width", "calc(${clientWidth}px")
//            style.setProperty("--pager-height", "calc(${clientHeight}px")
//        }.observe(this)
//        style.setProperty("--pager-width", "calc(${clientWidth}px")
//        style.setProperty("--pager-height", "calc(${clientHeight}px")
//        val rc = RecyclerController2(
//            root = this,
//            newViews = newViews,
//            vertical = false
//        ).apply {
//            this.contentHolder.classList.add("viewPager")
//        }
//        rc.forceCentering = true
//        this.asDynamic().__ROCK__controller = rc
//
//        button {
//            native.addClass("touchscreenOnly")
//            native.style.run {
//                position = "absolute"
//                left = "0"
//                top = "50%"
//                transform = "translateY(-50%)"
//            }
//            icon {
//                source = Icon.chevronLeft
//            }
//            onClick {
//                rc.jump(rc.centerVisible.value - 1, Align.Center, true)
//            }
//        }
//        button {
//            native.addClass("touchscreenOnly")
//            native.style.run {
//                position = "absolute"
//                right = "0"
//                top = "50%"
//                transform = "translateY(-50%)"
//            }
//            icon {
//                source = Icon.chevronRight
//            }
//            onClick {
//                rc.jump(rc.centerVisible.value + 1, Align.Center, true)
//            }
//        }
//        setup(ViewPager(this))
//    }
//}
//
//actual val ViewPager.index: Writable<Int> get() {
//    return (native.asDynamic().__ROCK__controller as RecyclerController2).centerVisible
//        .withWrite {
//            (native.asDynamic().__ROCK__controller as RecyclerController2).jump(it, Align.Center, animationsEnabled)
//        }
//}
//
//actual fun <T> ViewPager.children(
//    items: Readable<List<T>>,
//    render: ViewWriter.(value: Readable<T>) -> Unit
//) {
//    (native.asDynamic().__ROCK__controller as RecyclerController2).let {
//        it.renderer = ItemRenderer<T>(
//            create = { value ->
//                val prop = Property(value)
//                render(it.newViews, prop)
//                it.newViews.rootCreated!!.also {
//                    it.asDynamic().__ROCK_prop__ = prop
//                }
//            },
//            update = { element, value ->
//                (element.asDynamic().__ROCK_prop__ as Property<T>).value = value
//            }
//        )
//        reactiveScope {
//            it.data = items.await().asIndexed()
//        }
//    }
//}