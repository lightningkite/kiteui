package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("reactivity-alt")
object ReactivityAltScreen : Screen {
    override val title: Readable<String>
        get() = super.title

    override fun ViewWriter.render() {
        val local = Property("Local")
        val persist = PersistentProperty("persistent-example", "Persistent")
        val dependency = Property(0)
        val fetching = shared {
            dependency
            async() { delay(1000) }
            "Loaded!"
        }
        col {
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
                    onClick { dependency.value++ }
                } in important
            } in card

            fetching.addListener { println("Fetching state $fetching") }

            col {
                h2 { content = "Using reactiveScope()" }
                text { reactiveScope { content = local() } }
                text { reactiveScope { content = persist() } }
                text { reactiveScope { content = fetching() } }
            } in card
        }
    }
}