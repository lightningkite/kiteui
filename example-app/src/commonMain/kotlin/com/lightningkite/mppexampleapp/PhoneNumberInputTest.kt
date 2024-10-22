package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.bind
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.bold
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field

@Routable("test/phone-number")
class PhoneNumberInputTest : Screen {
    val phone = Property("")
    val general = Property("")
    override fun ViewWriter.render() {
        col {
            field("Phone Number") {
                phoneNumberInput {
                    hint = "(123) 456-7890"
                    content bind phone
                }
            }

            row {
                bold - text("Stored:")
                text { ::content { phone() } }
            }

            space()

            field("General Formatted Input") {
                formattedTextInput {
                    hint = "(Whatever you typed)"
                    content bind general

                    format(
                        isRawData = { it != '(' && it != ')' },
                        formatter = { if (it.isNotBlank()) "($it)" else "" }
                    )
                }
            }

            row {
                bold - text("Stored:")
                text { ::content { general() } }
            }

            space()

            field("Number Comparison") {
                numberInput {
                    content bind Property<Double?>(null)
                }
            }
        }
    }
}