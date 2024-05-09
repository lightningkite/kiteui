package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.bind
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("sample/login")
object SampleLogInScreen : Screen {
    override fun ViewWriter.render() {
        val email = Property("")
        val password = Property("")
        stack {
            spacing = 0.rem
            image {
                source = Resources.imagesSolera
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }
            padded - scrolls - col {
                expanding - space()
                centered - sizeConstraints(maxWidth = 50.rem) - themeFromLast {
                    it.copy(
                        background = Color.orange.withAlpha(0.25f),
                        backdropFilters = listOf(BackdropFilter.Blur(1.rem)),
                        card = { it }
                    )
                } - col {
                    h1 { content = "My App" }
                    label {
                        content = "Email"
                        textField {
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }
                    label {
                        content = "Password"
                        textField {
                            keyboardHints = KeyboardHints.password
                            content bind password
                            action = Action(
                                title = "Log In",
                                icon = Icon.login,
                                onSelect = {
                                    fakeLogin(email)
                                }
                            )
                        }
                    }
                    important - button {
                        h6 { content = "Log In" }
                        onClick {
                            delay(1000)
                            fakeLogin(email)
                        }
                    }
                }
                expanding - space()
            }
        } 
    }

    private suspend fun ViewWriter.fakeLogin(email: Property<String>) {
        fetch("fake-login/${email.await()}")
        navigator.navigate(ControlsScreen)
    }
}