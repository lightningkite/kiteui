package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.canvas.*
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.readable.Property
import com.lightningkite.readable.Readable
import com.lightningkite.readable.equalTo
import kotlinx.datetime.LocalDate
import kotlin.math.PI
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Routable("docs/available-views")
object AvailableViews : DocPage {
    override val covers: List<String>
        get() = listOf("Available KiteUI Views")

    @OptIn(ExperimentalUuidApi::class)
    override fun ViewWriter.render(): ViewModifiable = run {
        article {
            titledSection("Available Views") {
                text("Activity Indicator")
                example("""
                    centered - col {
                         sizeConstraints(width = 2.rem, height = 2.rem) - centered - activityIndicator {  }
                         centered - text("Loading...")
                    }
                """.trimIndent()) {
                     centered - col {
                         sizeConstraints(width = 2.rem, height = 2.rem) - centered - activityIndicator {  }
                         centered - text("Loading...")
                    }
                }
                text("AutoCompleteTextField")
                example("""
                    centered - col {
                        autoCompleteTextField { keyboardHints = KeyboardHints() }
                    }
                """.trimIndent()) {
                    centered - col {
                        autoCompleteTextField { keyboardHints = KeyboardHints() }
                    }
                }
                text("Button")
                example("""
                    centered - col {
                        button {
                            action = Action("Button Pressed", icon = Icon.help) {
                                toast(text = "Boom!!")
                            }
                            text { content = "Press The Button!"}
                        }
                    }""") {
                    centered - col {
                        centered - sizeConstraints(width = 16.rem) - button {
                            action = Action("Button Pressed", icon = Icon.help) {
                                toast(text = "Boom!!")
                            }
                            centered - text { content = "Press The Button!"}
                        }
                    }
                }
//                text("Canvas, The canvas component matches the WEB Canvas Api,")
                text {
                    setBasicHtmlContent("""
                        <p>Canvas, The canvas component is based off of the WEB Canvas Api, the following examples are from the MDN Canvas docs found here <a href="https://developer.mozilla.org/en-US/docs/Web/API/Canvas_API/Tutorial/Drawing_shapes">MDN Shapes Tutorial</a></p>
                    """.trimIndent())
                }
                example("""
                    centered - col {
                        canvas {
                            delegate = object : CanvasDelegate() {
                                override fun draw(context: DrawingContext2D) = with(context) {
                                    super.draw(context)

                                    context.fillRect(25.0, 25.0, 100.0, 100.0)
                                    context.clearRect(45.0, 45.0, 60.0, 60.0)
                                    context.strokeRect(50.0, 50.0, 50.0, 50.0)
                                }
                            }
                        }
                        canvas {
                            delegate = object : CanvasDelegate() {
                                override fun draw(ctx: DrawingContext2D) = with(ctx) {
                                    super.draw(ctx)

                                    repeat(5) { i ->
                                        repeat(4) { j ->
                                            ctx.beginPath()
                                            val x = 25.0 + j * 50.0 // x coordinate
                                            val y = 25.0 + i * 50.0 // y coordinate
                                            val radius = 20.0 // Arc radius
                                            val startAngle = Angle(0.0) // Starting point on circle
                                            val endAngle = Angle(PI + (PI * j) / 2) // End point on circle
                                            val counterclockwise = i % 2 != 0 // clockwise or counterclockwise

                                            ctx.appendArc(x, y, radius, startAngle, endAngle, counterclockwise)

                                            if (i > 1) {
                                                ctx.fill()
                                            } else {
                                                ctx.stroke()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                """.trimIndent()) {
                    centered - col {
                        canvas {
                            delegate = object : CanvasDelegate() {
                                override fun draw(context: DrawingContext2D) = with(context) {
                                    super.draw(context)

                                    context.fillPaint = Color.white
                                    context.strokePaint = Color.white
                                    context.fillRect(25.0, 25.0, 100.0, 100.0)
                                    context.clearRect(45.0, 45.0, 60.0, 60.0)
                                    context.strokeRect(50.0, 50.0, 50.0, 50.0)
                                }
                            }
                        }
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
                    }
                }
                text("Checkbox")
                example("""
                    centered - col {
                        var check: Checkbox
                        sizeConstraints(width = 2.rem, height = 2.rem) - centered - checkbox {
                            check = this
                            enabled = true
                        }
                        centered - text {
                            reactive {
                                content = "Checkbox is ${'$'}{ if(check.checked()) "checked" else "not checked" }"
                            }
                        }
                    }
                """.trimIndent()) {
                    centered - col {
                        var check: Checkbox
                        sizeConstraints(width = 2.rem, height = 2.rem) - centered - checkbox {
                            check = this
                            enabled = true
                        }
                        centered - text {
                            ::content { "Checked is ${ if(check.checked()) "true" else "false" }" }
                        }
                    }
                }
                text("DismissBackground")
                example("""
                    col {
                        text { content = "The following container will now no background"}
                        centered - sizeConstraints(height = 4.rem) - col {
                            dismissBackground {  }
                        }
                    }
                """.trimIndent()) {
                    col {
                        centered - text { content = "The following container will have no background"}
                        centered - sizeConstraints(height = 8.rem, width = 40.rem) - col {
                            dismissBackground {  }
                        }
                    }
                }
                text("Add images to your application with an image block, within the block provide a source ")
                example("""val random = Random(2349052345)
                    centered - col {
                        row {
                            expanding - frame {}
                            sizeConstraints(height = 100.dp) - image {
                                source = ImageRemote(url = "https://picsum.photos/seed/${'$'}{random.nextInt()}/100/100")
                            }
                            sizeConstraints(height = 100.dp) - image {
                                source = ImageRemote(url = "https://picsum.photos/seed/${'$'}{random.nextInt()}/100/100")
                            }
                            sizeConstraints(height = 100.dp) - image {
                                source = ImageRemote(url = "https://picsum.photos/seed/${'$'}{random.nextInt()}/100/100")
                            }
                            sizeConstraints(height = 100.dp) - image {
                                source = Icon.help.copy(width = 100.dp, height = 100.dp).toImageSource(Color.white)
                            }
                            expanding - frame {}
                        }
                    }""") {
                    val random = Random(2349052345)
                    centered - col {
                        row {
                            expanding - frame {}
                            sizeConstraints(height = 100.dp) - image {
                                source = ImageRemote(url = "https://picsum.photos/seed/${random.nextInt()}/100/100")
                            }
                            sizeConstraints(height = 100.dp) - image {
                                source = ImageRemote(url = "https://picsum.photos/seed/${random.nextInt()}/100/100")
                            }
                            sizeConstraints(height = 100.dp) - image {
                                source = ImageRemote(url = "https://picsum.photos/seed/${random.nextInt()}/100/100")
                            }
                            sizeConstraints(height = 100.dp) - image {
                                source = Icon.help.copy(width = 100.dp, height = 100.dp).toImageSource(Color.white)
                            }
                            expanding - frame {}
                        }
                    }
                }
                text("Field")
                example("""
                    centered - col {
                        label {
                            content = "First Name"
                            object : Semantic(key = "inputbackground") {
                                override fun default(theme: Theme): ThemeAndBack {
                                    return theme.copy(background = Color.black).withBack
                                }
                            }.onNext - textInput {  }
                        }
                    }""") {
                    centered - col {
                        field(label = "First Name") {
                            object : Semantic(key = "inputbackground") {
                                override fun default(theme: Theme): ThemeAndBack {
                                    return theme.copy(background = Color.black).withBack
                                }
                            }.onNext - textInput {  }
                        }
                    }
                }
                text("LocalDateField allows you to select a date. You can listen to updates using the views content property which is of type ImmediateWritable<LocalDate>. Set a specific Date / DateTime range using the range property.")
                example("""
                    centered - col {
                        val dateField  = localDateField {
                            range = LocalDate(1970, 1, 1)..LocalDate(year = 1971, 12, 31)
                        }
                        text { ::content { "Date Selected: ${'$'}{dateField.content()}" }}
                    }
                    """) {
                    centered - col {
                        val dateField  = localDateField {
                            range = LocalDate(1970, 1, 1)..LocalDate(year = 1971, 12, 31)
                        }
                        text { ::content { "Date Selected: ${dateField.content()}" }}
                    }
                }
                text("RadioButton")
                example("""
                    centered - col {
                        val selected = Property(0)
                        centered - row {
                            repeat(3) { index -> radioButton {
                                checked bind selected.equalTo(index) }
                            }
                        }
                    }""") {
                    centered - col {
                        val selected = Property(0)
                        centered - row {
                            repeat(3) { index -> radioButton {
                                checked bind selected.equalTo(index) }
                            }
                        }
                    }
                }
                text("RadioToggleButton")
                example("""""") {
                    centered - col {
                        centered - radioToggleButton {  }
                    }
                }
                text("RecyclerView to be used with very long lists.")
                example("""                    
                    sizeConstraints(height = 200.dp) - Recycler2(this, vertical = true)
                        .apply {
                            //Determines the layout of the data.
                            placer = RecyclerViewPlacerVerticalGrid(1)

                            val mainRenderer: RecyclerViewRenderer<String> = object : RecyclerViewRenderer<String> {
                                override fun render(
                                    viewWriter: ViewWriter,
                                    data: Readable<String>,
                                    index: Readable<Int>,
                                ): ViewModifiable {
                                    return with(viewWriter) {
                                        text { ::content { data() } }
                                    }
                                }
                            }

                            rendererSet = object : RecyclerViewRendererSet<String, String> {
                                override fun id(item: String): String = item
                                override fun renderer(item: String): RecyclerViewRenderer<String> = mainRenderer
                            }

                            val ids: List<String> = (0..100).toList().map { Uuid.random().toString() }

                            data = object : RecyclerViewData<String, String> {
                                override val range: IntRange
                                    get() = 0..100

                                override fun get(index: Int): String {
                                    return ids[index]
                                }
                            }
                        }
                    """) {

                    centered - sizeConstraints(height = 800.dp) - Recycler2(this, vertical = true)
                        .apply {
                            weight(1f)
                            //Determines the layout of the data.
                            placer = RecyclerViewPlacerVerticalGrid(1)

                            val mainRenderer: RecyclerViewRenderer<StringID> = object : RecyclerViewRenderer<StringID> {
                                override fun render(
                                    viewWriter: ViewWriter,
                                    data: Readable<String>,
                                    index: Readable<Int>,
                                ): ViewModifiable {
                                    return with(viewWriter) {
                                        text { ::content { data() } }
                                    }
                                }
                            }

                            rendererSet = object : RecyclerViewRendererSet<String, StringID> {
                                override fun id(item: String): String = item
                                override fun renderer(item: String): RecyclerViewRenderer<String> = mainRenderer
                            }

                            val ids: List<String> = (0..500).toList().map { Uuid.random().toString() }

                            data = object : RecyclerViewData<String, StringID> {
                                override val range: IntRange
                                    get() = 0..100

                                override fun get(index: Int): StringID {
                                    return ids[index]
                                }
                            }
                        }
                }
                text("Select")
                example("""
                     centered - col {
                        text { content = "LOTR Characters" }
                        val characters = Property(listOf("Bilbo", "Frodo", "Gandalf", "Thorin"))
                        val valueChanged = Property(characters.state.get().first())
                        select {
                            bind(edits = valueChanged, data = characters) { character ->
                                character
                            }
                        }

                        text { ::content { "Character Selected ${'$'}{valueChanged()}" } }
                    }
                """.trimIndent()) {
                    centered - col {
                        text { content = "LOTR Characters" }
                        val characters = Property(listOf("Bilbo", "Frodo", "Gandalf", "Thorin"))
                        val valueChanged = Property(characters.state.get().first())
                        select {
                            bind(edits = valueChanged, data = characters) { character ->
                                character
                            }
                        }

                        text { ::content { "Character Selected ${valueChanged()}" } }
                    }
                }
                text("Separator, provides dividers between items")
                example("""
                    centered - col {
                        text { content = "ONE" }
                        separator {}
                        text { content = "TWO" }
                        separator {}
                        text { content = "THREE" }
                        separator {}
                        text { content = "FOUR" }
                    }""") {
                    centered - col {
                        text { content = "ONE" }
                        separator {}
                        text { content = "TWO" }
                        separator {}
                        text { content = "THREE" }
                        separator {}
                        text { content = "FOUR" }
                    }
                }
                text("Space, Add some space between views")
                example("""
                    centered - col {
                        text { content = "ONE" }
                        space {}
                        text { content = "TWO" }
                        text { content = "THREE" }
                    }
                """.trimIndent()) {
                    centered - col {
                        text { content = "ONE" }
                        space {}
                        text { content = "TWO" }
                        text { content = "THREE" }
                    }
                }

                text("Switch")
                example("""
                    val switchValue = Property(false)
                    centered - col {
                        switch { checked bind  switchValue }
                    }
                    text { ::content { "Switch Is ${'$'}{ if(switchValue()) "ON" else "OFF" }" } }""") {
                    val switchValue = Property(false)
                    centered - col {
                        switch { checked bind  switchValue }
                        text { ::content { "Switch Is ${ if(switchValue()) "ON" else "OFF" }" } }
                    }
                }
                text("TextArea: Enter a large amount of text")
                example("""
                    centered - col {
                        var textInput: TextArea
                        sizeConstraints(height = 120.dp, width = 600.dp) - scrolling - textArea { textInput = this }
                        sizeConstraints(height = 120.dp, width = 600.dp) - scrolling - text { ::content { "Entered Input: ${'$'}{textInput.content()}" } }
                    }
                """.trimIndent()) {
                    centered - col {
                        var textInput: TextArea
                        sizeConstraints(height = 120.dp, width = 600.dp) - scrolling - textArea { textInput = this }
                        sizeConstraints(height = 120.dp, width = 600.dp) - scrolling - text { ::content { "Entered Input: ${textInput.content()}" } }
                    }
                }
                text("TextInput: get some user input")
                example("""
                    centered - col {
                        val input = textInput { hint = "Enter some text" }
                        scrolling - text { ::content { "Entered Input: ${'$'}{input.content()}" } }
                    }
                """.trimIndent()) {
                    centered - col {
                        val input = textInput { hint = "Enter some text" }
                        sizeConstraints(height = 120.dp, width = 600.dp) - scrolling - text { ::content { "Entered Input: ${input.content()}" } }
                    }
                }
                text("TextView: Give the user some information")
                example("""
                    centered - col {
                        text { content = "Benjamin Franklin performed his famous kite experiment in June 1752." }
                    }
                """.trimIndent()) {
                    centered - col {
                        text { content = "Benjamin Franklin performed his famous kite experiment in June 1752." }
                    }
                }
                text("ToggleButton")
                example("""
                    centered - col {
                        toggleButton {
                            text { content = "Select Me" }
                        }.also { button ->
                            text { ::content { "Selected: ${'$'}{ button.checked() }" } }
                        }
                    }
                """.trimIndent()) {
                    centered - col {
                        var button: ToggleButton
                        centered - sizeConstraints(width = 400.dp) - toggleButton {
                            button = this
                            centered - text { content = "Select Me" }
                        }
                        centered - text { ::content { "Selected: ${ button.checked() }" } }
                    }
                }
//                text("TwoPane")
//                example("""""") {
//                    centered - col {
//                    }
//                }
//                text("WebView")
//                example("""""") {
//                    centered - col {
//
//                    }
//                }
            }
        }
    }
}

typealias StringID = String