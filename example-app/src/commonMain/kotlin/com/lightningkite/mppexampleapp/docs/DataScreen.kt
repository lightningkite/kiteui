package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.danger
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.important
import com.lightningkite.kiteui.views.minus
import com.lightningkite.mppexampleapp.widgets.code
import kotlinx.coroutines.delay

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
            h2("Readable")
            text("Readable is the root of reactivity in KiteUI.  A readable is something you can read and be notified when it changes.")
            text("The simplest example of a readable is 'Property', which directly contains a value we can change.")

            space()
            h3("Property")
            code { content = """
                val counter = Property<Int>(0)
                val callToStopListening = counter.addListener { 
                    println("Value has changed to ${'$'}{counter.value}")
                }
                counter.value = 10
                callToStopListening()
            """.trimIndent() }
            val counter = Property<Int>(0)
            val callToStopListening = counter.addListener {
                println("Value has changed to ${counter.value}")
            }
            counter.value = 10
            callToStopListening()
            counter.value = 10

            text("The above should have printed 'Value has changed to 10' in your console.")
            text("Because we called the function returned from addListener, further changes won't print.")
            space()

            h3("sharedProcess")

            text("Other kinds of readables can also contain loading and error states, which can be displayed in your UI with no modifications.")
            text("Another example is 'sharedProcess', which runs a Kotlin Coroutine that emits values.  For example, let's create a counter for seconds since opening the screen:")
            code {
                content = """
                    val secondsElapsed = sharedProcess<Int> {
                        // Starts out as 'loading'
                        var n = 0
                        while(true) {
                            delay(1000)
                            emit(n++)
                        }
                    }
                    secondsElapsed.addListener { println("secondsElapsed.state: ${'$'}{secondsElapsed.state}") }  
                """.trimIndent()
            }
            val secondsElapsed = sharedProcess<Int> {
                // Starts out as 'loading'
                var n = 0
                while(true) {
                    delay(1000)
                    emit(n++)
                }
            }
            secondsElapsed.addListener { println("secondsElapsed.state: ${secondsElapsed.state}") }
            text("This should print an increasing number every second.")
            space()

            h2("Connecting to UI")
            space()
            h3("Painful manual connecting to UI")
            text("We could manually connect a view to this data like so:")
            example("""
                text {
                    val endListening = secondsElapsed.addListener { 
                        content = secondsElapsed.state.getOrNull()?.toString() ?: "Not ready"
                    }
                    onRemove(endListening)
                }
            """.trimIndent()) {
                text {
                    val endListening = secondsElapsed.addListener {
                        secondsElapsed.state.handle(
                            success = { content = it.toString() },
                            exception = { content = "Failed: ${it.message}" },
                            notReady = { content = "Not ready" }
                        )
                    }
                    onRemove(endListening)
                }
            }
            text("However, this is not only long and tedious, but error-prone.  It's easy to forget to remove a listener.  Enter reactive scopes.")
            space()

            h3("Plain Reactive Scope")
            text("A reactive scope is a block of code that reruns whenever one of its dependencies change.  Inside a reactive scope, one may 'call' a readable by using parentheses afterwards.  An example:")
            example("""
                text {
                    reactive { content = secondsElapsed().toString() }
                }
            """.trimIndent()) {
                text {
                    reactive { content = secondsElapsed().toString() }
                }
            }
            text("This method has tons of advantages:")
            text("- Listeners are automatically removed when the view is removed")
            text("- Declaring a set of code to run from multiple listeners is easy")
            text("- Loading states are automatically handled")
            text("- Error states are automatically handled")
            space()

            h3("Property Reactive Scopes")
            text("We can do better than that.  There's a syntactic shorthand for the above that enforces good practice:")
            example("""
                text {
                    ::content { secondsElapsed().toString() }
                }
            """.trimIndent()) {
                text {
                    ::content { secondsElapsed().toString() }
                }
            }
            text("This is generally the best way to bind data to views.")
            space()

            danger - h3("Don't create views in reactive scopes")
            text("Unless you know what you're doing, you should NOT create views in a reactive scope.  This code:")
            code { content = """
                stack {
                    reactive {
                        if(secondsElapsed() % 2 == 0) {
                            text("We're on an even second")
                        } else {
                            text("We're on an odd second")
                        }
                    }
                }
            """.trimIndent()}
            text("would insert a new piece of text EVERY SECOND, which is most certainly not what you intend.")
            text("You should prefer changing properties on views, and if that's not possible, you should prefer hiding or showing views when necessary.")
            example("""
                col {
                    onlyWhen { secondsElapsed() % 2 == 0 } - text("We're on an even second")
                    onlyWhen { secondsElapsed() % 2 != 0 } - text("We're on an odd second")
                }
            """.trimIndent()) {
                col {
                    onlyWhen { secondsElapsed() % 2 == 0 } - text("We're on an even second")
                    onlyWhen { secondsElapsed() % 2 != 0 } - text("We're on an odd second")
                }
            }
            text("This is much easier on the DOM, so you'll get better performance too.")
            text("If for some reason you truly need to dynamically create a new, you can use a swapView.")
            space()

            h2("shared")
            text("You can create a readable out of a reactive scope using 'shared', like this:")
            val otherCounter = Property(0)
            val combinedCounters = shared { counter() + otherCounter() }
            code { content = """
                val otherCounter = Property(0)
                val combinedCounters = shared { counter() + otherCounter() }
            """.trimIndent()}
            text("This is particularly useful for creating a readable whose value is calculated from other readables.")
            text("'shared' is short for 'shared calculation'.  If multiple people listen to this property, they share the calculated result.")
            text("'shared' is also lazy - it won't begin calculating until someone is listing.  It's safe to use 'shared' at the top level for this reason!")
            danger - text("You should not use a Property to hold a view of another Property.  Use 'shared' instead/")
            text("Now, we can access this calculation like this:")
            example("""
                col {
                    important - button {
                        text { ::content { "Increment the other counter, which is at ${'$'}{otherCounter()}" } }
                        onClick { otherCounter.value++ }
                    }
                    text {
                        ::content { "${'$'}{counter()} + ${'$'}{otherCounter()} = ${'$'}{combinedCounters()}" }
                    }
                }
                """.trimIndent()) {
                col {
                    important - button {
                        text { ::content { "Increment the other counter, which is at ${otherCounter()}" } }
                        onClick { otherCounter.value++ }
                    }
                    text {
                        ::content { "${counter()} + ${otherCounter()} = ${combinedCounters()}" }
                    }
                }
            }
            text("Notice that you can increment either of the two counters now")
//            space()
//            h2("Starting simple with Property")
//            text("KiteUI is roughly based on Solid.js, which uses smaller sections to contain actions that should run when dependencies change.")
//            text("However, to begin using that, we have to first look at Property.")
//
//            code { content = ("val counter = Property<Int>(0)") }
//            val counter = Property<Int>(0)
//
//            text("A Property is a changing value that we want to observe changes on.  We can now manipulate the value freely:")
//            code { content = "counter.value = 0" }
//            counter.value = 0
//
//            text("Anyone who listens to the property will be notified when the value changes.  Now, how do we use it?  We can use what's called a reactive scope:")
//            example("""
//                text {
//                    reactive { content = "The current counter value is ${'$'}{counter()}" }
//                }
//                """.trimIndent()) {
//                text {
//                    reactive { content = "The current counter value is ${counter()}" }
//                }
//            }
//            text("The code within the reactive block will run any time the things it depends on change.")
//
//            text("Now, let's make a button to increment it:")
//            example("""
//                important - button {
//                    text("Increment the counter")
//                    onClick { counter.value++ }
//                }
//                """.trimIndent()) {
//                important - button {
//                    text("Increment the counter")
//                    onClick { counter.value++ }
//                }
//            }
//
//            text("That's the simplest way to start.")
//            space()
//
//            h2("Readable: the root of all KiteUI reactivity")
//            text("A Property implements the Readable interface, which is the root of all the reactive tools in KiteUI.")
//            text("If you have any kind of Readable, you may 'call' it with () within a reactive scope to both get the value and rerun when it changes.")
//            text("However, there are other kinds of readables too.  The most commonly used one is shared:")
//            val otherCounter = Property(0)
//            val combinedCounters = shared { counter() + otherCounter() }
//            code { content = """
//                val otherCounter = Property(0)
//                val combinedCounters = shared { counter() + otherCounter() }
//            """.trimIndent()}
//            text("Now, we can access this calculation like this:")
//            example("""
//                col {
//                    important - button {
//                        text { reactive { content = "Increment the other counter, which is at ${'$'}{otherCounter()}" } }
//                        onClick { otherCounter.value++ }
//                    }
//                    text {
//                        reactive { content = "${'$'}{counter()} + ${'$'}{otherCounter()} = ${'$'}{combinedCounters()}" }
//                    }
//                }
//                """.trimIndent()) {
//                col {
//                    important - button {
//                        text { reactive { content = "Increment the other counter, which is at ${otherCounter()}" } }
//                        onClick { otherCounter.value++ }
//                    }
//                    text {
//                        reactive { content = "${counter()} + ${otherCounter()} = ${combinedCounters()}" }
//                    }
//                }
//            }
//            text("Notice that you can increment either of the two counters now")
        }
    }

}