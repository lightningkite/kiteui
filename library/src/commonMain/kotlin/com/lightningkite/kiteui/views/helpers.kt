package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

@ViewModifierDsl3
public val ViewWriter.atStart get() = align(Align.Start, Align.Stretch)
@ViewModifierDsl3
public val ViewWriter.atEnd get() = align(Align.End, Align.Stretch)
@ViewModifierDsl3
public val ViewWriter.atTop get() = align(Align.Stretch, Align.Start)
@ViewModifierDsl3
public val ViewWriter.atBottom get() = align(Align.Stretch, Align.End)
@ViewModifierDsl3
public val ViewWriter.centeredHorizontally get() = align(Align.Center, Align.Stretch)
@ViewModifierDsl3
public val ViewWriter.centeredVertically get() = align(Align.Stretch, Align.Center)

@ViewModifierDsl3
public val ViewWriter.atTopStart get() = align(Align.Start, Align.Start)
@ViewModifierDsl3
public val ViewWriter.atCenterStart get() = align(Align.Start, Align.Center)
@ViewModifierDsl3
public val ViewWriter.atBottomStart get() = align(Align.Start, Align.End)
@ViewModifierDsl3
public val ViewWriter.atTopCenter get() = align(Align.Center, Align.Start)
@ViewModifierDsl3
public val ViewWriter.centered get() = align(Align.Center, Align.Center)
@ViewModifierDsl3
public val ViewWriter.atBottomCenter get() = align(Align.Center, Align.End)
@ViewModifierDsl3
public val ViewWriter.atTopEnd get() = align(Align.End, Align.Start)
@ViewModifierDsl3
public val ViewWriter.atCenterEnd get() = align(Align.End, Align.Center)
@ViewModifierDsl3
public val ViewWriter.atBottomEnd get() = align(Align.End, Align.End)


@ViewModifierDsl3
public val ViewWriter.expanding get() = weight(1f)

@ViewModifierDsl3
public fun ViewWriter.maxWidthCentered(width: Dimension) = align(Align.Center, Align.Stretch) - sizedBox(SizeConstraints(maxWidth = width))
@ViewModifierDsl3
public fun ViewWriter.maxHeight(height: Dimension) = sizedBox(SizeConstraints(maxHeight = height))

@ViewDsl
public fun ViewWriter.icon(source: ReactiveContext.()->Icon, description: String, setup: IconView.()->Unit = {}) {
    icon {
        ::source { source() }
        this.description = description
        setup(this)
    }
}

public val Icon.Companion.empty get() = Icon(2.rem, 2.rem, 0, -960, 960, 960, listOf())

fun <T> RView.forEach(
    items: Reactive<List<T>>,
    render: ViewWriter.(T) -> Unit
) {
    reactiveScope {
        clearChildren()
        items().forEach { render(it) }
    }
}

fun <T> RView.forEachUpdating(
    items: Reactive<List<T>>,
    placeholdersWhileLoading: Int = 5,
    render: ViewWriter.(Reactive<T>) -> Unit
) {
    val currentViews = ArrayList<LateInitSignal<T>>()
    val currentView = this
    reactiveScope(onLoad = {
        currentView.withoutAnimation {
            if (placeholdersWhileLoading <= 0) return@reactiveScope
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
    preHidingModifiers: ViewWriter.(ID)-> ViewWrapper = { ViewWrapper },
    render: ViewWriter.(Reactive<T>) -> ViewModifiable
) {
    val oldEarly = ArrayList<Any>()
    data class OldViewInfo(
        var oldIndex: Int,
        val oldId: ID,
        val data: Signal<T>,
        val view: RView,
        val shown: Signal<Boolean>
    ) {
        public var livenessIter = 0
        public fun show() {
            livenessIter++
            shown.value = true
        }
        public fun hide() {
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
                val indexWriter = object: ViewWriter() {
                    override val context: RContext get() = this@forEachById.context
                    override val coroutineContext: CoroutineContext get() = this@forEachById.coroutineContext
                    override fun addChild(view: RView) {
                        addChild(oldPos, view)
                    }

                    override fun willAddChild(view: RView) {
                        this@forEachById.willAddChild(view)
                    }
                }
                val view = with(indexWriter) { preHidingModifiers(id(toRender)) - shownWhen { shown() } - render(data) }
                old.add(oldPos, OldViewInfo(
                    oldIndex = index,
                    oldId = id(toRender),
                    data = data,
                    view = view.rView,
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
    preHidingModifiers: ViewWriter.(T)-> ViewWrapper = { ViewWrapper },
    render: ViewWriter.(T) -> ViewModifiable
) {
    val oldEarly = ArrayList<Any>()
    data class OldViewInfo(
        var oldIndex: Int,
        val data: T,
        val view: RView,
        val shown: Signal<Boolean>
    ) {
        public var livenessIter = 0
        public fun show() {
            livenessIter++
            shown.value = true
        }
        public fun hide() {
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
                val indexWriter = object: ViewWriter() {
                    override val context: RContext get() = this@forEachAnimated.context
                    override val coroutineContext: CoroutineContext get() = this@forEachAnimated.coroutineContext
                    override fun addChild(view: RView) {
                        addChild(oldPos, view)
                    }

                    override fun willAddChild(view: RView) {
                        this@forEachAnimated.willAddChild(view)
                    }
                }
                val view = with(indexWriter) { preHidingModifiers(toRender) - shownWhen { shown() } - render(toRender) }
                old.add(oldPos, OldViewInfo(
                    oldIndex = index,
                    data = toRender,
                    view = view.rView,
                    shown = shown
                ))
                shown.value = true
                oldPos++
            }
        }
        old.subList(oldPos, old.size).forEach { it.hide() }
    }
}