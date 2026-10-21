package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RenderTest {
    @Test
    fun loginLikePage() = runKiteUiTest {
        val name = Signal("")
        val greeting = Signal("Nobody greeted yet")
        val root = showKiteUi {
            col {
                text("Sign in")
                card.col {
                    text("Name")
                    textInput {
                        hint = "Your name"
                        content bind name
                    }
                    important.button {
                        text("Greet")
                        onClick { greeting.value = "Hello, ${name.value}!" }
                    }
                }
                text { ::content { greeting() } }
            }
        }

        onNodeWithText("Nobody greeted yet").assertExists()
        onNode(hasSetTextAction()).performTextInput("Ada")
        waitForIdle()
        check(name.value == "Ada") { "Typing didn't reach the bound signal: '${name.value}'" }
        onNodeWithText("Greet").performClick()
        waitForIdle()
        onNodeWithText("Hello, Ada!").assertExists()

        saveScreenshot(root, "login")
    }
}
