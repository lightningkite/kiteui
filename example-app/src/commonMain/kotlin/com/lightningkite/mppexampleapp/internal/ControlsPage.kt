package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember
import com.lightningkite.reactive.extensions.equalTo
import com.lightningkite.reactive.extensions.invokeAllSafe
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

@Routable("controls")
object ControlsPage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        class PerfProperty<T>(startValue: T) : MutableReactiveValue<T> {
            private val listeners = ArrayList<() -> Unit>()
            override var value: T = startValue
                set(value) {
                    if (field != value) {
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

        scrolling.col {
            debugName = "Controls"

            h1 { content = "Controls" }

            card.col {
                h2 { content = "Progress Bars" }
                val ratio = Signal(0.5f)
                var inc = 0.01f
                launch {
                    while (true) {
                        delay(100L)
                        ratio.value += inc
                        if (ratio.value <= 0f || ratio.value >= 1f) {
                            inc = -inc
                        }
                    }
                }

                text { ::content { ratio().times(100).roundToInt().toString() + "%" } }

                scrollingHorizontally.row {
                    expanding.space()
                    sizeConstraints(width = 5.rem).run {
                        progressBar(ratio)
                        card.progressBar(ratio)
                        important.progressBar(ratio)
                        critical.progressBar(ratio)
                        warning.progressBar(ratio)
                        danger.progressBar(ratio)
                    }
                    expanding.space()
                }
            }

            card.col {
                h2 { content = "Buttons" }
                scrollingHorizontally.row {
                    expanding.space()
                    centered.important.compact.compact.hintPopover {
                        text("Hint")
                    }.button {
                        icon {
                            source = Icon.star
                        }
                    }
                    button {
                        onClick {
                            delay(1000L)
                            context.toast("OK! RUN!", 3.seconds)
                        }
                        text { content = "Sample" }
                        ::enabled bind booleanContent
                    }
                    card.button {
                        var error = false
                        onClick {
                            delay(100)
                            error = !error
                            if (error) fetch("https://lightningkite.com/wrong-page").let {
                                if (!it.ok) throw Exception(it.text())
                            }
                            else delay(100)
                        }
                        text { content = "Card" }
                        ::enabled { booleanContent() }
                    }
                    important.button {
                        text("Important")

                        onClick { delay(1000L) }
                        ::enabled { booleanContent() }
                    }
                    critical.button {
                        text("Critical")

                        onClick { delay(1000L) }
                        ::enabled { booleanContent() }
                    }
                    warning.button {
                        text("Warning")

                        onClick { delay(1000L) }
                        ::enabled { booleanContent() }
                    }
                    danger.button {
                        text("Danger")

                        onClick { delay(1000L) }
                        ::enabled { booleanContent() }
                    }
                    expanding.space()
                }
//                errorText()
            }

            card.col {
                h2 { content = "Toggle Buttons" }
                scrollingHorizontally.row {
                    expanding.space()
                    toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled, "star"
                        ); centered.text { content = "Sample" }
                    }
                    }
                    card.toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled, "star"
                        ); centered.text { content = "Card" }
                    }
                    }
                    important.toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled, "star"
                        ); centered.text { content = "Important" }
                    }
                    }
                    critical.toggleButton {
                        checked bind booleanContent; row {
                        icon(
                            Icon.starFilled, "star"
                        ); centered.text { content = "Critical" }
                    }
                    }
                    expanding.space()
                }
            }

            card.col {
                h2 { content = "Menus" }
                scrollingHorizontally.row {
                    expanding.space()
                    menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                menuButton {
                                    text("1")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A"); onClick { context.closePopovers() } }
                                            button { text("B"); onClick { context.closePopovers() } }
                                            button { text("C"); onClick { context.closePopovers() } }
                                        }
                                    }
                                }
                                menuButton {
                                    text("2")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A"); onClick { context.closePopovers() } }
                                            button { text("B"); onClick { context.closePopovers() } }
                                            button { text("C"); onClick { context.closePopovers() } }
                                        }
                                    }
                                }
                                menuButton {
                                    text("3")
                                    preferredDirection = PopoverPreferredDirection.rightTop
                                    opensMenu {
                                        col {
                                            button { text("A"); onClick { context.closePopovers() } }
                                            button { text("B"); onClick { context.closePopovers() } }
                                            button { text("C"); onClick { context.closePopovers() } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    card.menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A"); onClick { context.closePopovers() } }
                                button { text("B"); onClick { context.closePopovers() } }
                                button { text("C"); onClick { context.closePopovers() } }
                            }
                        }
                    }
                    important.menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A"); onClick { context.closePopovers() } }
                                button { text("B"); onClick { context.closePopovers() } }
                                button { text("C"); onClick { context.closePopovers() } }
                            }
                        }
                    }
                    critical.menuButton {
                        text("Menu")
                        opensMenu {
                            col {
                                button { text("A"); onClick { context.closePopovers() } }
                                button { text("B"); onClick { context.closePopovers() } }
                                button { text("C"); onClick { context.closePopovers() } }
                            }
                        }
                    }
                    expanding.space()
                }
            }

            card.col {
                h2 { content = "Switches" }
                col {
                    padded.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            switch { checked bind booleanContent; }
                        }
                    }
                    card.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            switch { checked bind booleanContent; }
                        }
                    }
                    important.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            switch { checked bind booleanContent; }
                        }
                    }
                    critical.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            switch { checked bind booleanContent; }
                        }
                    }
                }
            }

            card.col {
                h2 { content = "Checkboxes" }
                col {
                    padded.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            checkbox { checked bind booleanContent }
                        }
                    }
                    card.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            checkbox { checked bind booleanContent }
                        }
                    }
                    important.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            checkbox { checked bind booleanContent }
                        }
                    }
                    critical.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            checkbox { checked bind booleanContent }
                        }
                    }
                }
            }

            card.col {
                h2 { content = "Radio Buttons" }
                val selected = Signal(1)
                col {
                    padded.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            radioButton { checked bind selected.equalTo(1) }
                        }
                    }
                    card.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            radioButton { checked bind selected.equalTo(2) }
                        }
                    }
                    important.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            radioButton { checked bind selected.equalTo(3) }
                        }
                    }
                    critical.frame {
                        row {
                            expanding.h3 { content = "Example Setting" }
                            radioButton { checked bind selected.equalTo(4) }
                        }
                    }
                }
            }

            card.col {
                h2 { content = "Activity Indicators" }
                scrollingHorizontally.row {
                    expanding.space()
                    padded.frame { activityIndicator() }
                    card.frame { activityIndicator() }
                    important.frame { activityIndicator() }
                    critical.frame { activityIndicator() }
                    warning.frame { activityIndicator() }
                    danger.frame { activityIndicator() }
                    expanding.space()
                }
            }

            card.col {
                h2 { content = "Drop Downs" }
                val options = remember { listOf("Apple", "Banana", "Crepe") }
                val value = Signal("Banana")

                for (theme in listOf(padded, card, important, critical, warning, danger))
                    theme.fieldTheme.select {
                        bind(value, data = options) { it }
                    }
            }

            card.col {
                val date = Signal<LocalDate?>(null)
                h2 { content = "Date Fields" }
                text { ::content { date()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
                }
                fieldTheme.localDateField { content bind date }
                card.fieldTheme.localDateField { content bind date }
                important.fieldTheme.localDateField { content bind date }
                critical.fieldTheme.localDateField { content bind date }
            }

            card.col {
                val date = Signal<LocalTime?>(null)
                h2 { content = "Time Fields" }
                text { ::content { date()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time }
                }
                fieldTheme.localTimeField { content bind date }
                card.fieldTheme.localTimeField { content bind date }
                important.fieldTheme.localTimeField { content bind date }
                critical.fieldTheme.localTimeField { content bind date }
            }

            card.col {
                val date = Signal<LocalDateTime?>(null)
                h2 { content = "Date Time Fields" }
                text { ::content { date()?.renderToString() ?: "Not Selected" } }
                button {
                    text("Set to now")
                    onClick { date set Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
                }
                fieldTheme.localDateTimeField { content bind date }
                card.fieldTheme.localDateTimeField { content bind date }
                important.fieldTheme.localDateTimeField { content bind date }
                critical.fieldTheme.localDateTimeField { content bind date }
            }

            card.col {
                val number = Signal<Double?>(1.0)
                h2 { content = "Number Fields" }
                text { ::content { "Value: ${number()}" } }
                fieldTheme.numberInput { content bind number }
                card.fieldTheme.numberInput { content bind number }
                important.fieldTheme.numberInput { content bind number }
                critical.fieldTheme.numberInput { content bind number }
            }

            card.col {
                val number = Signal(1)
                val text = Signal("text")
                h2 { content = "Text Fields" }
                text { ::content { "Text: ${text()}" } }
                fieldTheme.textInput { content bind text }
                card.fieldTheme.textInput { content bind text }
                important.fieldTheme.textInput { content bind text }
                critical.fieldTheme.textInput { content bind text }
            }

            card.col {
                val text = Signal("Longer form text\n with newlines goes here")
                h2 { content = "Text Areas" }
                fieldTheme.textArea { content bind text }
                card.fieldTheme.textArea { content bind text }
                important.fieldTheme.textArea { content bind text }
                critical.fieldTheme.textArea { content bind text }
            }

            card.col {
                h2 { content = "Images" }
                scrollingHorizontally.row {
                    sizedBox(
                        SizeConstraints(
                            width = 5.rem
                        )
                    ).image { source = ImageRemote("https://picsum.photos/seed/0/200/300") }
                    frame {
                        sizedBox(
                            SizeConstraints(
                                width = 5.rem
                            )
                        ).image { source = ImageRemote("https://picsum.photos/seed/1/200/300") }
                    }
                    padded.frame {
                        sizedBox(
                            SizeConstraints(
                                width = 5.rem
                            )
                        ).image { source = ImageRemote("https://picsum.photos/seed/2/200/300") }
                    }
                }
            }
        }
    }
}