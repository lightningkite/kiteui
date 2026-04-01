@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class, OverrideOnly::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack

/**
 * Platform-specific native container element implementation.
 *
 * This extends [NativeElement] with the ability to contain child elements. Each platform
 * provides its own implementation for managing child views in the native view hierarchy.
 */
expect abstract class NativeContainerElement(context: ElementContext) : ContainerElement, NativeContainerElementCommonCode {
    override fun nativeAddChild(index: Int, element: Element)
    override fun nativeRemoveChild(index: Int)
    override fun nativeClearChildren()
}

/**
 * Shared platform-independent code for native container elements.
 *
 * This class extends [NativeElement] with child management functionality including:
 * - Child lifecycle management (startup/shutdown)
 * - Theme cascading to children
 * - Child addition/removal with proper native view hierarchy updates
 *
 * You should never extend this directly. If you want to create a custom native component inherit from [NativeContainerElement].
 */
abstract class NativeContainerElementCommonCode internal constructor(context: ElementContext) : NativeElement(context), ContainerElement {
    override val underlyingNativeElement: NativeContainerElement get() = this as NativeContainerElement

    // --- CHILDREN ---

    private val internalChildren = ArrayList<Element>()
    override val children: List<Element> get() = internalChildren

    protected abstract fun nativeAddChild(index: Int, element: Element)
    protected abstract fun nativeRemoveChild(index: Int)
    protected abstract fun nativeClearChildren()

    override fun willAddChild(element: Element) {
        if (checkIsShutdown("willAddChild")) return
        element.underlyingNativeElement.parent = outermostElement as? ContainerElement ?: this
    }

    final override fun addChild(index: Int, element: Element) {
        if (checkIsShutdown("addChild")) return
        internalChildren.add(index, element)
        nativeAddChild(index, element)
        if (element.parent?.underlyingNativeElement !== this) {
            element.underlyingNativeElement.parent = outermostElement as? ContainerElement ?: this
        }
    }
    final override fun addChild(element: Element) = addChild(children.size, element)

    final override fun removeChild(index: Int) {
        if (checkIsShutdown("removeChild")) return
        if (index !in children.indices) throw IndexOutOfBoundsException("$index not in range ${children.indices}")
        nativeRemoveChild(index)
        internalChildren.removeAt(index).onShutdown()
    }

    final override fun removeChild(element: Element) {
        if (checkIsShutdown("removeChild")) return
        val i = children.indexOf(element)
        if (i != -1) {
            nativeRemoveChild(i)
            internalChildren.removeAt(i).onShutdown()
        }
        else throw IllegalArgumentException("$element is not a child of $this!")
    }

    final override fun clearChildren() {
        if (checkIsShutdown("clearChildren")) return
        nativeClearChildren()
        for (e in children) e.onShutdown()
        internalChildren.clear()
    }



    // --- LIFECYCLE ---

    override fun onShutdown() {
        if (isShutdown) return
        if (Element.Debugger.removeBeforeShutdown) {
            for (index in internalChildren.lastIndex downTo 0) {
                removeChild(index)
                internalChildren.removeAt(index).onShutdown()
            }
        } else {
            internalChildren.forEach { it.onShutdown() }
            internalChildren.clear()
        }
        super.onShutdown()
    }


    // --- THEMING ---

    final override var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
        set(value) {
            val oldCascading = field.theme.let { it.revert ?: it }
            field = value       // Do not call super.themeAndBack = value, it breaks everything for some reason
            nativeApplyTheme(value)
            refreshPadding()
            val newCascading = value.theme.let { it.revert ?: it }
            if (oldCascading != newCascading) {
                for (child in children) child.underlyingNativeElement.refreshTheming()
            }
        }
}