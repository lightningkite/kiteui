package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.InvalidSemantic
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

    val draft = Draft(
        TestModel(
            list = List(30) { "" }
        )
    )

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

            val number = draft
                .lens(
                    name = "number",
                    get = { it.number },
                    modify = { o, it -> o.copy(number = it) }
                )
                .validate {
                    if (it == null) "Cannot be null"
                    else if (it <= 0) "Must be greater than 0"
                    else null
                }

            val strings = draft
                .lens(
                    get = { it.list },
                    modify = { o, it -> o.copy(list = it) }
                )
                .lensByElementAssumingSetNeverManipulates { it.vet { if (it.isBlank()) throw InvalidException("Cannot be blank"); it } }


            col {
                spacing = 0.2.rem
                fieldTheme - textField {
                    hint = "Name"
                    validates(name)
                    content bind name
                }
                onlyWhen { name.invalid() != null } - text { ::content { name.invalid()?.summary ?: "" } }
            }

            warningText {
                val otherText = Property("").validate { if (it == "abc") "Cannot be abc" else null }
                ::content { otherText.invalid()?.summary }

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
                onlyWhen { id.invalid() != null } - text { ::content { id.invalid()?.summary ?: "" } }
            }

            col {
                spacing = 0.2.rem
                fieldTheme - numberField {
                    hint = "Number"
                    validates(number)
                    content bind number.asDouble()
                }
                onlyWhen { number.invalid() != null } - text { ::content { number.invalid()?.summary ?: "" } }
            }

            expanding - scrolls - card - recyclerView {
                spacing = 1.rem
                children(strings) { str ->
                    col {
                        fieldTheme - textField {
                            content bind str.flatten()
                            dynamicTheme { if (str().invalid() != null) InvalidSemantic else null }
                        }
                        onlyWhen { str().invalid() != null } - text { ::content { str().invalid()?.summary ?: "" } }
                    }
                }
            }
        }
    }
}