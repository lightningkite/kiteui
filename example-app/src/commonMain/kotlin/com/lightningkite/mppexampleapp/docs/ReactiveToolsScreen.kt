package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.titledSection
import kotlinx.coroutines.delay

@Routable("docs/reactive-tools")
object ReactiveToolsScreen : Screen, DocScreen {
    override val covers: List<String> = listOf(
        "data",
        "reactivity",
        "shared",
        "LazyProperty"
    )

    fun ViewWriter.bufferedNumberInput(sets: Writable<in Double>) {
        fieldTheme - row {
            spacing = 0.5.rem
            val buffer = Property<Double?>(null)
            space(0.5)
            weight(2f) - numberInput {
                hint = "New Value"
                content bind buffer
            }
            card - button {
                spacing = 0.5.rem
                ::enabled { buffer().let { it != null } }
                centered - text("Set Value")
                onClick {
                    sets set (buffer() ?: return@onClick)
                    buffer.value = null
                }
            }
        }
    }

    override fun ViewWriter.render() {
        article {
            titledSection("Classes and functions to help with reactivity") {
                text("This article will give an overview of all the tools available in KiteUI to handle common reactive use cases. If you haven't already read through the basic reactive documentation, you should do so. ")
                atStart - card - link {
                    centered - text("Reactive Basics")
                    to = { DataScreen }
                }
                text("The purpose of this article is to give a list of all of the available tools, so there will be some overlap with the basic reactivity documentation.")

                text("To demonstrate the uses of these various tools we're going to use a basic timer, counter, and number entry. Feel free to mess around with these to get a better feel for the tools.")

                val timer = sharedProcess {
                    var time = 0
                    while (true) {
                        emit(time++)
                        delay(1000)
                    }
                }
                val counter = Property(1)
                val inputNumber = Property<Double?>(null)

                example("""
                    val timer = sharedProcess {
                        var time = 0
                        while (true) {
                            emit(time++)
                            delay(1000)
                        }
                    }
                    val counter = Property(1)
                    val inputNumber = Property<Double?>(null)
                """.trimIndent()) {
                    col {
                        spacing = 2.rem
                        row {
                            bold - text("Timer: ")
                            text { ::content { "${timer()} seconds" } }
                        }
                        row {
                            centered - bold - text("Counter: ")

                            fieldTheme - row {
                                spacing = 0.5.rem
                                centered - sizeConstraints(width = 5.rem) - text {
                                    align = Align.Center
                                    ::content { counter().toString() }
                                }
                                card - button {
                                    spacing = 0.5.rem
                                    centered - text("-")
                                    onClick { counter.value-- }
                                }
                                card - button {
                                    spacing = 0.5.rem
                                    centered - text("+")
                                    onClick { counter.value++ }
                                }
                            }
                        }
                        row {
                            centered - bold - text("Input Number: ")
                            fieldTheme - numberInput {
                                hint = "Type a Number..."
                                content bind inputNumber
                            }
                        }
                    }
                }

                val calculation = shared { timer() * counter() + (inputNumber() ?: 0.0) }
                titledSection("Basics") {
                    text("These tools are the most fundamental, and most frequently used, so you should be familiar with them.")

                    titledSection("Property") {
                        text("This is the most basic reactive container, or Writable. It holds a value, and notifies it's listeners when that value changes. It's currently being used for both the counter and the number entry. For an additional demonstration we'll add a \"Hello World!\" line to the value stored in a String Property each time a button is clicked.")

                        example("""
                            val textInput = Property("")
                            col {
                                expanding - text { ::content { textInput() } }
                                card - button {
                                    centered - text("+\"Hello World!\"")
                                    onClick { textInput.value += "Hello World!" }
                                }
                            }
                        """.trimIndent()) {
                            val textInput = Property("")
                            col {
                                expanding - scrolls - text { ::content { textInput() } }
                                card - button {
                                    centered - text("+\"Hello World!\"")
                                    onClick { textInput.value += "\nHello World!" }
                                }
                            }
                        }
                    }

                    titledSection("bind") {
                        text("This function is used to bind Writables together, so that they always have the same value. This function is especially useful for input fields, binding the input data to whatever Writable you wish to use throughout your app.")

                        example("""
                            val textInput = Property("")
                            col {
                                expanding - field("Input") {
                                    textArea {
                                        hint = "Type Something..."
                                        content bind textInput
                                    }
                                }
                                expanding - label {
                                    content = "Stored Value"
                                    text { ::content { textInput() } }
                                }
                            }
                        """.trimIndent()) {
                            val textInput = Property("")
                            col {
                                expanding - field("Input") {
                                    expanding - textArea {
                                        hint = "Type Something..."
                                        content bind textInput
                                    }
                                }
                                expanding - label {
                                    content = "Stored Value"
                                    text { ::content { textInput() } }
                                }
                            }
                        }
                    }

                    titledSection("shared") {
                        text("Shared is essentially a dependency tracking calculation. If any of its dependencies change, then shared will recalculate and notify its listeners of the new result. These dependencies, are, of course, Readables. It's called shared because it \"shares\" its calculation with all of its listeners, which is obviously much more efficient than redoing the same calculation in multiple places.")

                        example("""
                        val calculation = shared { timer() * counter() + (inputNumber() ?: 0.0) }
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

                titledSection("Less-Basic Tools") {
                    text("These tools aren't used as often as the basics, but are extremely helpful in the right situations, and can make your life much easier.")

                    titledSection("LazyProperty") {
                        text("Imagine a situation where you need to calculate an initial value, but later on you will want to override that initial value for something else manually. This is where LazyProperty comes in.")
                        text("LazyProperty is basically the same thing as shared, but you can set values to override the calculation, and then reset it back to the calculation if needed.")
                        text("To demonstrate, let's use the same arbitrary calculation shown in the shared demonstration, but using LazyProperty instead.")

                        val calculationProperty = LazyProperty(stopListeningWhenOverridden = false) { timer() * counter() + (inputNumber() ?: 0.0) }
                        example("""
                            val calculationProperty = LazyProperty { timer() * counter() + (inputNumber() ?: 0.0) }
                            text { 
                                ::content { "calculationProperty = ${'$'}{calculationProperty()}" } 
                            }
                        """.trimIndent()) {
                            text {
                                ::content { "calculationProperty = ${calculationProperty()}" }
                            }
                        }

                        text("We can see this has the exact same result as shared, but with LazyProperty we now have the ability to override the calculation. Below are controls to set and reset 'calculationProperty'. Try them out and see the effects.")

                        row {
                            expanding - bufferedNumberInput(calculationProperty)
                            expanding - card - button {
                                centered - row {
                                    icon(Icon.sync, "Reset")
                                    text("Reset Value")
                                }
                                onClick { calculationProperty.reset() }
                            }
                        }
                    }

                    titledSection("LateInitProperty") {
                        text("LateInitProperty functions very similarly to a regular Property, but with one key difference, it doesn't always have a value inside it. If you recall, Readables are capable of conveying loading and error states. LateInitProperty makes use of this, telling its listeners when it does and doesn't have a value ready.")
                        text("LateInitProperties don't have an initial value, and thus start out being 'NotReady'. Anything that depends on a LateInitProperty will be put into a loading state until you set a value into the LateInitProperty. This makes LateInitProperties very useful for dealing with information that may have long calculation times, or information that is unavailable at the time of declaration.")
                        text("These properties also have an unset() method, which removes any held value and puts the LateInitProperty back into a NotReady state.")

                        example("""
                            val late = LateInitProperty<Double>()
                            col { 
                                row { 
                                    bold - text("LateInitProperty value: ")
                                    text { ::content { late().toString() } }
                                }
                                row { 
                                    weight(2f) - bufferedNumberInput(late)
                                    weight(1f) - card - button { 
                                        centered - row {
                                            icon(Icon.close, "Unset")
                                            text("Unset")
                                        }
                                        onClick { late.unset() }
                                    }
                                }
                            }
                        """.trimIndent()) {
                            val late = LateInitProperty<Double>()
                            col {
                                sizeConstraints(height = 5.rem) - row {
                                    centered - bold - text("LateInitProperty value: ")
                                    centered - expanding -  text { ::content { late().toString() } }
                                }
                                row {
                                    weight(2f) - bufferedNumberInput(late)
                                    weight(1f) - card - button {
                                        centered - row {
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
                        text("In cases where you need to create a quick custom Writable, withWrite is your friend.")
                        text("Recall that a Writable is simply a Readable with a .set(value) method. Readable.withWrite() adds this method onto any Readable, using a function you provide, creating a new Writable. This is typically done with shared, but it works with any Readable.")
                        text("To demonstrate, let's convert the shared calculation above into a Writable<Double>, like LazyProperty did, but instead of overriding the calculation we instead set the value of the number input to get the desired value.")

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
                                    bold - text("Calculation: ")
                                    text { ::content { calculation().toString() } }
                                }
                                row {
                                    centered - bold - text("Input Number: ")
                                    fieldTheme - numberInput {
                                        hint = "Type a Number..."
                                        content bind inputNumber
                                    }
                                }
                                fieldTheme - bufferedNumberInput(inputNumber)
                            }
                        }
                    }
                }


                titledSection("Lensing") {

                }

                space(4.0)
            }
        }
    }
}