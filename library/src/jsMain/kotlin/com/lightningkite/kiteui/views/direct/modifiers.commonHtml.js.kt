package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Console
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.signal.ReactiveContext
import com.lightningkite.signal.reactiveScope
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.animationsEnabled
import com.lightningkite.kiteui.views.hidden
import com.lightningkite.signal.AppScope
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.dom.addClass
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import kotlin.js.Json
import kotlin.js.json
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal actual fun RView.nativeAnimateShow() {
    log?.info("${children.singleOrNull()?.debugName}.nativeAnimateShow")
    (
            showHideQueue ?: run {
                val newMap = HashMap<RView, Boolean>()
                showHideQueue = newMap
                window.setTimeout(showHideWorker, 32)
                newMap
            }
            ).put(this, true)
}

internal actual fun RView.nativeAnimateHide() {
    log?.info("${children.singleOrNull()?.debugName}.nativeAnimateHide")
    (
            showHideQueue ?: run {
                val newMap = HashMap<RView, Boolean>()
                showHideQueue = newMap
                window.setTimeout(showHideWorker, 32)
                newMap
            }
            ).put(this, false)
}

private val showHideAnimating = HashMap<RView, OngoingAnimation>()
private var showHideQueue: HashMap<RView, Boolean>? = null

private data class OngoingAnimation(
    public val on: RView,
    public val from: Json,
    public val to: Json,
    public val goal: Boolean,
    public val startRatio: Double,
) {
    public var totalTime: Double = 1000.0
    public val myElement = on.native.element as HTMLElement
    public val child = on.children[0].native.element as HTMLElement
    public var animation: Animation? = null
    private var widthChildResume: String = ""
    private var maxWidthChildResume: String = ""
    private var heightChildResume: String = ""
    private var maxHeightChildResume: String = ""
    public fun animRatio() = animation!!.currentTime.toFloat() / totalTime
    init {
        log?.info(
            "QueuedAnimation: ${on.children.singleOrNull()?.debugName} / $goal: ${JSON.stringify(from)} -> ${
                JSON.stringify(
                    to
                )
            }"
        )
    }

    public fun play() {
        log?.info("Starting animation on ${on.children.singleOrNull()?.debugName}")
        myElement.hidden = false
        totalTime = on.theme.transitionDuration.inWholeMilliseconds.toDouble()
        animation = (on.native.element as HTMLElement).animate(
            arrayOf(from, to),
            json(
                "duration" to totalTime,
                "easing" to "linear"
            )
        ).also {
            it.currentTime = (startRatio * totalTime).also { log?.info("Starting at $it") }
            it.onfinish = { done() }
            it.oncancel = { done() }
            it.onremove = { done() }
        }
        myElement.classList.add("animatingShowHide")
        showHideAnimating[on] = this
        log?.log("showHideAnimating: ${showHideAnimating.keys.joinToString { it.children.singleOrNull()?.debugName ?: "?" }}")
    }

    private var closed = false
    public fun cancel() {
        animation!!.finish()
        done()
    }
    public val done = label@{
        if(closed) return@label
        closed = true
        showHideAnimating.remove(on)
        log?.log("showHideAnimating: ${showHideAnimating.keys.joinToString { it.children.singleOrNull()?.debugName ?: "?" }}")
        myElement.hidden = !goal
        (on.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
        myElement.classList.remove("animatingShowHide")
        child.style.width = "100%"
        child.style.removeProperty("maxWidth")
        child.style.height = "100%"
        child.style.removeProperty("maxHeight")
    }

    public fun pretendEnd() {
        log?.info("pretendEnd")
        animation!!.pause()
        myElement.hidden = !goal
//        forEach(to) { k, v -> myElement.style.setProperty(k, v) }
        widthChildResume = child.style.width
        maxWidthChildResume = child.style.maxWidth
        heightChildResume = child.style.height
        maxHeightChildResume = child.style.maxHeight
        child.style.width = "100%"
        child.style.removeProperty("maxWidth")
        child.style.height = "100%"
        child.style.removeProperty("maxHeight")
    }

    public fun continueNow() {
        log?.info("continueNow")
        myElement.hidden = false
        child.style.width = widthChildResume
        child.style.maxWidth = maxWidthChildResume
        child.style.height = heightChildResume
        child.style.maxHeight = maxHeightChildResume
        animation!!.play()
    }
}

private val log: Console? = null// ConsoleRoot.tag("showHide")
private val showHideWorker = label@{

    val showHideQueue = run {
        val it = showHideQueue
        showHideQueue = null
        it
    } ?: return@label

//    val delayLevel: Duration? = 150.milliseconds
    val delayLevel: Duration? = null
    AppScope.launch {

        log?.info("//////////////////////////////////////////")
        log?.info("Queued: ${showHideQueue.size}")
        val pastRatios = showHideAnimating.entries.toList().mapNotNull {
            if (it.key in showHideQueue.keys) {
                log?.info("Cancelling animation on ${it.key.children.singleOrNull()?.debugName}")
                val r = it.key to it.value.animRatio()
                it.value.cancel()
                r
            } else null
        }.associate { it }
        showHideQueue.keys.removeAll { it.parent == null || it.children.isEmpty() || it.native.children.isEmpty() || it.native.element == null }
        showHideQueue.forEach { it ->
            log?.info("  View ${it.key.children.singleOrNull()?.debugName} -> ${it.value}")
        }

        // Lock current sizes for views that are disappearing.
        log?.info("// Lock current sizes for views that are disappearing.")
        showHideQueue.forEach { (on, goal) ->
            if (goal) return@forEach
            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            child.style.width = myElement.clientWidth.toString() + "px"
            child.style.maxWidth = "unset"
            child.style.height = myElement.clientHeight.toString() + "px"
            child.style.maxHeight = "unset"
        }
        delayLevel?.let { delay(it) }

        // Get the whole layout into the goal state for measurement.
        log?.info("// Get the whole layout into the goal state for measurement.")
        showHideAnimating.forEach { it.value.pretendEnd() }
        val beforeVisibility = showHideQueue.map {
            val was = (it.key.native.element as HTMLElement).hidden
            (it.key.native.element as HTMLElement).hidden = !it.value
            (it.key.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
            log?.info("View ${it.key.children.singleOrNull()?.debugName} -> ${it.value}")
            it.key to was
        }
        delayLevel?.let { delay(it * 4) }

        // Lock current sizes for views that are appearing.
        log?.info("// Lock current sizes for views that are appearing.")
        showHideQueue.forEach { (on, goal) ->
            if (!goal) return@forEach
            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            child.style.width = myElement.clientWidth.toString() + "px"
            child.style.maxWidth = "unset"
            child.style.height = myElement.clientHeight.toString() + "px"
            child.style.maxHeight = "unset"
            log?.info("  View ${on.children.singleOrNull()?.debugName} appearing locked to ${myElement.clientWidth}px x ${myElement.clientHeight}px")
        }
        delayLevel?.let { delay(it) }

        // Queue the animations.
        log?.info("// Queue the animations.")
        val queuedAnimations = showHideQueue.map { (on, goal) ->

            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            val parent = on.parent!!.native.element as HTMLElement
            val myStyle = window.getComputedStyle(myElement)
            val childStyle = window.getComputedStyle(child)
            val parentStyle = window.getComputedStyle(parent)

            val x =
                parentStyle.display == "flex" && parentStyle.flexDirection.contains("row") ||
                        parentStyle.display != "flex" && childStyle.display.contains("inline")
            val y =
                parentStyle.display == "flex" && parentStyle.flexDirection.contains("column") ||
                        parentStyle.display != "flex" && childStyle.display.let { it.contains("block") && !it.contains("inline") }
            val weighted = myStyle.flexGrow.takeIf { it.isNotBlank() && it != "0" }

            val before = js("{}")
            val after = js("{}")
            val full = if (goal) after else before
            val fullTransform = ArrayList<String>()
            val gone = if (goal) before else after
            val goneTransform = ArrayList<String>()

            when {
                x -> {
                    log?.info("Anim x")
                    val gap = when {
                        parentStyle.display == "flex" -> parentStyle.columnGap
                        parentStyle.display.contains("block") -> childStyle.marginBottom
                        parentStyle.display.contains("inline") -> childStyle.marginRight
                        else -> "0px"
                    }
                    goneTransform.add("scaleX(0)")
                    fullTransform.add("scaleX(1)")
                    gone.marginLeft = "calc($gap / -2.0)"
                    gone.paddingLeft = "0px"
                    gone.marginRight = "calc($gap / -2.0)"
                    gone.paddingRight = "0px"

                    weighted?.let {
                        full.flexGrow = weighted
                        full.flexShrink = weighted
                        gone.flexGrow = "0"
                        gone.flexShrink = "0"
                    } ?: run {
                        val fullWidth = childStyle.width
                        gone.width = "0px"
                        gone.minWidth = "0px"
                        gone.maxWidth = "0px"
                        full.width = fullWidth
                        full.minWidth = fullWidth
                        full.maxWidth = fullWidth
                    }
                }

                y -> {
                    val gap = when {
                        parentStyle.display == "flex" -> parentStyle.columnGap
                        parentStyle.display.contains("block") -> childStyle.marginBottom
                        parentStyle.display.contains("inline") -> childStyle.marginRight
                        else -> "0px"
                    }
                    log?.info("Anim y")
                    goneTransform.add("scaleY(0)")
                    fullTransform.add("scaleY(1)")
                    gone.marginTop = "calc($gap / -2.0)"
                    gone.paddingTop = "0px"
                    gone.marginBottom = "calc($gap / -2.0)"
                    gone.paddingBottom = "0px"

                    weighted?.let {
                        full.flexGrow = weighted
                        full.flexShrink = weighted
                        gone.flexGrow = "0"
                        gone.flexShrink = "0"
                    } ?: run {
                        val fullHeight = childStyle.height
                        gone.height = "0px"
                        gone.minHeight = "0px"
                        gone.maxHeight = "0px"
                        full.height = fullHeight
                        full.minHeight = fullHeight
                        full.maxHeight = fullHeight
                    }
                }

                else -> {
                    full.opacity = "1"
                    gone.opacity = "0"
                }
            }
            goneTransform.takeUnless { it.isEmpty() }?.let {
//                gone.transform = it.joinToString(" ")
//                gone.transformOrigin = "top left"
            }
            fullTransform.takeUnless { it.isEmpty() }?.let {
//                full.transform = it.joinToString(" ")
//                full.transformOrigin = "top left"
            }

            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            OngoingAnimation(
                on = on,
                from = before as Json,
                to = after as Json,
                goal = goal,
                startRatio = 1.0 - pastRatios.getOrElse(on) { 1.0 } .also { log?.info("Past for ${on.children.singleOrNull()?.debugName} is $it") },
            )
        }
        delayLevel?.let { delay(it) }

        // Revert to what we were.
        log?.info("// Revert to what we were.")
        showHideAnimating.forEach { it.value.continueNow() }
        beforeVisibility.forEach {
            (it.first.native.element as HTMLElement).hidden = it.second
            log?.info("View ${it.first.children.singleOrNull()?.debugName} -> ${it.second}")
            (it.first.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
        }
        delayLevel?.let { delay(it) }

        // Begin animating the views in question.
        log?.info("// Begin animating the views in question.")
        queuedAnimations.forEach {
            it.play()
        }
        delayLevel?.let { delay(it) }
    }
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
public inline fun HTMLElement.animate(keyframes: Array<dynamic>, options: dynamic): Animation =
    asDynamic().animate(keyframes, options) as Animation

@Suppress("NOTHING_TO_INLINE")
public inline fun HTMLElement.getAnimations(): Array<Animation> = asDynamic().getAnimations as Array<Animation>
public external interface Animation {
    public var oncancel: ((Event) -> Unit)?
    public var onfinish: ((Event) -> Unit)?
    public var onremove: ((Event) -> Unit)?
    public fun cancel()
    public fun commitStyles()
    public fun finish()
    public fun pause()
    public fun play()
    public fun reverse()
    public var currentTime: Double
    public var startTime: Double
}

private fun forEach(receiver: Json, action: (key: String, value: dynamic) -> Unit) {
    for (key in js("Object.keys(receiver)")) {
        action(key, receiver[key])
    }
}