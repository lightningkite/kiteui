package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.PersistentProperty
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay

@Routable("reactivity")
public object ReactivityPage : Page {
    public override val title: Reactive<String>
        get() = super.title

    public override fun ViewWriter.render(): ViewModifiable = run {
        val local = Signal("Local")
        val persist = PersistentProperty("persistent-example", "Persistent")
        val indirect = remember { local() + " " + persist() }
        val debounced = Signal("Debounced").debounceWrite(500.milliseconds)
        val dependency = Signal(0)
        val fetching = remember {
            async(dependency()) { delay(1000) }
            "Loaded!"
        }
        scrolling - col {
            col {
                h1 { content = "This screen demonstrates various forms of reactivity." }
                text { content = "Note the use of the multi-layer 'Reactive' in `fetching`." }
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