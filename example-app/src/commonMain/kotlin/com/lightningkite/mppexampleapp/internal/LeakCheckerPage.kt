package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("ultra-basic")
object UltraBasicPage : Page {
    val count = Signal(0)

    override fun ViewWriter.render(): Unit = run {
//        frame {
//            onRemove { println("stack onRemove") }
        col {
            text("Wait...")
            launch {
                delay(0.5.seconds)
                pageNavigator.navigate(CounterPage)
            }
//            button {
//                text {
//                    ::content { count().toString() }
//                }
//            text { reactive { content = count().toString() } }
//            text("Increment")
//                onClick { count.value++ }
//            }
//            link { to = { CounterPage }; text("Jump to counter") }
        }
//        }
    }
}

@Routable("counter")
object CounterPage : Page {
    val count = object : MutableReactiveValue<Int> {
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

    override fun ViewWriter.render(): Unit = run {
        col {
            text {
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
    val stringProp = Signal("X")
    val doubleProp = Signal<Double?>(0.0)
    val makers = listOf<Pair<String, ViewWriter.() -> Unit>>(
        "scrolling" to { frame { scrolling.col { text("A") } } },
        "justFrame" to { frame { } },
        "button" to { frame { button { text("hey"); onClick { } } } },
        "link" to { frame { link { text("hey"); onNavigate { }; to = { RootPage } } } },
        "textField" to { frame { textField { content bind stringProp } } },
        "numberField" to { frame { numberField { content bind doubleProp } } },
        "textArea" to { frame { textArea { content bind stringProp } } },
        "select" to { frame { select { bind(Signal(0), Constant(listOf(1, 2, 3)), { it.toString() }) } } },
        "space" to { frame { space() } },
        "text" to { frame { text { ::content { "My string prop: ${stringProp()}" } } } },
        "stack" to { frame { frame { frame { } } } },
        "col" to { frame { col { col { } } } },
        "separator" to { frame { col { separator() } } },
        "sizing" to { frame { col { sizeConstraints(minHeight = 10.rem).text("Size") } } },
        "activityIndicator" to { frame { activityIndicator {} } },
        "checkbox" to { frame { checkbox {} } },
        "dismissBackground" to { frame { dismissBackground {} } },
        "icon" to { frame { icon { source = Icon.send } } },
        "image" to { frame { image { source = Resources.imagesLightningBackground } } },
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
//        "recyclerView" to { frame { recyclerView { children<Int>(Constant((1..50).toList())) { text { ::content { it().toString() } } } } } },
//        "viewPager" to { frame { viewPager { children<Int>(Constant((1..50).toList())) { text { ::content { it().toString() } } } } } },
    )

    override fun ViewWriter.render(): Unit = run {
        val index = Signal(0)
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
