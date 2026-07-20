package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.asListItem
import com.lightningkite.kiteui.views.direct.atIndex
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.setupAsListContainer
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.LateInitSignal
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.value
import kotlin.math.min


@InternalKiteUi
public fun <T> ContainerElement.forEach(
    items: Reactive<List<T>>,
    beforeListModifier: ViewWriter.()->ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(T) -> Unit
) {
    setupAsListContainer()
    reactive {
        clearChildren()
        items().forEach {
            beforeListModifier().asListItem.render(it)
        }
    }
}

@InternalKiteUi
public fun <T> ContainerElement.forEachUpdating(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    beforeListModifier: ViewWriter.()->ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) {
    setupAsListContainer()
    val currentViews = ArrayList<LateInitSignal<T>>()
    val currentView = this
    reactive(onLoad = {
        currentView.withoutAnimation {
            if (placeholdersWhileLoading <= 0) return@reactive
            if (currentViews.size < placeholdersWhileLoading) {
                repeat(placeholdersWhileLoading - currentViews.size) {
                    val newProp = LateInitSignal<T>()
                    beforeListModifier().asListItem.render(newProp)
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
                    beforeListModifier().asListItem.render(newProp)
                    currentViews.add(newProp)
                }
            }/* else if(currentViews.size > itemList.size) {
            currentView.listNViews().takeLast(currentViews.size - itemList.size).forEach {
                currentView.removeNView(it)
                currentViews.removeLast()
            }
        }*/
            val children = currentView.children
            for (index in 0..<min(oldCurrentViewsSize, itemList.size)) {
                children[index].shown = true
                currentViews[index].value = itemList[index]
            }
            for (index in itemList.size..<currentViews.size) {
                children[index].shown = false
            }
        }
    }
}

@InternalKiteUi
public fun <T, ID> RowOrCol.forEachById(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    preHidingModifiers: ViewWriter.(ID) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(Reactive<T>) -> Unit
) {
    setupAsListContainer()
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
                if (n == livenessIter) {
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
            for (checkIndex in oldPos..<old.size) {
                if (old[checkIndex].oldId == id(toRender)) {
                    matchIndex = checkIndex
                    break
                }
            }
            if (matchIndex != -1) {
                for (index in oldPos until matchIndex) {
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
                val result: Element = this@forEachById.atIndex(oldPos).produceExactlyOneView {
                    preHidingModifiers(id(toRender)).asListItem.shownWhen { shown() }.render(data)
                }
                old.add(
                    oldPos, OldViewInfo(
                        oldIndex = index,
                        oldId = id(toRender),
                        data = data,
                        view = result,
                        shown = shown
                    )
                )
                afterTimeout(1) { shown.value = true }
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}

@InternalKiteUi
public fun <T, ID> RowOrCol.forEachByIdWithoutAnimation(
    items: Reactive<List<T>>,
    id: (T) -> ID,
    beforeListModifier: ViewWriter.()->ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddListElementModifier.(Reactive<T>) -> Unit
) {
    setupAsListContainer()
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
                if (n == livenessIter) {
                    removeChild(view)
                    oldEarly.remove(this)
                }
            }
        }
    }

    val old = oldEarly as ArrayList<OldViewInfo>
    reactive {
        withoutAnimation {
            val new = items()
            var oldPos = 0
            new.forEachIndexed { index, toRender ->
                var matchIndex = -1
                for (checkIndex in oldPos..<old.size) {
                    if (old[checkIndex].oldId == id(toRender)) {
                        matchIndex = checkIndex
                        break
                    }
                }
                if (matchIndex != -1) {
                    for (index in oldPos until matchIndex) {
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
                    val result: Element = this@forEachByIdWithoutAnimation.atIndex(oldPos).produceExactlyOneView {
                        beforeListModifier().asListItem.render(data)
                    }
                    old.add(
                        oldPos, OldViewInfo(
                            oldIndex = index,
                            oldId = id(toRender),
                            data = data,
                            view = result,
                            shown = shown
                        )
                    )
                    afterTimeout(1) { shown.value = true }
                    oldPos++
                }
            }
            old.subList(oldPos, old.size).forEach { it.hide() }
        }
    }
}

@InternalKiteUi
public fun <T> RowOrCol.forEachAnimated(
    items: Reactive<List<T>>,
    preHidingModifiers: ViewWriter.(T) -> ElementWriter.CanAddListElementModifier = { this },
    render: ElementWriter.CanAddTheme.(T) -> Unit
) {
    setupAsListContainer()
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
                if (n == livenessIter) {
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
            for (checkIndex in oldPos..<old.size) {
                if (old[checkIndex].data == toRender) {
                    old[checkIndex].show()
                    matchIndex = checkIndex
                    break
                }
            }
            if (matchIndex != -1) {
                for (index in oldPos until matchIndex) {
                    old[index].hide()
                }
                oldPos = matchIndex + 1
            } else {
                val shown = Signal(false)
                val result: Element = this@forEachAnimated.atIndex(oldPos).produceExactlyOneView {
                    preHidingModifiers(toRender).asListItem.shownWhen { shown() }.render(toRender)
                }
                old.add(
                    oldPos, OldViewInfo(
                        oldIndex = index,
                        data = toRender,
                        view = result,
                        shown = shown
                    )
                )
                shown.value = true
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}