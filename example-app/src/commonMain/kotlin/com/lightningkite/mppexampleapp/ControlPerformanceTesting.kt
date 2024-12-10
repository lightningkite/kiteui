package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0
import kotlin.time.Duration
import kotlin.time.measureTime

@Routable("control-performance-testing")
object ControlPerformanceTesting : Screen {
    enum class BindMode {
        None, Instant, Launch, Reactive
    }
    val bindMode = Property(BindMode.Reactive)

    operator inline fun <V> KMutableProperty0<V>.invoke(default: V, crossinline actionToCalculate: ReactiveContext.()->V) {
        when(bindMode.value) {
            BindMode.None -> {}
            BindMode.Instant -> {
                this@invoke.set(default)
            }
            BindMode.Launch -> CoroutineScopeStack.current().launch {
//                this@invoke.set(actionToCalculate.invoke(this))
            }
            BindMode.Reactive -> {
                CoroutineScopeStack.current().reactiveScope {
                    this@invoke.set(actionToCalculate(this))
                }
            }
        }
    }
    infix fun <V> ImmediateWritable<V>.bind(master: ImmediateWritable<V>) {
        when(bindMode.value) {
            BindMode.None -> {}
            BindMode.Instant -> {
                this@bind.value = master.value
            }
            BindMode.Launch -> CoroutineScopeStack.current().launch {
                with(CoroutineScopeStack.current()) {
                    launch {
                        this@bind.set(master.await())
                    }
                }
            }
            BindMode.Reactive -> {
                with(CoroutineScopeStack.current()) {
                    var setting = false
                    launch {
                        this@bind.set(master.await())
                        master.addListener {
                            if (setting) return@addListener
                            setting = true
                            launch {
                                this@bind.set(master.await())
                            }
                            setting = false
                        }.also { onRemove(it) }
                        this@bind.addListener {
                            if (setting) return@addListener
                            setting = true
                            launch {
                                master.set(this@bind.await())
                            }
                            setting = false
                        }.also { onRemove(it) }
                    }

                }
            }
        }
    }

    override fun ViewWriter.render() {
        val ms = BindMode.values().associateWith { Property(Duration.ZERO) to Property(0) }
        col {
            row {
                for(item in BindMode.values()) {
                    expanding - radioToggleButton {
                        checked bind bindMode.equalTo(item)
                        centered - subtext {
                            ::content { "${item.name} (${(ms[item]!!.first() / ms[item]!!.second().coerceAtLeast(1)).inWholeMilliseconds}ms)" }
                        }
                    }
                }
            }
            expanding - swapView {
                swapping(
                    current = { bindMode() },
                    views = {
                        ms[it]!!.first.value += measureTime {
                            renderMain()
                        }
                        ms[it]!!.second.value += 1
                    }
                )
            }
        }
    }

    fun ViewWriter.renderMain() {
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
                text { (::content)("X%") { ratio().times(100).roundToInt().toString() + "%" } }
                row {
                    expanding - space {}
                    sizeConstraints(width = 5.rem) - progressBar { (::ratio)(0.5f) { ratio() } }
                    card - sizeConstraints(width = 5.rem) - progressBar { (::ratio)(0.5f) { ratio() } }
                    important - sizeConstraints(width = 5.rem) - progressBar { (::ratio)(0.5f) { ratio() } }
                    critical - sizeConstraints(width = 5.rem) - progressBar { (::ratio)(0.5f) { ratio() } }
                    warning - sizeConstraints(width = 5.rem) - progressBar { (::ratio)(0.5f) { ratio() } }
                    danger - sizeConstraints(width = 5.rem) - progressBar { (::ratio)(0.5f) { ratio() } }
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
                            val x = fetch("https://sitedoesnotexist.com")
                            println(x.text())
                        }; text {
                        content = "Sample"
                    }; (::enabled)(false) { booleanContent() }
                    }
                    card - button {
                        onClick { delay(1000L) }; text {
                        content = "Card"
                    }; (::enabled)(false) { booleanContent() }
                    }
                    important - button {
                        onClick { delay(1000L) }; text {
                        content = "Important"
                    }; (::enabled)(false) { booleanContent() }
                    }
                    critical - button {
                        onClick { delay(1000L) }; text {
                        content = "Critical"
                    }; (::enabled)(false) { booleanContent() }
                    }
                    warning - button {
                        onClick { delay(1000L) }; text {
                        content = "Warning"
                    }; (::enabled)(false) { booleanContent() }
                    }
                    danger - button {
                        onClick { delay(1000L) }; text {
                        content = "Danger"
                    }; (::enabled)(false) { booleanContent() }
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
                select { /*bind(value, data = options, render = { it })*/ } in padded
                select { /*bind(value, data = options, render = { it })*/ } in card
                select { /*bind(value, data = options, render = { it })*/ } in important
                select { /*bind(value, data = options, render = { it })*/ } in critical
                select { /*bind(value, data = options, render = { it })*/ } in warning
                select { /*bind(value, data = options, render = { it })*/ } in danger
            } in card

            col {
                val date = Property<LocalDate?>(null)
                h2 { content = "Date Fields" }
                text { (::content)("Set without bind") { date()?.renderToString() ?: "Not Selected" } }
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
                text { (::content)("Set without bind") { date()?.renderToString() ?: "Not Selected" } }
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
                text { (::content)("Set without bind") { date()?.renderToString() ?: "Not Selected" } }
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
                text { (::content)("Set without bind") { "Value: ${number()}" } }
                numberField { content bind number }
                numberField { content bind number } in card
                numberField { content bind number } in important
                numberField { content bind number } in critical
            } in card

            col {
                val number = Property(1)
                val text = Property("text")
                h2 { content = "Text Fields" }
                text { (::content)("Set without bind") { "Text: ${text()}" } }
                textInput { content bind text }
                textInput { content bind text } in card
                textInput { content bind text } in important
                textInput { content bind text } in critical
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