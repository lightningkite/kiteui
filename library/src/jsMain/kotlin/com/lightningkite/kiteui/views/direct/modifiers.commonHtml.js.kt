package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.ReactiveContext
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewHelper.Companion.animationsEnabled
import com.lightningkite.kiteui.views.hidden
import kotlinx.browser.window
import kotlinx.dom.addClass
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.js.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal actual fun RView.nativeAnimateHideBinding(
    default: Boolean,
    condition: ReactiveContext.() -> Boolean
) {
    native.attributes.hidden = !default
    var last = !default
    reactiveScope {
        val myElement = native.element as? HTMLElement
        val child = native.children.firstOrNull()?.element as? HTMLElement
        val value = !condition()
        if (value == last) return@reactiveScope
        last = value
        if (animationsEnabled && child != null && myElement != null) {
            myElement.addClass("animatingShowHide")

            val myStyle = window.getComputedStyle(child)
            val transitionTime = myStyle.transitionDuration.let { Duration.parseOrNull(it) } ?: 150.milliseconds
            val totalTime = transitionTime.inWholeMilliseconds.toDouble()
            var oldAnimTime = totalTime
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            (this.asDynamic().__kiteui__hiddenAnim as? Animation)?.let {
                oldAnimTime = it.currentTime
                it.cancel()
            }
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            ((this.asDynamic().__kiteui__hiddenAnim2 as? Animation)?.cancel())
            this.asDynamic().__kiteui__goalHidden = value
            myElement.hidden = false
            val parent = generateSequence(myElement) { it.parentElement as? HTMLElement }.drop(1)
                .firstOrNull { !it.classList.contains("toggle-button") } ?: return@reactiveScope
            val parentStyle = window.getComputedStyle(parent)
            val x =
                parentStyle.display == "flex" && parentStyle.flexDirection.contains("row") ||
                        parentStyle.display != "flex" && myStyle.display.contains("inline")
            val y =
                parentStyle.display == "flex" && parentStyle.flexDirection.contains("column") ||
                        parentStyle.display != "flex" && myStyle.display.let { it.contains("block") && !it.contains("inline") }

            val before = js("{}")
            val after = js("{}")
            val full = if (value) before else after
            val fullTransform = ArrayList<String>()
            val gone = if (value) after else before
            val goneTransform = ArrayList<String>()

            var fullWidth = ""
            var fullHeight = ""
            var gap = ""
            if (myElement.hidden) {
                myElement.hidden = false
                fullWidth = myStyle.width
                fullHeight = myStyle.height
                gap = when {
                    parentStyle.display == "flex" -> parentStyle.columnGap
                    parentStyle.display.contains("block") -> myStyle.marginBottom
                    parentStyle.display.contains("inline") -> myStyle.marginRight
                    else -> "0px"
                }
                myElement.hidden = true
            } else {
                fullWidth = myStyle.width
                fullHeight = myStyle.height
                gap = when {
                    parentStyle.display == "flex" -> parentStyle.columnGap
                    parentStyle.display.contains("block") -> myStyle.marginBottom
                    parentStyle.display.contains("inline") -> myStyle.marginRight
                    else -> "0px"
                }
            }
            child.style.width = myStyle.width
            child.style.maxWidth = "unset"
            child.style.height = myStyle.height
            child.style.maxHeight = "unset"

            if (x) {
                goneTransform.add("scaleX(0)")
                fullTransform.add("scaleX(1)")
                gone.marginLeft = "calc($gap / -2.0)"
                gone.paddingLeft = "0px"
                gone.marginRight = "calc($gap / -2.0)"
                gone.paddingRight = "0px"
                gone.width = "0px"
                gone.minWidth = "0px"
                gone.maxWidth = "0px"
                full.width = fullWidth
                full.minWidth = fullWidth
                full.maxWidth = fullWidth
            }
            if (y) {
                goneTransform.add("scaleY(0)")
                fullTransform.add("scaleY(1)")
                gone.marginTop = "calc($gap / -2.0)"
                gone.paddingTop = "0px"
                gone.marginBottom = "calc($gap / -2.0)"
                gone.paddingBottom = "0px"
                gone.height = "0px"
                gone.minHeight = "0px"
                gone.maxHeight = "0px"
                full.height = fullHeight
                full.minHeight = fullHeight
                full.maxHeight = fullHeight
            }
            if (!x && !y) {
                full.opacity = "1"
                gone.opacity = "0"
            }
            goneTransform.takeUnless { it.isEmpty() }?.let {
//                        gone.transform = it.joinToString(" ")
//                        gone.transformOrigin = "top left"
            }
            fullTransform.takeUnless { it.isEmpty() }?.let {
//                        full.transform = it.joinToString(" ")
//                        full.transformOrigin = "top left"
            }
            myElement.animate(
                arrayOf(before, after),
                json(
                    "duration" to totalTime,
                    "easing" to "ease-out"
                )
            ).let {
                it.currentTime = (totalTime - oldAnimTime).coerceAtLeast(0.0)
                it.onfinish = { ev ->
                    if (this.asDynamic().__kiteui__hiddenAnim == it) {
                        myElement.hidden = value
                        myElement.classList.remove("animatingShowHide")
                        this.asDynamic().__kiteui__hiddenAnim = null
                        child.style.width = "100%"
                        child.style.removeProperty("maxWidth")
                        child.style.height = "100%"
                        child.style.removeProperty("maxHeight")
                    }
                }
                it.oncancel = { ev ->
                    if (this.asDynamic().__kiteui__hiddenAnim == it) {
                        myElement.hidden = value
                        myElement.classList.remove("animatingShowHide")
                        this.asDynamic().__kiteui__hiddenAnim = null
                        child.style.width = "100%"
                        child.style.removeProperty("maxWidth")
                        child.style.height = "100%"
                        child.style.removeProperty("maxHeight")
                    }
                }
                it.onremove = { ev ->
                    if (this.asDynamic().__kiteui__hiddenAnim == it) {
                        myElement.hidden = value
                        myElement.classList.remove("animatingShowHide")
                        this.asDynamic().__kiteui__hiddenAnim = null
                        child.style.width = "100%"
                        child.style.removeProperty("maxWidth")
                        child.style.height = "100%"
                        child.style.removeProperty("maxHeight")
                    }
                }
                this.asDynamic().__kiteui__hiddenAnim = it
            }
        } else {
            native.attributes.hidden = value
        }
    }
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
inline fun HTMLElement.animate(keyframes: Array<dynamic>, options: dynamic): Animation =
    this.asDynamic().animate(keyframes, options) as Animation

inline fun HTMLElement.getAnimations(): Array<Animation> = this.asDynamic().getAnimations as Array<Animation>
external interface Animation {
    var oncancel: ((Event) -> Unit)?
    var onfinish: ((Event) -> Unit)?
    var onremove: ((Event) -> Unit)?
    fun cancel()
    fun commitStyles()
    fun finish()
    fun pause()
    fun play()
    fun reverse()
    var currentTime: Double
    var startTime: Double
}