package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.clockMillis
import com.lightningkite.kiteui.debugger
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.absoluteValue
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlin.time.measureTime


actual class RecyclerView actual constructor(context: RContext) : RView(context) {
    private var controller: RecyclerController2? = null
    private var onController = ArrayList<(RecyclerController2) -> Unit>()
    private fun onController(action: (RecyclerController2) -> Unit) {
        controller?.let(action) ?: onController.add(action)
    }

    private val newViews = NewViewWriter(this, context)

    init {
        native.tag = "div"
        native.classes.add("recyclerView")
        native.onElement {
            val controller = RecyclerController2(it as HTMLDivElement, vertical)
            this.controller = controller
            onController.forEach { it(controller) }
            onRemove { controller.ready = false }
            onController.clear()
        }
    }

    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }


    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ): Unit {
        onController { controller ->
            controller.renderer = ItemRenderer<T>(
                create = { value ->
                    val prop = Property(value)
                    render(newViews, prop)
                    val new = newViews.newView!!
                    addChild(new)
                    new.asDynamic().__ROCK__prop = prop
                    new.native.create() as HTMLElement
                },
                update = { element, value ->
                    @Suppress("UNCHECKED_CAST")
                    (children.find { it.native.element === element }?.asDynamic().__ROCK__prop as? Property<T>)?.value =
                        value
                },
                shutdown = { element ->
                    val i = children.indexOfFirst { it.native.element === element }
                    if (i != -1) removeChild(i)
                }
            )
            reactiveScope {
                controller.data = items().asIndexed()
            }
        }
    }

    actual var columns: Int = 1
        set(value) {
            field = value
            onController { it.columns = value }
        }

    actual fun scrollToIndex(
        index: Int,
        align: Align?,
        animate: Boolean
    ) {
        onController { it.jump(index, align ?: Align.Center, animate) }
    }

    private val _firstVisibleIndex = Property(0)
    actual val firstVisibleIndex: Readable<Int> = _firstVisibleIndex

    init {
        onController { it.firstVisible.addListener { _firstVisibleIndex.value = it.firstVisible.value } }
    }

    private val _lastVisibleIndex = Property(0)
    actual val lastVisibleIndex: Readable<Int> = _lastVisibleIndex

    init {
        onController { it.lastVisible.addListener { _lastVisibleIndex.value = it.lastVisible.value } }
    }

    actual var vertical: Boolean = true
        set(value) {
            field = value
            onController { controller -> controller.vertical = value }
        }

    init {
        onRemove {
            controller?.shutdown()
        }
    }
}


// TODO
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NRecyclerView = HTMLDivElement
//
//@ViewDsl
//actual inline fun ViewWriter.recyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit {
//    themedElement<HTMLDivElement>("div", viewDraws = false) {
//        classList.add("recyclerView")
//        val newViews: ViewWriter = newViews()
//        this.asDynamic().__ROCK__controller = RecyclerController2(
//            root = this,
//            newViews = newViews,
//            vertical = true
//        )
//        setup(RecyclerView(this))
//    }
//}
//
//@ViewDsl
//actual inline fun ViewWriter.horizontalRecyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit {
//    themedElement<HTMLDivElement>("div", viewDraws = false) {
//        classList.add("recyclerView")
//        val newViews: ViewWriter = newViews()
//        this.asDynamic().__ROCK__controller = RecyclerController2(
//            root = this,
//            newViews = newViews,
//            vertical = false
//        )
//        setup(RecyclerView(this))
//    }
//}
//
//actual var RecyclerView.columns: Int
//    get() = (native.asDynamic().__ROCK__controller as RecyclerController2).columns
//    set(value) {
//        (native.asDynamic().__ROCK__controller as RecyclerController2).columns = value
//    }
//
//actual fun <T> RecyclerView.children(
//    items: Readable<List<T>>,
//    render: ViewWriter.(value: Readable<T>) -> Unit
//): Unit {
//    (native.asDynamic().__ROCK__controller as RecyclerController2).let {
//        it.renderer = ItemRenderer<T>(
//            create = { value ->
//                val prop = Property(value)
//                render(it.newViews, prop)
//                it.newViews.rootCreated!!.also {
//                    it.asDynamic().__ROCK_prop__ = prop
//                }
//            },
//            update = { element, value ->
//                (element.asDynamic().__ROCK_prop__ as Property<T>).value = value
//            }
//        )
//        reactiveScope {
//            it.data = items.await().asIndexed()
//        }
//    }
//}
//
//actual fun RecyclerView.scrollToIndex(
//    index: Int,
//    align: Align?,
//    animate: Boolean
//) {
//    (native.asDynamic().__ROCK__controller as RecyclerController2).jump(index, align ?: Align.Center, animate)
//}
//
//actual val RecyclerView.firstVisibleIndex: Readable<Int>
//    get() = (native.asDynamic().__ROCK__controller as RecyclerController2).firstVisible
//
//actual val RecyclerView.lastVisibleIndex: Readable<Int>
//    get() = (native.asDynamic().__ROCK__controller as RecyclerController2).lastVisible
//

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
    },
    shutdown = {
        repeat(count) { index ->
            (it.children.get(index) as? HTMLElement)?.let(shutdown)
        }
    }
)

class RecyclerController2(
    val root: HTMLDivElement,
    vertical: Boolean = true,
) {
    var vertical: Boolean = vertical
        set(value) {
            field = value
            rendererDirect = rendererDirect
        }
    val me = Random.nextInt()
    val firstVisible = Property(0)
    val centerVisible = Property(0)
    val lastVisible = Property(0)
    val beyondEdgeRendering = 100

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
                                -(clientWidth.toDouble() + this@RecyclerController2.spacing),
                                behavior = ScrollBehavior.SMOOTH
                            )
                        )
                        ev.preventDefault()
                    }

                    KeyCodes.right -> {
                        scrollBy(
                            ScrollToOptions(
                                (clientWidth.toDouble() + this@RecyclerController2.spacing),
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
        className = "recyclerViewCap"
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
    private val org.w3c.dom.css.CSSStyleDeclaration.marginStart
        get() = if (vertical) marginTop else marginLeft
    private val org.w3c.dom.css.CSSStyleDeclaration.marginEnd
        get() = if (vertical) marginBottom else marginRight
    private var org.w3c.dom.css.CSSStyleDeclaration.size
        get() = if (vertical) height else width
        set(value) {
            if (vertical) height = value else width = value
        }
    private var org.w3c.dom.css.CSSStyleDeclaration.start
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
    var renderer: ItemRenderer<*> =
        ItemRenderer<Int>({ document.createElement("div") as HTMLDivElement }, { _, _ -> }, {})
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
        ItemRenderer<Int>({ document.createElement("div") as HTMLDivElement }, { _, _ -> }, {})
        set(value) {
            dataDirect = Indexed.EMPTY
            val old = field
            field = value
            if (ready) {
                allSubviews.forEach {
                    contentHolder.removeChild(it.element)
                    old.shutdown(it.element)
                }
                allSubviews.clear()
                populate()
            }
        }

    fun shutdown() {
        allSubviews.forEach {
            contentHolder.removeChild(it.element)
            rendererDirect.shutdown(it.element)
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
                        allSubviews.toList().forEach {
                            it.index += shift
                            if (it.index in value.min..value.max) {
                                it.element.withoutAnimation {
                                    rendererDirect.updateAny(it.element, value[it.index])
                                }
                            } else {
                                contentHolder.removeChild(it.element)
                                allSubviews.remove(it)
                            }
                        }
                        if(allSubviews.isNotEmpty()) {
                            if (shift > 0) {
                                // Force to top
                                if (allSubviews.first().startPosition < 0) {
                                    offsetWholeSystem(-allSubviews.first().startPosition)
                                } else {
                                    viewportOffset = allSubviews.first().startPosition
                                }
                            } else if (shift < 0) {
                                // Force to bottom
                                viewportOffset = allSubviews.last().let { it.startPosition + it.size } - viewportSize
                            }
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
    var spacing: Int =
        window.getComputedStyle(root).columnGap.removeSuffix("px").let { it.toDoubleOrNull() ?: 0.0 }.toInt()
        set(value) {
            if (value != field) {
                field = value
                relayout()
            }
        }
    var padding: Int = spacing
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
            if (value < 0) IllegalStateException("Offset cannot be $value").printStackTrace2()
            _viewportOffsetField = value
            suppressTrueScroll = true
            contentHolder.scrollStart = value.toDouble()
        }
    var suppressTrueScroll = true
    var suppressFakeScroll = true

    private var lastForceCenteringDismiss: Int = -1
    fun nonEmergencyEdges() {
        if (allSubviews.isNotEmpty()) {
            if (allSubviews.first()
                    .let { it.index <= dataDirect.min && it.startPosition >= viewportOffset + padding }
            ) {
                // shift and attach to top
                if ((allSubviews.first().startPosition - padding).absoluteValue > 2) {
                    offsetWholeSystem(-allSubviews.first().startPosition + padding)
                }
            } else {
                if (viewportOffset > reservedScrollingSpace * 7 / 8) {
                    offsetWholeSystem(reservedScrollingSpace / -2)
                } else if (viewportOffset < reservedScrollingSpace / 8) {
                    offsetWholeSystem(reservedScrollingSpace / 2)
                }
            }
            capViewAtBottom = allSubviews.last().index >= dataDirect.max
        }
    }

    var forceCentering = false
    var suppressFakeScrollEnd = false
    var suppressTrueScrollEnd = false

    init {
        var startupTime: Double = 0.0
        contentHolder.onscroll = event@{ ev ->
            if (clockMillis() < startupTime + 200) {
                // Safari... my old nemesis
                contentHolder.scrollStart = _viewportOffsetField.toDouble()
                return@event Unit
            }
            if (suppressTrueScroll) {
                suppressTrueScroll = false
                suppressTrueScrollEnd = true
                return@event Unit
            }
            suppressTrueScrollEnd = false
            lock("onscroll") {
                window.clearTimeout(lastForceCenteringDismiss)
                lastForceCenteringDismiss = window.setTimeout(::nonEmergencyEdges, 1000)

                _viewportOffsetField = contentHolder.scrollStart.toInt()
                populate()
                emergencyEdges()
                updateVisibleIndexes()
            }
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
                lastForceCenteringDismiss = window.setTimeout(::nonEmergencyEdges, 1000)
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
            nonEmergencyEdges()
        })
        fakeScroll.addEventListener("scrollend", event@{
            if (suppressFakeScrollEnd) {
                suppressFakeScrollEnd = false
                return@event Unit
            }
            window.clearTimeout(lastForceCenteringDismiss)
            nonEmergencyEdges()
        })
        window.setTimeout({
            lock("ready") {
                viewportSize = root.clientSize
                spacing = window.getComputedStyle(root).columnGap.removeSuffix("px")
                    .let { it.toDoubleOrNull() ?: throw IllegalArgumentException("Number fail B $it") }.toInt()
                padding = window.getComputedStyle(root).paddingTop.removeSuffix("px")
                    .let { it.toDoubleOrNull() ?: throw IllegalArgumentException("Number fail C $it") }.toInt()
                ready = true
                startupTime = clockMillis()
                populate()
                nonEmergencyEdges()
            }
        }, 1)
    }

    private var lockState: String? = null
    private inline fun lock(key: String, action: () -> Unit) {
        if (lockState != null) {
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
                    .let { it.index <= dataDirect.min && it.startPosition >= viewportOffset + padding }
            ) {
                // shift and attach to top
                if ((allSubviews.first().startPosition - padding).absoluteValue > 2) {
                    offsetWholeSystem(-allSubviews.first().startPosition + padding)
                    populate()
                }
            } else {
                if (viewportOffset > reservedScrollingSpace) {
                    offsetWholeSystem(reservedScrollingSpace / -2)
                    populate()
                } else if (viewportOffset < 0) {
                    offsetWholeSystem(reservedScrollingSpace / 2)
                    populate()
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

    var startCreatingViewsAt: Pair<Int, Align> = 0 to Align.Start
    fun jump(index: Int, align: Align, animate: Boolean, onlyIfNear: Boolean = false) {
        if (allSubviews.isEmpty() || viewportSize < 1) {
            startCreatingViewsAt = index to align
        }
        if (index !in dataDirect.min..dataDirect.max) return
        if (!ready) return
        if (allSubviews.isEmpty()) return
        lock("jump $index $align") {
            val rowIndex = index / columns
            allSubviews.find { it.index == rowIndex }?.let {
                when (align) {
                    Align.Start -> scrollTo(it.startPosition.toDouble(), animate)
                    Align.End -> scrollTo((it.startPosition + it.size - viewportSize).toDouble(), animate)
                    else -> scrollTo((it.startPosition + it.size / 2 - viewportSize / 2).toDouble(), animate)
                }
                return
            }
            if (onlyIfNear) return@lock
            if (animate) {
                fun move() {
                    val existingTop = allSubviews.first().index
                    val existingBottom = allSubviews.last().index
                    val shift = if (rowIndex < existingTop)
                        (rowIndex - existingTop)
                    else if (rowIndex > existingBottom)
                        (rowIndex - existingBottom)
                    else 0
                    allSubviews.toList().forEach {
                        it.index += shift
                        if (it.index in dataDirect.min..dataDirect.max) {
                            it.element.withoutAnimation {
                                rendererDirect.updateAny(it.element, dataDirect[it.index])
                            }
                        } else {
                            contentHolder.removeChild(it.element)
                            allSubviews.remove(it)
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
                }
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
                allSubviews.toList().forEach {
                    it.index += shift
                    if (it.index == rowIndex) target = it
                    if (it.index in dataDirect.min..dataDirect.max) {
                        it.element.withoutAnimation {
                            rendererDirect.updateAny(it.element, dataDirect[it.index])
                        }
                    } else {
                        contentHolder.removeChild(it.element)
                        allSubviews.remove(it)
                    }
                }
                viewportOffset = when (align) {
                    Align.Start -> allSubviews.first().startPosition - padding
                    Align.End -> allSubviews.last().let { it.startPosition + it.size } - viewportSize - padding
                    else -> target?.let { it.startPosition + it.size / 2 - viewportSize / 2 }
                        ?: (allSubviews.first().startPosition - padding)
                }
                populate()
                emergencyEdges()
                updateVisibleIndexes()
                nonEmergencyEdges()
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
        index: Int,
    ) {
        var index: Int = index
            set(value) {
                field = value
                element.className =
                    element.className.split(' ').filter { !it.startsWith("index-") }.plus("index-$index")
                        .joinToString(" ")
            }

        init {
            element.classList.add("index-$index")
        }

        var startPosition: Int = 0
            set(value) {
                field = value
                element.style.start = "${value}px"
            }
        var size: Int = -1
            get() {
                return if (forceCentering) viewportSize
                else field
            }

        init {
            if (!forceCentering) {
                ResizeObserver { entries, obs ->
                    val newSize = element.scrollSize
                    if (size != newSize && newSize > 0) {
                        size = newSize
                        relayout()
                    }
                }.observe(element)
            }
        }

        fun measure() {
            if (!forceCentering) {
                size = element.scrollSize
            }
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
        val byFiltered = by.coerceAtLeast(-viewportOffset)
        for (view in allSubviews) {
            view.startPosition += by
        }
        capViewAtBottom = capViewAtBottom
        viewportOffset += byFiltered
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
        val element = makeSubview(startCreatingViewsAt.first.coerceIn(dataDirect.min, dataDirect.max), false)
        element.measure()
        element.startPosition = when (startCreatingViewsAt.second) {
            Align.Start -> viewportOffset + padding
            Align.End -> viewportOffset + viewportSize - padding - element.size
            else -> viewportOffset + viewportSize / 2 - element.size / 2
        }.also {
        }
        repeat(100) {
            afterTimeout(it * 10L) {
            }
        }
        return element
    }

    private inline fun preventAnchoring(action: ()->Unit) {
        val before = contentHolder.scrollStart
        action()
        val after = contentHolder.scrollStart
        if(before != after) {
            contentHolder.scrollStart = before
        }
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
        while ((bottom < viewportSize + viewportOffset + beyondEdgeRendering).also {
            }) {
            val nextIndex = anchor.index + 1
            if (nextIndex > dataDirect.max) break
            // Get the element to place
            val element: Subview = allSubviews.first().takeIf {
                (it.startPosition + it.size < viewportOffset - beyondEdgeRendering)
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
        while ((top > viewportOffset - beyondEdgeRendering).also {
            }) {
            val nextIndex = anchor.index - 1
            if (nextIndex < dataDirect.min) break
            // Get the element to place
            val element: Subview = allSubviews.last().takeIf {
                it.startPosition > viewportOffset + viewportSize + beyondEdgeRendering
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

    init {

        ResizeObserver { entries, obs ->
            if (!ready) return@ResizeObserver
            val newSize = root.clientSize
            if (viewportSize != newSize && newSize != 0) {
                viewportSize = newSize
                nonEmergencyEdges()
            }
        }.observe(root)
    }
}


val reservedScrollingSpace = 100_000

class ItemRenderer<T>(
    val create: (T) -> HTMLElement,
    val update: (HTMLElement, T) -> Unit,
    val shutdown: (HTMLElement) -> Unit
) {
    @Suppress("UNCHECKED_CAST")
    fun createAny(t: Any?) = create(t as T)

    @Suppress("UNCHECKED_CAST")
    fun updateAny(element: HTMLElement, t: Any?) = update(element, t as T)
}


external class ResizeObserver(callback: (Array<ResizeObserverEntry>, observer: ResizeObserver) -> Unit) {
    fun disconnect()
    fun observe(target: Element, options: ResizeObserverOptions = definedExternally)
    fun unobserve(target: Element)
}

external interface ResizeObserverOptions {
    val box: String
}

external interface ResizeObserverEntry {
    val target: Element
    val contentRect: DOMRectReadOnly
    val contentBoxSize: ResizeObserverEntryBoxSize
    val borderBoxSize: ResizeObserverEntryBoxSize
}

external interface ResizeObserverEntryBoxSize {
    val blockSize: Double
    val inlineSize: Double
}
