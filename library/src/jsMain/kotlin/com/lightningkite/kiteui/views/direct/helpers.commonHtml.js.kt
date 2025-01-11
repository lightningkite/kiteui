package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.reactive.BaseListenable
import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.views.*
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.get

actual fun HtmlElementLike.resizeObserver(): Listenable {
    return object: BaseListenable() {
        var observer: ResizeObserver? = null
        override fun activate() {
            observer = ResizeObserver({ _, _->
                invokeAllListeners()
            }).apply {
                this@resizeObserver.onElement {
                    this.observe(it)
                }
            }
        }

        override fun deactivate() {
            observer?.disconnect()
            observer = null
        }
    }
}

inline fun HtmlElementLike.suppressMutationObserverForStyle(change: ()->Unit) {
    (this.element as? HTMLElement)?.let {
        it.suppressMutationObserverForStyle { change() }
    } ?: change()
}
inline fun HTMLElement.suppressMutationObserverForStyle(change: ()->Unit) {
    val e = this.asDynamic().__suppressMutationObserverStyle as? ArrayList<String> ?: run {
        val n = ArrayList<String>()
        this.asDynamic().__suppressMutationObserverStyle = n
        n
    }
    change()
    e.add(this.getAttribute("style") ?: "")
}
inline fun HTMLElement.suppressMutationObserverForClass(change: ()->Unit) {
    val e = this.asDynamic().__suppressMutationObserverClass as? ArrayList<String> ?: run {
        val n = ArrayList<String>()
        this.asDynamic().__suppressMutationObserverClass = n
        n
    }
    change()
    e.add(this.getAttribute("class") ?: "")
}

fun HTMLElement.measure(max: Size): Size {
    val tempchildwidth = this.style.width
    val tempchildheight = this.style.height
    val tempchildmaxWidth = this.style.maxWidth
    val tempchildmaxHeight = this.style.maxHeight
    suppressMutationObserverForStyle {
        this.style.position = "fixed"
        this.style.width = "unset"
        this.style.height = "unset"
        this.style.maxWidth = "${max.width}px"
        this.style.maxHeight = "${max.height}px"
    }
    val out = Size(this.scrollWidth.toDouble() + 1.0, this.scrollHeight.toDouble() + 1.0)
    suppressMutationObserverForStyle {
        this.style.position = "absolute"
        this.style.width = tempchildwidth
        this.style.height = tempchildheight
        this.style.maxWidth = tempchildmaxWidth
        this.style.maxHeight = tempchildmaxHeight
    }
    return out
}

actual fun HtmlElementLike.mutationObserver(recursive: Boolean): Listenable {
    return object: BaseListenable() {
        var observer: MutationObserver? = null
        override fun activate() {
            observer = MutationObserver({ rec, _->
                val e = this@mutationObserver.element ?: return@MutationObserver
                val suppressedStyleChanges = e.asDynamic().__suppressMutationObserverStyle as? ArrayList<String> ?: ArrayList()
                val suppressedClassChanges = e.asDynamic().__suppressMutationObserverClass as? ArrayList<String> ?: ArrayList()
                val s = (e.getAttribute("style") ?: "")
                val c = (e.className)
//                println("Checking '$s' against ${suppressedStyleChanges}")
//                println("Checking '$c' against ${suppressedClassChanges}")
                var anyNotSuppressed = false
                for(it in rec) {
                    val suppressedStyle = it.type == "attributes" && it.attributeName == "style" && it.target == e && s in suppressedStyleChanges
                    if(suppressedStyle) {
//                        println("Suppressed Style Change")
                        continue
                    }
                    val suppressedClass = it.type == "attributes" && it.attributeName == "class" && it.target == e && c in suppressedClassChanges
                    if(suppressedClass) {
//                        println("Suppressed Class Change")
                        continue
                    }
                    if(it.attributeName != null) {
                        val newValue = (it.target as? HTMLElement)?.getAttributeNS(
                            it.attributeNamespace,
                            it.attributeName!!
                        )
                        if((it.oldValue ?: "") == (newValue ?: "")) continue
                        ConsoleRoot.tag(it.attributeName ?: "???").log(
                            it.oldValue + "  ->  " + (it.target as? HTMLElement)?.getAttributeNS(
                                it.attributeNamespace,
                                it.attributeName!!
                            )
                        )
                    } else {
                        ConsoleRoot.tag(it.type).log(it)
                    }
                    anyNotSuppressed = true
                }
                suppressedStyleChanges.clear()
                suppressedClassChanges.clear()
                if(anyNotSuppressed) {
                    println("Mutation detected on ${this@mutationObserver}")
                    invokeAllListeners()
                }
            }).apply {
                this@mutationObserver.onElement {
                    this.observe(it, MutationObserverInit(childList = true, attributes = true, subtree = recursive, attributeOldValue = true))
                }
            }
        }

        override fun deactivate() {
            observer?.disconnect()
            observer = null
        }
    }
}