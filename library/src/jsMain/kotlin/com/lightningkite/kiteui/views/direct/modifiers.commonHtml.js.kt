package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.models.ScreenTransition
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.native
import com.lightningkite.kiteui.views.theme
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.*
import kotlin.js.Json
import kotlin.js.json
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.events.Event
import org.w3c.dom.get
import kotlin.time.Duration.Companion.milliseconds

private data class ShowHideRequest(val goal: Boolean, val transition: ScreenTransition)

// by Claude - shared scheduling for show/hide and weight animations
private val showHideQueue = HashMap<ContainerElement, ShowHideRequest>()
private val weightChangeQueue = HashMap<ContainerElement, Pair<Float, Float>>()
private var workerScheduled = false

private fun ensureWorkerScheduled() {
    if (!workerScheduled) {
        workerScheduled = true
        window.setTimeout(combinedAnimationWorker, 32)
    }
}

internal actual fun ContainerElement.nativeAnimateShow(transition: ScreenTransition) {
    log?.info("${children.singleOrNull()?.debugName}.nativeAnimateShow")
    showHideQueue[this] = ShowHideRequest(true, transition)
    ensureWorkerScheduled()
}

internal actual fun ContainerElement.nativeAnimateHide(transition: ScreenTransition) {
    log?.info("${children.singleOrNull()?.debugName}.nativeAnimateHide")
    showHideQueue[this] = ShowHideRequest(false, transition)
    ensureWorkerScheduled()
}

// by Claude - weight animation queuing
internal actual fun ContainerElement.nativeAnimateWeight(fromWeight: Float, toWeight: Float) {
    log?.info("${children.singleOrNull()?.debugName}.nativeAnimateWeight: $fromWeight -> $toWeight")
    val existing = weightChangeQueue[this]
    if (existing != null) {
        // Keep original 'from', update 'to'
        weightChangeQueue[this] = existing.first to toWeight
    } else {
        weightChangeQueue[this] = fromWeight to toWeight
    }
    ensureWorkerScheduled()
}

private val showHideAnimating = HashMap<ContainerElement, OngoingAnimation>()
// by Claude - tracking ongoing weight animations
private val weightAnimating = HashMap<ContainerElement, OngoingWeightAnimation>()

private data class OngoingAnimation(
    val on: ContainerElement,
    val from: Json,
    val to: Json,
    val goal: Boolean,
    val startRatio: Double,
    val easingCss: String = "linear",
) {
    var totalTime: Double = 1000.0
    val myElement = on.native.element as HTMLElement
    val child = on.children[0].native.element as HTMLElement
    var animation: Animation? = null
    private var widthChildResume: String = ""
    private var maxWidthChildResume: String = ""
    private var heightChildResume: String = ""
    private var maxHeightChildResume: String = ""
    fun animRatio() = animation!!.currentTime.toFloat() / totalTime

    init {
        log?.info(
            "QueuedAnimation: ${on.children.singleOrNull()?.debugName} / $goal: ${JSON.stringify(from)} -> ${
                JSON.stringify(
                    to
                )
            }"
        )
    }

    fun play() {
        log?.info("Starting animation on ${on.children.singleOrNull()?.debugName}")
        myElement.hidden = false
        totalTime = on.theme.transitionDuration.inWholeMilliseconds.toDouble()
        animation = (on.native.element as HTMLElement).animate(
            arrayOf(from, to),
            json(
                "duration" to totalTime,
                "easing" to easingCss
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
    fun cancel() {
        animation!!.finish()
        done()
    }

    val done = label@{
        if (closed) return@label
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

    fun pretendEnd() {
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

    fun continueNow() {
        log?.info("continueNow")
        myElement.hidden = false
        child.style.width = widthChildResume
        child.style.maxWidth = maxWidthChildResume
        child.style.height = heightChildResume
        child.style.maxHeight = maxHeightChildResume
        animation!!.play()
    }
}

// by Claude - weight animation class, mirrors OngoingAnimation but for flex-grow/flex-shrink.
// fromBasis/toBasis are pixel values (e.g. "150px") when weight is 0, or "0" when weight is non-zero.
// CSS can't interpolate between "0" and "auto", so we resolve "auto" to measured pixels.
private class OngoingWeightAnimation(
    val on: ContainerElement,
    val fromWeight: Float,
    val toWeight: Float,
    val fromBasis: String,
    val toBasis: String,
    val startRatio: Double,
) {
    var totalTime: Double = 1000.0
    val myElement = on.native.element as HTMLElement
    val child = on.children[0].native.element as HTMLElement
    var animation: Animation? = null
    private var widthChildResume: String = ""
    private var maxWidthChildResume: String = ""
    private var heightChildResume: String = ""
    private var maxHeightChildResume: String = ""
    // by Claude - saved inline styles for pretendEnd/continueNow
    private var savedFlexGrow: String = ""
    private var savedFlexShrink: String = ""
    private var savedFlexBasis: String = ""
    private var savedProgress: Double = startRatio * 1000.0
    fun animRatio() = animation!!.currentTime.toFloat() / totalTime

    private fun buildKeyframes(): Array<dynamic> = arrayOf(
        json("flexGrow" to "$fromWeight", "flexShrink" to "$fromWeight", "flexBasis" to fromBasis),
        json("flexGrow" to "$toWeight", "flexShrink" to "$toWeight", "flexBasis" to toBasis)
    )

    fun play() {
        log?.info("Starting weight animation on ${on.children.singleOrNull()?.debugName}: $fromWeight($fromBasis) -> $toWeight($toBasis)")
        totalTime = on.theme.transitionDuration.inWholeMilliseconds.toDouble()
        animation = myElement.animate(
            buildKeyframes(),
            json("duration" to totalTime, "easing" to "linear")
        ).also {
            it.currentTime = (startRatio * totalTime).also { log?.info("Weight anim starting at $it") }
            it.onfinish = { done() }
            it.oncancel = { done() }
            it.onremove = { done() }
        }
        myElement.classList.add("animatingShowHide")
        weightAnimating[on] = this
    }

    private var closed = false

    val done = label@{
        if (closed) return@label
        closed = true
        weightAnimating.remove(on)
        myElement.classList.remove("animatingShowHide")
        // Apply final inline styles
        myElement.style.flexGrow = "$toWeight"
        myElement.style.flexShrink = "$toWeight"
        myElement.style.flexBasis = if (toWeight != 0f) "0" else "auto"
        (on.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
        // Unlock child dimensions
        child.style.width = "100%"
        child.style.removeProperty("maxWidth")
        child.style.height = "100%"
        child.style.removeProperty("maxHeight")
    }

    fun cancel() {
        // Cancel the Web Animation (removes its style override)
        animation?.cancel()
        closed = true
        weightAnimating.remove(on)
        myElement.classList.remove("animatingShowHide")
        // Unlock child dimensions
        child.style.width = "100%"
        child.style.removeProperty("maxWidth")
        child.style.height = "100%"
        child.style.removeProperty("maxHeight")
    }

    // by Claude - pretendEnd must cancel the animation (not just pause) because Web Animations
    // override inline styles. Unlike shownWhen which uses the `hidden` DOM attribute (independent
    // of CSS), weight uses inline flex-grow/flex-shrink which the animation would override.
    fun pretendEnd() {
        log?.info("weight pretendEnd")
        savedProgress = animation?.currentTime ?: (startRatio * totalTime)
        animation?.cancel()
        // Set target inline styles so layout can be measured at goal state
        savedFlexGrow = myElement.style.flexGrow
        savedFlexShrink = myElement.style.flexShrink
        savedFlexBasis = myElement.style.flexBasis
        myElement.style.flexGrow = "$toWeight"
        myElement.style.flexShrink = "$toWeight"
        myElement.style.flexBasis = if (toWeight != 0f) "0" else "auto"
        // Save and unlock child dimensions for measurement
        widthChildResume = child.style.width
        maxWidthChildResume = child.style.maxWidth
        heightChildResume = child.style.height
        maxHeightChildResume = child.style.maxHeight
        child.style.width = "100%"
        child.style.removeProperty("maxWidth")
        child.style.height = "100%"
        child.style.removeProperty("maxHeight")
    }

    // by Claude - continueNow recreates the animation since we had to cancel it in pretendEnd
    fun continueNow() {
        log?.info("weight continueNow")
        // Restore pre-pretendEnd inline styles
        myElement.style.flexGrow = savedFlexGrow
        myElement.style.flexShrink = savedFlexShrink
        myElement.style.flexBasis = savedFlexBasis
        // Restore locked child dimensions
        child.style.width = widthChildResume
        child.style.maxWidth = maxWidthChildResume
        child.style.height = heightChildResume
        child.style.maxHeight = maxHeightChildResume
        // Recreate the animation from saved progress
        totalTime = on.theme.transitionDuration.inWholeMilliseconds.toDouble()
        animation = myElement.animate(
            buildKeyframes(),
            json("duration" to totalTime, "easing" to "linear")
        ).also {
            it.currentTime = savedProgress
            it.onfinish = { done() }
            it.oncancel = { done() }
            it.onremove = { done() }
        }
    }
}

private val log: Log? = null//Log.tag("anim")

// by Claude - combined worker processes both show/hide and weight queues in a single batch
private val combinedAnimationWorker = label@{
    // Snapshot and clear both queues
    val currentShowHideQueue = if (showHideQueue.isNotEmpty()) HashMap(showHideQueue) else null
    showHideQueue.clear()
    val currentWeightQueue = if (weightChangeQueue.isNotEmpty()) HashMap(weightChangeQueue) else null
    weightChangeQueue.clear()
    workerScheduled = false

    if (currentShowHideQueue == null && currentWeightQueue == null) return@label

//    val delayLevel: Duration? = 150.milliseconds
    val delayLevel: Duration? = null
    AppScope.launch {

        log?.info("//////////////////////////////////////////")

        // === Phase 1: Cancel overlapping animations and capture ratios ===

        val pastShowHideRatios = if (currentShowHideQueue != null) {
            log?.info("Show/Hide Queued: ${currentShowHideQueue.size}")
            showHideAnimating.entries.map { it.key to it.value }.mapNotNull {
                if (it.first in currentShowHideQueue.keys) {
                    log?.info("Cancelling show/hide animation on ${it.first.children.singleOrNull()?.debugName}")
                    val r = it.first to it.second.animRatio()
                    it.second.cancel()
                    r
                } else null
            }.associate { it }
        } else emptyMap()

        val pastWeightRatios = if (currentWeightQueue != null) {
            log?.info("Weight Queued: ${currentWeightQueue.size}")
            weightAnimating.entries.map { it.key to it.value }.mapNotNull {
                if (it.first in currentWeightQueue.keys) {
                    log?.info("Cancelling weight animation on ${it.first.children.singleOrNull()?.debugName}")
                    val r = it.first to it.second.animRatio()
                    it.second.cancel()
                    r
                } else null
            }.associate { it }
        } else emptyMap()

        // Clean up invalid entries
        currentShowHideQueue?.keys?.removeAll { it.parent == null || it.children.isEmpty() || it.native.children.isEmpty() || it.native.element == null }
        currentWeightQueue?.keys?.removeAll { it.parent == null || it.children.isEmpty() || it.native.children.isEmpty() || it.native.element == null }

        currentShowHideQueue?.forEach {
            log?.info("  ShowHide: ${it.key.children.singleOrNull()?.debugName} -> ${it.value.goal}")
        }
        currentWeightQueue?.forEach {
            log?.info("  Weight: ${it.key.children.singleOrNull()?.debugName} -> ${it.value}")
        }

        // by Claude - measure pre-goal sizes for elements currently at weight 0 (transitioning FROM 0).
        // We need pixel values because CSS can't interpolate between "0px" and "auto".
        val weightFromBasis = currentWeightQueue?.mapNotNull { (on, weights) ->
            if (weights.first != 0f) return@mapNotNull null
            val myElement = on.native.element as HTMLElement
            val parentEl = on.parent?.native?.element as? HTMLElement ?: return@mapNotNull null
            val parentStyle = window.getComputedStyle(parentEl)
            val isColumn = parentStyle.flexDirection.contains("column")
            val rect = myElement.getBoundingClientRect()
            on to "${if (isColumn) rect.height else rect.width}px"
        }?.associate { it } ?: emptyMap()

        // === Phase 2: Lock sizes for disappearing/shrinking views ===
        log?.info("// Lock current sizes for disappearing/shrinking views.")

        currentShowHideQueue?.forEach { (on, request) ->
            if (request.goal) return@forEach
            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            child.style.width = myElement.clientWidth.toString() + "px"
            child.style.maxWidth = "unset"
            child.style.height = myElement.clientHeight.toString() + "px"
            child.style.maxHeight = "unset"
        }

        currentWeightQueue?.forEach { (on, weights) ->
            val (fromWeight, toWeight) = weights
            if (toWeight >= fromWeight) return@forEach  // Not shrinking
            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            child.style.width = myElement.clientWidth.toString() + "px"
            child.style.maxWidth = "unset"
            child.style.height = myElement.clientHeight.toString() + "px"
            child.style.maxHeight = "unset"
        }

        val displayValuesPreHide = currentShowHideQueue?.asSequence()
            ?.filter { !(it.key.native.element as HTMLElement).hidden }
            ?.associate { it.key to window.getComputedStyle(it.key.native.element as HTMLElement).display }
            ?: emptyMap()

        delayLevel?.let { delay(it) }

        // === Phase 3: Set goal state for measurement ===
        log?.info("// Get the whole layout into the goal state for measurement.")

        // Pretend-end all ongoing animations
        showHideAnimating.forEach { it.value.pretendEnd() }
        weightAnimating.forEach { it.value.pretendEnd() }

        // Set goal state for show/hide queue
        val beforeVisibility = currentShowHideQueue?.map {
            val was = (it.key.native.element as HTMLElement).hidden
            (it.key.native.element as HTMLElement).hidden = !it.value.goal
            (it.key.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
            log?.info("View ${it.key.children.singleOrNull()?.debugName} -> ${it.value.goal}")
            it.key to was
        }

        val isLastVisibleAfterHide = currentShowHideQueue?.asSequence()
            ?.mapNotNull { (on, _) ->
                val parentEl = on.parent?.native?.element as? HTMLElement ?: return@mapNotNull null
                if (!parentEl.classList.contains("optimized")) return@mapNotNull null
                val myElement = on.native.element as HTMLElement
                val lastVisible = (parentEl.children.length.minus(1) downTo 0).asSequence()
                    .mapNotNull { parentEl.children[it] as? HTMLElement }
                    .find { !it.hidden }
                if (lastVisible == myElement) on else null
            }?.toSet() ?: emptySet()

        log?.log("isLastVisibleAfterHide: ${isLastVisibleAfterHide.map { it.children.singleOrNull()?.debugName ?: "?" }}")

        // Set goal state for weight queue
        val beforeWeightStyles = currentWeightQueue?.map { (on, weights) ->
            val (_, toWeight) = weights
            val myElement = on.native.element as HTMLElement
            val savedGrow = myElement.style.flexGrow
            val savedShrink = myElement.style.flexShrink
            val savedBasis = myElement.style.flexBasis
            myElement.style.flexGrow = "$toWeight"
            myElement.style.flexShrink = "$toWeight"
            myElement.style.flexBasis = if (toWeight != 0f) "0" else "auto"
            (on.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
            on to Triple(savedGrow, savedShrink, savedBasis)
        }

        // by Claude - measure goal-state sizes for elements transitioning TO weight 0.
        val weightToBasis = currentWeightQueue?.mapNotNull { (on, weights) ->
            if (weights.second != 0f) return@mapNotNull null
            val myElement = on.native.element as HTMLElement
            val parentEl = on.parent?.native?.element as? HTMLElement ?: return@mapNotNull null
            val parentStyle = window.getComputedStyle(parentEl)
            val isColumn = parentStyle.flexDirection.contains("column")
            val rect = myElement.getBoundingClientRect()
            on to "${if (isColumn) rect.height else rect.width}px"
        }?.associate { it } ?: emptyMap()

        delayLevel?.let { delay(it * 4) }

        // === Phase 4: Lock sizes for appearing/expanding views ===
        log?.info("// Lock current sizes for appearing/expanding views.")

        currentShowHideQueue?.forEach { (on, request) ->
            if (!request.goal) return@forEach
            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            child.style.width = myElement.clientWidth.toString() + "px"
            child.style.maxWidth = "unset"
            child.style.height = myElement.clientHeight.toString() + "px"
            child.style.maxHeight = "unset"
            log?.info("  View ${on.children.singleOrNull()?.debugName} appearing locked to ${myElement.clientWidth}px x ${myElement.clientHeight}px")
        }

        currentWeightQueue?.forEach { (on, weights) ->
            val (fromWeight, toWeight) = weights
            if (toWeight < fromWeight) return@forEach  // Not expanding
            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            child.style.width = myElement.clientWidth.toString() + "px"
            child.style.maxWidth = "unset"
            child.style.height = myElement.clientHeight.toString() + "px"
            child.style.maxHeight = "unset"
            log?.info("  Weight ${on.children.singleOrNull()?.debugName} expanding locked to ${myElement.clientWidth}px x ${myElement.clientHeight}px")
        }

        delayLevel?.let { delay(it) }

        // === Phase 5: Build keyframes ===
        log?.info("// Queue the animations.")

        val queuedShowHideAnimations = currentShowHideQueue?.map { (on, request) ->
            val goal = request.goal
            val transition = request.transition

            val myElement = on.native.element as HTMLElement
            val child = on.children[0].native.element as HTMLElement
            val parent = on.parent!!.native.element as HTMLElement
            val myStyle = window.getComputedStyle(myElement)
            val childStyle = window.getComputedStyle(child)
            val parentStyle = window.getComputedStyle(parent)

            // Frame / CoordinatorFrame are z-stacks — hiding a child shouldn't collapse layout
            val isZStack = on.parent?.underlyingNativeElement is Frame || on.parent?.underlyingNativeElement is CoordinatorFrame
            val x = !isZStack && (
                parentStyle.display == "flex" && parentStyle.flexDirection.contains("row") ||
                        parentStyle.display != "flex" && (displayValuesPreHide[on]
                    ?: myStyle.display).contains("inline"))
            val y = !isZStack && (
                parentStyle.display == "flex" && parentStyle.flexDirection.contains("column") ||
                        parentStyle.display != "flex" && parent.classList.contains("optimized"))
            val weighted = myStyle.flexGrow.takeIf { it.isNotBlank() && it != "0" }
            val usingFlexGap = parentStyle.display == "flex"

            val before = js("{}")
            val after = js("{}")
            val full = if (goal) after else before
            val gone = if (goal) before else after

            // Layout collapse keyframes (automatic based on container type)
            if (x) {
                val gapX = parentStyle.columnGap
                gone.marginLeft = "calc($gapX / -2.0)"
                gone.paddingLeft = "0px"
                gone.marginRight = "calc($gapX / -2.0)"
                gone.paddingRight = "0px"
                if (weighted == null) {
                    val fullWidth = childStyle.width
                    gone.width = "0px"
                    gone.minWidth = "0px"
                    gone.maxWidth = "0px"
                    full.width = fullWidth
                    full.minWidth = fullWidth
                    full.maxWidth = fullWidth
                }
            }
            if (y) {
                if (usingFlexGap) {
                    val gapY = parentStyle.columnGap
                    gone.marginTop = "calc($gapY / -2.0)"
                    gone.paddingTop = "0px"
                    gone.marginBottom = "calc($gapY / -2.0)"
                    gone.paddingBottom = "0px"
                } else {
                    val gap = parentStyle.columnGap
                    val afterLast = on in isLastVisibleAfterHide
                    if(afterLast) {
                        full.marginBottom = "0px"
                    }
                    gone.marginBottom = "0px"
                }
                if (weighted == null) {
                    val fullHeight = childStyle.height
                    gone.height = "0px"
                    gone.minHeight = "0px"
                    gone.maxHeight = "0px"
                    full.height = fullHeight
                    full.minHeight = fullHeight
                    full.maxHeight = fullHeight
                }
            }
            weighted?.let {
                full.flexGrow = weighted
                full.flexShrink = weighted
                gone.flexGrow = "0"
                gone.flexShrink = "0"
            }

            // Visual transition keyframes (from ScreenTransition parameter)
            if (transition.fade) {
                full.opacity = "1"
                gone.opacity = "0"
            }

            // Build CSS transform from Transformation fields for visual effect
            fun com.lightningkite.kiteui.models.Transformation.toCssTransform(): String? {
                val parts = mutableListOf<String>()
                if (translationX != 0.0 || translationY != 0.0) parts.add("translate(${(translationX * 100).toInt()}%, ${(translationY * 100).toInt()}%)")
                if (scaleX != 1.0 || scaleY != 1.0) parts.add("scale($scaleX, $scaleY)")
                if (rotation != 0.0) parts.add("rotate(${rotation}deg)")
                if (rotationX != 0.0) parts.add("rotateX(${rotationX}deg)")
                if (rotationY != 0.0) parts.add("rotateY(${rotationY}deg)")
                return if (parts.isEmpty()) null else parts.joinToString(" ")
            }

            val entryTransformCss = transition.entryTransform.toCssTransform()
            val exitTransformCss = transition.exitTransform.toCssTransform()
            if (entryTransformCss != null || exitTransformCss != null) {
                // "before" is the entry state when showing, the full state when hiding
                // "after" is the full state when showing, the exit state when hiding
                if (goal) {
                    // Showing: before=entry, after=identity
                    before.transform = entryTransformCss ?: "none"
                    after.transform = "none"
                } else {
                    // Hiding: before=identity, after=exit
                    before.transform = "none"
                    after.transform = exitTransformCss ?: "none"
                }
            }

            val easing = transition.easing
            val easingCss = "cubic-bezier(${easing.x1}, ${easing.y1}, ${easing.x2}, ${easing.y2})"

            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            OngoingAnimation(
                on = on,
                from = before as Json,
                to = after as Json,
                goal = goal,
                startRatio = 1.0 - pastShowHideRatios.getOrElse(on) { 1.0 }
                    .also { log?.info("Past for ${on.children.singleOrNull()?.debugName} is $it") },
                easingCss = easingCss,
            )
        }

        val queuedWeightAnimations = currentWeightQueue?.map { (on, weights) ->
            val (fromWeight, toWeight) = weights
            OngoingWeightAnimation(
                on = on,
                fromWeight = fromWeight,
                toWeight = toWeight,
                fromBasis = if (fromWeight != 0f) "0" else (weightFromBasis[on] ?: "auto"),
                toBasis = if (toWeight != 0f) "0" else (weightToBasis[on] ?: "auto"),
                startRatio = 1.0 - pastWeightRatios.getOrElse(on) { 1.0 }
                    .also { log?.info("Weight past for ${on.children.singleOrNull()?.debugName} is $it") },
            )
        }

        delayLevel?.let { delay(it) }

        // === Phase 6: Revert to pre-goal state ===
        log?.info("// Revert to what we were.")

        showHideAnimating.forEach { it.value.continueNow() }
        weightAnimating.forEach { it.value.continueNow() }

        beforeVisibility?.forEach {
            (it.first.native.element as HTMLElement).hidden = it.second
            log?.info("View ${it.first.children.singleOrNull()?.debugName} -> ${it.second}")
            (it.first.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
        }

        beforeWeightStyles?.forEach { (on, saved) ->
            val myElement = on.native.element as HTMLElement
            myElement.style.flexGrow = saved.first
            myElement.style.flexShrink = saved.second
            myElement.style.flexBasis = saved.third
            (on.parent as? RowOrCol)?.rerunOptimizedBottomMarginCalc()
        }

        delayLevel?.let { delay(it) }

        // === Phase 7: Start all animations ===
        log?.info("// Begin animating.")

        queuedShowHideAnimations?.forEach { it.play() }
        queuedWeightAnimations?.forEach { it.play() }

        delayLevel?.let { delay(it) }
    }
}

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
inline fun HTMLElement.animate(keyframes: Array<dynamic>, options: dynamic): Animation =
    asDynamic().animate(keyframes, options) as Animation

@Suppress("NOTHING_TO_INLINE")
inline fun HTMLElement.getAnimations(): Array<Animation> = asDynamic().getAnimations as Array<Animation>
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

private fun forEach(receiver: Json, action: (key: String, value: dynamic) -> Unit) {
    for (key in js("Object.keys(receiver)")) {
        action(key, receiver[key])
    }
}

private class PullToRefreshState {
    var startY = 0.0
    var isPulling = false
    var isRefreshing = false
    var indicator: HTMLElement? = null
    var circle: HTMLElement? = null
    var arrow: HTMLElement? = null
    var spinner: HTMLElement? = null

    fun resetIndicator() {
        val ind = indicator ?: return
        ind.style.transition = "height 0.2s ease, opacity 0.2s ease"
        ind.style.height = "0px"
        ind.style.opacity = "0"
        circle?.style?.transform = "rotate(0deg)"
        arrow?.style?.display = ""
        spinner?.style?.display = "none"
        isPulling = false
        isRefreshing = false
    }

    fun updatePosition(scrollElement: HTMLElement) {
        val ind = indicator ?: return
        val rect = scrollElement.getBoundingClientRect()
        ind.style.top = "${rect.top}px"
        ind.style.left = "${rect.left}px"
        ind.style.width = "${rect.width}px"
        // Inherit theme colors from the scroll element
        val computed = window.getComputedStyle(scrollElement)
        ind.style.color = computed.color
        val bg = computed.getPropertyValue("--nearest-background-color").trim()
        if (bg.isNotEmpty()) {
            circle?.style?.background = bg
        }
    }
}

@PublishedApi
internal actual fun Element.nativeSetupPullToRefresh(refreshAction: Action) {
    val state = PullToRefreshState()
    val PULL_THRESHOLD = 60.0
    val MAX_PULL = 120.0

    native.onElement { scrollElement ->
        scrollElement as HTMLElement

        // Append indicator to document.body with fixed positioning.
        // We must NOT insert raw DOM nodes into any RView-managed container
        // because it breaks FutureElement child index tracking (e.g. Recycler2).
        val indicator = document.createElement("div") as HTMLDivElement
        indicator.className = "ptr-indicator"
        indicator.style.position = "fixed"
        indicator.style.transition = "height 0.2s ease, opacity 0.2s ease"
        indicator.style.opacity = "0"

        val circle = document.createElement("div") as HTMLDivElement
        circle.className = "ptr-icon-circle"

        val arrow = document.createElement("div") as HTMLDivElement
        arrow.className = "ptr-arrow"
        arrow.innerHTML =
            """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></svg>"""

        val spinner = document.createElement("div") as HTMLDivElement
        spinner.className = "ptr-spinner"
        spinner.style.display = "none"

        circle.appendChild(arrow)
        circle.appendChild(spinner)
        indicator.appendChild(circle)
        document.body!!.appendChild(indicator)

        state.indicator = indicator
        state.circle = circle
        state.arrow = arrow
        state.spinner = spinner

        scrollElement.addEventListener("touchstart", { event ->
            if (state.isRefreshing) return@addEventListener
            val touch = event.asDynamic().touches[0]
            state.startY = (touch.clientY as Number).toDouble()
        }, js("{ passive: true }"))

        scrollElement.addEventListener("touchmove", { event ->
            if (state.isRefreshing) return@addEventListener
            if (scrollElement.scrollTop > 0) {
                if (state.isPulling) state.resetIndicator()
                return@addEventListener
            }

            val touch = event.asDynamic().touches[0]
            val currentY = (touch.clientY as Number).toDouble()
            val deltaY = currentY - state.startY

            if (deltaY > 0) {
                state.isPulling = true
                event.preventDefault()

                // Apply resistance: sqrt curve for natural feel
                val pullDistance = min(sqrt(deltaY * MAX_PULL), MAX_PULL)

                // Position indicator at top of scroll element
                state.updatePosition(scrollElement)
                indicator.style.transition = "none"
                indicator.style.height = "${pullDistance}px"
                indicator.style.opacity = "1"

                // Rotate circle based on progress toward threshold
                val rotation = min(pullDistance / PULL_THRESHOLD, 1.0) * 180.0
                circle.style.transform = "rotate(${rotation}deg)"
            } else if (state.isPulling) {
                state.resetIndicator()
            }
        }, js("{ passive: false }"))

        val touchEndHandler: (dynamic) -> Unit = { _ ->
            if (state.isPulling && !state.isRefreshing) {
                val indicatorHeight = indicator.style.height.removeSuffix("px").toDoubleOrNull() ?: 0.0
                if (indicatorHeight >= PULL_THRESHOLD) {
                    // Trigger refresh
                    state.isRefreshing = true
                    arrow.style.display = "none"
                    spinner.style.display = ""
                    indicator.style.transition = "height 0.2s ease"
                    indicator.style.height = "${PULL_THRESHOLD}px"
                    refreshAction.startAction(this@nativeSetupPullToRefresh)
                    // For actions that complete synchronously, the reactive scope
                    // may not re-fire (state stays in "success" type). Schedule
                    // a fallback reset to catch this case.
                    window.setTimeout({
                        if (state.isRefreshing) {
                            state.resetIndicator()
                        }
                    }, 500)
                } else {
                    state.resetIndicator()
                }
            }
        }
        scrollElement.addEventListener("touchend", touchEndHandler)
        scrollElement.addEventListener("touchcancel", touchEndHandler)
    }

    // Reactive scope to observe action completion — reset when not loading
    reactive {
        val loading = refreshAction.state().handle(
            success = { false },
            exception = { false },
            notReady = { true }
        )
        if (!loading && state.isRefreshing) {
            state.resetIndicator()
        }
    }
}
