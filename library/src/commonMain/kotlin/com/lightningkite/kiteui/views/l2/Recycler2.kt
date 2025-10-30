package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class Recycler2(
    viewWriter: ViewWriter,
    val vertical: Boolean = true,
    var log: Log? = null//ConsoleRoot.tag("Recycler2"),
) : ViewModifiable {
    override val coroutineContext: CoroutineContext
        get() = outerFrame.coroutineContext
    val outerFrame: Frame
    internal lateinit var scroll: ScrollingBehaviors
        private set
    internal val cells: ProgrammaticLayout
    internal val scrollSentinel: Frame
    internal lateinit var fakeScroll: ScrollingBehaviors
        private set
    internal val fakeScrollContent: ProgrammaticLayout
    internal val fakeScrollIndicator: Frame
    override val rView: RView
        get() = outerFrame

    var gap: Dimension?
        get() = cells.gap
        set(value) {
            cells.gap = value
        }
    var paddingByEdge: Edges?
        get() = cells.paddingByEdge
        set(value) {
            cells.paddingByEdge = value
        }
    var padding: Dimension?
        get() = cells.padding
        set(value) {
            cells.padding = value
        }
    var exists: Boolean
        get() = outerFrame.shown
        set(value) {
            outerFrame.shown = value
        }

    var recycling: Boolean = true

    private val _centerIndex = Signal(0)
    private val _displayedRangeFirst = Signal(0)
    val firstIndex: Reactive<Int> = _displayedRangeFirst.withWrite {
        if (it != _displayedRangeFirst.value)
            scrollToIndex(it, Align.Start)
    }
    private val _displayedRangeLast = Signal(0)
    val lastIndex: Reactive<Int> = _displayedRangeLast.withWrite {
        if (it != _displayedRangeLast.value)
            scrollToIndex(it, Align.End)
    }
    val centerIndex: MutableReactive<Int> = _centerIndex.withWrite {
        if (it != _centerIndex.value)
            scrollToIndex(it, Align.Center)
    }

    var snapToElements: Align? = null
        set(value) {
            field = value
            scroll.snapToElements = if (vertical) null to value else value to null
        }
    var scrollSnapStop: Boolean = false
        set(value) {
            field = value
            scroll.scrollSnapStop = value
        }

    init {
        with(viewWriter) {
            frame {
                padding = 0.px
                outerFrame = this
                beforeNextElementSetup {
                    padding = 0.px
                    themeTakeNonCascadingFromParent = true
                }.scrolling(vertical = vertical, horizontal = !vertical) {
                    scroll = this
                    showScrollBars = false
                }.apply(ThemeDerivation { if(this@frame.themeAndBack.drawBackground) it.withBack else it.withoutBack }).programmatic {
                    padding = null
                    themeTakeNonCascadingFromParent = true
//                    viewDebugTarget = this
                    cells = this
                    unpadded.frame {
                        scrollSentinel = this
                    }
                }
                if (vertical) atEnd.sizeConstraints(width = 1.rem, maxWidth = 1.rem)
                else atBottom.sizeConstraints(height = 1.rem, maxHeight = 1.rem)
                scrolling(vertical = vertical, horizontal = !vertical) {
                    fakeScroll = this
                    ignoreInteraction = Platform.current != Platform.Web
                }.programmatic {
                    fakeScrollContent = this
                    ignoreInteraction = Platform.current != Platform.Web
                    ThemeDerivation {
                        it.copy(
                            id = "scrollindicator",
                            background = it.foreground.applyAlpha(0.5f)
                        ).withBack
                    }.onNext.unpadded.frame {
                        ignoreInteraction = Platform.current != Platform.Web
                        fakeScrollIndicator = this
                        opacity = 0.0
                    }
                }
            }
        }
    }

    var overdraw = AppState.windowInfo.value.height.viewUnits / 3.0

    private var anchor: RecyclerViewAnchor? = RecyclerViewAnchor.SpecificElement(0, Align.Start)

    var placer: RecyclerViewPlacer = RecyclerViewPlacerVerticalGrid(1)
        set(value) {
            field = value
            log?.log("placer set calls invalidateLayout()")
            cells.invalidateLayout()
        }
    var data: RecyclerViewData<*, *>? = RecyclerViewData.Empty
        set(value) {
            field = value
            log?.log("data set calls invalidateLayout()")
            cells.invalidateLayout()
        }
    var rendererSet: RecyclerViewRendererSet<*, *>? = RecyclerViewRendererSet.Empty
        set(value) {
            field = value
            (cells.children.lastIndex downTo 1).forEach { cells.removeChild(it) }
            activeCells = ArrayList()
            reuseableCells = ArrayList()
            log?.log("rendererSet set calls invalidateLayout()")
            cells.invalidateLayout()
        }

    private var activeCells = ArrayList<MyCell<*>>()
    private var reuseableCells = ArrayList<MyCell<*>>()

    fun scrollToIndex(toIndex: Int, align: Align, animate: Boolean = true) {
        activeCells.find { it.index == toIndex }?.let {
            // Nice!  Just scroll away!
            scroll.scrollTo(
                left = if (vertical) 0.0 else when (align) {
                    Align.Start -> it.left - lastRecordedPaddingLeft
                    Align.End -> it.right - previousViewport.width
                    else -> it.centerX - previousViewport.width / 2
                },
                top = if (!vertical) 0.0 else when (align) {
                    Align.Start -> it.top - lastRecordedPaddingTop
                    Align.End -> it.bottom - previousViewport.height
                    else -> it.centerY - previousViewport.height / 2
                },
                animated = animate
            )
        } ?: run {
            if (animate && activeCells.isNotEmpty()) {
                val destinationAhead = toIndex > activeCells.maxOf { it.index }
                if (destinationAhead) {
                    anchor = RecyclerViewAnchor.SpecificElement(toIndex, Align.End)
                } else {
                    anchor = RecyclerViewAnchor.SpecificElement(toIndex, Align.Start)
                }
                cells.invalidateLayout()
                afterTimeout(16) {
                    activeCells.find { it.index == toIndex }?.let {
                        // Nice!  Just scroll away!
                        scroll.scrollTo(
                            left = if (vertical) 0.0 else when (align) {
                                Align.Start -> it.left - lastRecordedPaddingLeft
                                Align.End -> it.right - previousViewport.width
                                else -> it.centerX - previousViewport.width / 2
                            },
                            top = if (!vertical) 0.0 else when (align) {
                                Align.Start -> it.top - lastRecordedPaddingTop
                                Align.End -> it.bottom - previousViewport.height
                                else -> it.centerY - previousViewport.height / 2
                            },
                            animated = animate
                        )
                    } ?: run {
                        anchor = RecyclerViewAnchor.SpecificElement(toIndex, align)
                        cells.invalidateLayout()
                    }
                }
            } else {
                anchor = RecyclerViewAnchor.SpecificElement(toIndex, align)
                cells.invalidateLayout()
            }
        }
    }

    private inner class MyCell<T> : RecyclerViewPlaceable {
        val indexProp = Signal(-1)
        val data = RawReactive<T>()
        override lateinit var type: RecyclerViewRenderer<*>
        lateinit var view: RView
        private var constraint: Size = Size.Zero
        private var inProgress: ProgrammingLayoutInProgress? = null
        private var _size: Size? = null
        fun setup(
            type: RecyclerViewRenderer<T>,
            constrain: Size,
            data: ReactiveState<T>,
            index: Int,
            inProgress: ProgrammingLayoutInProgress
        ) {
            this.type = type
            this.data.state = data
            this.indexProp.value = index
            log?.log("CELL CREATED: from $data at $index")
            val writer = object: ViewWriter() {
                override val context: RContext
                    get() = cells.context

                override fun willAddChild(view: RView) {
                    return cells.willAddChild(view)
                }

                override fun addChild(view: RView) {
                    // Accessibility: we're going to ensure this is inserted in the correct order.
                    this@MyCell.view = view
                    if(recycling) {
                        cells.addChild(view)
                    } else {
                        val index = if(activeCells.isEmpty()) -1
                        else if (index > activeCells.last().index) -1
                        else if (index < activeCells.first().index) 0
                        else activeCells.binarySearchBy(index) { it.index }
                        if (index == -1) cells.addChild(view)
                        else cells.addChild(index, view)
                    }
                }

                override val coroutineContext: CoroutineContext
                    get() = cells.coroutineContext

            }
            type.render(writer, this.data, indexProp)
            constraint = constrain
            _size = null
            this.inProgress = inProgress
        }

        fun onPullForPlacing(constrain: Size, data: ReactiveState<T>, index: Int, inProgress: ProgrammingLayoutInProgress) {
//            view.withoutAnimation {
            view.shown = true
            view.opacity = 1.0
//            }
//            if (data != this.data.value) log?.log("CELL RECYCLED: Change from ${this.data.value} to $data at $index, ${this.indexProp.value}")
            this.data.state = data
            this.indexProp.value = index
            constraint = constrain
            _size = null
            this.inProgress = inProgress
        }

        fun animatedDismiss() {
            view.opacity = 0.0
            val reuse = reuseableCells
            activeCells.remove(this)
            afterTimeout(view.theme.transitionDuration.inWholeMilliseconds) {
                view.shown = false
                if(recycling) {
                    reuse.add(this@MyCell)
                } else {
                    cells.removeChild(view)
                }
            }
        }

        fun instantDismiss() {
            view.shown = false
            activeCells.remove(this)
            if(recycling) {
                reuseableCells.add(this@MyCell)
            } else {
                cells.removeChild(view)
            }
        }

        override val index: Int get() = indexProp.value
        override val item: Any? get() = data.state.getOrNull()
        override val size: Size
            get() {
                return _size ?: run {
                    statsMeasures++
                    val n = inProgress!!.measure(view, constraint)
                    _size = n
                    n
                }
            }
        override var left: Double = 0.0
        override var top: Double = 0.0
        override var right: Double = 0.0
        override var bottom: Double = 0.0

        var leftOld: Double = 0.0
        var topOld: Double = 0.0
        var rightOld: Double = 0.0
        var bottomOld: Double = 0.0

        override fun place(left: Double, top: Double, right: Double, bottom: Double) {
            this.left = left
            this.top = top
            this.right = right
            this.bottom = bottom
        }

        fun offset(x: Double, y: Double) {
            left += x
            right += x
            top += y
            bottom += y
        }
    }

    private val reallyBig = 50_000.0
    private var inLayout: Boolean = false

    init {
    }

    // statistics
    private var statsLayouts: Int = 0
    private var statsMeasures: Int = 0
    private var statsCellMoves: Int = 0
    private val statsCellCount: Int get() = cells.children.size

    private var hideIndicatorJob: Job? = null
    private var indicatorShown: Boolean = false
    private fun setFakeScrollByRatio(start: Double, end: Double) {
        log?.log("setFakeScrollByRatio $start - $end")
        if (Platform.current == Platform.Web) {
            val vp = fakeScroll.viewport.state.getOrNull() ?: return
            val vps = if (vertical) vp.height else vp.width
            fakeScrollSize = vps / (end - start)
            fakeScrollOffset = start * (fakeScrollSize)
            log?.log("fakeScrollSize: $fakeScrollSize")
            log?.log("vps: $vps")
            log?.log("end: $end")
            log?.log("start: $start")
            log?.log("fakeScrollOffset: $fakeScrollOffset")
            log?.log("setFakeScrollByRatio calls invalidateLayout() on fake")
            fakeScrollContent.invalidateLayout()
        } else {
            val vp = fakeScroll.viewport.state.getOrNull() ?: return
            val vps = if (vertical) vp.height else vp.width
            fakeScrollSize = vps - 1.0
            fakeScrollIndicatorStart = start
            fakeScrollIndicatorEnd = end

            if (end - start < 0.99) {
                if (!indicatorShown) {
                    fakeScrollIndicator.opacity = 1.0
                    indicatorShown = true
                }
                hideIndicatorJob?.cancel()
                hideIndicatorJob = fakeScrollIndicator.launch {
                    delay(1.seconds)
                    fakeScrollIndicator.opacity = 0.0
                    indicatorShown = false
                }
            } else if (indicatorShown) {
                fakeScrollIndicator.opacity = 0.0
            }

            log?.log("setFakeScrollByRatio calls invalidateLayout() on fake")
            fakeScrollContent.invalidateLayout()
        }
    }

    private var fakeScrollIndicatorStart = 0.0
    private var fakeScrollIndicatorEnd = 0.01
    private var fakeScrollInControl: Boolean = false
    private var suppressScrollEvent: Boolean = false
    private var suppressFakeScrollEvent: Boolean = false
    private var fakeScrollSize: Double = 0.0
    private var fakeScrollOffset: Double = 0.0
    private var lastRecordedPaddingTop: Double = 0.0
    private var lastRecordedPaddingLeft: Double = 0.0
    private var lastRecordedPaddingRight: Double = 0.0
    private var lastRecordedPaddingBottom: Double = 0.0

    private val fakeScrollLayoutDelegate = object : ProgrammaticLayoutDelegate {
        override fun measure(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size): Size {
            return if (vertical) Size(within.width, fakeScrollSize) else Size(fakeScrollSize, within.height)
        }

        override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            // nothing to do.
            if (Platform.current == Platform.Web) {
                log?.log("")
                suppressFakeScrollEvent = true
                val s = min(within.width, within.height)
                if (vertical) {
                    inProgress.place(
                        fakeScrollIndicator,
                        left = 0.0,
                        top = fakeScrollSize - 1,
                        right = 1.0,
                        bottom = fakeScrollSize,
                    )
                } else {
                    inProgress.place(
                        fakeScrollIndicator,
                        left = fakeScrollSize - 1,
                        top = 0.0,
                        right = fakeScrollSize,
                        bottom = 1.0,
                    )
                }
                fakeScroll.scrollTo(
                    if (vertical) 0.0 else fakeScrollOffset,
                    if (vertical) fakeScrollOffset else 0.0,
                    false
                )
            } else {
                if (vertical) {
                    val size = ((fakeScrollIndicatorEnd - fakeScrollIndicatorStart) * within.height)
                        .coerceAtLeast(within.width * 2)
                        .div(2)
                    val center = (fakeScrollIndicatorEnd + fakeScrollIndicatorStart) / 2 * within.height
                    inProgress.place(
                        fakeScrollIndicator,
                        left = within.width * 0.6,
                        top = center - size,
                        right = within.width * 0.8,
                        bottom = center + size,
                    )
                } else {
                    val size = ((fakeScrollIndicatorEnd - fakeScrollIndicatorStart) * within.width)
                        .coerceAtLeast(within.height * 2)
                        .div(2)
                    val center = (fakeScrollIndicatorEnd + fakeScrollIndicatorStart) / 2 * within.width
                    inProgress.place(
                        fakeScrollIndicator,
                        top = within.height * 0.6,
                        left = center - size,
                        bottom = within.height * 0.8,
                        right = center + size,
                    )
                }
            }
        }
    }
    private val isMoving = Signal(false)

    init {
        fakeScrollContent.delegate = fakeScrollLayoutDelegate
        if (Platform.current == Platform.Web) {
            var lastSize: Size = Size.Zero
            cells.reactive {
                val fsv = fakeScroll.viewport()
                if (lastSize != fsv.size) {
                    lastSize = fsv.size
                    return@reactive
                }
                if (suppressFakeScrollEvent) {
                    suppressFakeScrollEvent = false
                    return@reactive
                }
                val data = data ?: return@reactive
                if (data.range.last - data.range.first <= 0) return@reactive
                if (fakeScrollSize == 0.0) return@reactive
                if (vertical) {
                    val topRatio = fsv.top / fakeScrollSize
                    val bottomRatio = fsv.bottom / fakeScrollSize
                    val centerRatio = (topRatio + bottomRatio) / 2
                    val controlRatio = (topRatio + (bottomRatio - topRatio) * centerRatio)
                    val controlIndex = controlRatio * (data.range.last - data.range.first + 1)
                    if (snapToElements == null)
                        anchor = RecyclerViewAnchor.FuzzyIndex(controlIndex, centerRatio)
                    else
                        anchor = RecyclerViewAnchor.SpecificElement(controlIndex.roundToInt(), snapToElements!!)
                    this@Recycler2.log?.log("anchor = ${anchor} (web fake scroll)")
                } else {
                    val topRatio = fsv.left / fakeScrollSize
                    val bottomRatio = fsv.right / fakeScrollSize
                    val centerRatio = (topRatio + bottomRatio) / 2
                    val controlRatio = (topRatio + (bottomRatio - topRatio) * centerRatio)
                    val controlIndex = controlRatio * (data.range.last - data.range.first + 1)
                    if (snapToElements == null)
                        anchor = RecyclerViewAnchor.FuzzyIndex(controlIndex, centerRatio)
                    else
                        anchor = RecyclerViewAnchor.SpecificElement(controlIndex.roundToInt(), snapToElements!!)
                    this@Recycler2.log?.log("anchor = ${anchor} (web fake scroll)")
                }
                fakeScrollInControl = true
                log?.log("fakeScrollInControl = true")
                log?.log("fakeScroll.viewport calls invalidateLayout()")
                cells.invalidateLayout()
            }
        }
        var timeoutRemover = {}
        cells.onRemove(scroll.viewport.addListener {

            isMoving.value = true
            timeoutRemover()
            timeoutRemover = afterTimeout(100) {

                isMoving.value = false
                cells.invalidateLayout()
            }
            if (suppressScrollEvent) {
                suppressScrollEvent = false
            } else {
                fakeScrollInControl = false
                log?.log("fakeScrollInControl = false")
            }
            if (!inLayout) {
                log?.log("scroll.viewport calls invalidateLayout()")
                cells.invalidateLayout()
            }
        })
    }

    private var previousViewport: Rect = Rect.Zero
    private val programmaticLayoutDelegate = object : ProgrammaticLayoutDelegate {
        var queuedScrollJump: Pair<Double, Double>? = null
        var viewport: Rect = Rect.Zero
        var lastMeasure: Size? = null
        var sentinelSize: Double = reallyBig
        var stahp = false
        var needToLayoutFirst = false
        override fun measure(
            layout: ProgrammaticLayout,
            inProgress: ProgrammingLayoutInProgress,
            within: Size
        ): Size {
            lastRecordedPaddingTop = inProgress.paddingTop
            lastRecordedPaddingLeft = inProgress.paddingLeft
            lastRecordedPaddingRight = inProgress.paddingRight
            lastRecordedPaddingBottom = inProgress.paddingBottom
            if (stahp) return lastMeasure!!
            val default = within
            if (within == Size.Zero) {
                log?.log("measure stop: if (within == Size.Zero) {")
                return default
            }
            if(!needToLayoutFirst) {
                viewport = scroll.viewport.state.getOrNull() ?: run {
                    log?.log("measure stop: viewport = scroll.viewport.state.getOrNull() ?: run {")
                    return default
                }
            }
            log?.log("measure: Loaded viewport, found ${viewport}")
            if (viewport.width == 0.0 || viewport.height == 0.0) {
                log?.log("measure stop: if (viewport.width == 0.0 || viewport.height == 0.0) {")
                return default
            }
            log?.log("LAYOUT STARTING with size $within")

            @Suppress("UNCHECKED_CAST")
            val data = data as? RecyclerViewData<Any?, Any?> ?: run {
                log?.log("measure stop: val data = data as? RecyclerViewData<Any, Any> ?: run {")
                return default
            }

            @Suppress("UNCHECKED_CAST")
            val rendererSet = rendererSet as? RecyclerViewRendererSet<Any?, Any?> ?: run {
                log?.log("measure stop: val rendererSet = rendererSet as? RecyclerViewRendererSet<Any, Any> ?: run {")
                return default
            }

            @Suppress("UNCHECKED_CAST")
            val activeCells = activeCells as? MutableList<MyCell<Any?>> ?: run {
                log?.log("measure stop: val activeCells = activeCells as? MutableList<MyCell<Any>> ?: run {")
                return default
            }

            @Suppress("UNCHECKED_CAST")
            val reuseableCells = reuseableCells as? MutableList<MyCell<Any?>> ?: run {
                log?.log("measure stop: val reuseableCells = reuseableCells as? MutableList<MyCell<Any>> ?: run {")
                return default
            }
            log?.log("LAYOUT STARTED IN VIEWPORT $viewport, isMoving: ${isMoving.value}")

            // Time to run the placer.
            //  Track the used cells so that we can handle them properly later
            val usedCells = HashSet<MyCell<*>>()

            activeCells.toSet().intersect(reuseableCells.toSet()).forEach {
                log?.log("WARNING!!! Active and reusable cell $it")
            }

            fun runPlacer() {
                statsLayouts++
                usedCells.clear()

                val overdraw = viewport.copy(
                    left = viewport.left - overdraw,
                    top = viewport.top - overdraw,
                    right = viewport.right + overdraw,
                    bottom = viewport.bottom + overdraw,
                )
//                // Just nuke the offscreen cells immediately.
                val previousCells = activeCells.toList()
                val instantDismissCount = activeCells.toList().count {
                    if (!rectOverlaps(
                            it.left,
                            it.top,
                            it.right,
                            it.bottom,
                            overdraw.left,
                            overdraw.top,
                            overdraw.right,
                            overdraw.bottom,
                        )
                    ) {
                        it.view.shown = false
                        activeCells.remove(it)
                        if(recycling) {
                            reuseableCells.add(it)
                        } else {
                            cells.removeChild(it.view)
                        }
                        true
                    } else false
                }
                log?.log("OFFSCREEN CELLS DISMISSED: ${instantDismissCount}")

                if (data.range.isEmpty()) return

                log?.log("RUN PLACER IN $viewport, anchor is $anchor")
                placer.place(
                    dataRange = data.range,
                    anchor = anchor,
                    existingCells = previousCells,
                    getNewCell = { index, size ->
                        if (index !in data.range) throw IllegalStateException("You can't pull a cell not in data range.")
                        val item = data[index]
                        val renderer = rendererSet.renderer(item)
                        val id = rendererSet.id(item)
                        //Pulling a cell should prefer (in order) same item ID, off-screen, create new
                        (activeCells.find {
                            it.data.state.handle(
                                success = {
                                    rendererSet.id(it) == id
                                },
                                exception = { false },
                                notReady = { false }
                            )
                        }?.also {
                            // Same item ID: Data change should be animated here
                            it.onPullForPlacing(size, ReactiveState(item), index, inProgress)
                        } ?: reuseableCells.takeIf { recycling }?.popOrNull {  it.type == renderer }?.also {
                            // If placing just offscreen, place without animation.
                            it.view.withoutAnimation { it.onPullForPlacing(size, ReactiveState(item), index, inProgress) }
                            activeCells += it
                        } ?: MyCell<Any?>().also {
                            // If creating a new cell, make sure we don't animate.
                            cells.withoutAnimation {
                                it.setup(rendererSet.renderer(item), size, ReactiveState(item), index, inProgress)
                            }
                            activeCells += it
                        }).also { usedCells += it }
                    },
                    previousViewport = previousViewport,
                    viewport = viewport,
                    overdraw = overdraw,
                    paddingTop = inProgress.paddingTop,
                    paddingLeft = inProgress.paddingLeft,
                    paddingRight = inProgress.paddingRight,
                    paddingBottom = inProgress.paddingBottom,
                    gap = inProgress.gap,
                )
                previousViewport = viewport
                anchor = null
            }
            runPlacer()

            // Check for attachment to top/bottom
            var firstCell: MyCell<*>? = null
            var lastCell: MyCell<*>? = null
            usedCells.forEach {
                if (it.index == data.range.first) firstCell = it
                if (it.index == data.range.last) lastCell = it
            }

            log?.log("ATTACHMENT: $firstCell / $lastCell")
            // Move the sentinel to create a good end for scrolling
            sentinelSize = if (lastCell != null) {
                (
                        if (vertical)
                            lastCell.bottom + inProgress.paddingBottom
                        else
                            lastCell.right + inProgress.paddingRight
                        )
                    .also {
                        if (viewport.bottom > it) {
                            suppressScrollEvent = true
                            suppressFakeScrollEvent = true
                        }
                    }
            } else reallyBig
            log?.log("SENTINEL SIZE: $sentinelSize")

            // Pull everything in a direction seamlessly, without interrupting animations.
            fun requestOffset(x: Double, y: Double) {
                needToLayoutFirst = true
                log?.log("OFFSET: $x / $y")
                val vx = x.coerceAtLeast(-viewport.left)
                val vy = y.coerceAtLeast(-viewport.top)
                log?.log("OFFSET2: $vx / $vy")
                activeCells.forEach {
                    it.offset(x, y)
                }
                previousViewport = previousViewport.copy(
                    left = previousViewport.left + vx,
                    top = previousViewport.top + vy,
                    right = previousViewport.right + vx,
                    bottom = previousViewport.bottom + vy
                )
                viewport = viewport.copy(
                    left = viewport.left + vx,
                    top = viewport.top + vy,
                    right = viewport.right + vx,
                    bottom = viewport.bottom + vy
                )
                queuedScrollJump = viewport.left to viewport.top
                sentinelSize = (sentinelSize + if (vertical) vy else vx).coerceAtMost(reallyBig)
            }

            val atScrollEdge = (if (vertical) viewport.top else viewport.left) < 1 ||
                    (if (vertical) viewport.bottom else viewport.right) > reallyBig - 1
            if (firstCell != null) {
                // Shift everyone to attach to the top, preventing scrolling away past there
                if (vertical) {
                    if (abs(inProgress.paddingTop - firstCell.top) > 1.0) {
                        log?.log("WILL OFFSET AT ${viewport}")
                        if (viewport.top < firstCell.top - inProgress.paddingTop + 0.1) {
                            log?.log("JERK REQUIRED: ${viewport.top} < ${firstCell.top} - ${inProgress.paddingTop}")
                            requestOffset(0.0, -firstCell.top + inProgress.paddingTop)
                            anchor = RecyclerViewAnchor.SpecificElement(data.range.first, Align.Start)
                            log?.log("anchor = ${anchor} (Offset follow up)")
                            //dang it, we have to rerun the layout to ensure every space is properly populated.
                            runPlacer()
                        } else if (!isMoving.value) {
                            log?.log("SHIFTING CELLS TO ATTACH TO TOP: ${firstCell.top} -> ${inProgress.paddingTop}")
                            requestOffset(0.0, -firstCell.top + inProgress.paddingTop)
                        }
                    }
                } else {
                    if (abs(inProgress.paddingLeft - firstCell.left) > 1.0) {
                        log?.log("WILL OFFSET AT ${viewport}")
                        if (viewport.left < firstCell.left - inProgress.paddingLeft + 0.1) {
                            log?.log("JERK REQUIRED: ${viewport.left} < ${firstCell.left} - ${inProgress.paddingLeft}")
                            requestOffset(-firstCell.left + inProgress.paddingLeft, 0.0)
                            anchor = RecyclerViewAnchor.SpecificElement(data.range.first, Align.Start)
                            log?.log("anchor = ${anchor} (Offset follow up)")
                            //dang it, we have to rerun the layout to ensure every space is properly populated.
                            runPlacer()
                        } else if (!isMoving.value) {
                            log?.log("SHIFTING CELLS TO ATTACH TO TOP: ${firstCell.left} -> ${inProgress.paddingLeft}")
                            requestOffset(-firstCell.left + inProgress.paddingLeft, 0.0)
                        }
                    }
                }
            } else if (usedCells.isNotEmpty() && (!isMoving.value || atScrollEdge)) {
                val sampleCell = usedCells.first()
                // Ensure there is enough space to scroll upwards
                val needsRecentering = if (vertical) sampleCell.top !in (reallyBig * 1 / 4)..(reallyBig * 3 / 4)
                else sampleCell.left !in (reallyBig * 1 / 4)..(reallyBig * 3 / 4)
                log?.log("${sampleCell.left} (at index ${sampleCell.index}) !in (${reallyBig * 1 / 4})..(${reallyBig * 3 / 4})")
                log?.log("needsRecentering: $needsRecentering, atFalseEdge: $atScrollEdge, isMoving: ${isMoving.value}")
                if (needsRecentering) {
                    if (vertical) requestOffset(0.0, reallyBig * 0.5 - sampleCell.top)
                    else requestOffset(reallyBig * 0.5 - sampleCell.left, 0.0)
                }
            }

            // Dismiss the cells we don't need anymore
            val unusedCells = activeCells - usedCells
            unusedCells.forEach {
                log?.log("Dismissing cell at ${it.index}")
                if (rectOverlaps(
                        it.left,
                        it.top,
                        it.right,
                        it.bottom,
                        viewport.left,
                        viewport.top,
                        viewport.right,
                        viewport.bottom,
                    )
                ) {
                    it.animatedDismiss()
                } else {
                    it.instantDismiss()
                }
            }

            val v = if (vertical) within.copy(height = sentinelSize) else within.copy(width = sentinelSize)
            lastMeasure = v
            log?.log("Measure complete, result: $v")
            return v
        }

        override fun layout(layout: ProgrammaticLayout, inProgress: ProgrammingLayoutInProgress, within: Size) {
            this@Recycler2.log?.log("LAYOUT PROPER STARTED: $within")
            if (stahp) return
            if (!needToLayoutFirst) measure(layout, inProgress, within)
            if (vertical)
                inProgress.place(scrollSentinel, 0.0, sentinelSize, within.width, sentinelSize + 1)
            else
                inProgress.place(scrollSentinel, sentinelSize, 0.0, sentinelSize + 1, within.height)
            if (within == Size.Zero) return
            val viewport = scroll.viewport.state.getOrNull() ?: return
            if (viewport.width == 0.0 || viewport.height == 0.0) return
            try {
                inLayout = true

                fun commitPosition(it: MyCell<*>) {
                    if (it.left.roundToInt() != it.leftOld.roundToInt() ||
                        it.top.roundToInt() != it.topOld.roundToInt() ||
                        it.right.roundToInt() != it.rightOld.roundToInt() ||
                        it.bottom.roundToInt() != it.bottomOld.roundToInt()
                    ) {
                        log?.log("CHANGING POSITION FOR ${it.index} from (${it.leftOld}, ${it.topOld}) to (${it.left}, ${it.top})")
                        statsCellMoves++
                        inProgress.place(it.view, it.left, it.top, it.right, it.bottom)
                        it.leftOld = it.left
                        it.topOld = it.top
                        it.rightOld = it.right
                        it.bottomOld = it.bottom
                    }
                }

                activeCells.forEach { commitPosition(it) }
            } finally {
                inLayout = false
            }

            // We need to update the fake scroller
            data?.let { data ->
                if (fakeScrollInControl) return@let
                if (activeCells.isEmpty()) return@let

                fun MyCell<*>.visibleRatio(): Double {
                    // Return the ratio of area inside the viewport.
                    val h = (min(right, viewport.right) - max(left, viewport.left)) / (right - left)
                    val v = (min(bottom, viewport.bottom) - max(top, viewport.top)) / (bottom - top)
                    return (h * v).takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0
                }

                val totalWeight = activeCells.sumOf { it.visibleRatio() }
                if (totalWeight == 0.0) return@let
                val averageIndex = activeCells.sumOf { it.visibleRatio() * (it.index + 0.5) } / totalWeight
                val averagePosition =
                    if (vertical) activeCells.sumOf { it.visibleRatio() * (it.top + it.bottom) / 2 } / totalWeight
                    else activeCells.sumOf { it.visibleRatio() * (it.left + it.right) / 2 }
                val estimatedElementPx = (if (vertical) viewport.height else viewport.width) / totalWeight
                val totalElements = (data.range.last - data.range.first + 1)
                val estimatedTotalPx = estimatedElementPx * totalElements

                log?.log("totalWeight: $totalWeight")
                log?.log("averageIndex: $averageIndex")
                log?.log("averagePosition: $averagePosition")
                log?.log("estimatedElementPx: $estimatedElementPx")
                log?.log("totalElements: $totalElements")
                log?.log("estimatedTotalPx: $estimatedTotalPx")
                log?.log("min: ${(averageIndex - totalWeight / 2) / totalElements}")
                log?.log("max: ${(averageIndex + totalWeight / 2) / totalElements}")

                setFakeScrollByRatio(
                    (averageIndex - totalWeight / 2) / totalElements,
                    (averageIndex + totalWeight / 2) / totalElements,
                )
            }
            queuedScrollJump?.let {
                log?.log("EXECUTING SCROLL JUMP ${scroll.viewport.state.getOrNull()} = ${it}")
                suppressScrollEvent = true
                scroll.scrollToKeepAnimations(it.first, it.second)
                queuedScrollJump = null
//                stahp = true
            } ?: run {
                // Don't update the center index if we've just executed a scroll jump; it will be wrong as cell positions
                // have not yet updated
                _centerIndex.value = activeCells.minByOrNull {
                    val dx = it.centerX - viewport.centerX
                    val dy = it.centerY - viewport.centerY
                    dx * dx + dy * dy
                }?.index ?: 0
            }
            _displayedRangeFirst.value = (activeCells.minOfOrNull { it.index } ?: 0)
            _displayedRangeLast.value = (activeCells.maxOfOrNull { it.index } ?: 0)
            needToLayoutFirst = false
            log?.log("LAYOUT COMPLETE")
//            if(didJump) stahp = true
            log?.info("statsLayouts: $statsLayouts, statsMeasures: $statsMeasures, statsCellMoves: $statsCellMoves, statsCellCount: $statsCellCount")
        }

    }

    init {
        cells.delegate = programmaticLayoutDelegate
    }

    // STARTUP PROCEDURE
    // Await a non-zero scroll view size
    // Starting anchor should be defined at this point; we should have both an index and location set out.  Default to zero/top
    // Run the layout manager's placement routine with the given starting anchor - see below

    // ON SCROLL, RESIZE, NEW DATA, or NEW PLACER
    // Run the layout manager's placement routine with an updated viewport
    //   Pulling a cell should prefer (in order) same item ID, off-screen, create new
    //     Same item ID: Data change should be animated here
    //     Pulling from offscreen
    //       If placing just offscreen, place without animation.
    //       Otherwise, fade in
    //       Data change should be NOT be animated here
    //     Creating a new cell does the same thing as pulling from offscreen
    //   Raw element placement must be deferred to after logic can be run for gluing the first or last cell properly.
    //   Hide unused cells
    //     Fade it out
    //     Post-animation they should be reusable for pulling
    //   If we're showing...
    //     ...the first cell, then we offset the elements to attach the first one to the top.
    //     ...the last cell, then we move the sentinel to match the end of the last cell.
    //     ...neither of the above AND were previously attached to the top, we offset the elements to center them in the huge scroll and move the sentinel very very far down.

    // ON JUMP
    // If the target cell is on screen, perform a simple scrollTo.  Otherwise...
    // Run the layout manager's placement routine with a hard anchor
    //   If scrolling upwards, anchor is the target cell at the top
    //   If scrolling downwards, anchor is the target cell at the bottom
    // Scroll to the newly-created target cell using a simple scrollTo.


    @Deprecated("Please, don't use this. This is BAD.  It won't identify the elements properly.")
    fun <T> children(items: Reactive<List<T>>, render: ViewWriter.(value: Reactive<T>) -> ViewModifiable): Unit {
        var currentData: List<T> = listOf()
        rendererSet = object : RecyclerViewRendererSet<T, Int> {
            override fun id(item: T): Int = currentData.indexOf(item)
            val r = object : RecyclerViewRenderer<T> {
                override fun render(viewWriter: ViewWriter, data: Reactive<T>, index: Reactive<Int>): ViewModifiable {
                    return viewWriter.render(data)
                }
            }

            override fun renderer(item: T): RecyclerViewRenderer<T> = r
        }
        reactive {
            currentData = items()
            data = object : RecyclerViewData<T, Int> {
                override val range: IntRange = currentData.indices
                override fun get(index: Int): T {
                    if (index !in currentData.indices) throw IllegalStateException("Index $index out of range for ${currentData.indices}")
                    return currentData[index]
                }
            }
        }
    }

    @Deprecated("Set your placer instead. ")
    var columns: Int = 1
        set(value) {
            field = value

            placer = if (vertical)
                RecyclerViewPlacerVerticalGrid(columns)
            else
                RecyclerViewPlacerHorizontalGrid(columns)
        }

    @Deprecated("Renamed to 'firstIndex'") val firstVisibleIndex: Reactive<Int> get() = firstIndex
    @Deprecated("Renamed to 'lastIndex'") val lastVisibleIndex: Reactive<Int> get() = lastIndex
    @Deprecated("Renamed to 'centerIndex'") val index: MutableReactive<Int> get() = centerIndex
    @Deprecated("Just use directly") val new get() = this
}

internal fun <T> MutableList<T>.popOrNull(): T? = if (!isEmpty()) removeAt(lastIndex) else null
internal fun <T> MutableList<T>.popOrNull(condition: (T)->Boolean): T? = indexOfFirst(condition).let { if(it != -1) removeAt(it) else null }
internal fun rectOverlaps(
    l1: Double,
    t1: Double,
    r1: Double,
    b1: Double,
    l2: Double,
    t2: Double,
    r2: Double,
    b2: Double,
): Boolean = l1 < r2 && r1 > l2 && t1 < b2 && b1 > t2

fun estimateJumpAnchor(
    activeCells: List<RecyclerViewPlaceable>,
    vertical: Boolean,
    viewport: Rect
): RecyclerViewAnchor.FuzzyIndex? {
    if (activeCells.isEmpty()) return null
    val totalWeight = activeCells.size
    val averageIndex = activeCells.sumOf { it.index } / totalWeight
    val averagePosition =
        if (vertical) activeCells.sumOf { (it.top + it.bottom) / 2 } / totalWeight
        else activeCells.sumOf { (it.left + it.right) / 2 }
    val min = activeCells.minOf { if (vertical) it.top else it.left }
    val max = activeCells.maxOf { if (vertical) it.bottom else it.right }
    val estimatedElementPx = (max - min) / totalWeight
    val diff = (if (vertical) viewport.centerY else viewport.centerX) - averagePosition
    return RecyclerViewAnchor.FuzzyIndex(
        index = averageIndex + diff / estimatedElementPx,
        ratioOfFocus = 0.5
    )
}