package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atStart get() = align(Align.Start, Align.Stretch)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atEnd get() = align(Align.End, Align.Stretch)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atTop get() = align(Align.Stretch, Align.Start)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atBottom get() = align(Align.Stretch, Align.End)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.centeredHorizontally get() = align(Align.Center, Align.Stretch)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.centeredVertically get() = align(Align.Stretch, Align.Center)

@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atTopStart get() = align(Align.Start, Align.Start)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atCenterStart get() = align(Align.Start, Align.Center)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atBottomStart get() = align(Align.Start, Align.End)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atTopCenter get() = align(Align.Center, Align.Start)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.centered get() = align(Align.Center, Align.Center)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atBottomCenter get() = align(Align.Center, Align.End)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atTopEnd get() = align(Align.End, Align.Start)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atCenterEnd get() = align(Align.End, Align.Center)
@ViewModifierDsl3 val ElementWriter.CanAddAlignment.atBottomEnd get() = align(Align.End, Align.End)

@ViewModifierDsl3 val ElementWriter.CanAddWeight.expanding get() = weight(1f)

@ViewModifierDsl3 fun ElementWriter.CanAddAlignment.maxWidthCentered(width: Dimension) = align(Align.Center, Align.Stretch).sizedBox(SizeConstraints(maxWidth = width))
@ViewModifierDsl3 fun ElementWriter.CanAddSizing.maxHeight(height: Dimension) = sizedBox(SizeConstraints(maxHeight = height))

@ViewDsl
fun ElementWriter.icon(source: ReactiveContext.()->Icon, description: String, setup: IconView.()->Unit = {}) {
    icon {
        ::source { source() }
        this.description = description
        setup(this)
    }
}

val Icon.Companion.empty get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())

fun <T> ContainerElement.forEach(
    items: Reactive<List<T>>,
    render: ViewWriter.(T) -> Unit
) {
    reactive {
        clearChildren()
        items().forEach { render(it) }
    }
}

fun <T> ContainerElement.forEachUpdating(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: ViewWriter.(Reactive<T>) -> Unit
) {
    val currentViews = ArrayList<LateInitSignal<T>>()
    val currentView = this
    reactive<Unit>(onLoad = {
        currentView.withoutAnimation {
            if (placeholdersWhileLoading <= 0) return@reactive
            if (currentViews.size < placeholdersWhileLoading) {
                repeat(placeholdersWhileLoading - currentViews.size) {
                    val newProp = LateInitSignal<T>()
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
                    val newProp = LateInitSignal<T>()
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

fun <T, ID> RowOrCol.forEachById(
    items: Reactive<List<T>>,
    id: (T)->ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddShownWhen = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) {
    val oldEarly = ArrayList<Any>()
    data class OldViewInfo(
        var oldIndex: Int,
        val oldId: ID,
        val data: Signal<T>,
        val view: Element,
        val shown: Signal<Boolean>
    ) {
        var livenessIter = 0
        fun show() {
            livenessIter++
            shown.value = true
        }
        fun hide() {
            val n = ++livenessIter
            shown.value = false
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds + 100) {
                if(n == livenessIter) {
                    removeChild(view)
                    oldEarly.remove(this)
                }
            }
        }
    }
    val old = oldEarly as ArrayList<OldViewInfo>
    reactive {
        val new = items()
        var oldPos = 0
        new.forEachIndexed { index, toRender ->
            var matchIndex = -1
            for(checkIndex in oldPos..<old.size) {
                if(old[checkIndex].oldId == id(toRender)) {
                    matchIndex = checkIndex
                    break
                }
            }
            if (matchIndex != -1) {
                for(index in oldPos until matchIndex) {
                    old[index].hide()
                }
                oldPos = matchIndex + 1
                old[matchIndex].let {
                    it.data.value = toRender
                    it.show()
                }
            } else {
                val shown = Signal(false)
                val data = Signal(toRender)
                val result: Element = this@forEachById.produceExactlyOneView {
                    preHidingModifiers(id(toRender)).shownWhen { shown() }.render(data)
                }
                old.add(oldPos, OldViewInfo(
                    oldIndex = index,
                    oldId = id(toRender),
                    data = data,
                    view = result,
                    shown = shown
                ))
                afterTimeout(1) { shown.value = true }
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}
fun <T> RowOrCol.forEachAnimated(
    items: Reactive<List<T>>,
    preHidingModifiers: ViewWriter.(T) -> ElementWriter.CanAddShownWhen = { this },
    render: ElementWriter.CanAddTheme.(T) -> Unit
) {
    val oldEarly = ArrayList<Any>()
    data class OldViewInfo(
        var oldIndex: Int,
        val data: T,
        val view: Element,
        val shown: Signal<Boolean>
    ) {
        var livenessIter = 0
        fun show() {
            livenessIter++
            shown.value = true
        }
        fun hide() {
            val n = ++livenessIter
            shown.value = false
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds + 100) {
                if(n == livenessIter) {
                    removeChild(view)
                    oldEarly.remove(this)
                }
            }
        }
    }
    val old = oldEarly as ArrayList<OldViewInfo>
    reactive {
        val new = items()
        var oldPos = 0
        new.forEachIndexed { index, toRender ->
            var matchIndex = -1
            for(checkIndex in oldPos..<old.size) {
                if(old[checkIndex].data == toRender) {
                    old[checkIndex].show()
                    matchIndex = checkIndex
                    break
                }
            }
            if (matchIndex != -1) {
                for(index in oldPos until matchIndex) {
                    old[index].hide()
                }
                oldPos = matchIndex + 1
            } else {
                val shown = Signal(false)
                val result: Element = this@forEachAnimated.produceExactlyOneView {
                    preHidingModifiers(toRender).shownWhen { shown() }.render(toRender)
                }
                old.add(oldPos, OldViewInfo(
                    oldIndex = index,
                    data = toRender,
                    view = result,
                    shown = shown
                ))
                shown.value = true
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}