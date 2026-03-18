@file:OptIn(InternalKiteUi::class, ExperimentalKiteUi::class)

package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.Alignment
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack


expect abstract class NativeContainerElement(context: ElementContext) : NativeContainerElementCommonCode {

}

sealed class NativeContainerElementCommonCode(context: ElementContext) : NativeElement(context), ContainerElement {
    final override var childDefaultAlignment: Alignment? = null

    // --- CHILDREN ---

    private val internalChildren = ArrayList<Element>()
    override val children: List<Element> get() = internalChildren

    protected abstract fun nativeAddChild(index: Int, element: Element)
    protected abstract fun nativeRemoveChild(index: Int)
    protected abstract fun nativeClearChildren()

    final override fun addChild(index: Int, element: Element) {
        if (!checkActive("addChild")) return
        if (element.parent !== this) {
            element.underlyingNativeElement.parent = this as NativeContainerElement
            if (element is ContainerElement && element.childDefaultAlignment == null) element.childDefaultAlignment = this.childDefaultAlignment
        }
        internalChildren.add(index, element)
        nativeAddChild(index, element)
    }
    final override fun addChild(element: Element) = addChild(children.size, element)
    final override fun removeChild(index: Int) {
        if (!checkActive("removeChild")) return
        if (index !in children.indices) throw IllegalArgumentException("$index not in range ${children.indices}")
        nativeRemoveChild(index)
        internalChildren.removeAt(index).underlyingNativeElement.shutdown()
    }
    final override fun removeChild(element: Element) {
        if (!checkActive("removeChild")) return
        val i = children.indexOf(element)
        if (i != -1) {
            nativeRemoveChild(i)
            internalChildren.removeAt(i).underlyingNativeElement.shutdown()
        }
        else throw IllegalStateException("$element is not a child of $this!")
    }
    final override fun clearChildren() {
        if (!checkActive("clearChildren")) return
        nativeClearChildren()
        for (e in children) e.underlyingNativeElement.shutdown()
        internalChildren.clear()
    }



    // --- LIFECYCLE ---

    override fun shutdown() {
        if (isShutdown) return
        if (Element.Debugger.removeBeforeShutdown) {
            for (index in internalChildren.lastIndex downTo 0) {
                removeChild(index)
                internalChildren.removeAt(index).underlyingNativeElement.shutdown()
            }
        } else {
            internalChildren.forEach { it.underlyingNativeElement.shutdown() }
            internalChildren.clear()
        }
        super.shutdown()
    }


    // --- THEMING ---

    override var themeAndBack: ThemeAndBack = Theme.placeholder.withBack
        set(value) {
            val oldCascading = field.theme.let { it.revert ?: it }
            super.themeAndBack = value
            val newCascading = field.theme.let { it.revert ?: it }
            if (oldCascading != newCascading) {
                for (child in children) child.underlyingNativeElement.refreshTheming()
            }
        }
}