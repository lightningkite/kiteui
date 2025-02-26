package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.delay
import com.lightningkite.kiteui.fetch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.pageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.readable.Property
import com.lightningkite.readable.await
import com.lightningkite.readable.bind
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.Resources
import kotlinx.coroutines.delay

@Routable("sample/login")
object SampleLogInPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val email = Property("")
        val password = Property("")
        frame {
            spacing = 0.rem
            image {
                source = Resources.imagesSolera
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }
            padded - scrolling - col {
                expanding - space()
                centered - sizeConstraints(maxWidth = 50.rem) - card - col {
                    h1 { content = "My App" }
                    label {
                        content = "Email"
                        sizeConstraints(width = 20.rem) - fieldTheme - textField {
                            hint = "Email"
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }
                    label {
                        content = "Password"
                        sizeConstraints(width = 20.rem) - fieldTheme - textField {
                            hint = "Password"
                            keyboardHints = KeyboardHints.password
                            content bind password
                            action = Action(
                                title = "Log In",
                                icon = Icon.login,
                            ) {
                                fakeLogin(email)
                            }
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
        pageNavigator.navigate(ControlsPage)
    }
}