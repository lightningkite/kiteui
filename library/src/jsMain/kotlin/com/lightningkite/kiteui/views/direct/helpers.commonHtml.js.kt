package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.models.Size
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.signal.BaseListenable
import com.lightningkite.signal.Listenable
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import org.w3c.dom.get

public actual fun HtmlElementLike.resizeObserver(): Listenable {
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

fun HTMLElement.measureByTempEdit(max: Size): Size {
    val tempchildwidth = this.style.width
    val tempchildheight = this.style.height
    val tempchildmaxWidth = this.style.maxWidth
    val tempchildmaxHeight = this.style.maxHeight
    suppressMutationObserverForStyle {
//        this.style.position = "fixed"
        this.style.width = "unset"
        this.style.height = "unset"
        this.style.maxWidth = "${max.width}px"
        this.style.maxHeight = "${max.height}px"
    }
    val out = Size(this.scrollWidth.toDouble() + 1.0, this.scrollHeight.toDouble() + 1.0)
    suppressMutationObserverForStyle {
//        this.style.position = "absolute"
        this.style.width = tempchildwidth
        this.style.height = tempchildheight
        this.style.maxWidth = tempchildmaxWidth
        this.style.maxHeight = tempchildmaxHeight
    }
    return out
}

fun HTMLElement.measureByDuplicate(max: Size): Size {
    println("Measuring by duplicate...")
    // This is nasty, but this is the only cross-browser safe way to do this.
    // We clone the view and check its size.
    val clone = this.cloneNode(true) as HTMLElement
    clone.style.visibility = "hidden"
    clone.style.width = if(max.width < 9999.0) "${max.width}px" else "unset"
    clone.style.height = if(max.height < 9999.0) "${max.height}px" else "unset"
    clone.style.maxWidth = "unset"
    clone.style.maxHeight = "unset"
    clone.style.position = "fixed"
    document.body!!.appendChild(clone)
    val out = Size(clone.scrollWidth.toDouble() + 1.0, clone.scrollHeight.toDouble() + 1.0)
    document.body!!.removeChild(clone)
//    console.log("Measuring by duplicate with max size $sizeConstraints got ${out}", this)
    return out
}

fun HTMLElement.measureByDuplicate(sizeConstraints: SizeConstraints): Size {
    // This is nasty, but this is the only cross-browser safe way to do this.
    // We clone the view and check its size.
    val clone = this.cloneNode(true) as HTMLElement
    clone.style.visibility = "hidden"
    clone.style.minWidth = sizeConstraints.minWidth?.value?.toString() ?: "unset"
    clone.style.maxWidth = sizeConstraints.maxWidth?.value?.toString() ?: "unset"
    clone.style.minHeight = sizeConstraints.minHeight?.value?.toString() ?: "unset"
    clone.style.maxHeight = sizeConstraints.maxHeight?.value?.toString() ?: "unset"
    clone.style.width = sizeConstraints.width?.value?.toString() ?: "unset"
    clone.style.height = sizeConstraints.height?.value?.toString() ?: "unset"
    clone.style.position = "fixed"
    document.body!!.appendChild(clone)
    val out = Size(clone.scrollWidth.toDouble() + 1.0, clone.scrollHeight.toDouble() + 1.0)
    document.body!!.removeChild(clone)
//    console.log("Measuring by duplicate with max size $sizeConstraints got ${out}", this)
    return out
}

public actual fun HtmlElementLike.mutationObserver(recursive: Boolean): Listenable {
    return object: BaseListenable() {
        var observer: MutationObserver? = null
        override fun activate() {
            observer = MutationObserver({ rec, _->
                val e = this@mutationObserver.element ?: return@MutationObserver
//                rec.forEach {
//                    ConsoleRoot.log(it.target, " updated ", it.type, it.attributeName)
//                }
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
                    }
                    anyNotSuppressed = true
                }
                suppressedStyleChanges.clear()
                suppressedClassChanges.clear()
                if(anyNotSuppressed) {
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