package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.icon
import kotlinx.datetime.*
import kotlin.math.roundToInt

@Routable("controls")
object ControlsScreen : Screen {
    override fun ViewWriter.render() {
        val booleanContent = Property(true).also {
            it.addListener { println("booleanContent changed!") }
        }
        col {

            h1 { content = "Controls" }

            card - col {
                h2 { content = "Progress Bars" }
                val ratio = Property(0.5f)
                launch {
                    while (true) {
                        delay(20L)
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
                        onClick { delay(1000L) }; text {
                        content = "Sample"
                    }; ::enabled { booleanContent.await() }
                    }
                    card - button {
                        onClick { delay(1000L) }; text {
                        content = "Card"
                    }; ::enabled { booleanContent.await() }
                    }
                    important - button {
                        onClick { delay(1000L) }; text {
                        content = "Important"
                    }; ::enabled { booleanContent.await() }
                    }
                    critical - button {
                        onClick { delay(1000L) }; text {
                        content = "Critical"
                    }; ::enabled { booleanContent.await() }
                    }
                    warning - button {
                        onClick { delay(1000L) }; text {
                        content = "Warning"
                    }; ::enabled { booleanContent.await() }
                    }
                    danger - button {
                        onClick { delay(1000L) }; text {
                        content = "Danger"
                    }; ::enabled { booleanContent.await() }
                    }
                    expanding - space {}
                } in scrollsHorizontally
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
                                            button { text("A") }
                                            button { text("B") }
                                            button { text("C") }
                                        }
                                    }
                                }
                                menuButton {
                                    text("2")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A") }
                                            button { text("B") }
                                            button { text("C") }
                                        }
                                    }
                                }
                                menuButton {
                                    text("3")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A") }
                                            button { text("B") }
                                            button { text("C") }
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
                                button { text("A") }
                                button { text("B") }
                                button { text("C") }
                            }
                        }
                    } in card
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A") }
                                button { text("B") }
                                button { text("C") }
                            }
                        }
                    } in important
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A") }
                                button { text("B") }
                                button { text("C") }
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
                select { bind(value, data = options, render = { it }) } in padded
                select { bind(value, data = options, render = { it }) } in card
                select { bind(value, data = options, render = { it }) } in important
                select { bind(value, data = options, render = { it }) } in critical
                select { bind(value, data = options, render = { it }) } in warning
                select { bind(value, data = options, render = { it }) } in danger
            } in card

            col {
                val date = Property<LocalDate?>(null)
                h2 { content = "Date Fields" }
                text { ::content { date.await()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
                }
                localDateField { content bind date }
                localDateField { content bind date } in card
                localDateField { content bind date } in important
                localDateField { content bind date } in critical
            } in card

            col {
                val date = Property<LocalTime?>(null)
                h2 { content = "Time Fields" }
                text { ::content { date.await()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
                }
                localTimeField { content bind date }
                localTimeField { content bind date } in card
                localTimeField { content bind date } in important
                localTimeField { content bind date } in critical
            } in card

            col {
                val date = Property<LocalDateTime?>(null)
                h2 { content = "Date Time Fields" }
                text { ::content { date.await()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
                }
                localDateTimeField { content bind date }
                localDateTimeField { content bind date } in card
                localDateTimeField { content bind date } in important
                localDateTimeField { content bind date } in critical
            } in card

            col {
                val number = Property<Double?>(1.0)
                h2 { content = "Number Fields" }
                text { ::content { "Value: ${number.await()}" } }
                numberField { content bind number }
                numberField { content bind number } in card
                numberField { content bind number } in important
                numberField { content bind number } in critical
            } in card

            col {
                val number = Property(1)
                val text = Property("text")
                h2 { content = "Text Fields" }
                text { ::content { "Text: ${text.await()}" } }
                textField { content bind text }
                textField { content bind text } in card
                textField { content bind text } in important
                textField { content bind text } in critical
            } in card

            col {
                val text = Property("Longer form text\n with newlines goes here")
                h2 { content = "Text Areas" }
                textArea { content bind text }
                textArea { content bind text } in card
                textArea { content bind text } in important
                textArea { content bind text } in critical
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