package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class FocusTest {
    // Like the web's :focus-visible: clicking a button doesn't show focus, clicking into a text field does.
    @Test
    fun clickShowsFocusOnTextFieldsOnly() = runKiteUiTest {
        lateinit var button: Button
        lateinit var field: TextInput
        showKiteUi {
            col {
                button = button { text("Press") }
                field = textInput { hint = "Field" }
                text("Elsewhere")
            }
        }
        fun Element.themeId() = underlyingNativeElement.themeAndBack.theme.id

        val buttonResting = button.themeId()
        onNodeWithText("Press").performMouseInput { click(); moveTo(Offset(-10f, -10f)) }
        waitForIdle()
        onNodeWithText("Elsewhere").performMouseInput { moveTo(center) }
        waitForIdle()
        assertEquals(buttonResting, button.themeId(), "A clicked button shouldn't look focused")

        val fieldResting = field.themeId()
        onNode(hasSetTextAction()).performMouseInput { click() }
        onNodeWithText("Elsewhere").performMouseInput { moveTo(center) }
        waitForIdle()
        assertNotEquals(fieldResting, field.themeId(), "A clicked text field should look focused")
    }

    @Test
    fun requestFocusFocusesAFieldOnScreen() = runKiteUiTest {
        lateinit var field: TextInput
        showKiteUi { col { field = textInput { hint = "Field" } } }
        runBlocking(Dispatchers.Main) { field.requestFocus() }
        onNode(hasSetTextAction()).assertIsFocused()
    }

    // As on the web, focusing an element that isn't on screen does nothing, then or once it's shown.
    @Test
    fun requestFocusLapsesForElementsNotOnScreen() = runKiteUiTest {
        lateinit var unshown: TextInput
        lateinit var invisible: TextInput
        showKiteUi {
            col {
                unshown = textInput {
                    hint = "Unshown"
                    shown = false
                }
                invisible = textInput {
                    hint = "Invisible"
                    visible = false
                }
            }
        }
        runBlocking(Dispatchers.Main) {
            unshown.requestFocus()
            invisible.requestFocus()
        }
        waitForIdle()
        Thread.sleep(300)
        runBlocking(Dispatchers.Main) {
            unshown.shown = true
            invisible.visible = true
        }
        waitForIdle()
        onAllNodes(hasSetTextAction()).assertCountEquals(2)
        onAllNodes(isFocused()).assertCountEquals(0)
    }
}
