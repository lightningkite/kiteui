package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.locale.RenderSize
import com.lightningkite.kiteui.locale.renderToString
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.atBottom
import com.lightningkite.kiteui.views.atBottomEnd
import com.lightningkite.kiteui.views.atEnd
import com.lightningkite.kiteui.views.atStart
import com.lightningkite.kiteui.views.atTop
import com.lightningkite.kiteui.views.atTopStart
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.dynamicTheme
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.mppexampleapp.internal.RootPage
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import kotlin.math.PI
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

@Routable("docs/available-views")
object CheatSheet : DocPage {
    override val covers: List<String>
        get() = listOf("Available KiteUI Views")

    data class ExampleEntry(
        val name: String,
        val tags: Set<String> = setOf()
    )

    data object LinkSemantic : Semantic("link") {
        override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
            semanticOverrides = SemanticOverrides(
                HoverSemantic.override {
                    it.withoutBack(
                        font = it.font.copy(underline = true)
                    )
                }
            )
        )
    }

    val known = HashSet<ExampleEntry>()
    val jump = Signal<ExampleEntry?>(null)
    fun ViewWriter.example(
        name: String,
        description: String,
        code: String,
        references: Set<ExampleEntry> = emptySet(),
        result: RowOrCol.() -> Unit
    ): Unit {
        val e = ExampleEntry(name)
        known += e
        card.dynamicThemed {
            if (jump() == e) ImportantSemantic
            else null
        }.rowCollapsingToColumn(79.rem) {
            reactive {
                if (jump() == e) scrollIntoView(null, Align.Center, true)
            }
            weight(1f).col {
                gap = 0.25.rem
                text(name)
                subtext { setBasicHtmlContent(description) }

                if (references.isNotEmpty()) {
                    space()
                    label {
                        content = "See Also:"
                        for (reference in references) {
                            themed(LinkSemantic).button {
                                subtext("- ${reference.name}")
                                onClick {
                                    jump.value = reference
                                    delay(1000)
                                    jump.value = null
                                }
                            }
                        }
                    }
                }
            }
            separator()
            weight(3f).rowCollapsingToColumn(40.rem) {
                weight(2f).scrollingHorizontally.code { content = code }
                separator()
                weight(1f).col {
                    result()
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override fun ElementWriter.CanAddTheme.render(): Unit {
        frame {
            article {
                titledSection("Available Views") {

                    titledSection("Containers") {
                        example(
                            name = "row",
                            description = "Horizontal stuff",
                            code = """
                            row {
                                card.text("A")
                                weight(1f).card.text("B")
                                card.text("C")
                            }
                        """.trimIndent(),
                            result = {
                                row {
                                    card.text("A")
                                    weight(1f).card.text("B")
                                    card.text("C")
                                }
                            }
                        )
                        example(
                            name = "col",
                            description = "Vertical stuff",
                            code = """
                            sizeConstraints(height = 10.rem).col {
                                card.text("A")
                                weight(1f).card.text("B")
                                card.text("C")
                            }
                        """.trimIndent(),
                            result = {
                                sizeConstraints(height = 10.rem).col {
                                    card.text("A")
                                    weight(1f).card.text("B")
                                    card.text("C")
                                }
                            }
                        )
                        example(
                            name = "rowCollapsingToColumn",
                            description = "Horizontal stuff that becomes vertical stuff at specified screen width",
                            code = """
                            rowCollapsingToColumn(70.rem) {
                                card.text("A")
                                weight(1f).card.text("B")
                                card.text("C")
                            }
                        """.trimIndent(),
                            result = {
                                rowCollapsingToColumn(70.rem) {
                                    card.text("A")
                                    weight(1f).card.text("B")
                                    card.text("C")
                                }
                            }
                        )
                        example(
                            name = "frame",
                            description = "Stuff stacked on top of one another",
                            code = """
                            sizeConstraints(height = 10.rem).card.frame {
                                atTopStart.text("A")
                                centered.text("B")
                                atBottomEnd.text("C")
                            }
                        """.trimIndent(),
                            result = {
                                sizeConstraints(height = 10.rem).frame {
                                    atTopStart.card.text("A")
                                    centered.card.text("B")
                                    atBottomEnd.card.text("C")
                                }
                            }
                        )
                    }

                    titledSection("Display Only") {
                        example(
                            name = "activityIndicator",
                            description = "Creates a spinning icon to indicate loading",
                            code = "activityIndicator()",
                            result = { activityIndicator() }
                        )
                        example(
                            name = "progressBar",
                            description = "Horizontal progress indicator",
                            code = """
                            progressBar { ratio = 0.4f }
                            progressBar { ratio = 0.6f }
                            progressBar { ratio = 0.8f }
                            progressBar { ratio = 1f }
                    """.trimIndent(),
                            result = {
                                progressBar { ratio = 0.4f }
                                progressBar { ratio = 0.6f }
                                progressBar { ratio = 0.8f }
                                progressBar { ratio = 1f }
                            }
                        )
                        example(
                            name = "circularProgress",
                            description = "Circular progress indicator",
                            code = """
                            row {
                                sizeConstraints(width = 3.rem).circularProgress { ratio = 0.4f }
                                sizeConstraints(width = 3.rem).circularProgress { ratio = 0.6f }
                                sizeConstraints(width = 3.rem).circularProgress { ratio = 0.8f }
                                sizeConstraints(width = 3.rem).circularProgress { ratio = 1f }
                            }
                    """.trimIndent(),
                            result = {
                                row {
                                    sizeConstraints(width = 3.rem).circularProgress { ratio = 0.4f }
                                    sizeConstraints(width = 3.rem).circularProgress { ratio = 0.6f }
                                    sizeConstraints(width = 3.rem).circularProgress { ratio = 0.8f }
                                    sizeConstraints(width = 3.rem).circularProgress { ratio = 1f }
                                }
                            }
                        )
                        example(
                            name = "icon",
                            description = "Displays a changeable icon.",
                            code = """
                            icon(Icon.help, "Help")
                            icon {
                                source = Icon.notification
                                description = "Notification"
                            }
                        """.trimIndent(),
                            result = {
                                icon(Icon.help, "Help")
                                icon {
                                    source = Icon.notification
                                    description = "Notification"
                                }
                            }
                        )
                        example(
                            name = "image",
                            description = "Displays a changeable image.",
                            code = """
                    sizeConstraints(height = 8.rem).image {
                        source = ImageRemote(url = "https://picsum.photos/200/200")
                    }
                    """.trimIndent(),
                            result = {
                                sizeConstraints(height = 8.rem).image {
                                    source = ImageRemote(url = "https://picsum.photos/200/200")
                                }
                            }
                        )
                        example(
                            name = "video",
                            description = "Displays a changeable video.",
                            code = """
                            sizeConstraints(height = 10.rem).video {
                                source = VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                                showControls = true
                            }
                    """.trimIndent(),
                            result = {
                                sizeConstraints(height = 10.rem).video {
                                    source =
                                        VideoRemote("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                                    showControls = true
                                }
                            }
                        )
                        example(
                            name = "separator",
                            description = "Creates a thin line in a row or column.",
                            code = """
                        text("ONE")
                        separator {}
                        text("TWO")
                    """.trimIndent(),
                            result = {
                                text("ONE")
                                separator {}
                                text("TWO")
                            }
                        )
                        example(
                            name = "space",
                            description = "Creates a thin line in a row or column.",
                            code = """
                        text("ONE")
                        space()
                        text("TWO")
                        text("THREE")
                    """.trimIndent(),
                            result = {
                                text("ONE")
                                space()
                                text("TWO")
                                text("THREE")
                            }
                        )

                        example(
                            name = "textView",
                            description = "Show a piece of text",
                            code = """
                        text("Hello world!")
                        text { content = "Also, hello world." }
                        text {
                            setBasicHtmlContent("Supported tags can be found &lt;a href=\"https://stackoverflow.com/questions/9754076/which-html-tags-are-supported-by-android-textview\">here</a>.
                        }
                    """.trimIndent(),
                            result = {
                                text("Hello world!")
                                text { content = "Also, hello world." }
                                text {
                                    setBasicHtmlContent("Supported tags can be found <a href=\"https://stackoverflow.com/questions/9754076/which-html-tags-are-supported-by-android-textview\">here</a>.")
                                }
                            }
                        )
                        example(
                            name = "subtext",
                            description = "Show a smaller piece of text",
                            code = """
                        subtext("Hello world!")
                        subtext { content = "Also, hello world." }
                    """.trimIndent(),
                            result = {
                                subtext("Hello world!")
                                subtext { content = "Also, hello world." }
                            }
                        )
                        example(
                            name = "h1.h6",
                            description = "Headers h1 through h6",
                            code = """
                            h1("Header 1")
                            h2("Header 2")
                            h3("Header 3")
                            h4("Header 4")
                            h5("Header 5")
                            h6("Header 6")
                    """.trimIndent(),
                            result = {
                                h1("Header 1")
                                h2("Header 2")
                                h3("Header 3")
                                h4("Header 4")
                                h5("Header 5")
                                h6("Header 6")
                            }
                        )
                        example(
                            name = "graph",
                            description = "Display a graph",
                            code = """
                                graph {
                                    ::data { listOf(Point(0.0, 0.0), Point(1.0, 100.0), Point(2.0, 350.0), Point(3.0, 200.0), Point(4.0, 500.0), Point(5.0, 750.0), Point(6.0, 700.0)) }
                                    ::xAxisLabels { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
                                    padding = 1.rem
                                    pointShape = GraphDelegate.PointShape.Circle
                                    xAxisLabel = "Past Week"
                                    yAxisLabel = "Value"
                                }
                            """.trimIndent(),
                            result = {
                                graph {
                                    ::data { listOf(Point(0.0, 0.0), Point(1.0, 100.0), Point(2.0, 350.0), Point(3.0, 200.0), Point(4.0, 500.0), Point(5.0, 750.0), Point(6.0, 700.0)) }
                                    ::xAxisLabels { listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") }
                                    padding = 1.rem
                                    pointShape = GraphDelegate.PointShape.Circle
                                    xAxisLabel = "Past Week"
                                    yAxisLabel = "Value"
                                }
                            }
                        )
                    }
                    titledSection("Interactive Elements") {
                        example(
                            name = "button",
                            description = "A button that triggers an action, which can be long-running.",
                            code = """
                        button {
                            text("A dangerous button")
                            action = Action("Explode") {
                                delay(1.seconds)
                                toast("KABOOM!!!")
                            }
                        } 
                    """.trimIndent(),
                            result = {
                                button {
                                    text("A dangerous button")
                                    action = Action("Explode") {
                                        delay(1.seconds)
                                        toast("KABOOM!!!")
                                    }
                                }
                            }
                        )
                        example(
                            name = "menuButton",
                            description = "Button that opens a menu",
                            code = """
                        menuButton {
                                requireClick = true
                                preferredDirection = PopoverPreferredDirection.aboveCenter

                                text("Click me")
                                opensMenu {
                                    col {
                                        centered.text("Menu")
                                        button { text("I am a button") }
                                    }
                                }
                            }
                    """.trimIndent(),
                            result = {
                                menuButton {
                                    requireClick = true
                                    preferredDirection = PopoverPreferredDirection.aboveCenter

                                    text("Click me")
                                    opensMenu {
                                        col {
                                            centered.text("Menu")
                                            button { text("I am a button") }
                                        }
                                    }
                                }
                            }
                        )
                        example(
                            name = "link",
                            description = "Link to an internal page",
                            code = """
                        link {
                                text { content = "Home page" }
                                to = { RootPage }
                                newTab = true
                            }
                    """.trimIndent(),
                            result = {
                                link {
                                    text { content = "Home page" }
                                    to = { RootPage }
                                    newTab = true
                                }
                            }
                        )
                        example(
                            name = "externalLink",
                            description = "Link to an external page",
                            code = """
                        externalLink {
                                text { content = "Link to Stack Overflow" }
                                to = "https://stackoverflow.com/questions/9754076/which-html-tags-are-supported-by-android-textview"
                                newTab = true
                            }
                    """.trimIndent(),
                            result = {
                                externalLink {
                                    text { content = "Link to Stack Overflow" }
                                    to =
                                        "https://stackoverflow.com/questions/9754076/which-html-tags-are-supported-by-android-textview"
                                    newTab = true
                                }
                            }
                        )
                    }
                    titledSection("Form Controls") {
                        example(
                            name = "checkbox",
                            description = "Used in forms to include things.",
                            code = """
                        col {
                            val selected = Signal<Set<String>>(setOf("Ketchup"))
                            for(option in listOf("Ketchup", "Mustard", "Mayo")) {
                                row {
                                    centered.checkbox {
                                        checked bind selected.contains(option)
                                    }
                                    centered.expanding.text(option)
                                }
                            }
                            centered.text {
                                ::content { "Include " + selected().joinToString(", ") }
                            }
                        }
                    """.trimIndent(),
                            result = {
                                col {
                                    val selected = Signal<Set<String>>(setOf("Ketchup"))
                                    for (option in listOf("Ketchup", "Mustard", "Mayo")) {
                                        row {
                                            centered.checkbox {
                                                checked bind selected.contains(option)
                                            }
                                            centered.expanding.text(option)
                                        }
                                    }
                                    centered.text {
                                        ::content { "Include " + selected().joinToString(", ") }
                                    }
                                }
                            }
                        )
                        example(
                            name = "radioButton",
                            description = "Used in forms to pick a single option.",
                            code = """
                        col {
                            val selected = Signal<String>("Chicken")
                            centered.text("Pick one")
                            for(option in listOf("Chicken", "Steak", "Shrimp")) {
                                row {
                                    centered.radioButton {
                                        checked bind selected.equalTo(option)
                                    }
                                    centered.expanding.text(option)
                                }
                            }
                            centered.text {
                                ::content { "Meat selected: " + selected() }
                            }
                        }
                    """.trimIndent(),
                            result = {
                                col {
                                    val selected = Signal<String>("Chicken")
                                    centered.text("Pick one")
                                    for (option in listOf("Chicken", "Steak", "Shrimp")) {
                                        row {
                                            centered.radioButton {
                                                checked bind selected.equalTo(option)
                                            }
                                            centered.expanding.text(option)
                                        }
                                    }
                                    centered.text {
                                        ::content { "Meat selected: " + selected() }
                                    }
                                }
                            }
                        )
                        example(
                            name = "toggleButton",
                            description = "Button that switches between two states",
                            code = """
                            val toggled = Signal<Boolean>(false)
                            toggleButton {
                                centered.text {
                                    ::content { if (toggled()) "Toggled ON" else "Toggled OFF" }
                                }
                                checked bind toggled
                            }
                    """.trimIndent(),
                            result = {
                                val toggled = Signal<Boolean>(false)
                                toggleButton {
                                    centered.text {
                                        ::content { if (toggled()) "Toggled ON" else "Toggled OFF" }
                                    }
                                    checked bind toggled
                                }
                            }
                        )
                        example(
                            name = "field",
                            description = "Wraps another input with a label.",
                            code = """
                        val name = Signal("")
                        field(label = "First Name") {
                            textInput { content bind name }
                        }
                        text { ::content { "Name is ${'$'}{name()}" } }
                    """.trimIndent(),
                            result = {
                                val name = Signal("")
                                field(label = "First Name") {
                                    textInput { content bind name }
                                }
                                text { ::content { "Name is ${name()}" } }
                            }
                        )
                        example(
                            name = "localDateField",
                            description = "A field for entering a date.",
                            code = """
                        val date = Signal<LocalDate?>(null)
                        localDateField {
                            range = LocalDate(1970, 1, 1)..LocalDate(year = 1971, 12, 31)
                            content bind date
                        }
                        text { ::content { "Date Selected: ${'$'}{date()?.renderToString(RenderSize.Full) ?: "N/A"}" } }
                   
                    """.trimIndent(),
                            result = {
                                val date = Signal<LocalDate?>(null)
                                localDateField {
                                    range = LocalDate(1970, 1, 1)..LocalDate(year = 1971, 12, 31)
                                    content bind date
                                }
                                text { ::content { "Date Selected: ${date()?.renderToString(RenderSize.Full) ?: "N/A"}" } }
                            }
                        )
                        example(
                            name = "localTimeField",
                            description = "A field for entering a time.",
                            code = """
                        val time = Signal<LocalTime?>(null)
                        localTimeField {
                            content bind time
                        }
                        text { ::content { "Time Selected: ${'$'}{time()?.renderToString(RenderSize.Full) ?: "N/A"}" } }
                   
                    """.trimIndent(),
                            result = {
                                val time = Signal<LocalTime?>(null)
                                localTimeField {
                                    content bind time
                                }
                                text { ::content { "Time Selected: ${time()?.renderToString(RenderSize.Full) ?: "N/A"}" } }
                            }
                        )
                        example(
                            name = "localDateTimeField",
                            description = "A field for entering both a date and a time.",
                            code = """
                        val date = Signal<LocalDateTime?>(null)
                        localDateTimeField {
                            content bind date
                        }
                        text { ::content { "Date Selected: ${'$'}{date()?.renderToString(RenderSize.Full) ?: "N/A"}" } }
                   
                    """.trimIndent(),
                            result = {
                                val date = Signal<LocalDateTime?>(null)
                                localDateTimeField {
                                    content bind date
                                }
                                text { ::content { "Date Selected: ${date()?.renderToString(RenderSize.Full) ?: "N/A"}" } }
                            }
                        )
                        example(
                            name = "select",
                            description = "A drop-down selection input.",
                            code = """
                        text { content = "LOTR Characters" }
                        val characters = Constant(listOf("Bilbo", "Frodo", "Gandalf", "Thorin"))
                        val valueChanged = Signal(characters.value.first())
                        select {
                            bind(edits = valueChanged, data = characters) { character -> character }
                        }
                        text { ::content { "Character Selected ${'$'}{valueChanged()}" } }
                    """.trimIndent(),
                            result = {
                                text { content = "LOTR Characters" }
                                val characters = Constant(listOf("Bilbo", "Frodo", "Gandalf", "Thorin"))
                                val valueChanged = Signal(characters.value.first())
                                select {
                                    bind(edits = valueChanged, data = characters) { character -> character }
                                }
                                text { ::content { "Character Selected ${valueChanged()}" } }
                            }
                        )

                        example(
                            name = "switch",
                            description = "A switch, intended to do something immediately when changed.",
                            code = """
                        val switchValue = Signal(false)
                        row {
                            expanding.text("My switch")
                            switch { checked bind switchValue } 
                        }
                        text { ::content { "Switch is ${'$'}{ if(switchValue()) "ON" else "OFF" }" } }
                    """.trimIndent(),
                            result = {
                                val switchValue = Signal(false)
                                row {
                                    expanding.text("My switch")
                                    switch { checked bind switchValue }
                                }
                                text { ::content { "Switch is ${if (switchValue()) "ON" else "OFF"}" } }
                            }
                        )
                        example(
                            name = "textArea",
                            description = "The text area",
                            code = """
                        val longText = Signal("")
                        sizeConstraints(height = 120.dp).scrolling.textArea { 
                            content bind longText
                            hint = "Some hint"
                        }
                        sizeConstraints(height = 120.dp).scrolling.text { ::content { "Entered Input: ${'$'}{longText()}" } }
                    """.trimIndent(),
                            result = {
                                val longText = Signal("")
                                sizeConstraints(height = 120.dp).scrolling.textArea {
                                    content bind longText
                                    hint = "Some hint"
                                }
                                sizeConstraints(height = 120.dp).scrolling.text { ::content { "Entered Input: ${longText()}" } }
                            }
                        )
                        example(
                            name = "textInput",
                            description = "The text area",
                            code = """
                        val text = Signal("")
                        textInput { 
                            content bind text
                            hint = "Some hint"
                        }
                        text { ::content { "Entered Input: ${'$'}{text()}" } }
                    """.trimIndent(),
                            result = {
                                val text = Signal("")
                                textInput {
                                    content bind text
                                    hint = "Some hint"
                                }
                                text { ::content { "Entered Input: ${text()}" } }
                            }
                        )
                        example(
                            name = "numberInput",
                            description = "Input for numbers",
                            code = """
                            val number = Signal<Double?>(null)
                            numberInput {
                                content bind number
                                hint = "A number"
                            }
                            text { ::content { "Entered Number: ${'$'}{number()}" } }
                        """.trimIndent()
                        ) {
                            val number = Signal<Double?>(null)
                            numberInput {
                                content bind number
                                hint = "A number"
                            }
                            text { ::content { "Entered Number: ${number()}" } }
                        }
                        example(
                            name = "phoneNumberInput",
                            description = "Input for phone numbers. Uses formattedTextInput under the hood.",
                            code = """
                            val number = Signal<String>("")
                            field("Phone Number") {
                                phoneNumberInput {
                                    format = PhoneNumberFormat.USA
    
                                    content bind number
                                    hint = "A phone number"
                                }
                            }
                            text { ::content { "Filtered Phone Number: ${'$'}{number()}" } }
                        """.trimIndent(),
                            references = setOf(ExampleEntry("formattedTextInput"))
                        ) {
                            val number = Signal<String>("")
                            field("Phone Number") {
                                phoneNumberInput {
                                    format = PhoneNumberFormat.USA

                                    content bind number
                                    hint = "A phone number"
                                }
                            }
                            text { ::content { "Filtered Phone Number: ${number()}" } }
                        }

                    }

                    titledSection("View Modifiers") {
                        example(
                            name = "scrolling", description = "Adds vertical scrolling to the view", code = """
                        sizeConstraints(height = 100.dp).scrolling.col {
                            for(it in 0..10) {
                                text { content = "Element ${'$'}it" }
                            }
                        }
                    """.trimIndent(), result = {
                                sizeConstraints(height = 100.dp).scrolling.col {
                                    for (it in 0..10) {
                                        text { content = "Element $it" }
                                    }
                                }
                            })
                        example(
                            name = "scrollingHorizontally",
                            description = "Adds horizontal scrolling to the view.",
                            code = """
                         scrollingHorizontally.row {
                            for(it in 0..10) {
                                text { content = "Element ${'$'}it" }
                            }
                        }
                    """.trimIndent(),
                            result = {
                                col {
                                    scrollingHorizontally.row {
                                        for (it in 0..10) {
                                            text { content = "Element $it" }
                                        }
                                    }
                                }
                            })
                        example(
                            name = "scrollingBoth",
                            description = "Adds vertical and horizontal scrolling to the view.",
                            code = """
                        scrollingBoth {}.col {
                            for(y in 0..10) {
                                row {
                                    for(x in 0..10) {
                                        text { content = "(${'$'}x, ${'$'}y)" }
                                    }
                                }
                            }
                        }
                    """.trimIndent(),
                            result = {
                                sizeConstraints(height = 150.dp).scrollingBoth {}.col {
                                    for (y in 0..10) {
                                        row {
                                            for (x in 0..10) {
                                                text { content = "($x, $y)" }
                                            }
                                        }
                                    }
                                }
                            })

                        example(
                            name = "hintPopover",
                            description = "Shows a hint view on hover.",
                            code = """
                            hintPopover {
                                row {
                                    icon(Icon.help, "")
                                    text("Some rich information")
                                }
                            }.card.text("Hover over me!")
                        """.trimIndent(),
                            result = {
                                card.hintPopover {
                                    row {
                                        icon(Icon.help, "")
                                        text("Some rich information")
                                    }
                                }.text("Hover over me!")
                            }
                        )
                        example(
                            name = "textPopover",
                            description = "Shows a hint view with text only on hover.",
                            code = """
                            textPopover("Some info!").card.text("Hover over me!")
                        """.trimIndent(),
                            result = {
                                card.textPopover("Some info!").text("Hover over me!")
                            }
                        )
                        example(
                            name = "weight",
                            description = "Indicates this view should take a ratio of the remaining space.",
                            code = """
                            row {
                                weight(1f).card.text("A")
                                weight(2f).card.text("B")
                                card.text("C")
                            }
                        """.trimIndent(),
                            result = {
                                row {
                                    weight(1f).card.text("A")
                                    weight(2f).card.text("B")
                                    card.text("C")
                                }
                            }
                        )
                        example(
                            name = "expanding",
                            description = "A shortcut for weight(1f).",
                            code = """
                            row {
                                expanding.card.text("A")
                                card.text("B")
                            }
                        """.trimIndent(),
                            result = {
                                row {
                                    expanding.card.text("A")
                                    card.text("B")
                                }
                            }
                        )
                        example(
                            name = "centered",
                            description = "Centers within a container",
                            code = """
                          sizeConstraints(height = 8.rem).card.row {
                                card.expanding.text("I am not centered")
                                centered.card.expanding.text("I am centered")
                            }
                            card.col {
                                card.text("I am not centered")
                                centered.card.text("I am centered")
                            }
                        """.trimIndent(),
                            result = {
                                card.row {
                                    expanding.card.text("I am not centered")
                                    centered.expanding.card.text("I am centered")
                                }
                                card.col {
                                    card.text("I am not centered")
                                    centered.card.text("I am centered")
                                }
                            }
                        )
                        example(
                            name = "align",
                            description = "Controls alignment within a container.  Has shortcuts in the form of 'at[Y][X}'.",
                            code = """
                            sizeConstraints(height = 7.rem).card.row {
                                atTop.card.text("T")
                                centered.card.text("C")
                                atBottom.card.text("B")
                                align(Align.Stretch, Align.Stretch).card.text("S")
                            }
                            card.col {
                                atStart.card.text("Start")
                                centered.card.text("Centered")
                                atEnd.card.text("End")
                                align(Align.Stretch, Align.Stretch).card.text("Stretch")
                            }
                            sizeConstraints(height = 12.rem).card.frame {
                                atTopStart.card.text("Top Start")
                                align(Align.Stretch, Align.Center).card.text("Stretch/Center")
                                atEnd.card.text("End")
                            }
                        """.trimIndent(),
                            result = {
                                sizeConstraints(height = 7.rem).card.row {
                                    atTop.card.text("T")
                                    centered.card.text("C")
                                    atBottom.card.text("B")
                                    align(Align.Stretch, Align.Stretch).card.text("S")
                                }
                                card.col {
                                    atStart.card.text("Start")
                                    centered.card.text("Centered")
                                    atEnd.card.text("End")
                                    align(Align.Stretch, Align.Stretch).card.text("Stretch")
                                }
                                sizeConstraints(height = 12.rem).card.frame {
                                    atTopStart.card.text("Top Start")
                                    align(Align.Stretch, Align.Center).card.text("Stretch/Center")
                                    atEnd.card.text("End")
                                }
                            }
                        )
                        example(
                            name = "sizeConstraints",
                            description = "Sets requirements on the size of the view.",
                            code = """
                            sizeConstraints(height = 3.rem).card.text("Sized")
                            atStart.sizeConstraints(width = 5.rem).card.text("Very Short")
                            atStart.sizeConstraints(width = 200.rem).card.text("Try for 200")
                        """.trimIndent(),
                            result = {
                                sizeConstraints(height = 3.rem).card.text("Sized")
                                atStart.sizeConstraints(width = 5.rem).card.text("Very Short")
                                atStart.sizeConstraints(width = 200.rem).card.text("Try for 200")
                            }
                        )
                        example(
                            name = "padded",
                            description = "Forces a view to have padding.",
                            code = """
                            card.text("Naturally has padding")
                            padded.text("Has virtual padding")
                        """.trimIndent(),
                            result = {
                                card.text("Naturally has padding")
                                padded.text("Has virtual padding")
                            }
                        )
                        example(
                            name = "unpadded",
                            description = "Forces a view to have no padding.",
                            code = """
                            text("Naturally no padding")
                            unpadded.card.text("Forced no padding")
                        """.trimIndent(),
                            result = {
                                text("Naturally no padding")
                                card.unpadded.text("Forced no padding")
                            }
                        )
                        example(
                            name = "compact",
                            description = "Reduces the padding on a view",
                            code = """
                            card.text("Regular card")
                            compact.card.text("Compact card")
                        """.trimIndent(),
                            result = {
                                card.text("Regular card")
                                compact.card.text("Compact card")
                            }
                        )
                        example(
                            name = "shownWhen",
                            description = "Shows or hides a view dynamically.  Animated.",
                            code = """
                            val visible = Signal(true)
                            row {
                                expanding.text("Visible")
                                switch { checked bind visible }
                            }
                            shownWhen { visible() }.text("Only visible when on")
                        """.trimIndent(),
                            result = {
                                val visible = Signal(true)
                                row {
                                    expanding.text("Visible")
                                    switch { checked bind visible }
                                }
                                shownWhen { visible() }.text("Only visible when on")
                            }
                        )
                        example(
                            name = "(themes)",
                            description = "There are a lot of theme-oriented modifiers.  They change the look of something.  The exact look depends on the theme.",
                            code = """
                            text("None")
                            card.text("card")
                            fieldTheme.text("fieldTheme")
                            bar.text("bar")
                            nav.text("nav")
                            important.text("important")
                            critical.text("critical")
                            warning.text("warning")
                            danger.text("danger")
                            affirmative.text("affirmative")
                            emphasized.text("emphasized")
                            InsetSemantic.onNext.text("InsetSemantic.onNext")
                        """.trimIndent(),
                            result = {
                                text("None")
                                card.text("card")
                                fieldTheme.text("fieldTheme")
                                bar.text("bar")
                                nav.text("nav")
                                important.text("important")
                                critical.text("critical")
                                warning.text("warning")
                                danger.text("danger")
                                affirmative.text("affirmative")
                                emphasized.text("emphasized")
                                themed(InsetSemantic).text("InsetSemantic.onNext")
                            }
                        )
                    }

                    titledSection("Advanced Components") {
                        example(
                            name = "canvas",
                            description = "The canvas component is based off of the WEB Canvas Api, the following examples are from the <a href=\"https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API/Tutorial/Drawing_shapes\">MDN Shapes Tutorial</a>.",
                            code = """
                        canvas {
                        delegate = object : CanvasDelegate() {
                            override fun draw(context: DrawingContext2D) = with(context) {
                                super.draw(context)
                                context.fillPaint = Color.white
                                context.strokePaint = Color.white
                                repeat(5) { i ->
                                    repeat(4) { j ->
                                        context.beginPath()
                                        val x = 25.0 + j * 50.0 // x coordinate
                                        val y = 25.0 + i * 50.0 // y coordinate
                                        val radius = 20.0 // Arc radius
                                        val startAngle = Angle(0.0) // Starting point on circle
                                        val endAngle = Angle(PI + (PI * j) / 2) // End point on circle
                                        val counterclockwise = i % 2 != 0 // clockwise or counterclockwise

                                        context.appendArc(x, y, radius, startAngle, endAngle, counterclockwise)

                                        if (i > 1) {
                                            context.fill()
                                        } else {
                                            context.stroke()
                                        }
                                    }
                                }
                            }
                        }
                    }
                    """.trimIndent(),
                            result = {
                                canvas {
                                    delegate = object : CanvasDelegate() {
                                        override fun draw(context: DrawingContext2D) = with(context) {
                                            super.draw(context)
                                            context.fillPaint = Color.white
                                            context.strokePaint = Color.white
                                            repeat(5) { i ->
                                                repeat(4) { j ->
                                                    context.beginPath()
                                                    val x = 25.0 + j * 50.0 // x coordinate
                                                    val y = 25.0 + i * 50.0 // y coordinate
                                                    val radius = 20.0 // Arc radius
                                                    val startAngle = Angle(0.0) // Starting point on circle
                                                    val endAngle = Angle(PI + (PI * j) / 2) // End point on circle
                                                    val counterclockwise = i % 2 != 0 // clockwise or counterclockwise

                                                    context.appendArc(
                                                        x,
                                                        y,
                                                        radius,
                                                        startAngle,
                                                        endAngle,
                                                        counterclockwise
                                                    )

                                                    if (i > 1) {
                                                        context.fill()
                                                    } else {
                                                        context.stroke()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        example(
                            name = "dismissBackground",
                            description = "Creates a dimmed background for making floating modals.",
                            code = """
                    dismissBackground {
                        centered.card.text("I'm like a dialog")
                        onClick {
                            println("Put logic to dismiss here")
                        }
                    }
                    """.trimIndent(),
                            result = {
                                dismissBackground {
                                    centered.card.text("I'm like a dialog")
                                    onClick {
                                        println("Put logic to dismiss here")
                                    }
                                }
                            }
                        )

                        example(
                            name = "recyclerView",
                            description = "Used when you have many, many items that needed represented in a scrollable area.",
                            code = """
                        expanding.recyclerView {
                            //Determines the layout of the data.
                            placer = RecyclerViewPlacerVerticalGrid(1)
    
                            children(Constant((1..10000).toList()), id = { it }, render = { item ->
                                card.text { ::content { "Item ${'$'}{item()}" } }
                            })
                        }
                    """.trimIndent(),
                            result = {
                                expanding.recyclerView {
                                    //Determines the layout of the data.
                                    placer = RecyclerViewPlacerVerticalGrid(1)

                                    children(Constant((1..10000).toList()), id = { it }, render = { item ->
                                        card.text { ::content { "Item ${item()}" } }
                                    })
                                }
                            }
                        )

                        example(
                            name = "formattedTextInput",
                            description = "raw text input that you can filter and format as you please.",
                            code = """
                            val input = Signal("")
                            {
                            field("Enter Hex Color") {
                                formattedTextInput {
                                    content bind input
    
                                    // Hexadecimal format
                                    val hexCharacters = ('0'..'9') + ('a'..'f') + ('A'..'F')
                                    format(
                                        isRawData = { it in hexCharacters.toSet() },
                                        formatter = { if (it.isBlank()) "" else "#${'$'}{it.take(6)}" }
                                    )
                                }
                            }
                            text {    
                                // theme code not shown
                                ::content { "Filtered input: ${'$'}{input()}" }
                            }
                        """.trimIndent()
                        ) {
                            val input = Signal("")
                            field("Enter Hex Color") {
                                formattedTextInput {
                                    content bind input

                                    // Hexadecimal format
                                    val hexCharacters = ('0'..'9') + ('a'..'f') + ('A'..'F')
                                    format(
                                        isRawData = { it in hexCharacters.toSet() },
                                        formatter = { if (it.isBlank()) "" else "#${it.take(6)}" }
                                    )
                                }
                            }

                            val debounced = input.debounce(200)

                            dynamicThemed {
                                val color = try {
                                    Color.fromHexString(debounced())
                                } catch (e: NumberFormatException) {
                                    Color.white
                                }
                                SetForeground(color)
                            }.text {
                                ::content { "Filtered input: ${input()}" }
                            }
                        }
                    }
                    titledSection("Dialogs") {
                        example(
                            name = "toast",
                            description = "Brief message pop up to inform the user of an action outcome",
                            code = """
                        button {
                                text("Click Me")
                                action = Action("Show toast") {
                                    toast("I am a toast!")
                                }
                            }
                    """.trimIndent(),
                            result = {
                                button {
                                    text("Show Toast")
                                    action = Action("Show toast") {
                                        toast("I am a toast!")
                                    }
                                }
                            }
                        )
                        example(
                            name = "confirmDanger",
                            description = "Show a dialog to confirm an action",
                            code = """
                        button {
                                text("Do a dangerous thing")
                                onClick {
                                    confirmDanger(
                                        "Danger",
                                        "Are you sure you wish to do this dangerous thing?"
                                    ) {
                                        println("Did a dangerous thing!")
                                    }
                                }
                            }
                    """.trimIndent(),
                            result = {
                                button {
                                    text("Do a dangerous thing")
                                    onClick {
                                        confirmDanger(
                                            "Danger",
                                            "Are you sure you wish to do this dangerous thing?"
                                        ) {
                                            println("Did a dangerous thing!")
                                        }
                                    }
                                }
                            }
                        )
                        example(
                            name = "alert",
                            description = "Show an alert dialog",
                            code = """
                        button {
                                text("Alert Me")
                                onClick {
                                    alert("Alert", "This is an alert")
                                }
                            }
                    """.trimIndent(),
                            result = {
                                button {
                                    text("Alert Me")
                                    onClick {
                                        alert("Alert", "This is an alert")
                                    }
                                }
                            }
                        )
                    }
                    titledSection("Other (not yet categorized)") {
                        example(
                            name = "forEach",
                            description = "",
                            code = """
                            val fruits = listOf("Apples", "Oranges", "Plums", "Bananas", "Cherries")

                            col {
                                forEach(remember { fruits }) { fruit ->
                                    card.text(fruit)
                                }
                            }
                    """.trimIndent(),
                            result = {
                                val fruits = listOf("Apples", "Oranges", "Plums", "Bananas", "Cherries")

                                col {
                                    forEach(remember { fruits }) { fruit ->
                                        card.text(fruit)
                                    }
                                }
                            }
                        )
                    }
                }
            }
            atTopEnd.col {
                fieldTheme.row {
                    textInput {
                        hint = "Quick jump..."
                        action = Action("Quick jump") {
                            val match = known.filter() { it.name.contains(content.value, ignoreCase = true) }
                                .minByOrNull { it.name.length }
                                ?: known.filter { it.tags.any { it.contains(content.value, ignoreCase = true) } }
                                    .minByOrNull { it.name.length }
                            if (match != null) {
                                jump.value = match
                                delay(1.seconds)
                                jump.value = null
                            }
                        }
                    }
                }
            }
        }
    }
}

typealias StringID = String

private data class SetForeground(val color: Color) : Semantic("frgnd-${color.toInt()}") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        foreground = color,
        background = if (color.perceivedBrightness > 0.5f) Color.black else Color.gray(0.9f)
    )
}