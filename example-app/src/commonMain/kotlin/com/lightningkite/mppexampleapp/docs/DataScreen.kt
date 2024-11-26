package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.important
import com.lightningkite.kiteui.views.minus
import com.lightningkite.mppexampleapp.widgets.code

@Routable("docs/data")
object DataScreen: DocScreen {

    override val title: Readable<String>
        get() = Constant("Data: How to do Reactivity in KiteUI")

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
            h1("Data: How to do Reactivity in KiteUI")
            space()
            h2("Starting simple with Property")
            text("KiteUI is roughly based on Solid.js, which uses smaller sections to contain actions that should run when dependencies change.")
            text("However, to begin using that, we have to first look at Property.")

            code { content = ("val counter = Property<Int>(0)") }
            val counter = Property<Int>(0)

            text("A Property is a changing value that we want to observe changes on.  We can now manipulate the value freely:")
            code { content = "counter.value = 0" }
            counter.value = 0

            text("Anyone who listens to the property will be notified when the value changes.  Now, how do we use it?  We can use what's called a reactive scope:")
            example("""
                text {
                    reactive { content = "The current counter value is ${'$'}{counter()}" }
                }
                """.trimIndent()) {
                text {
                    reactive { content = "The current counter value is ${counter()}" }
                }
            }
            text("The code within the reactive block will run any time the things it depends on change.")

            text("Now, let's make a button to increment it:")
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

            text("That's the simplest way to start.")
            space()

            h2("Readable: the root of all KiteUI reactivity")
            text("A Property implements the Readable interface, which is the root of all the reactive tools in KiteUI.")
            text("If you have any kind of Readable, you may 'call' it with () within a reactive scope to both get the value and rerun when it changes.")
            text("However, there are other kinds of readables too.  The most commonly used one is shared:")
            val otherCounter = Property(0)
            val combinedCounters = shared { counter() + otherCounter() }
            code { content = """
                val otherCounter = Property(0)
                val combinedCounters = shared { counter() + otherCounter() }
            """.trimIndent()}
            text("Now, we can access this calculation like this:")
            example("""
                col {
                    important - button {
                        text { reactive { content = "Increment the other counter, which is at ${'$'}{otherCounter()}" } }
                        onClick { otherCounter.value++ }
                    }
                    text {
                        reactive { "The combined values of the counter is ${'$'}{combinedCounters()}" }
                    }
                }
                """.trimIndent()) {
                col {
                    important - button {
                        text { reactive { content = "Increment the other counter, which is at ${otherCounter()}" } }
                        onClick { otherCounter.value++ }
                    }
                    text {
                        reactive { content = "${counter()} + ${otherCounter()} = ${combinedCounters()}" }
                    }
                }
            }
            text("Notice that you can increment either of the two counters now")
        }
    }

}