package com.lightningkite.kiteui.views

import androidx.compose.ui.test.*
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.toAwtImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RenderTest {
    private val theme = Theme.flat2("test", 0.6.turns)

    @Test
    fun loginLikePage() = runComposeUiTest {
        val name = Signal("")
        val greeting = Signal("Nobody greeted yet")
        val root = runBlocking(Dispatchers.Main) {
            KiteUiRoot("kiteui-compose-test", { theme }) {
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
        }
        setContent { root.Content() }

        onNodeWithText("Nobody greeted yet").assertExists()
        onNode(hasSetTextAction()).performTextInput("Ada")
        waitForIdle()
        check(name.value == "Ada") { "Typing didn't reach the bound signal: '${name.value}'" }
        onNodeWithText("Greet").performClick()
        waitForIdle()
        onNodeWithText("Hello, Ada!").assertExists()

        val out = File(System.getProperty("user.dir"), "build/shots").apply { mkdirs() }
        ImageIO.write(runBlocking { root.screenshot() }.toAwtImage(), "png", File(out, "login.png"))
        root.close()
    }
}
