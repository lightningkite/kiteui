package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("leak-checker")
object LeakCheckerScreen : Screen {
    val stringProp = Property("X")
    val doubleProp = Property<Double?>(0.0)
    val makers = listOf<Pair<String, ViewWriter.()->Unit>>(
        "space" to { space() },
        "text" to { text("Test") },
        "stack" to { stack { stack { } } },
        "col" to { col { col { } } },
        "separator" to { col { separator() } },
        "sizing" to { col { sizeConstraints(minHeight = 10.rem) - text("Size") } },
        "scrolls" to { scrolls - col { text("A") } },
        "button" to { button { text("hey"); onClick {  } } },
        "link" to { link { text("hey"); onNavigate {  }; to = { RootScreen } } },
        "textField" to { textField { content bind stringProp } },
        "numberField" to { numberField { content bind doubleProp } },
        "textArea" to { textArea { content bind stringProp } },
        "select" to { select { bind(Property(0), Constant(listOf(1, 2, 3)), { it.toString() }) } },
    )
    override fun ViewWriter.render() {
        val index = Property(0)
        col {
            button {
                text {
                    ::content { "Current is ${makers[index() % makers.size].first}, next is ${makers[index().plus(1) % makers.size].first}" }
                }
                onClick { index.value++ }
            }
            stack {
                reactiveScope {
                    if(children.size > 0) {
                        children[0].leakDetect()
                        removeChild(0)
                    }
                    val m = makers[index() % makers.size]
                    m.second(this@stack)
                }
            }
        }
    }
}
