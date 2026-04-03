package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.label
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Routable("docs/reactive-tools")
object ReactiveToolsPage : Page, DocPage {
    override val covers: List<String> = listOf(
        "data",
        "reactivity",
        "remember",
        "MutableRemember"
    )

    fun ElementWriter.CanAddTheme.bufferedNumberInput(sets: MutableReactive<in Double>): Unit {
        fieldTheme.row {
            gap = 0.5.rem
            val buffer = Signal<Double?>(null)
            space(0.5)
            weight(2f).numberInput {
                hint = "New Value"
                content bind buffer
            }
            card.button {
                ::enabled { buffer().let { it != null } }
                centered.text("Set Value")
                onClick {
                    sets set (buffer() ?: return@onClick)
                    buffer.value = null
                }
            }
        }
    }

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Classes and functions to help with reactivity") {
                text("This article will give an overview of all the tools available in KiteUI to handle common reactive use cases. If you haven't already read through the basic reactive documentation, you should do so. ")
                atStart.card.link {
                    centered.text("Reactive Basics")
                    to = { DataPage }
                }
                text("The purpose of this article is to give a list of all of the available tools, so there will be some overlap with the basic reactivity documentation.")

                text("To demonstrate the uses of these various tools we're going to use a basic timer, counter, and number entry. Feel free to mess around with these to get a better feel for the tools.")

                val timer = reactiveProcess {
                    var time = 0
                    while (true) {
                        emit(time++)
                        delay(1000)
                    }
                }
                val counter = Signal(1)
                val inputNumber = Signal<Double?>(null)

                example("""
                    val timer = reactiveProcess {
                        var time = 0
                        while (true) {
                            emit(time++)
                            delay(1000)
                        }
                    }
                    val counter = Signal(1)
                    val inputNumber = Signal<Double?>(null)
                """.trimIndent()) {
                    col {
                        gap = 2.rem
                        row {
                            bold.text("Timer: ")
                            text { ::content { "${timer()} seconds" } }
                        }
                        row {
                            centered.bold.text("Counter: ")

                            fieldTheme.row {
                                gap = 0.5.rem
                                centered.sizeConstraints(width = 5.rem).text {
                                    align = Align.Center
                                    ::content { counter().toString() }
                                }
                                card.button {
                                    centered.text("-")
                                    onClick { counter.value-- }
                                }
                                card.button {
                                    centered.text("+")
                                    onClick { counter.value++ }
                                }
                            }
                        }
                        row {
                            centered.bold.text("Input Number: ")
                            fieldTheme.numberInput {
                                hint = "Type a Number..."
                                content bind inputNumber
                            }
                        }
                    }
                }

                val calculation = remember { timer() * counter() + (inputNumber() ?: 0.0) }
                titledSection("Basics") {
                    text("These tools are the most fundamental, and most frequently used, so you should be familiar with them.")

                    titledSection("Signal") {
                        text("This is the most basic reactive container, or MutableReactive. It holds a value, and notifies it's listeners when that value changes. It's currently being used for both the counter and the number entry. For an additional demonstration we'll add a \"Hello World!\" line to the value stored in a String Signal each time a button is clicked.")

                        example("""
                            val textInput = Signal("")
                            col {
                                expanding.text { ::content { textInput() } }
                                card.button {
                                    centered.text("+\"Hello World!\"")
                                    onClick { textInput.value += "Hello World!" }
                                }
                            }
                        """.trimIndent()) {
                            val textInput = Signal("")
                            col {
                                expanding.scrolling.text { ::content { textInput() } }
                                card.button {
                                    centered.text("+\"Hello World!\"")
                                    onClick { textInput.value += "\nHello World!" }
                                }
                            }
                        }
                    }

                    titledSection("bind") {
                        text("This function is used to bind MutableReactives together, so that they always have the same value. This function is especially useful for input fields, binding the input data to whatever MutableReactive you wish to use throughout your app.")

                        example("""
                            val textInput = Signal("")
                            col {
                                expanding.field("Input") {
                                    textArea {
                                        hint = "Type Something..."
                                        content bind textInput
                                    }
                                }
                                expanding.label {
                                    content = "Stored Value"
                                    text { ::content { textInput() } }
                                }
                            }
                        """.trimIndent()) {
                            val textInput = Signal("")
                            col {
                                expanding.field("Input") {
                                    textArea {
                                        hint = "Type Something..."
                                        content bind textInput
                                    }
                                }
                                expanding.label {
                                    content = "Stored Value"
                                    text { ::content { textInput() } }
                                }
                            }
                        }
                    }

                    titledSection("remember") {
                        text("Remember is essentially a ReactiveContext that returns a result. If any of its dependencies change, then remember will recalculate and notify its listeners of the new result. These dependencies, are, of course, Reactives. It's called remember because it \"remembers\" the result of the calculation, and shares that result among it's listeners, which is obviously much more efficient than redoing the same calculation in multiple places.")

                        example("""
                            val calculation = remember { timer() * counter() + (inputNumber() ?: 0.0) }
                            text { 
                                ::content { "Calculation = ${'$'}{calculation()}" } 
                            }
                            text { 
                                ::content { "Reusing the same calculation in another location: ${'$'}{calculation() % 3}" }
                            }
                        """.trimIndent()) {
                            col {
                                text {
                                    ::content { "Calculation = ${calculation()}" }
                                }
                                text {
                                    ::content { "Reusing the same calculation in another location: ${calculation() % 3}" }
                                }
                            }
                        }
                    }
                }

                titledSection("Using Reactive and MutableReactive outside of reactive scopes") {
                    text("Interacting with Reactive and MutableReactive outside of reactive scopes is easy - use these:")

                    titledSection("Get the current value of a Reactive") {
                        text("Reactives don't necessarily have a value all of the time - they might be currently loading, or perhaps have even errored out while loading or calculating.")
                        text("As such, you must either wait for a value or be prepared for there to be no value.")
                        titledSection("Wait for a value") {
                            example("""
                                text {
                                    launch {
                                        content = calculation().toString()
                                    }
                                }
                            """.trimIndent()) {
                                text {
                                    launch {
                                        content = calculation().toString()
                                    }
                                }
                            }
                        }
                        titledSection("Get the state immediately, if possible.") {
                            text("You can get the current state as it stands using 'state'.")
                            text("Note that the current state may be not ready or errored out.")
                            example("""
                                text {
                                    content = calculation.state.getOrNull().toString()
                                }
                            """.trimIndent()) {
                                text {
                                    content = calculation.state.getOrNull().toString()
                                }
                            }
                        }
                    }
                }

                titledSection("Less-Basic Tools") {
                    text("These tools aren't used as often as the basics, but are extremely helpful in the right situations, and can make your life much easier.")

                    titledSection("mutableRemember") {
                        text("mutableRemember is basically the same thing as remember, but you can set values to override the calculation, and then reset it back to the calculation if needed.")
                        text("To demonstrate, let's use the same arbitrary calculation shown in the remember demonstration, but using mutableRemember instead.")

                        val mutableCalculation = MutableRemember(stopListeningWhenOverridden = false) { timer() * counter() + (inputNumber() ?: 0.0) }
                        example("""
                            val mutableCalculation = mutableRemember { timer() * counter() + (inputNumber() ?: 0.0) }
                            text { 
                                ::content { "mutableCalculation = ${'$'}{mutableCalculation()}" } 
                            }
                        """.trimIndent()) {
                            text {
                                ::content { "mutableCalculation = ${mutableCalculation()}" }
                            }
                        }

                        text("We can see this has the exact same result as remember, but with MutableRemember we now have the ability to override the calculation. Below are controls to set and reset 'mutableCalculation'. Try them out and see the effects.")

                        row {
                            expanding.bufferedNumberInput(mutableCalculation)
                            expanding.card.button {
                                centered.row {
                                    icon(Icon.sync, "Reset")
                                    text("Reset Value")
                                }
                                onClick { mutableCalculation.reset() }
                            }
                        }
                    }

                    titledSection("LateInitSignal") {
                        text("LateInitSignal functions very similarly to a regular Signal, but with one key difference, it doesn't always have a value inside it. If you recall, Reactives are capable of conveying loading and error states. LateInitSignal makes use of this, telling its listeners when it does and doesn't have a value ready.")
                        text("LateInitProperties don't have an initial value, and thus start out being 'NotReady'. Anything that depends on a LateInitSignal will be put into a loading state until you set a value into the LateInitSignal. This makes LateInitSignals very useful for dealing with information that may have long calculation times, or information that is unavailable at the time of declaration.")
                        text("These properties also have an unset() method, which removes any held value and puts the LateInitSignal back into a NotReady state.")

                        example("""
                            val late = LateInitSignal<Double>()
                            col { 
                                row { 
                                    bold.text("LateInitSignal value: ")
                                    text { ::content { late().toString() } }
                                }
                                row { 
                                    weight(2f).bufferedNumberInput(late)
                                    weight(1f).card.button { 
                                        centered.row {
                                            icon(Icon.close, "Unset")
                                            text("Unset")
                                        }
                                        onClick { late.unset() }
                                    }
                                }
                            }
                        """.trimIndent()) {
                            val late = LateInitSignal<Double>()
                            col {
                                sizeConstraints(height = 5.rem).row {
                                    centered.bold.text("LateInitSignal value: ")
                                    centered.expanding.text { ::content { late().toString() } }
                                }
                                row {
                                    weight(2f).bufferedNumberInput(late)
                                    weight(1f).card.button {
                                        centered.row {
                                            icon(Icon.close, "Unset")
                                            text("Unset")
                                        }
                                        onClick { late.unset() }
                                    }
                                }
                            }
                        }
                    }

                    titledSection("withWrite") {
                        text("In cases where you need to create a quick custom MutableReactive, withWrite is your friend.")
                        text("Recall that a MutableReactive is simply a Reactive with a .set(value) method. Reactive.withWrite() adds this method onto any Reactive, using a function you provide, creating a new MutableReactive. This is typically done with remember, but it works with any Reactive.")
                        text("To demonstrate, let's convert the remember calculation above into a MutableReactive<Double>, like MutableRemember did, but instead of overriding the calculation we instead set the value of the number input to get the desired value.")

                        example("""
                            val demo = calculation.withWrite { newValue ->
                                val currentValue = this@withWrite.awaitOnce()
                                val difference = newValue - currentValue
                                inputNumber.value = difference.toString()
                            }
                        """.trimIndent()) {
                            val demo = calculation.withWrite { newValue ->
                                val currentValue = this@withWrite.awaitOnce()
                                val difference = newValue - currentValue
                                inputNumber.value = difference
                            }

                            col {
                                row {
                                    bold.text("Calculation: ")
                                    text { ::content { calculation().toString() } }
                                }
                                row {
                                    centered.bold.text("Input Number: ")
                                    fieldTheme.numberInput {
                                        hint = "Type a Number..."
                                        content bind inputNumber
                                    }
                                }
                                fieldTheme.bufferedNumberInput(inputNumber)
                            }
                        }
                    }
                }

                space(4.0)
            }
        }
    }
}