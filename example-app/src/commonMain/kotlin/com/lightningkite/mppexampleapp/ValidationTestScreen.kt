package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.lensing.*
import com.lightningkite.kiteui.reactive.lensing.lens
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

class WarningText(private val container: RowOrCol): ViewWriter(), CalculationContext by container {
    override val context: RContext get() = container.context
    override fun addChild(view: RView) = container.addChild(view)

    private val _content = Property<String?>(null)
    var content by _content
    private val _exists = Property(true)
    var exists by _exists

    fun ViewWriter.render() {
        onlyWhen { _content() != null && _exists() } - text { ::content { _content() ?: "" } }
    }
}
inline fun ViewWriter.warningText(setup: WarningText.() -> Unit) {
    col {
        spacing = 0.px
        WarningText(this).run {
            setup()
            render()
        }
    }
}

@Routable("/validation")
class ValidationTestScreen: Screen {
    data class TestModel(
        val name: String = "",
        val id: Int? = null,
        val number: Int? = null,
        val list: List<String> = emptyList()
    )

    val draft = Draft(TestModel())

    override fun ViewWriter.render() {
        col {
            spacing = 1.5.rem

            val name = draft.lens(
                get = { it.name },
                modify = { o, it -> o.copy(name = it) }
            ).validate { if (it.isBlank()) "Cannot be blank" else null }

            val id = draft.lens(
                get = { it.id?.toDouble() },
                modify = { o, it -> o.copy(id = it?.toInt()) }
            ).validate {
                if (it == null) "Cannot be blank"
                else {
                    if (it.toString().contains('.')) "Must be an integer"
                    else null
                }
            }

            val number = draft.validationLens(
                get = { it.number?.toDouble() },
                modify = { o, it ->
                    if (it == null) throw InvalidException("Cannot be empty")
                    else {
                        val int = it.toInt()
                        if (it.toString().contains('.')) throw InvalidException("Must be an integer")
                        else if (int == 0) throw WarningException("Should be larger than 0", "0 does nothing here")
                        else o.copy(number = int)
                    }
                }
            )

            col {
                spacing = 0.2.rem
                fieldTheme - textField {
                    hint = "Name"
                    validates(name)
                    content bind name
                }
                onlyWhen { name.invalid() != null } - text { ::content { name.invalid()?.errorSummary ?: "" } }
            }

            warningText {
                val otherText = Property("").validate { if (it == "abc") "Cannot be abc" else null }
                ::content { otherText.invalid()?.errorSummary }

                fieldTheme - textField {
                    hint = "other text"
                    validates(otherText)
                    content bind otherText
                }
            }

            col {
                spacing = 0.2.rem
                fieldTheme - numberField {
                    hint = "ID"
                    validates(id)
                    content bind id
                }
                onlyWhen { id.invalid() != null } - text { ::content { id.invalid()?.errorSummary ?: "" } }
            }

            col {
                spacing = 0.2.rem
                fieldTheme - numberField {
                    hint = "Number"
                    validates(number)
                    content bind number
                }
                onlyWhen { number.invalid() != null } - text { ::content { number.invalid()?.errorSummary ?: "" } }
            }


        }
    }
}