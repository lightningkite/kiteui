package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.atStart
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.danger
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.fieldTheme
import com.lightningkite.kiteui.views.important
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay

@Routable("docs/data")
object DataPage : DocPage {

    override val title: Reactive<String>
        get() = Constant("Data: How to do Reactivity in KiteUI")

    override val covers: List<String> = listOf(
        "data",
        "Signal",
        "PersistentProperty",
        "remember",
        "reactiveScope",
        "reactivity",
        "::prop { }",
        "launch"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.article {
            val secondsElapsed = reactiveProcess<Int> {
                // Starts out as 'loading'
                var n = 0
                while (true) {
                    delay(1000)
                    emit(n++)
                }
            }

            titledSection("Data: How to do Reactivity in KiteUI") {
                titledSection("Reactive") {
                    text("Reactive is the root of reactivity in KiteUI. Reactive data is data you can observe and be notified when its state changes.")
                    text("The simplest example of a Reactive is 'Signal', which directly contains a value we can change.")
                    titledSection("Signal") {
                        code {
                            content = """
                                val counter = Signal<Int>(0)
                                val callToStopListening = counter.addListener { 
                                    println("Value has changed to ${'$'}{counter.value}")
                                }
                                counter.value = 10
                                callToStopListening()
                            """.trimIndent()
                        }
                        val counter = Signal<Int>(0)
                        val callToStopListening = counter.addListener {
                            println("Value has changed to ${counter.value}")
                        }
                        counter.value = 10
                        callToStopListening()
                        counter.value = 10

                        text("The above should have printed 'Value has changed to 10' in your console.")
                        text("Because we called the function returned from addListener, further changes won't print.")
                    }

                    titledSection("reactiveProcess") {
                        text("Other kinds of Reactives can also contain loading and error states, which can be displayed in your UI with no modifications.")
                        text("Another example is 'reactiveProcess', which runs a Kotlin Coroutine that emits values.  For example, let's create a counter for seconds since opening the screen:")
                        atStart.card.externalLink { subtext("What is a coroutine?"); to = "https://kotlinlang.org/docs/coroutines-overview.html"; newTab = true }
                        code {
                            content = """
                                val secondsElapsed = reactiveProcess<Int> {
                                    // Starts out as 'loading'
                                    var n = 0
                                    while(true) {
                                        delay(1000)
                                        emit(n++)
                                    }
                                }
                                val listenerRemoveHandle = secondsElapsed.addListener { println("secondsElapsed.state: ${'$'}{secondsElapsed.state}") }
                                // You need to clean up your listeners!
                                // Calling the returned lambda allows you to do so.
                                // Here, we ask KiteUI to run it when the view is removed.
                                onRemove(listenerRemoveHandle)
                            """.trimIndent()
                        }
                        val listenerRemoveHandle = secondsElapsed.addListener { println("secondsElapsed.state: ${secondsElapsed.state}") }
                        onRemove(listenerRemoveHandle)
                        text("This should print an increasing number every second.")
                    }
                }

                titledSection("Connecting to UI") {

                    titledSection("Painful manual connecting to UI") {
                        text("We could manually connect a view to this data like so:")
                        example(
                            """
                                text {
                                    val endListening = secondsElapsed.addListener { 
                                        content = secondsElapsed.state.getOrNull()?.toString() ?: "Not ready"
                                    }
                                    onRemove(endListening)
                                }
                            """.trimIndent()
                        ) {
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
                    }

                    titledSection("Plain Reactive Scope") {
                        text("A reactive scope, or reactive context, is a block of code that reruns whenever one of its dependencies change. Inside a reactive scope, one may 'call' a Reactive by using parentheses afterwards.  An example:")
                        example(
                            """
                                text {
                                    reactive { content = secondsElapsed().toString() }
                                }
                            """.trimIndent()
                        ) {
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
                        example(
                            """
                                text {
                                    ::content { secondsElapsed().toString() }
                                }
                            """.trimIndent()
                        ) {
                            text {
                                ::content { secondsElapsed().toString() }
                            }
                        }
                        text("This is generally the best way to bind data to views.")
                    }

                    titledSection("Don't create views in reactive scopes") {
                        text("Unless you know what you're doing, you should NOT create views in a reactive scope.  This code:")
                        code {
                            content = """
                                frame {
                                    reactive {
                                        if(secondsElapsed() % 2 == 0) {
                                            text("We're on an even second")
                                        } else {
                                            text("We're on an odd second")
                                        }
                                    }
                                }
                            """.trimIndent()
                        }
                        text("would insert a new piece of text EVERY SECOND, which is most certainly not what you intend.")
                        text("You should prefer changing properties on views, and if that's not possible, you should prefer hiding or showing views when necessary.")
                        example(
                            """
                                col {
                                    onlyWhen { secondsElapsed() % 2 == 0 }.text("We're on an even second")
                                    onlyWhen { secondsElapsed() % 2 != 0 }.text("We're on an odd second")
                                }
                            """.trimIndent()
                        ) {
                            col {
                                shownWhen { secondsElapsed() % 2 == 0 }.text("We're on an even second")
                                shownWhen { secondsElapsed() % 2 != 0 }.text("We're on an odd second")
                            }
                        }
                        text("This is much easier on the DOM, so you'll get better performance too.")
                        text("If for some reason you truly need to dynamically create a new, you can use a swapView.")
                    }

                    titledSection("remember") {
                        text("You can create a Reactive out of a reactive scope using 'remember', like this:")
                        val counter = Signal(0)
                        val secondsElapsedPlusCounter = remember { secondsElapsed() + counter() }
                        code {
                            content = """
                                val counter = Signal(0)
                                val secondsElapsedPlusCounter = remember { secondsElapsed() + counter() }
                            """.trimIndent()
                        }
                        text("This is particularly useful for creating a Reactive whose value is calculated from other Reactives.")
                        text("'remember' is short for 'remember calculation'.  If multiple people listen to this property, they share the calculated result.")
                        text("'remember' is also lazy.it won't begin calculating until someone is listing.  It's safe to use 'remember' at the top level for this reason!")
                        danger.text("You should not use a Signal to hold a view of another Signal.  Use 'remember' instead.")
                        text("Now, we can access this calculation like this:")
                        example(
                            """
                                col {
                                    important.button {
                                        text { ::content { "Increment the other counter, which is at ${'$'}{otherCounter()}" } }
                                        onClick { counter.value++ }
                                    }
                                    text {
                                        ::content { "${'$'}{secondsElapsed()} + ${'$'}{counter()} = ${'$'}{secondsElapsedPlusCounter()}" }
                                    }
                                }
                        """.trimIndent()
                        ) {
                            col {
                                important.button {
                                    text { ::content { "Increment the other counter, which is at ${counter()}" } }
                                    onClick { counter.value++ }
                                }
                                text {
                                    ::content { "${secondsElapsed()} + ${counter()} = ${secondsElapsedPlusCounter()}" }
                                }
                            }
                        }
                        text("Notice that you can increment either of the two counters now.")
                    }
                }

                titledSection("MutableReactive") {
                    text("A MutableReactive is a Reactive that also has a suspending 'set' function.")
                    text("A Signal is also a MutableReactive.")
                    text("The purpose of MutableReactive is to create bidirectional data flow, for example with a text input.")
                    text("For this section, let's define a counter property:")
                    code {
                        content = """
                            val counter = Signal(0)
                        """.trimIndent()
                    }
                    val counter = Signal(0)

                    titledSection("bind") {
                        text("The 'bind' function connects MutableReactives together such that the one on the right always serves the one on the left.")
                        text("For a practical example, let's look at connecting a text input to a Signal.  TextInput's 'content' field is a MutableReactive<String>.")
                        val emailAddress = Signal("test@test.com")
                        example(
                            """
                                val emailAddress = Signal("test@test.com")
                                col { 
                                    textInput { content bind emailAddress }
                                    text { ::content { emailAddress() } }
                                }
                            """.trimIndent()
                        ) {
                            col {
                                fieldTheme.textInput { content bind emailAddress }
                                card.text { ::content { emailAddress() } }
                            }
                        }
                        text("Try editing the email address and you'll see the value immediately reflected in the text below it.")
                    }

                    titledSection("withWrite") {
                        text("Sometimes we want a field to edit a view on some other data.  The two tools for doing this: combining 'remember' and 'withWrite', and 'lens'.  Let's start with 'withWrite'.")
                        text("'withWrite' turns any Reactive into a MutableReactive with the set command being implemented with whatever action you pass in.")
                        text("As an example, let's take create a counter and edit it in a text field.")
                        val counterAsString = remember { counter().toString() }.withWrite {
                            val toSet = it.toIntOrNull()
                            if (toSet != null) counter.value = toSet
                        }
                        example(
                            """
                                val counterAsString = remember { counter().toString() }.withWrite {
                                    val toSet = it.toIntOrNull()
                                    if(toSet != null) counter.value = toSet
                                }
                                col { 
                                    fieldTheme.textInput { content bind counterAsString }
                                    card.text { ::content { counterAsString() } }
                                    card.button { text("Increment"); onClick { counter.value++ } }
                                }
                            """.trimIndent()
                        ) {
                            col {
                                fieldTheme.textInput { content bind counterAsString }
                                card.text { ::content { counterAsString() } }
                                card.button { text("Increment"); onClick { counter.value++ } }
                            }
                        }
                        text("It should have started out as '0', but we can increment it with the button and directly edit the value using the field.")
                    }

                    titledSection("lens") {
                        text("Lens allows you to create a view on other data too, assuming the calculation doesn't depend on any other data.  It has a slight performance advantage over the remember/withWrite combination above.")
                        val counterAsString2 = counter.lens(get = { it.toString() }, set = { it.toIntOrNull() ?: 0 })
                        example(
                            """
                                val counterAsString2 = counter.lens(get = { it.toString() }, set = { it.toIntOrNull() ?: 0 }) 
                                col { 
                                    fieldTheme.textInput { content bind counterAsString2 }
                                    card.text { ::content { counterAsString2() } }
                                    card.button { text("Increment"); onClick { counter.value++ } }
                                }
                            """.trimIndent()
                        ) {
                            col {
                                fieldTheme.textInput { content bind counterAsString2 }
                                card.text { ::content { counterAsString2() } }
                                card.button { text("Increment"); onClick { counter.value++ } }
                            }
                        }
                    }
                }

//                titledSection("Interaction with Coroutines") {
//                    atStart - card - externalLink { subtext("What is a coroutine?"); to = "https://kotlinlang.org/docs/coroutines-overview.html"; newTab = true }
//                    text("It's very common to display data calculated via a suspending calculation.  To support this, there are multiple tools to bridge the gap.")
//                    space()
//
//                    titledSection("sharedSuspending") {
//                        text("You can blend the reactive and suspending worlds using sharedSuspending.")
//                        val sharedSuspendingExample = sharedSuspending {
//                            delay(1.seconds)
//                            counter()
//                        }
//                        example(
//                            """
//            val sharedSuspendingExample = sharedSuspending {
//                counter()
//            }
//            """.trimIndent()
//                        ) {
//                            col {
//                                text("This is our counter value, but delayed by one second:")
//                                text { ::content { sharedSuspendingExample().toString() } }
//                            }
//                        }
//                        text("You'll notice that it shows a loading state while the calculation is going.")
//                    }
//                }

                // TODO: Interaction with coroutines
                // TODO: Incomplete calculation state
                // TODO: Error states

                space(5.0)
            }
        }
    }
}