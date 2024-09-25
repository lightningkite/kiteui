package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.errorText
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.kiteui.views.l2.toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.time.measureTime

@Routable("controls")
object ControlsScreen : Screen {
    override fun ViewWriter.render() {
        class PerfProperty<T>(startValue: T): ImmediateWritable<T> {
            private val listeners = ArrayList<() -> Unit>()
            override var value: T = startValue
            set(value) {
                if(field != value) {
                    field = value
                    measureTime {
                        listeners.invokeAllSafe()
                    }.also { println("Calling listeners took $it") }
                }
            }

            override fun addListener(listener: () -> Unit): () -> Unit {
                listeners.add(listener)
                return {
                    val pos = listeners.indexOfFirst { it === listener }
                    if (pos != -1) {
                        listeners.removeAt(pos)
                    }
                }
            }
            override suspend infix fun set(value: T) {
                this.value = value
            }
        }
        val booleanContent = PerfProperty(true).also {
            it.addListener { println("booleanContent changed!") }
        }
        col {

            h1 { content = "Controls" }

            card - col {
                h2 { content = "Progress Bars" }
                val ratio = Property(0.5f)
                launch {
                    while (true) {
                        delay(100L)
                        ratio.value += 0.01f
                        if (ratio.value > 1f) {
                            ratio.value = 0f
                        }
                    }
                }
                text { ::content { ratio().times(100).roundToInt().toString() + "%" } }
                row {
                    expanding - space {}
                    sizeConstraints(width = 5.rem) - progressBar { ::ratio { ratio() } }
                    card - sizeConstraints(width = 5.rem) - progressBar { ::ratio { ratio() } }
                    important - sizeConstraints(width = 5.rem) - progressBar { ::ratio { ratio() } }
                    critical - sizeConstraints(width = 5.rem) - progressBar { ::ratio { ratio() } }
                    warning - sizeConstraints(width = 5.rem) - progressBar { ::ratio { ratio() } }
                    danger - sizeConstraints(width = 5.rem) - progressBar { ::ratio { ratio() } }
                    expanding - space {}
                } in scrollsHorizontally
            }

            card - col {
                h2 { content = "Buttons" }
                row {
                    expanding - space {}
                    hintPopover {
                        text("Hint")
                    } - centered - important - compact - compact - button {
                        icon {
                            source = Icon.star
                        }
                    }
                    button {
                        onClick {
                            delay(1000L);
                            toast("OK!")
                        }; text {
                        content = "Sample"
                    }; ::enabled { booleanContent() }
                    }
                    card - button {
                        var error = false
                        onClick {
                            throw PlainTextException("We broke!")
//                            error = !error
//                            if(error) throw PlainTextException("We broke!")
//                            else delay(100)
                        }; text {
                        content = "Card"
                    }; ::enabled { booleanContent() }
                    }
                    important - button {
                        onClick { delay(1000L) }; text {
                        content = "Important"
                    }; ::enabled { booleanContent() }
                    }
                    critical - button {
                        onClick { delay(1000L) }; text {
                        content = "Critical"
                    }; ::enabled { booleanContent() }
                    }
                    warning - button {
                        onClick { delay(1000L) }; text {
                        content = "Warning"
                    }; ::enabled { booleanContent() }
                    }
                    danger - button {
                        onClick { delay(1000L) }; text {
                        content = "Danger"
                    }; ::enabled { booleanContent() }
                    }
                    expanding - space {}
                } in scrollsHorizontally
//                errorText()
            }

            col {
                h2 { content = "Toggle Buttons" }
                row {
                    space {} in weight(1f)
                    toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled,
                            "star"
                        ); centered - text { content = "Sample" }
                    }
                    }
                    toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled,
                            "star"
                        ); centered - text { content = "Card" }
                    }
                    } in card
                    toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled,
                            "star"
                        ); centered - text { content = "Important" }
                    }
                    } in important
                    toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled,
                            "star"
                        ); centered - text { content = "Critical" }
                    }
                    } in critical
                    space {} in weight(1f)
                } in scrollsHorizontally
            } in card

            col {
                h2 { content = "Menus" }
                row {
                    space {} in weight(1f)
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                menuButton {
                                    text("1")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A"); onClick { closePopovers() } }
                                            button { text("B"); onClick { closePopovers() } }
                                            button { text("C"); onClick { closePopovers() } }
                                        }
                                    }
                                }
                                menuButton {
                                    text("2")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A"); onClick { closePopovers() } }
                                            button { text("B"); onClick { closePopovers() } }
                                            button { text("C"); onClick { closePopovers() } }
                                        }
                                    }
                                }
                                menuButton {
                                    text("3")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A"); onClick { closePopovers() } }
                                            button { text("B"); onClick { closePopovers() } }
                                            button { text("C"); onClick { closePopovers() } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A"); onClick { closePopovers() } }
                                button { text("B"); onClick { closePopovers() } }
                                button { text("C"); onClick { closePopovers() } }
                            }
                        }
                    } in card
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A"); onClick { closePopovers() } }
                                button { text("B"); onClick { closePopovers() } }
                                button { text("C"); onClick { closePopovers() } }
                            }
                        }
                    } in important
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A"); onClick { closePopovers() } }
                                button { text("B"); onClick { closePopovers() } }
                                button { text("C"); onClick { closePopovers() } }
                            }
                        }
                    } in critical
                    space {} in weight(1f)
                } in scrollsHorizontally
            } in card

            card - col {
                h2 { content = "Switches" }
                col {
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            switch { checked bind booleanContent; }
                        }
                    } in padded
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            switch { checked bind booleanContent; }
                        }
                    } in card
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            switch { checked bind booleanContent; }
                        }
                    } in important
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            switch { checked bind booleanContent; }
                        }
                    } in critical
                }
            }

            col {
                h2 { content = "Checkboxes" }
                col {
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            checkbox { checked bind booleanContent }
                        }
                    } in padded
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            checkbox { checked bind booleanContent }
                        }
                    } in card
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            checkbox { checked bind booleanContent }
                        }
                    } in important
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            checkbox { checked bind booleanContent }
                        }
                    } in critical
                }
            } in card

            col {
                h2 { content = "Radio Buttons" }
                val selected = Property(1)
                col {
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            radioButton { checked bind selected.equalTo(1) }
                        }
                    } in padded
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            radioButton { checked bind selected.equalTo(2) }
                        }
                    } in card
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            radioButton { checked bind selected.equalTo(3) }
                        }
                    } in important
                    stack {
                        row {
                            h3 { content = "Example Setting" } in weight(1f)
                            radioButton { checked bind selected.equalTo(4) }
                        }
                    } in critical
                }
            } in card

            col {
                h2 { content = "Activity Indicators" }
                row {
                    space {} in weight(1f)
                    stack { activityIndicator { } } in padded
                    stack { activityIndicator { } } in card
                    stack { activityIndicator { } } in important
                    stack { activityIndicator { } } in critical
                    stack { activityIndicator { } } in warning
                    stack { activityIndicator { } } in danger
                    space {} in weight(1f)
                } in scrollsHorizontally
            } in card

            col {
                h2 { content = "Drop Downs" }
                val options = shared { listOf("Apple", "Banana", "Crepe") }
                val value = Property("Apple")
                fieldTheme - select { bind(value, data = options, render = { it }) } in padded
                fieldTheme - select { bind(value, data = options, render = { it }) } in card
                fieldTheme - select { bind(value, data = options, render = { it }) } in important
                fieldTheme - select { bind(value, data = options, render = { it }) } in critical
                fieldTheme - select { bind(value, data = options, render = { it }) } in warning
                fieldTheme - select { bind(value, data = options, render = { it }) } in danger
            } in card

            col {
                val date = Property<LocalDate?>(null)
                h2 { content = "Date Fields" }
                text { ::content { date()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
                }
                fieldTheme - localDateField { content bind date }
                fieldTheme - localDateField { content bind date } in card
                fieldTheme - localDateField { content bind date } in important
                fieldTheme - localDateField { content bind date } in critical
            } in card

            col {
                val date = Property<LocalTime?>(null)
                h2 { content = "Time Fields" }
                text { ::content { date()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
                }
                fieldTheme - localTimeField { content bind date }
                fieldTheme - localTimeField { content bind date } in card
                fieldTheme - localTimeField { content bind date } in important
                fieldTheme - localTimeField { content bind date } in critical
            } in card

            col {
                val date = Property<LocalDateTime?>(null)
                h2 { content = "Date Time Fields" }
                text { ::content { date()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
                }
                fieldTheme - localDateTimeField { content bind date }
                fieldTheme - localDateTimeField { content bind date } in card
                fieldTheme - localDateTimeField { content bind date } in important
                fieldTheme - localDateTimeField { content bind date } in critical
            } in card

            col {
                val number = Property<Double?>(1.0)
                h2 { content = "Number Fields" }
                text { ::content { "Value: ${number()}" } }
                fieldTheme - numberField { content bind number }
                fieldTheme - numberField { content bind number } in card
                fieldTheme - numberField { content bind number } in important
                fieldTheme - numberField { content bind number } in critical
            } in card

            col {
                val number = Property(1)
                val text = Property("text")
                h2 { content = "Text Fields" }
                text { ::content { "Text: ${text()}" } }
                fieldTheme - textField { content bind text }
                fieldTheme - textField { content bind text } in card
                fieldTheme - textField { content bind text } in important
                fieldTheme - textField { content bind text } in critical
            } in card

            col {
                val text = Property("Longer form text\n with newlines goes here")
                h2 { content = "Text Areas" }
                fieldTheme - textArea { content bind text }
                fieldTheme - textArea { content bind text } in card
                fieldTheme - textArea { content bind text } in important
                fieldTheme - textArea { content bind text } in critical
            } in card

            col {
                h2 { content = "Images" }
                row {
                    image { source = ImageRemote("https://picsum.photos/seed/0/200/300") } in sizedBox(
                        SizeConstraints(
                            width = 5.rem
                        )
                    )
                    stack {
                        image { source = ImageRemote("https://picsum.photos/seed/1/200/300") } in sizedBox(
                            SizeConstraints(
                                width = 5.rem
                            )
                        )
                    }
                    padded - stack {
                        spacing = 0.px
                        image { source = ImageRemote("https://picsum.photos/seed/2/200/300") } in sizedBox(
                            SizeConstraints(
                                width = 5.rem
                            )
                        )
                    }

                } in scrollsHorizontally
            } in card
        } in scrolls
    }
}