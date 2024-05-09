package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.direct.*
import kotlin.math.min

@ViewModifierDsl3 val RView.centered get() = gravity(Align.Center, Align.Center)
@ViewModifierDsl3 val RView.atStart get() = gravity(Align.Start, Align.Stretch)
@ViewModifierDsl3 val RView.atEnd get() = gravity(Align.End, Align.Stretch)
@ViewModifierDsl3 val RView.atTop get() = gravity(Align.Stretch, Align.Start)
@ViewModifierDsl3 val RView.atBottom get() = gravity(Align.Stretch, Align.End)
@ViewModifierDsl3 val RView.atTopStart get() = gravity(Align.Start, Align.Start)
@ViewModifierDsl3 val RView.atBottomStart get() = gravity(Align.Start, Align.End)
@ViewModifierDsl3 val RView.atTopCenter get() = gravity(Align.Center, Align.Start)
@ViewModifierDsl3 val RView.atBottomCenter get() = gravity(Align.Center, Align.End)
@ViewModifierDsl3 val RView.atTopEnd get() = gravity(Align.End, Align.Start)
@ViewModifierDsl3 val RView.atBottomEnd get() = gravity(Align.End, Align.End)

@ViewModifierDsl3 val RView.expanding get() = weight(1f)
@ViewModifierDsl3 operator fun ViewWrapper.minus(view: RView) = Unit
@ViewModifierDsl3 operator fun ViewWrapper.minus(unit: Unit) = Unit
@ViewModifierDsl3 operator fun ViewWrapper.minus(wrapper: ViewWrapper) = ViewWrapper

@ViewModifierDsl3 fun RView.maxWidthCentered(width: Dimension) = gravity(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(maxWidth = width))
@ViewModifierDsl3 fun RView.maxHeight(height: Dimension) = sizedBox(SizeConstraints(maxHeight = height))

@ViewDsl
fun RView.icon(icon: suspend ()->Icon, description: String, setup: IconView.()->Unit = {}) {
    icon {
        ::source { icon() }
        this.description = description
        setup(this)
    }
}

val Icon.Companion.empty get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())

fun <T> RView.forEach(
    items: Readable<List<T>>,
    render: RView.(T) -> Unit
) {
    reactiveScope {
        clearChildren()
        items().forEach { render(it) }
    }
}

fun <T> RView.forEachUpdating(
    items: Readable<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: RView.(Readable<T>) -> Unit
) {
        val currentViews = ArrayList<LateInitProperty<T>>()
        val currentView = this
        reactiveScope(onLoad = {
            currentView.withoutAnimation {
                if (placeholdersWhileLoading <= 0) return@reactiveScope
                if (currentViews.size < placeholdersWhileLoading) {
                    repeat(placeholdersWhileLoading - currentViews.size) {
                        val newProp = LateInitProperty<T>()
                        render(newProp)
                        currentViews.add(newProp)
                    }
                }/* else if(currentViews.size > itemList.size) {
                currentView.listNViews().takeLast(currentViews.size - itemList.size).forEach {
                    currentView.removeNView(it)
                    currentViews.removeLast()
                }
            }*/
                val children = currentView.children
                for (index in 0 until placeholdersWhileLoading) {
                    children[index].exists = true
                    currentViews[index].unset()
                }
                for (index in placeholdersWhileLoading..<currentViews.size) {
                    children[index].exists = false
                }
            }
        }) {
            val itemList = items()
            currentView.withoutAnimation {
                val oldCurrentViewsSize = currentViews.size
                if (currentViews.size < itemList.size) {
                    repeat(itemList.size - currentViews.size) {
                        val newProp = LateInitProperty<T>()
                        newProp.value = itemList[currentViews.size]
                        render(newProp)
                        currentViews.add(newProp)
                    }
                }/* else if(currentViews.size > itemList.size) {
                currentView.listNViews().takeLast(currentViews.size - itemList.size).forEach {
                    currentView.removeNView(it)
                    currentViews.removeLast()
                }
            }*/
                val children = currentView.children
                for (index in 0 ..< min(oldCurrentViewsSize, itemList.size)) {
                    children[index].exists = true
                    currentViews[index].value = itemList[index]
                }
                for (index in itemList.size..<currentViews.size) {
                    children[index].exists = false
                }
            }
        }
    }