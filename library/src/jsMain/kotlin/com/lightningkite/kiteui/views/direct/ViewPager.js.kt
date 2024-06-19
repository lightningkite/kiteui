package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.dom.addClass
import org.w3c.dom.*
import kotlin.math.roundToInt

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NViewPager = HTMLDivElement

@ViewDsl
actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) {
    themedElement<HTMLDivElement>("div", viewDraws = false) {
        classList.add("recyclerView")
        val newViews: ViewWriter = newViews()
        ResizeObserver { entries, obs ->
            style.setProperty("--pager-width", "calc(${clientWidth}px")
            style.setProperty("--pager-height", "calc(${clientHeight}px")
        }.observe(this)
        style.setProperty("--pager-width", "calc(${clientWidth}px")
        style.setProperty("--pager-height", "calc(${clientHeight}px")
        val rc = RecyclerController2(
            root = this,
            newViews = newViews,
            vertical = false
        ).apply {
            this.contentHolder.classList.add("viewPager")
        }
        println() /* Don't remove this. If you're tempted to do so, talk to Joseph or Wesley first. */
        rc.forceCentering = true
        this.asDynamic().__ROCK__controller = rc

        buttonTheme - button {
            native.addClass("touchscreenOnly")
            native.style.run {
                position = "absolute"
                zIndex = "2"
                left = "0"
                top = "50%"
                transform = "translateY(-50%)"
                margin = "0.5rem"
            }
            icon {
                source = Icon.chevronLeft
            }
            onClick {
                rc.jump(rc.centerVisible.value - 1, Align.Center, true)
            }
        }


        buttonTheme - button {
            native.addClass("touchscreenOnly")
            native.style.run {
                position = "absolute"
                zIndex = "2"
                right = "0"
                top = "50%"
                transform = "translateY(-50%)"
                margin = "0.5rem"
            }
            icon {
                source = Icon.chevronRight
            }
            onClick {
                rc.jump(rc.centerVisible.value + 1, Align.Center, true)
            }
        }
        setup(ViewPager(this))
    }
}

actual val ViewPager.index: Writable<Int> get() {
    return (native.asDynamic().__ROCK__controller as RecyclerController2).centerVisible
        .withWrite {
            (native.asDynamic().__ROCK__controller as RecyclerController2).jump(it, Align.Center, animationsEnabled)
        }
}

actual fun <T> ViewPager.children(
    items: Readable<List<T>>,
    render: ViewWriter.(value: Readable<T>) -> Unit
) {
    (native.asDynamic().__ROCK__controller as RecyclerController2).let {
        it.renderer = ItemRenderer<T>(
            create = { value ->
                val prop = Property(value)
                render(it.newViews, prop)
                it.newViews.rootCreated!!.also {
                    it.asDynamic().__ROCK_prop__ = prop
                }
            },
            update = { element, value ->
                (element.asDynamic().__ROCK_prop__ as Property<T>).value = value
            }
        )
        reactiveScope {
            it.data = items.await().asIndexed()
        }
    }
}