package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.readable.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Routable("ultra-basic")
object UltraBasicPage : Page {
    val count = object : ImmediateWritable<Int> {
        val listeners = ArrayList<() -> Unit>()
        override var value: Int = 0
            set(value) {
                field = value
                listeners.forEach { it() }
            }

        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            println("Added listener $listener")
            return {
                listeners.remove(listener)
                println("Removed listener $listener; remaining: ${listeners.joinToString()}")
            }
        }
    }

    override fun ViewWriter.render(): ViewModifiable = run {
//        stack {
//            onRemove { println("stack onRemove") }
            button {
                onRemove { println("button onRemove") }
                text {
                    onRemove { println("text onRemove") }
                    if (CoroutineScopeStack.current() !== this) throw IllegalStateException("Scopes don't match")
                    ::content { count().toString() }
                }
//            text { reactive { content = count().toString() } }
//            text("Increment")
                onClick { count.value++ }
            }
//        }
    }
}

@Routable("counter")
object CounterPage : Page {
    val count = object : ImmediateWritable<Int> {
        val listeners = ArrayList<() -> Unit>()
        override var value: Int = 0
            set(value) {
                field = value
                listeners.forEach { it() }
            }

        override fun addListener(listener: () -> Unit): () -> Unit {
            listeners.add(listener)
            println("Added listener $listener")
            return {
                listeners.remove(listener)
                println("Removed listener $listener; remaining: ${listeners.joinToString()}")
            }
        }
    }

    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            text {
                if (CoroutineScopeStack.current() !== this) throw IllegalStateException("Scopes don't match")
                ::content { "${count()}" }
            }
            button {
                text("Increment")
                onClick { count.value++ }
            }
        }
    }
}

@Routable("leak-checker")
object LeakCheckerPage : Page {
    val stringProp = Property("X")
    val doubleProp = Property<Double?>(0.0)
    val makers = listOf<Pair<String, ViewWriter.() -> Unit>>(
        "canvas" to { stack { canvas { delegate = DrawDelegate() } } },
        "recyclerView" to { stack { recyclerView { children<Int>(Constant((1..50).toList())) { text { ::content { it().toString() } } } } } },
        "viewPager" to { stack { viewPager { children<Int>(Constant((1..50).toList())) { text { ::content { it().toString() } } } } } },
        "button" to { stack { button { text("hey"); onClick { } } } },
        "link" to { stack { link { text("hey"); onNavigate { }; to = { RootPage } } } },
        "textField" to { stack { textField { content bind stringProp } } },
        "numberField" to { stack { numberField { content bind doubleProp } } },
        "textArea" to { stack { textArea { content bind stringProp } } },
        "select" to { stack { select { bind(Property(0), Constant(listOf(1, 2, 3)), { it.toString() }) } } },
        "space" to { stack { space() } },
        "text" to { stack { text { ::content { "My string prop: ${stringProp()}" } } } },
        "stack" to { stack { stack { stack { } } } },
        "col" to { stack { col { col { } } } },
        "separator" to { stack { col { separator() } } },
        "sizing" to { stack { col { sizeConstraints(minHeight = 10.rem) - text("Size") } } },
        "scrolls" to { stack { scrolls - col { text("A") } } },
        "activityIndicator" to { stack { activityIndicator {} } },
        "checkbox" to { stack { checkbox {} } },
        "dismissBackground" to { stack { dismissBackground {} } },
        "icon" to { stack { icon { source = Icon.send } } },
        "image" to { stack { image { source = Resources.imagesGraph126 } } },
        "phoneNumberInput" to { stack { phoneNumberInput {} } },
        "localDateField" to { stack { localDateField {} } },
        "localDateTimeField" to { stack { localDateTimeField {} } },
        "localTimeField" to { stack { localTimeField {} } },
        "circularProgress" to { stack { circularProgress {} } },
        "radioButton" to { stack { radioButton {} } },
        "radioToggleButton" to { stack { radioToggleButton {} } },
        "rowCollapsingToColumn" to { stack { rowCollapsingToColumn(50.rem) {} } },
        "switch" to { stack { switch {} } },
        "toggleButton" to { stack { toggleButton {} } },
        "video" to { stack { video {} } },
        "webView" to { stack { webView {} } },
    )

    override fun ViewWriter.render(): ViewModifiable = run {
        val index = Property(0)
        col {
//            launch {
//                while(true) {
//                    delay(5.seconds)
//                    index.value++
//                }
//            }
            button {
                text {
                    ::content { "Current is ${makers[index() % makers.size].first}, next is ${makers[index().plus(1) % makers.size].first}" }
                }
                action = Action("next", Icon.done, frequencyCap = 50.milliseconds) { index.value++ }
            }
            expanding - swapView {
                swapping(current = { index() }) {
                    this@swapView.children.forEach {
                        println("Leak detecting on $it")
                        it.leakDetect()
                    }
                    makers[it % makers.size].second(this)
                }
            }
//            stack {
//                reactiveScope {
//                    if(children.size > 0) {
//                        children[0].leakDetect()
//                        removeChild(0)
//                    }
//                    val m = makers[index() % makers.size]
//                    m.second(this@stack)
//                }
//            }
        }
    }
}
