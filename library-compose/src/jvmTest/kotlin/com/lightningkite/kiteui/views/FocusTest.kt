package com.lightningkite.kiteui.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class FocusTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    // Like the web's :focus-visible: clicking a button doesn't show focus, clicking into a text field does.
    @Test
    fun clickShowsFocusOnTextFieldsOnly() = runComposeUiTest {
        lateinit var button: Button
        lateinit var field: TextInput
        val root = runBlocking(Dispatchers.Main) {
            KiteUiRoot("kiteui-compose-test", { theme }) {
                col {
                    button = button { text("Press") }
                    field = textInput { hint = "Field" }
                    text("Elsewhere")
                }
            }
        }
        setContent { root.Content() }
        waitForIdle()
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
        root.close()
    }
}
