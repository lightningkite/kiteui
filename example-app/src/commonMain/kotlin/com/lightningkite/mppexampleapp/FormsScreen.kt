package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.titledSection

@Routable("forms")
object FormsScreen : Screen {

    val externals = HashMap<String, Property<String>>()
    fun leafExample(propName: String): FormLeaf {
        val prop = externals.getOrPut(propName) { Property("Test") }
        return FormLeaf(
            title = propName,
            editor = {
                label {
                    content = propName
                    textField { content bind prop }
                }
            },
            viewer = {
                row {
                    text {
                        content = propName
                    }
                    text {
                        ::content { prop.await() }
                    }
                }
            }
        )
    }

    val lp = Property(false)
    val form = FormSection(
        title = "Vehicle for Sale",
        subsections = {
            listOf(
                FormSection(
                    title = "Vehicle Information",
                    leaves = {
                        listOf(
                            leafExample("Year"),
                            leafExample("Make"),
                            leafExample("Model"),
                            leafExample("Submodel"),
                        )
                    }
                ),
                FormSection(
                    title = "Sale Information",
                    leaves = {
                        listOf(
                            leafExample("Mileage"),
                            leafExample("Price"),
                            leafExample("Seller"),
                        )
                    }
                ),
                FormSection(
                    title = "Requires Legal Paperwork",
                    leaves = {
                        (listOf(
                            FormLeaf(
                                title = "Requires Legal Paperwork",
                                editor = {
                                    row {
                                        checkbox {
                                            checked bind lp
                                        }
                                        text("Requires legal paperwork?")
                                    }
                                },
                                viewer = {}
                            )
                        ) + (if(lp.await()) {
                            listOf(
                                leafExample("Paperwork Entry"),
                            )
                        } else listOf()))
                    }
                )
            )
        }
    )

    override fun ViewWriter.render() {
        titledSection("Form Testing") {
            renderForm(form)
            renderFormReadOnly(form)
        } in scrolls
    }
}

fun ViewWriter.renderForm(section: FormSection) {
    titledSection(
        titleSetup = { content = section.title },
        content = {
            col {
                forEach(shared(section.subsections)) {
                    renderForm(it)
                }
            }
            col {
                forEach(shared(section.leaves)) {
                    it.editor(this)
                }
            }
        }
    )
}

fun ViewWriter.renderFormReadOnly(section: FormSection) {
    titledSection(
        titleSetup = { content = section.title },
        content = {
            col {
                forEach(shared(section.subsections)) {
                    renderFormReadOnly(it)
                }
            }
            col {
                forEach(shared(section.leaves)) {
                    it.viewer(this)
                }
            }
        }
    )
}


data class FormIssue(
    val field: String,
    val summary: String,
    val description: String,
    val importance: Importance
) {
    enum class Importance {
        WARNING, ERROR
    }
}

data class FormSection(
    val title: String,
    val icon: Icon? = null,
    val helperText: String? = null,
    val directIssues: suspend CalculationContext.() -> List<FormIssue> = { listOf() },
    val leaves: suspend CalculationContext.() -> List<FormLeaf> = { listOf() },
    val subsections: suspend CalculationContext.() -> List<FormSection> = { listOf() },
) {
    override fun toString(): String = title
}

data class FormLeaf(
    val title: String,
    val icon: Icon? = null,
    val helperText: String? = null,
    val directWorkSize: Int = 1,
    val directIssues: suspend CalculationContext.() -> List<FormIssue> = { listOf() },
    val editor: ViewWriter.() -> Unit,
    val viewer: ViewWriter.() -> Unit,
) {
    override fun toString(): String = title
}
