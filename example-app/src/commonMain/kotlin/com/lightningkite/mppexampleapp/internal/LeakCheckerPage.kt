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
//        frame {
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
        "justFrame" to { frame { } },
        "button" to { frame { button { text("hey"); onClick { } } } },
        "link" to { frame { link { text("hey"); onNavigate { }; to = { RootPage } } } },
        "textField" to { frame { textField { content bind stringProp } } },
        "numberField" to { frame { numberField { content bind doubleProp } } },
        "textArea" to { frame { textArea { content bind stringProp } } },
        "select" to { frame { select { bind(Property(0), Constant(listOf(1, 2, 3)), { it.toString() }) } } },
        "space" to { frame { space() } },
        "text" to { frame { text { ::content { "My string prop: ${stringProp()}" } } } },
        "stack" to { frame { frame { frame { } } } },
        "col" to { frame { col { col { } } } },
        "separator" to { frame { col { separator() } } },
        "sizing" to { frame { col { sizeConstraints(minHeight = 10.rem) - text("Size") } } },
        "scrolling" to { frame { scrolling - col { text("A") } } },
        "activityIndicator" to { frame { activityIndicator {} } },
        "checkbox" to { frame { checkbox {} } },
        "dismissBackground" to { frame { dismissBackground {} } },
        "icon" to { frame { icon { source = Icon.send } } },
        "image" to { frame { image { source = Resources.imagesGraph126 } } },
        "phoneNumberInput" to { frame { phoneNumberInput {} } },
        "localDateField" to { frame { localDateField {} } },
        "localDateTimeField" to { frame { localDateTimeField {} } },
        "localTimeField" to { frame { localTimeField {} } },
        "circularProgress" to { frame { circularProgress {} } },
        "radioButton" to { frame { radioButton {} } },
        "radioToggleButton" to { frame { radioToggleButton {} } },
        "rowCollapsingToColumn" to { frame { rowCollapsingToColumn(50.rem) {} } },
        "switch" to { frame { switch {} } },
        "toggleButton" to { frame { toggleButton {} } },
        "video" to { frame { video {} } },
        "webView" to { frame { webView {} } },
        "canvas" to { frame { canvas { delegate = DrawDelegate() } } },
        "recyclerView" to { frame { recyclerView { children<Int>(Constant((1..50).toList())) { text { ::content { it().toString() } } } } } },
        "viewPager" to { frame { viewPager { children<Int>(Constant((1..50).toList())) { text { ::content { it().toString() } } } } } },
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
//            expanding - swapView {
//                swapping(
//                    current = { index() }) {
//                    this@swapView.children.forEach {
//                        println("Leak detecting on $it")
//                        it.leakDetect()
//                    }
//                    makers[it % makers.size].second(this)
//                }
//            }
            frame {
                reactiveScope {
                    if(children.size > 0) {
                        children[0].leakDetect()
                        removeChild(0)
                    }
                    val m = makers[index() % makers.size]
                    m.second(this@frame)
                }
            }
        }
    }
}
