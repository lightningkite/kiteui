package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

@Routable("reactivity")
object ReactivityScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        val local = Property("Local")
        val persist = PersistentProperty("persistent-example", "Persistent")
        val dependency = Property(0)
        val fetching = shared {
            async(dependency()) { delay(1000) }
            "Loaded!"
        }
        scrolls - col {
            col {
                h1 { content = "This screen demonstrates various forms of reactivity." }
                text { content = "Note the use of the multi-layer 'Readable' in `fetching`." }
            } in padded

            col {
                h2 { content = "Data" }
                label {
                    content = "Locally Stored Value"
                    textField { content bind local }
                }
                label {
                    content = "Persistent Value - this will stay between refreshes"
                    textField { content bind persist }
                }
                button {
                    text { content = "Reload 'fetching'" }
                    onClick {
                        dependency.value++
                    }
                } in important
            } in card

            text {
                val timerFlow = flow<Int> {
                    var it = 0
                    while(true) {
                        emit(it++)
                        delay(it * 10L)
                    }
                }
                ::content { timerFlow().toString() }
            }

            col {
                h2 { content = "Using reactiveScope()" }
                text { reactiveScope { content = local() } }
                text { reactiveScope { content = persist() } }
                text { reactiveScope { content = fetching() } }
            } in card

            col {
                h2 { content = "Using reactiveScope()" }
                text { reactiveScope { content = local.await() } }
                text { reactiveScope { content = persist.await() } }
                text { reactiveScope { content = fetching.await() } }
            } in card

            col {
                h2 { content = "Using ::content {}" }
                text { ::content { local() } }
                text { ::content { persist() } }
                text { ::content { fetching() } }
            } in card

            col {
                h2 { content = "Using ::content {}" }
                text { ::content { local.await() } }
                text { ::content { persist.await() } }
                text { ::content { fetching.await() } }
            } in card
        }
    }
}