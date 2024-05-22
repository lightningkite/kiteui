package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.cinterop.*
import platform.CoreGraphics.*
import platform.UIKit.*
import kotlin.math.absoluteValue

@OptIn(ExperimentalForeignApi::class)
@ViewDsl
actual inline fun ViewWriter.recyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit = element(
    NRecyclerView(true, newViews())
) {
    calculationContext.onRemove {
        extensionStrongRef = null
    }
    backgroundColor = UIColor.clearColor
    handleTheme(
        this, viewDraws = false,
        foreground = {
            spacing = spacingOverride?.value ?: it.spacing
        },
    ) {
        extensionViewWriter = newViews()
        setup(RecyclerView(this))
    }
}

@OptIn(ExperimentalForeignApi::class)
@ViewDsl
actual inline fun ViewWriter.horizontalRecyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit = element(
    NRecyclerView(false, newViews())
) {
    calculationContext.onRemove {
        extensionStrongRef = null
    }
    backgroundColor = UIColor.clearColor
    handleTheme(
        this, viewDraws = false,
        foreground = {
            spacing = spacingOverride?.value ?: it.spacing
        },
    ) {
        extensionViewWriter = newViews()
        setup(RecyclerView(this))
    }
}

actual fun <T> RecyclerView.children(
    items: Readable<List<T>>,
    render: ViewWriter.(value: Readable<T>) -> Unit
): Unit {
    native.renderer = ItemRenderer<T>(
        create = { parent, value ->
            val prop = Property(value)
            render(native.newViews, prop)
            native.newViews.rootCreated!!.also {
                it.extensionProp = prop
            }
        },
        update = { parent, element, value ->
            @Suppress("UNCHECKED_CAST")
            (element.extensionProp as Property<T>).value = value
        }
    )
    calculationContext.reactiveScope {
        native.data = items.await().asIndexed()
    }
}

actual var RecyclerView.columns: Int
    get() = native.columns
    set(value) { native.columns = value }

actual fun RecyclerView.scrollToIndex(
    index: Int,
    align: Align?,
    animate: Boolean
) {
    native.jump(index, align ?: Align.Center, animate)
}

actual val RecyclerView.firstVisibleIndex: Readable<Int>
    get() = native.firstVisible

actual val RecyclerView.lastVisibleIndex: Readable<Int>
    get() = native.lastVisible

interface Indexed<out T> {
    val min: Int
    val max: Int
    operator fun get(index: Int): T

    companion object {
        val EMPTY = object : Indexed<Nothing> {
            override val max: Int
                get() = -1
            override val min: Int
                get() = 0

            override fun get(index: Int): Nothing {
                throw IndexOutOfBoundsException()
            }

            override fun copy(): Indexed<Nothing> = this
        }
    }

    fun copy(): Indexed<T>
}

fun <T> List<T>.asIndexed(): Indexed<T> = object : Indexed<T> {
    override val min: Int
        get() = 0
    override val max: Int
        get() = this@asIndexed.size - 1

    override fun get(index: Int): T {
        return this@asIndexed.get(index)
    }

    override fun copy(): Indexed<T> = this@asIndexed.toList().asIndexed()
}

class ItemRenderer<T>(
    val create: (NRecyclerView, T) -> UIView,
    val update: (NRecyclerView, UIView, T) -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    fun createAny(r: NRecyclerView, t: Any?) = create(r, t as T)

    @Suppress("UNCHECKED_CAST")
    fun updateAny(r: NRecyclerView, element: UIView, t: Any?) = update(r, element, t as T)
}


fun <T> Indexed<T>.columned(count: Int): Indexed<Indexed<T>> = object : Indexed<Indexed<T>> {
    val original = this@columned
    override val min: Int
        get() = original.min / count
    override val max: Int
        get() = original.max / count

    override fun get(index: Int): Indexed<T> {
        val basis = index * count
        return object : Indexed<T> {
            override val max: Int
                get() = (count - 1).coerceAtMost(original.max - basis)
            override val min: Int
                get() = (0).coerceAtLeast(original.min - basis)

            override fun get(index: Int): T = original.get(index + basis)
            override fun copy(): Indexed<T> = this
        }
    }

    override fun copy(): Indexed<Indexed<T>> = original.copy().columned(count)
}

fun <T> ItemRenderer<T>.columned(count: Int, vertical: Boolean) = ItemRenderer<Indexed<T>>(
    create = { parent, data ->
        LinearLayout().apply {
            this.spacing = parent.spacing
            horizontal = vertical
            repeat(count) {
                if (it in data.min..data.max) {
                    addSubview(this@columned.create(parent, data[it]).apply { extensionWeight = 1f })
                } else {
                    addSubview(NSpace().apply { extensionWeight = 1f })
                }
            }
        }
    },
    update = { parent, element, data ->
        repeat(count) { index ->
            val child = element.subviews[index] as UIView
            if (index in data.min..data.max) {
                val sub = data[index]
                if (child is NSpace) {
                    element.insertSubview(this@columned.create(parent, sub).apply { extensionWeight = 1f }, index.toLong())
                    child.removeFromSuperview()
                } else {
                    this@columned.update(parent, child, sub)
                }
                child.alpha = 1.0
            } else {
                child.alpha = 0.0
            }
        }
    }
)

private val reservedScrollingSpace: CGFloat = 50000.0
@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NRecyclerView(val vertical: Boolean = true, val newViews: ViewWriter): UIScrollView(CGRectMake(0.0, 0.0, 0.0, 0.0)), UIScrollViewDelegateProtocol,
    UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    val firstVisible = Property(0)
    val centerVisible = Property(0)
    val lastVisible = Property(0)

    val log = ConsoleRoot.tag("NRecyclerView")

    val allSubviews: ArrayList<Subview> = ArrayList()

    var viewportSize: CGFloat = 0.0
        set(value) {
            field = value
            relayout()
        }
    private var _viewportOffsetField: CGFloat = 0.0
    var viewportOffset: CGFloat
        get() = _viewportOffsetField
        set(value) {
            _viewportOffsetField = value
            suppressTrueScroll = true
            setContentOffset(CGPointMake(if(vertical) 0.0 else value, if(vertical) value else 0.0))
        }
    var suppressTrueScroll = true
    var suppressFakeScroll = true
    val beyondEdgeRendering = 10.0

    private var lastForceCenteringDismiss: Int = -1

    var forceCentering = false
    var elementsMatchSize: Boolean = false
        set(value) {
            field = value
            allSubviews.forEach { it.measure() }
            relayout()
        }
    var suppressFakeScrollEnd = false
    var suppressTrueScrollEnd = false
    var printing = false

    var capViewAtBottom: Boolean = false
        set(value) {
            field = value
            if (value) {
                val s = allSubviews.last().let { it.startPosition + it.size + spacingRaw }
                setContentSize(if(vertical) CGSizeMake(0.0, s) else CGSizeMake(s,  0.0))
            } else {
                val s = reservedScrollingSpace
                setContentSize(if(vertical) CGSizeMake(0.0, s) else CGSizeMake(s,  0.0))
            }
        }

    private fun CValue<CGRect>.withSize(value: CGFloat): CValue<CGRect> = useContents { CGRectMake(origin.x, origin.y, if(vertical) size.width else value, if(vertical) value else size.height) }
    private fun CValue<CGRect>.withStart(value: CGFloat): CValue<CGRect> = useContents { CGRectMake(if(vertical) origin.x else value, if(vertical) value else origin.y, size.width, size.height) }
    private fun CValue<CGRect>.size() = useContents { if(vertical) size.height else size.width }
    private fun CValue<CGRect>.start() = useContents { if(vertical) origin.y else origin.x }

    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride
    var spacing: Dimension = 0.px
        set(value) {
            if (value != field) {
                field = value
                relayout()
            }
        }
    val spacingRaw: CGFloat get() = spacing.value

    var existingAfterTimeout: (()->Unit)? = null
    override fun subviewDidChangeSizing(view: UIView?) {
        allSubviews.find { it.element === view }?.let {
            it.needsLayout = true
            val useAnim = animationsEnabled
            if(existingAfterTimeout == null)
            existingAfterTimeout = afterTimeout(16) {
                existingAfterTimeout?.invoke()
                existingAfterTimeout = null
                val before = animationsEnabled
                try {
                    animationsEnabled = useAnim
                    animateIfAllowed {
                        if (allSubviews.any { it.needsLayout }) {
                            relayout()
                        }
                    }
                } finally {
                    animationsEnabled = before
                }
            }
        }
    }

    inner class Subview(
        val element: UIView,
        var index: Int,
    ) {

        var needsLayout: Boolean = true
        var startPosition: CGFloat = 0.0
            set(value) {
                field = value
                element.setFrame(element.frame.withStart(value))
            }
        var size: CGFloat = -1.0
            get() {
                return if (forceCentering) viewportSize
                else {
                    if (needsLayout) measure()
                    field
                }
            }

        fun measure() {
            if(!needsLayout) return
            needsLayout = false
            val p = extensionPadding ?: 0.0
            if(elementsMatchSize) {
                if(vertical) {
                    size = if(forceCentering) viewportSize else this@NRecyclerView.bounds.useContents { size.height }
                    element.setFrame(CGRectMake(p, startPosition, this@NRecyclerView.bounds.useContents { size.width - p * 2 }, size))
                    element.layoutSubviewsAndLayers()
                } else {
                    size = if(forceCentering) viewportSize else this@NRecyclerView.bounds.useContents { size.width }
                    element.setFrame(CGRectMake(startPosition, p, size, this@NRecyclerView.bounds.useContents { size.height - p * 2 }))
                    element.layoutSubviewsAndLayers()
                }
            } else {
                if(vertical) {
                    size = if(forceCentering) viewportSize else element.sizeThatFits(CGSizeMake(this@NRecyclerView.bounds.useContents { size.width }, 10000.0)).useContents { height }
                    element.setFrame(CGRectMake(p, startPosition, this@NRecyclerView.bounds.useContents { size.width - p * 2 }, size))
                    element.layoutSubviewsAndLayers()
                } else {
                    size = if(forceCentering) viewportSize else element.sizeThatFits(CGSizeMake(10000.0, this@NRecyclerView.bounds.useContents { size.height })).useContents { width }
                    element.setFrame(CGRectMake(startPosition, p, size, this@NRecyclerView.bounds.useContents { size.height - p * 2 }))
                    element.layoutSubviewsAndLayers()
                }
            }
        }

        var visible: Boolean
            get() = element.exists
            set(value) {
                element.exists = value
            }

        fun placeBefore(top: CGFloat): CGFloat {
            startPosition = top - size - spacingRaw
            return top - size - spacingRaw
        }

        fun placeAfter(bottom: CGFloat): CGFloat {
            startPosition = bottom + spacingRaw
            return bottom + size + spacingRaw
        }
    }

    var columns: Int = 1
        set(value) {
            if (value != field) {
                field = value
                if (columns == 1) {
                    rendererDirect = renderer
                    dataDirect = data
                } else {
                    rendererDirect = renderer.columned(columns, vertical)
                    dataDirect = data.columned(columns)
                }
            }
        }
    var data: Indexed<*> = Indexed.EMPTY
        set(value) {
            field = value
            if (columns == 1) {
                dataDirect = value
            } else {
                dataDirect = value.columned(columns)
            }
        }
    var renderer: ItemRenderer<*> = ItemRenderer<Int>({ _, _ -> UIView() }, { _, _, _ -> })
        set(value) {
            field = value
            data = Indexed.EMPTY
            if (columns == 1) {
                rendererDirect = value
            } else {
                rendererDirect = value.columned(columns, vertical)
            }
        }
    var rendererDirect: ItemRenderer<*> =
        ItemRenderer<Int>({ _, _ -> UIView() }, { _, _, _ -> })
        set(value) {
            dataDirect = Indexed.EMPTY
            field = value
            if (ready) {
                allSubviews.forEach {
                    it.element.shutdown()
                    it.element.removeFromSuperview()
                }
                allSubviews.clear()
                populate()
            }
        }
    var dataDirect: Indexed<*> = Indexed.EMPTY
        set(value) {
            field = value
            if (ready) {
                lock("dataSet") {
                    if (allSubviews.isNotEmpty()) {
                        // Shift into range
                        val outOfBoundsBottom = allSubviews.last().index > value.max
                        val outOfBoundsTop = allSubviews.first().index < value.min
                        val shift = if (outOfBoundsBottom && outOfBoundsTop) {
                            value.min - allSubviews.first().index
                        } else if (outOfBoundsTop) {
                            value.min - allSubviews.first().index
                        } else if (outOfBoundsBottom) {
                            (value.max - allSubviews.last().index).coerceAtLeast(value.min - allSubviews.first().index)
                        } else 0
                        allSubviews.forEach {
//                            log.log("PERFORMING SHIFT C $shift")
                            it.index += shift
                            if (it.index in value.min..value.max) {
                                it.visible = true
                                it.element.withoutAnimation {
                                    rendererDirect.updateAny(this, it.element, value[it.index])
                                }
                            } else {
                                it.visible = false
                            }
                        }
                        if (shift > 0) {
                            // Force to top
                            viewportOffset = allSubviews.first().startPosition
                        } else if (shift < 0) {
                            // Force to bottom
                            viewportOffset = allSubviews.last().let { it.startPosition + it.size } - viewportSize
                        }
                        populate()
                    } else {
                        populate()
                    }
                    emergencyEdges()
                    updateVisibleIndexes()
                    updateFakeScroll()
                }
            }
        }

    private fun onScrollStop() {
        if (allSubviews.isNotEmpty()) {
            if (allSubviews.first().index <= dataDirect.min) {
                // shift and attach to top
                if ((allSubviews.first().startPosition - spacingRaw).absoluteValue > 2) {
                    offsetWholeSystem(-allSubviews.first().startPosition + spacingRaw)
                }
            } else {
                if (viewportOffset > reservedScrollingSpace * 7 / 8) {
                    offsetWholeSystem(3 * reservedScrollingSpace / -8)
                } else if (viewportOffset < reservedScrollingSpace / 8) {
                    offsetWholeSystem(3 * reservedScrollingSpace / 8)
                }
            }
            capViewAtBottom = allSubviews.last().index >= dataDirect.max
        }
        Unit
    }

    private var lockState: String? = null
    private inline fun lock(key: String, action: () -> Unit) {
        if (lockState != null) {
//            log.log("Cannot get lock for $key, already held by $lockState!!!")
            return
        }
        lockState = key
        val r = try {
            action()
        } catch (e: Exception) {
            e.printStackTrace2()
        } finally {
            lockState = null
        }
        return r
    }

    private fun emergencyEdges() {
        if (allSubviews.isNotEmpty()) {
            if (allSubviews.first()
                    .let { it.index <= dataDirect.min && it.startPosition >= viewportOffset + spacingRaw }
            ) {
                // shift and attach to top
                if ((allSubviews.first().startPosition - spacingRaw).absoluteValue > 2) {
                    offsetWholeSystem(-allSubviews.first().startPosition + spacingRaw)
                }
            } else {
                if (viewportOffset > reservedScrollingSpace) {
                    offsetWholeSystem(reservedScrollingSpace / -2)
                } else if (viewportOffset < 0) {
                    offsetWholeSystem(reservedScrollingSpace / 2)
                }
            }
            capViewAtBottom = allSubviews.last().index >= dataDirect.max
        }
    }

    private fun scrollTo(pos: Double, animate: Boolean) {
//        log.log("scrollTo: $pos")
        if (vertical) {
            setContentOffset(CGPointMake(
                0.0,
                pos,
            ), animated = animate)
        } else {
            setContentOffset(CGPointMake(
                pos,
                0.0,
            ), animated = animate)
        }
    }

    var startCreatingViewsAt: Pair<Int, Align> = 0 to Align.Start
    fun jump(index: Int, align: Align, animate: Boolean) {
        if (allSubviews.isEmpty() || viewportSize < 1) {
            startCreatingViewsAt = index to align
        }
        if (index !in dataDirect.min..dataDirect.max) return
        lock("jump $index $align") {
            val rowIndex = index / columns
            if (animate) {
                allSubviews.find { it.index == rowIndex }?.let {
                    when (align) {
                        Align.Start -> scrollTo(it.startPosition.toDouble(), animate)
                        Align.End -> scrollTo((it.startPosition + it.size - viewportSize).toDouble(), animate)
                        else -> scrollTo((it.startPosition + it.size / 2 - viewportSize / 2).toDouble(), animate)
                    }
                    return
                }
                fun move() {
                    val existingTop = allSubviews.first().index
                    val existingBottom = allSubviews.last().index
                    val shift = if (rowIndex < existingTop)
                        (rowIndex - existingTop)
                    else if (rowIndex > existingBottom)
                        (rowIndex - existingBottom)
                    else 0
                    allSubviews.forEach {
//                        log.log("PERFORMING SHIFT B $shift")
                        it.index += shift
                        if (it.index in dataDirect.min..dataDirect.max) {
                            it.visible = true
                            it.element.withoutAnimation {
                                rendererDirect.updateAny(this, it.element, dataDirect[it.index])
                            }
                        } else {
                            it.visible = false
                        }
                    }
                }
                move()
                populate()
                emergencyEdges()
                updateVisibleIndexes()
                move()
                allSubviews.find { it.index == rowIndex }?.let {
                    when (align) {
                        Align.Start -> scrollTo(it.startPosition.toDouble(), true)
                        Align.End -> scrollTo((it.startPosition + it.size - viewportSize).toDouble(), true)
                        else -> scrollTo((it.startPosition + it.size / 2 - viewportSize / 2).toDouble(), true)
                    }
                } ?: log.log("Wha?!")
            } else {
                val existingIndex = when (align) {
                    Align.Start -> allSubviews.first().index
                    Align.End -> allSubviews.last().index
                    else -> (allSubviews.first().index + allSubviews.last().index) / 2
                }
                var target: Subview? = null
                val shift = (rowIndex - existingIndex)
                    .coerceAtMost(dataDirect.max - allSubviews.last().index)
                    .coerceAtLeast(dataDirect.min - allSubviews.first().index)
                allSubviews.forEach {
                    log.log("PERFORMING SHIFT A $shift")
                    it.index += shift
                    if (it.index == rowIndex) target = it
                    if (it.index in dataDirect.min..dataDirect.max) {
                        it.visible = true
                        it.element.withoutAnimation {
                            rendererDirect.updateAny(this, it.element, dataDirect[it.index])
                        }
                    } else {
                        it.visible = false
                    }
                }
                viewportOffset = when (align) {
                    Align.Start -> allSubviews.first().startPosition - spacingRaw
                    Align.End -> allSubviews.last().let { it.startPosition + it.size } - viewportSize - spacingRaw
                    else -> target?.let { it.startPosition + it.size / 2 - viewportSize / 2 }
                        ?: (allSubviews.first().startPosition - spacingRaw)
                }
                log.log("Hopped to ${viewportOffset}, where the target starts at ${target?.startPosition} size ${target?.size} and the viewport size is $viewportSize")
                populate()
                emergencyEdges()
                updateVisibleIndexes()
            }
        }
    }

    private fun updateFakeScroll() {
        if (allSubviews.isEmpty()) return
        val startIndexPartial = (allSubviews.firstOrNull { it.startPosition + it.size > viewportOffset && it.size > 0 } ?: return)
            .let { it.index + ((viewportOffset - it.startPosition) / it.size) }
        val endIndexPartial = (allSubviews.lastOrNull { it.startPosition < viewportOffset + viewportSize && it.size > 0 } ?: return)
            .let { it.index + 1 + (viewportOffset + viewportSize - it.startPosition - it.size) / it.size }
        val numElements = dataDirect.max - dataDirect.min + 1
        val startRatio = (startIndexPartial + dataDirect.min) / (dataDirect.max - dataDirect.min).coerceAtLeast(1)
        val endRatio = (endIndexPartial + dataDirect.min) / (dataDirect.max - dataDirect.min).coerceAtLeast(1)
        val sw = bounds.useContents { size.width }
        val sh = bounds.useContents { size.height }
        fakeScrollIndicator.hidden = startIndexPartial.toInt() <= data.min && endIndexPartial >= data.max
        if(vertical) {
            fakeScrollIndicator.setFrame(CGRectMake(
                bounds.useContents { size.width - 4.0 },
                startRatio * sh + viewportOffset,
                4.0,
                (endRatio - startRatio) * sh
            ))
        } else {
            fakeScrollIndicator.setFrame(CGRectMake(
                startRatio * sw + viewportOffset,
                bounds.useContents { size.height - 4.0 },
                (endRatio - startRatio) * sw,
                4.0,
            ))
        }
//        fakeScrollInner.style.size = "${100 * numElements}%"
//        fakeScroll.scrollStart = (startIndexPartial + endIndexPartial - 1) / 2 * viewportSize
        updateVisibleIndexes()
    }

    var ready = false

    fun offsetWholeSystem(by: CGFloat) {
        for (view in allSubviews) {
            view.startPosition += by
        }
        viewportOffset += by
    }

    fun makeSubview(index: Int, atStart: Boolean): Subview {
        val element = rendererDirect.createAny(this, dataDirect[index])
        return Subview(
            element = element,
            index = index,
        ).also { if (atStart) allSubviews.add(0, it) else allSubviews.add(it); insertSubview(it.element, 0L) }
    }

    fun makeFirst(): Subview? {
        if (dataDirect.max < dataDirect.min) return null
        viewportOffset = reservedScrollingSpace / 2
        val element = makeSubview(startCreatingViewsAt.first.coerceIn(dataDirect.min, dataDirect.max), false)
        element.measure()
        element.startPosition = when(startCreatingViewsAt.second) {
            Align.Start -> viewportOffset + spacing.value
            Align.End -> viewportOffset + viewportSize - spacing.value - element.size
            else -> viewportOffset + viewportSize / 2 - element.size / 2
        }
        log.log("Rendered first element at ${element.startPosition} with size ${element.size}")
        return element
    }

    fun populate() {
        populateDown()
        populateUp()
        updateFakeScroll()
    }

    fun updateVisibleIndexes() {
        firstVisible.let {
            val v = allSubviews.firstOrNull()?.index?.times(columns) ?: -1
            if (v != it.value) it.value = v
        }
        centerVisible.let {
            val center = viewportOffset + viewportSize / 2
            allSubviews.find { center in it.startPosition..it.startPosition.plus(it.size) }?.index?.times(columns)
                ?.plus(columns / 2)?.let { v ->
                    if (v != it.value) it.value = v
                }
        }
        lastVisible.let {
            val v = allSubviews.lastOrNull()?.index?.times(columns)?.plus(columns - 1) ?: -1
            if (v != it.value) it.value = v
        }
    }

    fun populateDown() {
        var anchor = allSubviews.lastOrNull() ?: makeFirst() ?: return
        var bottom = anchor.startPosition + anchor.size
        while ((bottom < viewportSize + viewportOffset + beyondEdgeRendering)) {
            val nextIndex = anchor.index + 1
            if (nextIndex > dataDirect.max) break
            // Get the element to place
            val element: Subview = allSubviews.first().takeIf {
                (it.startPosition + it.size < viewportOffset - beyondEdgeRendering)
            }?.also {
                log.log("populateDown $nextIndex")
                it.index = nextIndex
                it.element.withoutAnimation {
                    rendererDirect.updateAny(this, it.element, dataDirect[nextIndex])
                }
                allSubviews.removeFirst()
                allSubviews.add(it)
            } ?: makeSubview(nextIndex, false)
            element.measure()
            bottom = element.placeAfter(bottom)
            anchor = element
        }
    }

    fun populateUp() {
        var anchor = allSubviews.firstOrNull() ?: makeFirst() ?: return
        var top = anchor.startPosition
        while ((top > viewportOffset - beyondEdgeRendering)) {
            val nextIndex = anchor.index - 1
            if (nextIndex < dataDirect.min) break
            // Get the element to place
            val element: Subview = allSubviews.last().takeIf {
                it.startPosition > viewportOffset + viewportSize + beyondEdgeRendering
            }?.also {
                log.log("populateUp $nextIndex")
                it.index = nextIndex
                it.element.withoutAnimation {
                    rendererDirect.updateAny(this, it.element, dataDirect[nextIndex])
                }
                allSubviews.removeLast()
                allSubviews.add(0, it)
            } ?: makeSubview(nextIndex, true)
            element.measure()
            top = element.placeBefore(top)
            anchor = element
        }
    }

    val anchorPosition: Align = Align.Start
    fun relayout() {
        if (allSubviews.isEmpty()) return
        when (anchorPosition) {
            Align.Start -> relayoutDown(0)
            Align.End -> relayoutUp(allSubviews.lastIndex)
            else -> {
                val centeredIndex = allSubviews.size / 2
                relayoutUp(centeredIndex)
                relayoutDown(centeredIndex)
            }
        }
        populate()
    }

    fun relayoutDown(startingIndex: Int) {
        var bottom = allSubviews[startingIndex].let { anchor -> anchor.startPosition + anchor.size }
        for (index in (startingIndex + 1)..allSubviews.lastIndex) {
            val element = allSubviews[index]
            bottom = element.placeAfter(bottom)
        }
    }

    fun relayoutUp(startingIndex: Int) {
        var top = allSubviews[startingIndex].let { anchor -> anchor.startPosition }
        for (index in (startingIndex - 1) downTo 0) {
            val element = allSubviews[index]
            top = element.placeBefore(top)
        }
    }

    override fun scrollViewDidScroll(scrollView: UIScrollView) {
        if (suppressTrueScroll) {
            suppressTrueScroll = false
            suppressTrueScrollEnd = true
            return
        }
        suppressTrueScrollEnd = false
        lock("onscroll") {
            _viewportOffsetField = contentOffset.useContents { if(vertical) y else x }
            populate()
            emergencyEdges()
            updateVisibleIndexes()
        }
    }

    override fun scrollViewWillBeginDragging(scrollView: UIScrollView) {
    }

    override fun scrollViewDidEndDecelerating(scrollView: UIScrollView) {
        if (suppressTrueScrollEnd) {
            suppressTrueScrollEnd = false
            return
        }
        onScrollStop()
    }

    override fun scrollViewWillEndDragging(
        scrollView: UIScrollView,
        withVelocity: CValue<CGPoint>,
        targetContentOffset: CPointer<CGPoint>?
    ) {
        if(targetContentOffset != null) {
            if (forceCentering) {
                if (vertical) {
                    val c = targetContentOffset.pointed.y
                    val closestToTarget = allSubviews.minBy { ((it.startPosition + it.size / 2) - (c + viewportSize / 2)).absoluteValue }
                    targetContentOffset.pointed.y = closestToTarget.let { it.startPosition + it.size / 2  } - viewportSize / 2
                } else {
                    val c = targetContentOffset.pointed.x
                    val closestToTarget = allSubviews.minBy { ((it.startPosition + it.size / 2) - (c + viewportSize / 2)).absoluteValue }
                    targetContentOffset.pointed.x = closestToTarget.let { it.startPosition + it.size / 2  } - viewportSize / 2
                }
            }
        }
    }

    val fakeScrollIndicator = UIView(CGRectMake(0.0,0.0,0.0,0.0)).also {
        it.layer.cornerRadius = 2.0
        it.backgroundColor = UIColor.grayColor.colorWithAlphaComponent(0.5)
        addSubview(it)
    }

    var lastOrthoSize: CGFloat = -1.0
    override fun layoutSubviews() {
        super.layoutSubviews()
        val orthoSize = bounds.useContents { if(vertical) size.width else size.height }
        allSubviews.forEach {
            if(lastOrthoSize != orthoSize) it.needsLayout = true
            it.measure()
        }
        viewportSize = bounds.useContents { if(vertical) size.height else size.width }
        updateFakeScroll()
    }

    init {
        afterTimeout(10) {
            this.delegate = this
            showsHorizontalScrollIndicator = false
            showsVerticalScrollIndicator = false
            setContentSize(
                if (vertical) CGSizeMake(0.0, reservedScrollingSpace) else CGSizeMake(
                    reservedScrollingSpace,
                    0.0
                )
            )
            setContentOffset(
                if (vertical) CGPointMake(0.0, reservedScrollingSpace / 2) else CGPointMake(
                    reservedScrollingSpace / 2,
                    0.0
                )
            )
            ready = true
            populate()
        }
    }
}