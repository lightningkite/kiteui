package com.lightningkite.kiteui.views

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class InputsTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    private fun ComposeUiTest.show(app: ViewWriter.() -> Unit): KiteUiRoot {
        // Built on the main thread, as kiteUiWindow does.
        val root = runBlocking(Dispatchers.Main) { KiteUiRoot("kiteui-compose-test", { theme }, app) }
        setContent { root.Content() }
        waitForIdle()
        return root
    }

    private fun save(image: ImageBitmap, name: String) {
        val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
        ImageIO.write(image.toAwtImage(), "png", File(out, "$name.png"))
    }

    private fun ComposeUiTest.field(index: Int) = onAllNodes(hasSetTextAction())[index]

    // `bind` syncs through coroutines on the main dispatcher, which waitForIdle doesn't wait for.
    private fun ComposeUiTest.awaitText(index: Int, text: String) = waitUntil("field $index shows '$text'", 2_000) {
        field(index).fetchSemanticsNode().config[SemanticsProperties.EditableText].text == text
    }

    private fun <T> ComposeUiTest.awaitValue(signal: Signal<T>, value: T) = waitUntil("signal holds $value, not ${signal.value}", 2_000) {
        signal.value == value
    }

    @Test
    fun textAreaTakesLinesAndSubmitsOnEnter() = runComposeUiTest {
        val notes = Signal("")
        var submitted = 0
        val root = show {
            col {
                fieldTheme.textArea {
                    hint = "Notes"
                    content bind notes
                    action = Action("Submit") { submitted++ }
                }
            }
        }
        field(0).performTextInput("one\ntwo")
        awaitValue(notes, "one\ntwo")
        field(0).performKeyInput { pressKey(Key.Enter) }
        waitUntil("submitted", 2_000) { submitted == 1 }
        field(0).assertTextEquals("one\ntwo")
        notes.value = "replaced"
        awaitText(0, "replaced")
        root.close()
    }

    @Test
    fun numberInputFormatsAndParses() = runComposeUiTest {
        val amount = Signal<Double?>(null)
        val root = show { col { fieldTheme.numberInput { content bind amount } } }
        field(0).performTextInput("1234.5")
        awaitValue(amount, 1234.5)
        field(0).assertTextEquals("1,234.5")
        amount.value = 98765.0
        awaitText(0, "98,765")
        field(0).performTextClearance()
        awaitValue(amount, null)
        root.close()
    }

    @Test
    fun formattedTextInputKeepsRawData() = runComposeUiTest {
        val phone = Signal("")
        val root = show { col { fieldTheme.phoneNumberInput { content bind phone } } }
        field(0).performTextInput("5551234567")
        awaitValue(phone, "5551234567")
        field(0).assertTextEquals("(555) 123-4567")
        phone.value = "8015550000"
        awaitText(0, "(801) 555-0000")
        root.close()
    }

    @Test
    fun dateAndTimeFieldsParseWhatIsTyped() = runComposeUiTest {
        val date = Signal<LocalDate?>(null)
        val time = Signal<LocalTime?>(null)
        val dateTime = Signal<LocalDateTime?>(null)
        val root = show {
            col {
                fieldTheme.localDateField { content bind date }
                fieldTheme.localTimeField { content bind time }
                fieldTheme.localDateTimeField { content bind dateTime }
            }
        }
        field(0).performTextInput("202406")
        waitForIdle()
        // A partial date stays as typed, and is no date yet.
        field(0).assertTextEquals("2024-06")
        assertEquals(null, date.value)
        field(0).performTextInput("01")
        awaitValue(date, LocalDate(2024, 6, 1))
        field(0).performTextClearance()
        field(0).performTextInput("20241399")
        awaitValue(date, null)
        field(0).assertTextEquals("2024-13-99")

        field(1).performTextInput("0830")
        awaitValue(time, LocalTime(8, 30))
        field(1).assertTextEquals("08:30")

        field(2).performTextInput("202406011745")
        awaitValue(dateTime, LocalDateTime(2024, 6, 1, 17, 45))
        field(2).assertTextEquals("2024-06-01 17:45")

        date.value = LocalDate(1999, 12, 31)
        time.value = LocalTime(23, 5, 59)
        dateTime.value = null
        awaitText(0, "1999-12-31")
        awaitText(1, "23:05")
        awaitText(2, "")
        root.close()
    }

    @Test
    fun selectOpensAListAndChooses() = runComposeUiTest {
        val fruit = Signal("Banana")
        lateinit var element: Select
        val root = show {
            col {
                element = fieldTheme.select { bind(fruit, Constant(listOf("Apple", "Banana", "Cherry"))) { it } }
                text("Below")
            }
        }
        onNodeWithText("Banana").performClick()
        waitForIdle()
        save(onAllNodes(isPopup())[0].captureToImage(), "select-open")
        onNodeWithText("Cherry").performClick()
        awaitValue(fruit, "Cherry")
        onAllNodes(isPopup()).assertCountEquals(0)
        fruit.value = "Apple"
        waitUntil("select shows Apple", 2_000) { onAllNodesWithText("Apple").fetchSemanticsNodes().isNotEmpty() }
        onNodeWithText("Apple").performClick()
        waitForIdle()
        onAllNodes(isPopup()).assertCountEquals(1)
        onNodeWithText("Below").performClick()
        waitForIdle()
        onAllNodes(isPopup()).assertCountEquals(0)
        val select = onNode(hasText("Apple") and !hasAnyAncestor(isPopup()))
        select.performClick()
        waitForIdle()
        select.performClick()
        waitForIdle()
        onAllNodes(isPopup()).assertCountEquals(0)

        assertEquals("Apple", element.driverValue)
        runBlocking { element.driverActions.getValue("setValue")(listOf("Cherry")) }
        awaitValue(fruit, "Cherry")
        root.close()
    }

    @Test
    fun autoCompleteSuggestsAndChooses() = runComposeUiTest {
        val fruit = Signal("")
        val root = show {
            col {
                fieldTheme.autoCompleteTextField {
                    suggestions = listOf("Apple", "Grape", "Cherry")
                    content bind fruit
                }
            }
        }
        field(0).performTextInput("ap")
        waitForIdle()
        onNodeWithText("Apple").assertExists()
        onNodeWithText("Grape").assertExists()
        onNodeWithText("Cherry").assertDoesNotExist()
        save(onAllNodes(isPopup())[0].captureToImage(), "autocomplete-open")
        onNodeWithText("Grape").performClick()
        awaitValue(fruit, "Grape")
        field(0).assertTextEquals("Grape")
        onAllNodes(isPopup()).assertCountEquals(0)
        root.close()
    }

    @Test
    fun formScreenshot() = runComposeUiTest {
        val root = show {
            col {
                text("Inputs")
                fieldTheme.textArea { hint = "Notes" }
                fieldTheme.numberInput { content.value = 1234.5 }
                fieldTheme.phoneNumberInput { hint = "Phone" }
                fieldTheme.localDateField { content.value = LocalDate(2024, 6, 1) }
                fieldTheme.localTimeField { }
                fieldTheme.localDateTimeField { }
                fieldTheme.select { bind(Signal("Banana"), Constant(listOf("Apple", "Banana"))) { it } }
                row { fieldTheme.select { bind(Signal("Short"), Constant(listOf("Short", "A much longer option"))) { it } } }
                fieldTheme.autoCompleteTextField { hint = "Fruit" }
            }
        }
        awaitText(1, "1,234.5")
        save(runBlocking { root.screenshot() }, "inputs")
        root.close()
    }
}
