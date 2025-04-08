package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.math.min

@ViewModifierDsl3 val ViewWriter.atStart get() = align(Align.Start, Align.Stretch)
@ViewModifierDsl3 val ViewWriter.atEnd get() = align(Align.End, Align.Stretch)
@ViewModifierDsl3 val ViewWriter.atTop get() = align(Align.Stretch, Align.Start)
@ViewModifierDsl3 val ViewWriter.atBottom get() = align(Align.Stretch, Align.End)
@ViewModifierDsl3 val ViewWriter.centeredHorizontally get() = align(Align.Center, Align.Stretch)
@ViewModifierDsl3 val ViewWriter.centeredVertically get() = align(Align.Stretch, Align.Center)

@ViewModifierDsl3 val ViewWriter.atTopStart get() = align(Align.Start, Align.Start)
@ViewModifierDsl3 val ViewWriter.atCenterStart get() = align(Align.Start, Align.Center)
@ViewModifierDsl3 val ViewWriter.atBottomStart get() = align(Align.Start, Align.End)
@ViewModifierDsl3 val ViewWriter.atTopCenter get() = align(Align.Center, Align.Start)
@ViewModifierDsl3 val ViewWriter.centered get() = align(Align.Center, Align.Center)
@ViewModifierDsl3 val ViewWriter.atBottomCenter get() = align(Align.Center, Align.End)
@ViewModifierDsl3 val ViewWriter.atTopEnd get() = align(Align.End, Align.Start)
@ViewModifierDsl3 val ViewWriter.atCenterEnd get() = align(Align.End, Align.Center)
@ViewModifierDsl3 val ViewWriter.atBottomEnd get() = align(Align.End, Align.End)


@ViewModifierDsl3 val ViewWriter.expanding get() = weight(1f)

@ViewModifierDsl3 fun ViewWriter.maxWidthCentered(width: Dimension) = align(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(maxWidth = width))
@ViewModifierDsl3 fun ViewWriter.maxHeight(height: Dimension) = sizedBox(SizeConstraints(maxHeight = height))

@ViewDsl
fun ViewWriter.icon(source: ReactiveContext.()->Icon, description: String, setup: IconView.()->Unit = {}) {
    icon {
        ::source { source() }
        this.description = description
        setup(this)
    }
}

val Icon.Companion.empty get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())

fun <T> RView.forEach(
    items: Readable<List<T>>,
    render: ViewWriter.(T) -> Unit
) {
    reactiveScope {
        clearChildren()
        items().forEach { render(it) }
    }
}

fun <T> RView.forEachUpdating(
    items: Readable<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: ViewWriter.(Readable<T>) -> Unit
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
                    children[index].shown = true
                    currentViews[index].unset()
                }
                for (index in placeholdersWhileLoading..<currentViews.size) {
                    children[index].shown = false
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
                    children[index].shown = true
                    currentViews[index].value = itemList[index]
                }
                for (index in itemList.size..<currentViews.size) {
                    children[index].shown = false
                }
            }
        }
    }