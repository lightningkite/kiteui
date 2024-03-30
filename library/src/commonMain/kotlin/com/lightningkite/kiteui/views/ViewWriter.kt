package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.MaterialLikeTheme
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import kotlin.math.min

/**
 * An object that writes view trees, similar to the way a Writer in Java sequentially writes text data.
 * Views rendered through here will be inserted into the given parent in the constructor.
 */
class ViewWriter(
    parent: NView?,
    val context: NContext = parent?.nContext ?: throw IllegalArgumentException(),
    private val startDepth: Int = 0,
) {
    val depth: Int get() = stack.size - 1 + startDepth
    var rootCreated: NView? = null

    /**
     * Additional data keyed by string attached to the context.
     * Copied to writers that are split off.
     * Easiest use comes from [viewWriterAddon] and [viewWriterAddonLateInit].
     */
    val addons: MutableMap<String, Any?> = mutableMapOf()

    /**
     * Creates a copy of the [ViewWriter] with the current view as its root.
     * Used for view containers that need their contents removed and replaced later.
     */
    fun split(): ViewWriter = ViewWriter(stack.last(), context = context, startDepth = depth).also {
        it.addons.putAll(this.addons)
        it.currentTheme = currentTheme
        it.lastTheme = lastTheme
        it.isRoot = isRoot
        it.transitionNextView = TransitionNextView.No
        it.changedThemes = changedThemes
    }

    /**
     * Creates a copy of the [ViewWriter] with no root view.
     * Used for view containers that need their contents removed and replaced later.
     */
    fun newViews(): ViewWriter = ViewWriter(null, context = context, startDepth = depth).also {
        it.addons.putAll(this.addons)
        it.currentTheme = currentTheme
        it.lastTheme = lastTheme
        it.isRoot = isRoot
        it.transitionNextView = TransitionNextView.No
    }

    /**
     * Creates a copy of the [ViewWriter] with no root view.
     * Used for view containers that need their contents removed and replaced later.
     */
    fun targeting(view: NView): ViewWriter = ViewWriter(view, context = context, startDepth = depth).also {
        it.addons.putAll(this.addons)
        it.currentTheme = currentTheme
        it.lastTheme = lastTheme
        it.isRoot = isRoot
        it.transitionNextView = TransitionNextView.No
    }

    val stack = if (parent == null) arrayListOf() else arrayListOf(parent)
    val currentView: NView get() = stack.last()
    private inline fun <T : NView> stackUse(item: T, action: T.() -> Unit) =
        CalculationContextStack.useIn(item.calculationContext) {
            stack.add(item)
            try {
                action(item)
            } finally {
                stack.removeLast()
            }
        }

    var rootTheme: suspend () -> Theme = { MaterialLikeTheme() }
    var lastTheme: suspend () -> Theme = { rootTheme() }
    var currentTheme: suspend () -> Theme = { rootTheme() }
    inline fun <T> withThemeGetter(crossinline calculate: suspend (suspend () -> Theme) -> Theme, action: () -> T): T {
        val old = currentTheme
        changedThemes = true
        val oldold = lastTheme
        lastTheme = old
        currentTheme = { calculate(old) }
        try {
            return action()
        } finally {
            currentTheme = old
            lastTheme = oldold
        }
    }

    @ViewModifierDsl3
    inline fun ViewWriter.themeModifier(crossinline calculate: suspend (suspend () -> Theme) -> Theme): ViewWrapper {
        val old = currentTheme
        changedThemes = true
        val oldold = lastTheme
        lastTheme = old
        currentTheme = { calculate(old) }
        afterNextElementSetup {
            currentTheme = old
            lastTheme = oldold
        }
        return ViewWrapper
    }

    /**
     * Adds a card / border / padding to the next view.
     */
    sealed interface TransitionNextView {
        object No : TransitionNextView
        object Yes : TransitionNextView
        class Maybe(val logic: suspend () -> Boolean) : TransitionNextView
    }

    var transitionNextView: TransitionNextView = TransitionNextView.No
    var changedThemes: Boolean = false
    var isRoot: Boolean = true
    var includePaddingAtStackEmpty = false
    val stackEmpty: Boolean get() = stack.isEmpty()

    val calculationContext: CalculationContext get() = stack.last().calculationContext

    /**
     * Runs the given [action] on the next created element before its setup block is run.
     */
    fun beforeNextElementSetup(action: NView.() -> Unit) {
        beforeNextElementSetupList.add(action)
    }

    /**
     * Runs the given [action] on the next created element after its setup block is run.
     */
    fun afterNextElementSetup(action: NView.() -> Unit) {
        afterNextElementSetupList.add(action)
    }

    private var beforeNextElementSetupList = ArrayList<NView.() -> Unit>()
    private var afterNextElementSetupList = ArrayList<NView.() -> Unit>()
    private var afterNextElementPopList = ArrayList<()->Unit>()

    //    private val wrapperToDoList = ArrayList<NView.() -> Unit>()
    private var popCount = 0

    /**
     * Wraps the next created element within this element.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : NView> wrapNext(element: T, setup: T.() -> Unit): ViewWrapper {
        stack.lastOrNull()?.addNView(element) ?: run { rootCreated = element }
        stack.add(element)
        val beforeCopy = if (beforeNextElementSetupList.isNotEmpty()) beforeNextElementSetupList.toList() else listOf()
        beforeNextElementSetupList = ArrayList()
        val afterCopy = if (afterNextElementSetupList.isNotEmpty()) afterNextElementSetupList.toList() else listOf()
        afterNextElementSetupList = ArrayList()
        CalculationContextStack.useIn(element.calculationContext) {
            val oldPop = popCount
            popCount = 0
            beforeCopy.forEach { it(element) }
            setup(element)
            afterNextElementPopList.add {
                CalculationContextStack.useIn(element.calculationContext) {
                    afterCopy.asReversed().forEach { it(element) }
                }
            }
            popCount = oldPop
        }
        popCount++
        return ViewWrapper
    }

    /**
     * Writes an element to the current parent.
     */
    fun <T : NView> element(initialElement: T, setup: T.() -> Unit) {
        initialElement.apply {
            stack.lastOrNull()?.addNView(this) ?: run { rootCreated = this }
            val beforeCopy =
                if (beforeNextElementSetupList.isNotEmpty()) beforeNextElementSetupList.toList() else listOf()
            beforeNextElementSetupList = ArrayList()
            val afterCopy = if (afterNextElementSetupList.isNotEmpty()) afterNextElementSetupList.toList() else listOf()
            afterNextElementSetupList = ArrayList()
            var toPop = popCount
            popCount = 0
            stackUse(this) {
                beforeCopy.forEach { it(this) }
                setup()
                afterCopy.asReversed().forEach { it(this) }
            }
            while (toPop > 0) {
                val item = stack.removeLast()
                toPop--
                afterNextElementPopList.removeLast().invoke()
            }
//            wrapperToDoList.clear()
        }
    }

    fun <T> forEachUpdating(
        items: Readable<List<T>>,
        placeholdersWhileLoading: Int = 5,
        render: ViewWriter.(Readable<T>) -> Unit
    ) {
        val split = split()
        val currentViews = ArrayList<LateInitProperty<T>>()
        val currentView = currentView
        calculationContext.reactiveScope(onLoad = {
            currentView.withoutAnimation {
                if (placeholdersWhileLoading <= 0) return@reactiveScope
                if (currentViews.size < placeholdersWhileLoading) {
                    repeat(placeholdersWhileLoading - currentViews.size) {
                        val newProp = LateInitProperty<T>()
                        split.render(newProp)
                        currentViews.add(newProp)
                    }
                }/* else if(currentViews.size > itemList.size) {
                currentView.listNViews().takeLast(currentViews.size - itemList.size).forEach {
                    currentView.removeNView(it)
                    currentViews.removeLast()
                }
            }*/
                val children = currentView.listNViews()
                for (index in 0 until placeholdersWhileLoading) {
                    children[index].exists = true
                    currentViews[index].unset()
                }
                for (index in placeholdersWhileLoading..<currentViews.size) {
                    children[index].exists = false
                }
            }
        }) {
            currentView.withoutAnimation {
                val itemList = items.await()
                val oldCurrentViewsSize = currentViews.size
                if (currentViews.size < itemList.size) {
                    repeat(itemList.size - currentViews.size) {
                        val newProp = LateInitProperty<T>()
                        newProp.value = itemList[currentViews.size]
                        split.render(newProp)
                        currentViews.add(newProp)
                    }
                }/* else if(currentViews.size > itemList.size) {
                currentView.listNViews().takeLast(currentViews.size - itemList.size).forEach {
                    currentView.removeNView(it)
                    currentViews.removeLast()
                }
            }*/
                val children = currentView.listNViews()
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
}
