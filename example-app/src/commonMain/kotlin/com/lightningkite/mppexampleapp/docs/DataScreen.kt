package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.important
import com.lightningkite.kiteui.views.minus
import com.lightningkite.kiteui.views.reactiveScope

@Routable("docs/data")
object DataScreen: DocScreen {

    override val title: Readable<String>
        get() = Constant("Data: how to do reactivity in KiteUI")

    override val covers: List<String> = listOf(
        "data",
        "Property",
        "PersistentProperty",
        "shared",
        "reactiveScope",
        "reactivity",
        "::prop { }",
        "launch"
    )

    override fun ViewWriter.render() {
        article {
            h1("Data")
            text("KiteUI is roughly based on Solid.js, which uses smaller blocks to contain actions that should run when dependencies change.\nHowever, to begin using that, we have to first look at Property.")

            code - text("val counter = Property<Int>(0)")
            val counter = Property<Int>(0)

            text("A Property is a changing value that we want to observe changes on.  We can now manipulate the value freely:")
            code - text("counter.value = 0")
            counter.value = 0

            text("Anyone who listens to the property will be notified when the value changes.  Now, how do we observe it?  We can use what's called a reactive scope:")
            example("""
                text {
                    reactiveScope { content = "The current counter value is ${'$'}{counter.await()}" }
                }
                """.trimIndent()) {
                text {
                    reactiveScope { content = "The current counter value is ${counter.await()}" }
                }
            }

            text("Now, a button to increment it:")
            example("""
                important - button {
                    text("Increment the counter")
                    onClick { counter.value++ }
                }
                """.trimIndent()) {
                important - button {
                    text("Increment the counter")
                    onClick { counter.value++ }
                }
            }
        }
    }

}