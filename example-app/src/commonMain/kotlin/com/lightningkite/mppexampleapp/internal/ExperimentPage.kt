package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.field
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@Routable("experiment")
public object ExperimentPage : Page {
    public override val title: Readable<String>
        get() = super.title

    @QueryParameter
    public val elementCount = Property(7)

    public override fun ViewWriter.render(): ViewModifiable = run {
        col {
            val c = Property("")
            text {
                ::content { c() }
            }
            button {
                text("Shorten")
                onClick { c.value = "Short" }
            }
            button {
                text("Lengthen")
                onClick { c.value = "Some lengthier text" }
            }
        }
//        col {
//            card - col {
//                val show = Property(false)
//                viewDebugTarget = this
//                card - text("A")
//                card - button {
//                    onClick { show.value = !show.value }
//                    text("OK")
//                }
//                shownWhen { show() } - card - text { content = "B" }
//            }
//        }

//        sizeConstraints(height = 30.rem) - card - col {
//            val show = Property(false)
//            viewDebugTarget = this
//            expanding - card - text("A")
//            expanding - shownWhen { !show() } - card - text { content = "B" }
//            expanding - shownWhen { !show() } - card - text { content = "D" }
//            expanding - card - button {
//                onClick { show.value = !show.value }
//                text("OK")
//            }
//            expanding - shownWhen { show() } - card - text { content = "C" }
//            expanding - shownWhen { show() } - card - text { content = "E" }
//        }

//        var expanded = Property(-1)
//        val items = shared { (1..elementCount()).toList() }
//        var recyclerView: Recycler2? = null
//        col {
//            row {
//                for (align in Align.values()) {
//                    expanding - button {
//                        subtext("Jump ${align.name}")
//                        onClick { recyclerView?.scrollToIndex(49, align, false) }
//                    }
//                }
//            }
//            row {
//                for (align in Align.values()) {
//                    expanding - button {
//                        subtext("Scroll ${align.name}")
//                        onClick { recyclerView?.scrollToIndex(49, align, true) }
//                    }
//                }
//            }
//            row {
//                repeat(4) {
//                    val cols = it + 1
//                    expanding - button {
//                        subtext("${cols} columns")
//                        onClick { recyclerView?.columns = cols }
//                    }
//                }
//                sizeConstraints(width = 10.rem) - field("Element Count") {
//                    numberInput { content bind elementCount.nullable().asDouble() }
//                }
//            }
//            recyclerView {
//                recyclerView = this
//                gap = 0.5.rem
////                columns = 2
//                reactive {
//                    val index = expanded()
//                    if (index == -1) return@reactive
//                    launch {
//                        delay(250)
////                        this@recyclerView.scrollToIndex(index - 1, Align.Start, true)
//                    }
//                }
//                this.scrollToIndex(10, Align.Start)
//                children(items, id = { it }) {
//                    col child@{
//                        dynamicTheme {
//                            if (it() % 7 == 0) ImportantSemantic
//                            else CardSemantic
//                        }
//                        row {
//                            expanding - centered - text { ::content { "Item ${it()} ".repeat(it()) } }
//                            centered - button {
//                                text {
//                                    ::content { if (expanded() == it()) "Expanded" else "Expand" }
//                                }
//                                onClick {
//                                    expanded.value = if (it.await() == expanded.value) -1 else it.await()
////                                    scrollIntoView(null, Align.Start, true)
//                                }
//                            }
//                        }
//                        shownWhen { expanded() == it() } - col {
////                            ::exists { expanded() == it() }
//                            text { ::content { "Content for ${it()} == ${expanded()}" } }
//                            text("More Content")
//                            text("More Content")
//                            text("More Content")
//                            text("More Content")
//                            text("More Content")
//                        }
//                    }
//                }
//            } in weight(1f)
//            row {
//                text {
//                    ::content {
//                        "Min: ${recyclerView!!.firstVisibleIndex()}, Max: ${recyclerView!!.lastVisibleIndex()}"
//                    }
//                }
//            }
//        }


//        col {
//            val data = Property<List<Char>>(listOf('A', 'B', 'C'))
//            text {::content { data().joinToString() }}
//            row {
//                forEachById(data, { it }) {
//                    card - text { ::content { it().toString()} }
//                }
//            }
//            row {
//                forEachAnimated(data) {
//                    card - text(it.toString())
//                }
//            }
//            button {
//                text("Insert")
//                action = Action("", frequencyCap = null) {
//                    val letter = 'A' + (Random.nextInt(26))
//                    if(letter in data.value) return@Action
//                    else data.value = data.value.toMutableList().apply { add(data.value.indices.randomOrNull() ?: 0, letter) }
//                }
//            }
//            button {
//                text("Remove")
//                action = Action("", frequencyCap = null) {
//                    data.value = data.value.toMutableList().apply {
//                        data.value.indices.randomOrNull()?.let { removeAt(it) }
//                    }
//                }
//            }
//            val img: ImageView
//            row {
//                sizeConstraints(width = 10.rem, height = 10.rem) - image {
//                    source = ImageRemote("https://timetracker-files20230201174841315900000003.s3.us-west-2.amazonaws.com/uploaded/577b5e22-57a4-498c-9966-908b26bba6bb.file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIARR4DEGXXWAKUENQZ%2F20250410%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20250410T162000Z&X-Amz-Expires=86400&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEDAaCXVzLXdlc3QtMiJGMEQCIDnaAHpWOWQIWCUFZr7x8qtpLbJ2OY1AH03FwFzh7pI1AiANItfnvbdUsAmO4X9DreK4l%2BWWc51lPQ8vD0VHyqS5Gir9Agip%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDEwNzExMjMxNDM1MSIMozSv3KEIOF6vbNRlKtECqeIpdmIPdwSvrXf3OL78T2ikmKMsu3rzZdHWAq4%2BSOvwiekbUzvIOKIyyBG4zRcKnQFYyIERxcTPh4T%2FBx68wAXFqMFdaYidglmHw3sA%2FU%2B%2FIPzqJFT3Az7kQ9dHC7MGsddim0Zcx8vVurNwItaJC5WcgdrPiKheP9WRvxQ4u31EbFD8n%2Bs5FV5Iy%2FZZ85DhND3PmDZCcq5HS%2FVM6TShTYE07iYUtwNTe3%2Fj31K7MKDJe%2Fnxrhj8BpaBX%2BIx7LOM%2BmZe5YzEZHE820q1wQOMw5zQri4cbY7Fuo6UZmyOsA4D9RYbgFVRFZXe2CP5U%2F9m4hM7xhOWcK%2BjEm%2FxXEx%2Fw9wuXGa51ULP7%2BkPPW1eAjmVx3gzD07VYSQtO2O8z%2BJ1BjjIEjPhZXBOFMxN0fwYu%2Brm7ENq89CpW7TioaXgVQCR%2F5YsjFBLaLwmyvm4ZB6aMjDz3N%2B%2FBjqfAaZF5HfWqPC3DKTXXwyszFAbBTBCrvJPsHvgZZqU1tOuOtT71d0OGSbiR1RQhdiRvHjNlVGX9QEtMw6YUiGNU8cKcNwpekzNqnsb9GSCWlyWOROCsUyul2zMzLJLPjywj2FAmNqO%2FOdARcduGnVPH4Xj98qhsNgXG3hiE0NxoZjAImTB0NRUVHHMRChmN9BGm5qHHv5wxYJwyoXb8xDHmg%3D%3D&X-Amz-SignedHeaders=host&X-Amz-Signature=0ae9b446ed5567b2241cb6807fd41b760e224dd1922226c9f8419f8WRONG")
//                    source = ImageRemote("https://timetracker-files20230201174841315900000003.s3.us-west-2.amazonaws.com/uploaded/577b5e22-57a4-498c-9966-908b26bba6bb.file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIARR4DEGXXWAKUENQZ%2F20250410%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20250410T162000Z&X-Amz-Expires=86400&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEDAaCXVzLXdlc3QtMiJGMEQCIDnaAHpWOWQIWCUFZr7x8qtpLbJ2OY1AH03FwFzh7pI1AiANItfnvbdUsAmO4X9DreK4l%2BWWc51lPQ8vD0VHyqS5Gir9Agip%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDEwNzExMjMxNDM1MSIMozSv3KEIOF6vbNRlKtECqeIpdmIPdwSvrXf3OL78T2ikmKMsu3rzZdHWAq4%2BSOvwiekbUzvIOKIyyBG4zRcKnQFYyIERxcTPh4T%2FBx68wAXFqMFdaYidglmHw3sA%2FU%2B%2FIPzqJFT3Az7kQ9dHC7MGsddim0Zcx8vVurNwItaJC5WcgdrPiKheP9WRvxQ4u31EbFD8n%2Bs5FV5Iy%2FZZ85DhND3PmDZCcq5HS%2FVM6TShTYE07iYUtwNTe3%2Fj31K7MKDJe%2Fnxrhj8BpaBX%2BIx7LOM%2BmZe5YzEZHE820q1wQOMw5zQri4cbY7Fuo6UZmyOsA4D9RYbgFVRFZXe2CP5U%2F9m4hM7xhOWcK%2BjEm%2FxXEx%2Fw9wuXGa51ULP7%2BkPPW1eAjmVx3gzD07VYSQtO2O8z%2BJ1BjjIEjPhZXBOFMxN0fwYu%2Brm7ENq89CpW7TioaXgVQCR%2F5YsjFBLaLwmyvm4ZB6aMjDz3N%2B%2FBjqfAaZF5HfWqPC3DKTXXwyszFAbBTBCrvJPsHvgZZqU1tOuOtT71d0OGSbiR1RQhdiRvHjNlVGX9QEtMw6YUiGNU8cKcNwpekzNqnsb9GSCWlyWOROCsUyul2zMzLJLPjywj2FAmNqO%2FOdARcduGnVPH4Xj98qhsNgXG3hiE0NxoZjAImTB0NRUVHHMRChmN9BGm5qHHv5wxYJwyoXb8xDHmg%3D%3D&X-Amz-SignedHeaders=host&X-Amz-Signature=0ae9b446ed5567b2241cb6807fd41b760e224dd1922226c9f8419f8d7d66e630")
//                }
//                sizeConstraints(width = 10.rem, height = 10.rem) - image {
//                    source = ImageRemote("https://timetracker-files20230201174841315900000003.s3.us-west-2.amazonaws.com/uploaded/577b5e22-57a4-498c-9966-908b26bba6bb.file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIARR4DEGXXWAKUENQZ%2F20250410%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20250410T162000Z&X-Amz-Expires=86400&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEDAaCXVzLXdlc3QtMiJGMEQCIDnaAHpWOWQIWCUFZr7x8qtpLbJ2OY1AH03FwFzh7pI1AiANItfnvbdUsAmO4X9DreK4l%2BWWc51lPQ8vD0VHyqS5Gir9Agip%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDEwNzExMjMxNDM1MSIMozSv3KEIOF6vbNRlKtECqeIpdmIPdwSvrXf3OL78T2ikmKMsu3rzZdHWAq4%2BSOvwiekbUzvIOKIyyBG4zRcKnQFYyIERxcTPh4T%2FBx68wAXFqMFdaYidglmHw3sA%2FU%2B%2FIPzqJFT3Az7kQ9dHC7MGsddim0Zcx8vVurNwItaJC5WcgdrPiKheP9WRvxQ4u31EbFD8n%2Bs5FV5Iy%2FZZ85DhND3PmDZCcq5HS%2FVM6TShTYE07iYUtwNTe3%2Fj31K7MKDJe%2Fnxrhj8BpaBX%2BIx7LOM%2BmZe5YzEZHE820q1wQOMw5zQri4cbY7Fuo6UZmyOsA4D9RYbgFVRFZXe2CP5U%2F9m4hM7xhOWcK%2BjEm%2FxXEx%2Fw9wuXGa51ULP7%2BkPPW1eAjmVx3gzD07VYSQtO2O8z%2BJ1BjjIEjPhZXBOFMxN0fwYu%2Brm7ENq89CpW7TioaXgVQCR%2F5YsjFBLaLwmyvm4ZB6aMjDz3N%2B%2FBjqfAaZF5HfWqPC3DKTXXwyszFAbBTBCrvJPsHvgZZqU1tOuOtT71d0OGSbiR1RQhdiRvHjNlVGX9QEtMw6YUiGNU8cKcNwpekzNqnsb9GSCWlyWOROCsUyul2zMzLJLPjywj2FAmNqO%2FOdARcduGnVPH4Xj98qhsNgXG3hiE0NxoZjAImTB0NRUVHHMRChmN9BGm5qHHv5wxYJwyoXb8xDHmg%3D%3D&X-Amz-SignedHeaders=host&X-Amz-Signature=0ae9b446ed5567b2241cb6807fd41b760e224dd1922226c9f8419f8WRONG")
//                    img = this
//                }
//            }
//            button {
//                text("Reattempt load")
//                onClick {
//                    img.source = ImageRemote("https://timetracker-files20230201174841315900000003.s3.us-west-2.amazonaws.com/uploaded/577b5e22-57a4-498c-9966-908b26bba6bb.file?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ASIARR4DEGXXWAKUENQZ%2F20250410%2Fus-west-2%2Fs3%2Faws4_request&X-Amz-Date=20250410T162000Z&X-Amz-Expires=86400&X-Amz-Security-Token=IQoJb3JpZ2luX2VjEDAaCXVzLXdlc3QtMiJGMEQCIDnaAHpWOWQIWCUFZr7x8qtpLbJ2OY1AH03FwFzh7pI1AiANItfnvbdUsAmO4X9DreK4l%2BWWc51lPQ8vD0VHyqS5Gir9Agip%2F%2F%2F%2F%2F%2F%2F%2F%2F%2F8BEAAaDDEwNzExMjMxNDM1MSIMozSv3KEIOF6vbNRlKtECqeIpdmIPdwSvrXf3OL78T2ikmKMsu3rzZdHWAq4%2BSOvwiekbUzvIOKIyyBG4zRcKnQFYyIERxcTPh4T%2FBx68wAXFqMFdaYidglmHw3sA%2FU%2B%2FIPzqJFT3Az7kQ9dHC7MGsddim0Zcx8vVurNwItaJC5WcgdrPiKheP9WRvxQ4u31EbFD8n%2Bs5FV5Iy%2FZZ85DhND3PmDZCcq5HS%2FVM6TShTYE07iYUtwNTe3%2Fj31K7MKDJe%2Fnxrhj8BpaBX%2BIx7LOM%2BmZe5YzEZHE820q1wQOMw5zQri4cbY7Fuo6UZmyOsA4D9RYbgFVRFZXe2CP5U%2F9m4hM7xhOWcK%2BjEm%2FxXEx%2Fw9wuXGa51ULP7%2BkPPW1eAjmVx3gzD07VYSQtO2O8z%2BJ1BjjIEjPhZXBOFMxN0fwYu%2Brm7ENq89CpW7TioaXgVQCR%2F5YsjFBLaLwmyvm4ZB6aMjDz3N%2B%2FBjqfAaZF5HfWqPC3DKTXXwyszFAbBTBCrvJPsHvgZZqU1tOuOtT71d0OGSbiR1RQhdiRvHjNlVGX9QEtMw6YUiGNU8cKcNwpekzNqnsb9GSCWlyWOROCsUyul2zMzLJLPjywj2FAmNqO%2FOdARcduGnVPH4Xj98qhsNgXG3hiE0NxoZjAImTB0NRUVHHMRChmN9BGm5qHHv5wxYJwyoXb8xDHmg%3D%3D&X-Amz-SignedHeaders=host&X-Amz-Signature=0ae9b446ed5567b2241cb6807fd41b760e224dd1922226c9f8419f8d7d66e630")
//                }
//            }
//        }
//        scrolling - col {
//            card - col {
//                gap = 0.5.rem
//                paddingByEdge = Edges(left = 3.rem, top = 1.rem, right = 0.rem, bottom = 2.rem)
//                h1("Weird gap time")
//                spacingOverrideBeforeNext(10.rem)
//                text("Really far down")
//                spacingOverrideBeforeNext(0.rem)
//                text("Really close")
//                spacingOverrideBeforeNext(0.rem)
//                text {
//                    content = "Really close"
//                    shown = false
//                }
//                spacingOverrideBeforeNext((-0.5).rem)
//                text("Pull up and overlap some")
//                spacingOverrideBeforeNext(1.rem)
//                text("Less close")
//            }
//        }

//        scrolling - col {
//            card - col {
//                gap = 0.5.rem
//                paddingByEdge = Edges(left = 3.rem, top = 1.rem, right = 0.rem, bottom = 2.rem)
//                h1("Weird gap time")
//                spacingOverrideBeforeNext(10.rem)
//                text("Really far down")
//                spacingOverrideBeforeNext(0.rem)
//                text("Really close")
//                spacingOverrideBeforeNext(0.rem)
//                text {
//                    content = "Really close"
//                    exists = false
//                }
//                spacingOverrideBeforeNext((-0.5).rem)
//                text("Pull up and overlap some")
//                spacingOverrideBeforeNext(1.rem)
//                text("Less close")
//            }
//        }

//        col {
//            expanding - recyclerView {
//                log = ConsoleRoot.tag("X")
////                children(Constant((1..20).toList()), id = { it }) {
////                    text { ::content { it().toString() } }
////                }
//                childrenMultipleTypes(Constant((1..200).toList()), id = { it }) {
//                    println("Building...")
//                    elementsMatching { it % 2 == 0 } renderedAs { text { ::content { it().toString() } } }
//                    elementsMatching { it % 2 == 1 } renderedAs { card - text { ::content { it().toString() } } }
//                }
//                println("OK")
//            }
//        }

//        col {
//            val expanded = Property(-1)
//            val data = Property((1..10).toList())
//            var recyclerView: Recycler2? = null
//            expanding
//            recyclerView = Recycler2(this, vertical = false).apply {
////                log = ConsoleRoot.tag("R2")
//                scrollToIndex(2, Align.Center, animate = false)
//                this.snapToElements = Align.Center
//                this.scrollSnapStop = true
//                val main: RecyclerViewRenderer<Int> = object : RecyclerViewRenderer<Int> {
//                    override fun render(viewWriter: ViewWriter, data: Readable<Int>, index: Readable<Int>) =
//                        with(viewWriter) {
//                            card - button {
//                                sizeConstraints(minHeight = 10.rem) - col {
//                                    text { ::content { data().toString() } }
//                                    onlyWhen { expanded() == data() } - col {
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                        text { content = "Expanded Content" }
//                                    }
//                                }
//                                onClick {
//                                    expanded.value = data()
//                                }
//                            }
//
//                        }
//                }
////                scrollToIndex(50, Align.Center)
//                placer = RecyclerViewPagingPlacer().apply { log = ConsoleRoot.tag("RVP2") }
//                rendererSet = object : RecyclerViewRendererSet<Int, Int> {
//                    override fun id(item: Int): Int = item
//                    override fun renderer(item: Int): RecyclerViewRenderer<Int> = main
//                }
//                reactive {
//                    val d = data()
//                    this@apply.data = object : RecyclerViewData<Int, Int> {
//                        override val range: IntRange = d.indices
//                        override fun get(index: Int): Int = d[index]
//                    }
//                }
//            }
//            row {
//                button {
//                    text("left")
//                    onClick {
//                        recyclerView.centerIndex set recyclerView.centerIndex() - 1
//                    }
//                }
//                button {
//                    text("delete")
//                    onClick {
//                        val toRemove = data().get(recyclerView.centerIndex())
//                        data.value = data.value.filter { it != toRemove }
//                    }
//                }
//                button {
//                    text("right")
//                    onClick {
//                        recyclerView.centerIndex set recyclerView.centerIndex() + 1
//                    }
//                }
//            }
//        }
    }
}