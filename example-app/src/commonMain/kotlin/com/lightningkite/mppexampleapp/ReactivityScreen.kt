package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import kotlin.time.Duration.Companion.milliseconds

@Routable("reactivity")
object ReactivityScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        val local = Property("Local")
        val persist = PersistentProperty("persistent-example", "Persistent")
        val indirect = shared { local() + " " + persist() }
        val debounced = Property("Debounced").debounceWrite(500.milliseconds)
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
                field("Locally Stored Value") {
                    textInput { content bind local }
                }
                field("Persistent Value - this will stay between refreshes") {
                    textInput { content bind persist }
                }
                field("Debounced Value") {
                    textInput { content bind debounced }
                }
                button {
                    text { content = "Reload 'fetching'" }
                    onClick {
                        dependency.value++
                    }
                } in important
            } in card

            col {
                h2 { content = "Using reactiveScope()" }
                text { reactiveScope { content = "local = ${local()}" } }
                text { reactiveScope { content = "persist = ${persist()}" } }
                text { reactiveScope { content = "indirect = ${indirect()}" } }
                text { reactiveScope { content = "debounced = ${debounced()}" } }
                text { reactiveScope { content = "fetching = ${fetching()}" } }
            } in card

            col {
                h2 { content = "Using ::content {}" }
                text { ::content { "local = ${local()}" } }
                text { ::content { "persist = ${persist()}" } }
                text { ::content { "indirect = ${indirect()}" } }
                text { ::content { "debounced = ${debounced()}" } }
                text { ::content { "fetching = ${fetching()}" } }
            } in card
        }
    }
}