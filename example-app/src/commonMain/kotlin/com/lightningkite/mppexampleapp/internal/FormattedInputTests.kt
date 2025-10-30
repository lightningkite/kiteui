package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.bold
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("test/formatted-input")
class FormattedInputTests : Page {
    val phone = Signal("")
    val general = Signal("")
    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            field("General Formatted Input") {
                formattedTextInput {
                    hint = "(Whatever you typed)"
                    content bind general

                    format(
                        isRawData = { it != '(' && it != ')' },
                        formatter = { clean -> if (clean.isNotBlank()) "($clean)" else "" }
                    )
                }
            }

            row {
                bold.text("Stored:")
                text { ::content { general() } }
            }

            space()

            field("US Phone Number") {
                sizeConstraints(height = 3.rem).phoneNumberInput {
                    format = PhoneNumberFormat.USA
                    hint = "(123) 456-7890"
                    content bind phone
                }
            }

            row {
                bold.text("Stored: ")
                text { ::content { phone() } }
            }

            space()

            field("Number Field") {
                numberInput {
                    content bind Signal<Double?>(null)
                }
            }

            field("Integer Field") {
                numberInput {
                    keyboardHints = KeyboardHints.integer
                    content bind Signal(null)
                }
            }
        }
    }
}