package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.titledSection


@Routable("forms")
public object FormsPage : Page {




    public val externals = HashMap<String, Property<String>>()
    public fun leafExample(propName: String): FormLeaf {
        val prop = externals.getOrPut(propName) { Property("Test") }
        return FormLeaf(
            title = propName,
            editor = {
                label {
                    content = propName
                    textInput { content bind prop }
                }
            },
            viewer = {
                row {
                    text {
                        content = propName
                    }
                    text {
                        ::content { prop() }
                    }
                }
            }
        )
    }

    public val lp = Property(false)
    public val form = FormSection(
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
                        ) + (if (lp()) {
                            listOf(
                                leafExample("Paperwork Entry"),
                            )
                        } else listOf()))
                    }
                )
            )
        }
    )

    public override fun ViewWriter.render(): ViewModifiable = run {
        scrolling - titledSection("Form Testing") {
            renderForm(form)
            renderFormReadOnly(form)
        }
    }
}

public fun ViewWriter.renderForm(section: FormSection) {
    titledSection(
        titleSetup = { content = section.title },
        content = {
            col {
                forEach(shared(action = section.subsections)) {
                    renderForm(it)
                }
            }
            col {
                forEach(shared(action = section.leaves)) {
                    it.editor(this)
                }
            }
        }
    )
}

public fun ViewWriter.renderFormReadOnly(section: FormSection) {
    titledSection(
        titleSetup = { content = section.title },
        content = {
            col {
                forEach(shared(action = section.subsections)) {
                    renderFormReadOnly(it)
                }
            }
            col {
                forEach(shared(action = section.leaves)) {
                    it.viewer(this)
                }
            }
        }
    )
}

public data class FormIssue(
    public val field: String,
    public val summary: String,
    public val description: String,
    public val importance: Importance
) {
    public enum class Importance {
        WARNING, ERROR
    }
}

public data class FormSection(
    public val title: String,
    public val icon: Icon? = null,
    public val helperText: String? = null,
    public val directIssues: ReactiveContext.() -> List<FormIssue> = { listOf() },
    public val leaves: ReactiveContext.() -> List<FormLeaf> = { listOf() },
    public val subsections: ReactiveContext.() -> List<FormSection> = { listOf() },
) {
    public override fun toString(): String = title
}

public data class FormLeaf(
    public val title: String,
    public val icon: Icon? = null,
    public val helperText: String? = null,
    public val directWorkSize: Int = 1,
    public val directIssues: ReactiveContext.() -> List<FormIssue> = { listOf() },
    public val editor: ViewWriter.() -> Unit,
    public val viewer: ViewWriter.() -> Unit,
) {
    public override fun toString(): String = title
}
