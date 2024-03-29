package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.JsAnyDelegate
import com.lightningkite.kiteui.JsAnyNativeDelegate
import org.w3c.dom.HTMLElement
import org.w3c.dom.css.CSSStyleDeclaration
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.random.Random

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NRecyclerView(override val js: HTMLDivElement): NView2<HTMLDivElement>() {
    lateinit var controller: RecyclerController2
}

@ViewDsl
actual inline fun ViewWriter.recyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit {
    themedElement("div", ::NRecyclerView, viewDraws = false) {
        js.classList.add("recyclerView")
        val newViews: ViewWriter = newViews()
        controller = RecyclerController2(
            root = js,
            newViews = newViews,
            vertical = true
        )
        setup(RecyclerView(this))
    }
}

@ViewDsl
actual inline fun ViewWriter.horizontalRecyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit {
    themedElement("div", ::NRecyclerView, viewDraws = false) {
        js.classList.add("recyclerView")
        val newViews: ViewWriter = newViews()
        controller = RecyclerController2(
            root = js,
            newViews = newViews,
            vertical = false
        )
        setup(RecyclerView(this))
    }
}

var HTMLElement.__ROCK_prop__ by JsAnyNativeDelegate<HTMLElement, Property<*>>("__ROCK_prop__")
actual var RecyclerView.columns: Int
    get() = (native.controller).columns
    set(value) {
        (native.controller).columns = value
    }

actual fun <T> RecyclerView.children(
    items: Readable<List<T>>,
    render: ViewWriter.(value: Readable<T>) -> Unit
): Unit {
    native.controller.let {
        it.renderer = ItemRenderer<T>(
            create = { value ->
                val prop = Property(value)
                render(it.newViews, prop)
                it.newViews.rootCreated!!.also {
                    it.js.__ROCK_prop__ = prop
                }.js
            },
            update = { element, value ->
                (element.__ROCK_prop__ as Property<T>).value = value
            }
        )
        reactiveScope {
            it.data = items.await().asIndexed()
        }
    }
}

actual fun RecyclerView.scrollToIndex(
    index: Int,
    align: Align?,
    animate: Boolean
) {
    (native.controller).jump(index, align ?: Align.Center, animate)
}

actual val RecyclerView.firstVisibleIndex: Readable<Int>
    get() = (native.controller).firstVisible

actual val RecyclerView.lastVisibleIndex: Readable<Int>
    get() = (native.controller).lastVisible

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

fun <T> ItemRenderer<T>.columned(count: Int) = ItemRenderer<Indexed<T>>(
    create = { data ->
        (document.createElement("div") as HTMLDivElement).apply {
            classList.add("recyclerViewGridSub")
            repeat(count) {
                if (it in data.min..data.max) {
                    appendChild(this@columned.create(data[it]))
                } else {
                    appendChild((document.createElement("div") as HTMLDivElement).apply {
                        classList.add("placeholder")
                    })
                }
            }
        }
    },
    update = { element, data ->
        repeat(count) {
            val child = (element.children[it] as HTMLElement)
            if (it in data.min..data.max) {
                val sub = data[it]
                if (child.classList.contains("placeholder")) {
                    element.replaceChild(this.create(sub), child)
                } else {
                    this.update(child, sub)
                }
                child.style.visibility = "visible"
            } else {
                child.style.visibility = "hidden"
            }
        }
    }
)

class RecyclerController2(
    val root: HTMLDivElement,
    val newViews: ViewWriter,
    val vertical: Boolean = true,
) {
    val me = Random.nextInt()
    val firstVisible = Property(0)
    val centerVisible = Property(0)
    val lastVisible = Property(0)

    val contentHolder = (document.createElement("div") as HTMLDivElement).apply {
        classList.add("contentScroll-${if (vertical) "V" else "H"}")

        tabIndex = -1
        this.addEventListener("keydown", { ev ->
            ev as KeyboardEvent
            if (forceCentering) {
                when (ev.key) {
                    KeyCodes.left -> {
                        scrollBy(
                            ScrollToOptions(
                                -(clientWidth.toDouble() - this@RecyclerController2.spacing),
                                behavior = ScrollBehavior.SMOOTH
                            )
                        )
                        ev.preventDefault()
                    }

                    KeyCodes.right -> {
                        scrollBy(
                            ScrollToOptions(
                                (clientWidth.toDouble() - this@RecyclerController2.spacing),
                                behavior = ScrollBehavior.SMOOTH
                            )
                        )
                        ev.preventDefault()
                    }
                }
            }
        })
    }
    val fakeScroll = (document.createElement("div") as HTMLDivElement).apply {
        classList.add("barScroll")
        style.position = "absolute"
        if (vertical) {
            style.width = "1rem"
            style.right = "0px"
            style.top = "0px"
            style.bottom = "0px"
            style.overflowY = "scroll"
        } else {
            style.height = "1rem"
            style.bottom = "0px"
            style.left = "0px"
            style.right = "0px"
            style.overflowX = "scroll"
        }
    }
    val fakeScrollInner = (document.createElement("div") as HTMLDivElement).apply {
        style.size = "${reservedScrollingSpace}px"
        if (vertical) {
            style.width = "1px"
        } else {
            style.height = "1px"
        }
        style.maxWidth = "unset"
        style.maxHeight = "unset"
    }.also { fakeScroll.appendChild(it) }
    val capView = (document.createElement("div") as HTMLDivElement).apply {
        style.size = "1px"
        style.start = "${reservedScrollingSpace}px"
        style.backgroundColor = "rbga(1, 1, 1, 0.01)"
    }
    var capViewAtBottom: Boolean = false
        set(value) {
            field = value
            if (value) {
                capView.style.start = allSubviews.last().let { it.startPosition + it.size + spacing }.let { "${it}px" }
            } else {
                capView.style.start = reservedScrollingSpace.let { "${it}px" }
            }
        }

    init {
        root.appendChild(contentHolder)
        root.appendChild(fakeScroll)
        contentHolder.appendChild(capView)
        ResizeObserver { entries, obs ->
            val newSize = root.clientSize
            if (viewportSize != newSize) {
                viewportSize = newSize
                enqueuedJump?.let { jump(it, Align.Center, false) }
                forceCenteringHandler()
            }
        }.observe(root)
    }

    private var HTMLElement.scrollStart
        get() = if (vertical) scrollTop else scrollLeft
        set(value) {
            if (vertical) scrollTop = value else scrollLeft = value
        }
    private val HTMLElement.offsetStart
        get() = if (vertical) offsetTop else offsetLeft
    private val HTMLElement.clientSize
        get() = if (vertical) clientHeight else clientWidth
    private val HTMLElement.scrollSize
        get() = if (vertical) scrollHeight else scrollWidth
    private val DOMRect.size
        get() = if (vertical) height else width
    private val DOMRect.start
        get() = if (vertical) top else left
    private val DOMRect.end
        get() = if (vertical) bottom else right
    private val CSSStyleDeclaration.marginStart
        get() = if (vertical) marginTop else marginLeft
    private val CSSStyleDeclaration.marginEnd
        get() = if (vertical) marginBottom else marginRight
    private var CSSStyleDeclaration.size
        get() = if (vertical) height else width
        set(value) {
            if (vertical) height = value else width = value
        }
    private var CSSStyleDeclaration.start
        get() = if (vertical) top else left
        set(value) {
            if (vertical) top = value else left = value
        }

    var columns: Int = 1
        set(value) {
            if (value != field) {
                field = value
                if (columns == 1) {
                    rendererDirect = renderer
                    dataDirect = data
                } else {
                    rendererDirect = renderer.columned(columns)
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
    var renderer: ItemRenderer<*> = ItemRenderer<Int>({ document.createElement("div") as HTMLDivElement }, { _, _ -> })
        set(value) {
            field = value
            data = Indexed.EMPTY
            if (columns == 1) {
                rendererDirect = value
            } else {
                rendererDirect = value.columned(columns)
            }
        }
    var rendererDirect: ItemRenderer<*> =
        ItemRenderer<Int>({ document.createElement("div") as HTMLDivElement }, { _, _ -> })
        set(value) {
            dataDirect = Indexed.EMPTY
            field = value
            if (ready) {
                allSubviews.forEach {
                    it.element.shutdown()
                    contentHolder.removeChild(it.element)
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
                            it.index += shift
                            if (it.index in value.min..value.max) {
                                it.visible = true
                                it.element.withoutAnimation {
                                    rendererDirect.updateAny(it.element, value[it.index])
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
                    } else {
                        populate()
                    }
                    emergencyEdges()
                    updateVisibleIndexes()
                    updateFakeScroll()
                }
                enqueuedJump?.let {
                    jump(it, Align.Center, false)
                }
            }
        }
    var spacing: Int = window.getComputedStyle(root).columnGap.removeSuffix("px").toDouble().toInt()
        set(value) {
            if (value != field) {
                field = value
                relayout()
            }
        }

    val allSubviews: ArrayList<Subview> = ArrayList()

    var viewportSize: Int = 0
        set(value) {
            field = value
            relayout()
        }
    private var _viewportOffsetField: Int = 0
    var viewportOffset: Int
        get() = _viewportOffsetField
        set(value) {
            _viewportOffsetField = value
            suppressTrueScroll = true
            contentHolder.scrollStart = value.toDouble()
        }
    var suppressTrueScroll = true
    var suppressFakeScroll = true

    private var lastForceCenteringDismiss: Int = -1
    private val forceCenteringHandler = { ->
        if (allSubviews.isNotEmpty()) {
            if (allSubviews.first().index <= dataDirect.min) {
                // shift and attach to top
                if ((allSubviews.first().startPosition - spacing).absoluteValue > 2) {
                    offsetWholeSystem(-allSubviews.first().startPosition + spacing)
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
        if (forceCentering) {
            val scrollCenter = viewportOffset + viewportSize / 2
            allSubviews.map { it.startPosition + it.size / 2 - scrollCenter }.minBy { it.absoluteValue }.let {
                if (it.absoluteValue > 10) {
                    if (vertical) {
                        contentHolder.scrollBy(
                            ScrollToOptions(
                                top = it.toDouble(),
                                behavior = ScrollBehavior.SMOOTH
                            )
                        )
                    } else {
                        contentHolder.scrollBy(
                            ScrollToOptions(
                                left = it.toDouble(),
                                behavior = ScrollBehavior.SMOOTH
                            )
                        )
                    }
                }
            }
        }
        null
    }
    var forceCentering = false
    var suppressFakeScrollEnd = false
    var suppressTrueScrollEnd = false
    var printing = false

    init {
        contentHolder.onscroll = event@{ ev ->
            if (suppressTrueScroll) {
                suppressTrueScroll = false
                suppressTrueScrollEnd = true
                return@event Unit
            }
            suppressTrueScrollEnd = false
            lock("onscroll") {
                window.clearTimeout(lastForceCenteringDismiss)
                lastForceCenteringDismiss = window.setTimeout(forceCenteringHandler, 1000)

                _viewportOffsetField = contentHolder.scrollStart.toInt()
                populate()
                emergencyEdges()
                updateVisibleIndexes()
            }
            enqueuedJump = null
            Unit
        }
        fakeScroll.onscroll = event@{ ev ->
            if (suppressFakeScroll) {
                suppressFakeScroll = false
                suppressFakeScrollEnd = true
                return@event Unit
            }
            suppressFakeScrollEnd = false
            lock("fakescroll") {
                window.clearTimeout(lastForceCenteringDismiss)
                lastForceCenteringDismiss = window.setTimeout(forceCenteringHandler, 1000)
                if (allSubviews.isEmpty()) return@event Unit

                val centerElementPartialIndex = (fakeScroll.scrollStart / viewportSize * 2 + 1) / 2

                // Try to find the element in question first and scroll it
                allSubviews.find { it.index == centerElementPartialIndex.toInt() }?.let { existingElement ->
                    viewportOffset =
                        (existingElement.startPosition + existingElement.size * centerElementPartialIndex.mod(1.0) - viewportSize / 2).toInt()
                    populate()
                    emergencyEdges()
                    updateVisibleIndexes()
                } ?: run {
                    // Darn, let's shift indices and then lock it in place
                    var diff = centerElementPartialIndex.toInt() - allSubviews[allSubviews.size / 2].index
                    diff = diff.coerceAtLeast(data.min - allSubviews.first().index)
                    diff = diff.coerceAtMost(data.max - allSubviews.last().index)
                    for (subview in allSubviews) {
                        val newIndex = subview.index + diff
                        subview.index = newIndex
                        subview.element.withoutAnimation {
                            rendererDirect.updateAny(subview.element, dataDirect[newIndex])
                        }
                    }
                    for (subview in allSubviews) {
                        subview.measure()
                    }
                    allSubviews.find { it.index == centerElementPartialIndex.toInt() }?.let { existingElement ->
                        viewportOffset =
                            (existingElement.startPosition + existingElement.size * centerElementPartialIndex.mod(1.0) - viewportSize / 2).toInt()
                    }
                    populate()
                    emergencyEdges()
                    updateVisibleIndexes()
                }
            }
            Unit
        }
        contentHolder.addEventListener("scrollend", event@{
            if (suppressTrueScrollEnd) {
                suppressTrueScrollEnd = false
                return@event Unit
            }
            window.clearTimeout(lastForceCenteringDismiss)
            forceCenteringHandler()
            null
        })
        fakeScroll.addEventListener("scrollend", event@{
            if (suppressFakeScrollEnd) {
                suppressFakeScrollEnd = false
                return@event Unit
            }
            window.clearTimeout(lastForceCenteringDismiss)
            forceCenteringHandler()
            null
        })
        window.setTimeout({
            lock("ready") {
                ready = true
                spacing = window.getComputedStyle(root).columnGap.removeSuffix("px").toDouble().toInt()
                populate()
                forceCenteringHandler()
            }
            enqueuedJump?.let {
                jump(it, Align.Center, false)
            }
            null
        }, 1)
    }

    private var lockState: String? = null
    private inline fun lock(key: String, action: () -> Unit) {
        if (lockState != null) {
            println("Cannot get lock for $key, already held by $lockState!!!")
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
                    .let { it.index <= dataDirect.min && it.startPosition >= viewportOffset + spacing }
            ) {
                // shift and attach to top
                if ((allSubviews.first().startPosition - spacing).absoluteValue > 2) {
                    offsetWholeSystem(-allSubviews.first().startPosition + spacing)
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
        if (vertical) {
            contentHolder.scrollTo(
                ScrollToOptions(
                    top = pos,
                    behavior = if (animate) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
                )
            )
        } else {
            contentHolder.scrollTo(
                ScrollToOptions(
                    left = pos,
                    behavior = if (animate) ScrollBehavior.SMOOTH else ScrollBehavior.INSTANT
                )
            )
        }
    }

    var enqueuedJump: Int? = null
        set(value) {
            field = value
        }

    fun jump(index: Int, align: Align, animate: Boolean) {
        enqueuedJump = index
        if (allSubviews.isEmpty()) return
        if (index !in dataDirect.min..dataDirect.max) return
        if (viewportSize < 1) return
        enqueuedJump = null
        lock("jump $index $align") {
            if (animate) {
                allSubviews.find { it.index == index }?.let {
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
                    val shift = if (index < existingTop)
                        (index - existingTop)
                    else if (index > existingBottom)
                        (index - existingBottom)
                    else 0
                    allSubviews.forEach {
                        it.index += shift
                        if (it.index in dataDirect.min..dataDirect.max) {
                            it.visible = true
                            it.element.withoutAnimation {
                                rendererDirect.updateAny(it.element, dataDirect[it.index])
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
                forceCenteringHandler()
                move()
                allSubviews.find { it.index == index }?.let {
                    when (align) {
                        Align.Start -> scrollTo(it.startPosition.toDouble(), true)
                        Align.End -> scrollTo((it.startPosition + it.size - viewportSize).toDouble(), true)
                        else -> scrollTo((it.startPosition + it.size / 2 - viewportSize / 2).toDouble(), true)
                    }
                } ?: println("Wha?!")
            } else {
                val existingIndex = when (align) {
                    Align.Start -> allSubviews.first().index
                    Align.End -> allSubviews.last().index
                    else -> (allSubviews.first().index + allSubviews.last().index) / 2
                }
                var target: Subview? = null
                val shift = (index - existingIndex)
                    .coerceAtMost(dataDirect.max - allSubviews.last().index)
                    .coerceAtLeast(dataDirect.min - allSubviews.first().index)
                allSubviews.forEach {
                    it.index += shift
                    if (it.index == index) target = it
                    if (it.index in dataDirect.min..dataDirect.max) {
                        it.visible = true
                        it.element.withoutAnimation {
                            rendererDirect.updateAny(it.element, dataDirect[it.index])
                        }
                    } else {
                        it.visible = false
                    }
                }
                viewportOffset = when (align) {
                    Align.Start -> allSubviews.first().startPosition - spacing
                    Align.End -> allSubviews.last().let { it.startPosition + it.size } - viewportSize - spacing
                    else -> target?.let { it.startPosition + it.size / 2 - viewportSize / 2 }
                        ?: (allSubviews.first().startPosition - spacing)
                }
                println("Hopped to ${viewportOffset}, where the target starts at ${target?.startPosition} size ${target?.size} and the viewport size is $viewportSize")
                populate()
                emergencyEdges()
                updateVisibleIndexes()
                forceCenteringHandler()
            }
        }
    }

    private fun updateFakeScroll() {
        if (allSubviews.isEmpty()) return
        val startIndexPartial = (allSubviews.firstOrNull { it.startPosition + it.size > viewportOffset } ?: return)
            .let { it.index + ((viewportOffset - it.startPosition) / it.size.toDouble()) }
        val endIndexPartial = (allSubviews.lastOrNull { it.startPosition < viewportOffset + viewportSize } ?: return)
            .let { it.index + 1 + (viewportOffset + viewportSize - it.startPosition - it.size) / it.size.toDouble() }
        val numElements = dataDirect.max - dataDirect.min + 1
        suppressFakeScroll = true
        fakeScroll.hidden = startIndexPartial.toInt() <= data.min && endIndexPartial >= data.max
        fakeScrollInner.style.size = "${100 * numElements}%"
        fakeScroll.scrollStart = (startIndexPartial + endIndexPartial - 1) / 2 * viewportSize
        updateVisibleIndexes()
    }

    var ready = false

    inner class Subview(
        val element: HTMLElement,
        var index: Int,
    ) {

        var startPosition: Int = 0
            set(value) {
                field = value
                element.style.start = "${value}px"
            }
        var size: Int = -1

        init {
            ResizeObserver { entries, obs ->
                val newSize = element.scrollSize
                if (size != newSize && newSize > 0) {
                    size = newSize
                    relayout()
                }
            }.observe(element)
        }

        fun measure() {
            size = element.scrollSize
                .also {
                    if (it == 0) {
                        Exception("Uhh, that isn't right.").printStackTrace2()
                    }
                }
        }

        var visible: Boolean
            get() = !element.hidden
            set(value) {
                element.hidden = !value
            }

        fun placeBefore(top: Int): Int {
            startPosition = top - size - spacing
            return top - size - spacing
        }

        fun placeAfter(bottom: Int): Int {
            startPosition = bottom + spacing
            return bottom + size + spacing
        }
    }

    fun offsetWholeSystem(by: Int) {
        for (view in allSubviews) {
            view.startPosition += by
        }
        capViewAtBottom = capViewAtBottom
        viewportOffset += by
    }

    fun makeSubview(index: Int, atStart: Boolean): Subview {
        val element = rendererDirect.createAny(dataDirect[index])
        return Subview(
            element = element,
            index = index,
        ).also { if (atStart) allSubviews.add(0, it) else allSubviews.add(it); contentHolder.appendChild(it.element) }
    }

    fun makeFirst(): Subview? {
        if (dataDirect.max < dataDirect.min) return null
        viewportOffset = reservedScrollingSpace / 2
        val element = makeSubview(dataDirect.min, false)
        element.measure()
        element.startPosition = reservedScrollingSpace / 2 + spacing
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
        if (printing) println("populateDown")
        while ((bottom < viewportSize + viewportOffset).also {
                if (printing) println("$bottom < $viewportSize + $viewportOffset")
            }) {
            val nextIndex = anchor.index + 1
            if (nextIndex > dataDirect.max) break
            // Get the element to place
            val element: Subview = allSubviews.first().takeIf {
                (it.startPosition + it.size < viewportOffset)
            }?.also {
                it.index = nextIndex
                it.element.withoutAnimation {
                    rendererDirect.updateAny(it.element, dataDirect[nextIndex])
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
        if (printing) println("populateUp")
        while ((top > viewportOffset).also {
                if (printing) println("$top > $viewportOffset")
            }) {
            val nextIndex = anchor.index - 1
            if (nextIndex < dataDirect.min) break
            // Get the element to place
            val element: Subview = allSubviews.last().takeIf {
                it.startPosition > viewportOffset + viewportSize
            }?.also {
                it.index = nextIndex
                it.element.withoutAnimation {
                    rendererDirect.updateAny(it.element, dataDirect[nextIndex])
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
}


val reservedScrollingSpace = 100_000

class ItemRenderer<T>(
    val create: (T) -> HTMLElement,
    val update: (HTMLElement, T) -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    fun createAny(t: Any?) = create(t as T)

    @Suppress("UNCHECKED_CAST")
    fun updateAny(element: HTMLElement, t: Any?) = update(element, t as T)
}
