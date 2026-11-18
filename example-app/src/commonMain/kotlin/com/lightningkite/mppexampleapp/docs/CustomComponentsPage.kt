package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.core.*

@Routable("docs/custom-components")
object CustomComponentsPage : DocPage {
    override val covers: List<String> = listOf(
        "custom components", "components", "reusable", "composition",
        "component design", "ViewWriter", "ElementWriter", "extensions"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Custom Components Guide") {
                text("Learn how to create reusable, composable components in KiteUI.")

                space()

                titledSection("What are Components?") {
                    text("In KiteUI, a component is an extension function on ElementWriter.CanAddTheme. That receiver gives it theme modifiers, and lets callers put modifiers in front of it, like expanding.myComponent().")

                    space()
                    code {
                        content = """
                            fun ElementWriter.CanAddTheme.myComponent() = card.col {
                                h3("My Component")
                                text("Component content here")
                            }

                            // Usage
                            col {
                                myComponent()
                                expanding.myComponent()  // modifiers apply to the component's outer view
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Component Design Principles") {
                    card.col {
                        text("✓ Components should be reusable (used more than once)")
                        text("✓ Single-use components should be inlined")
                        text("✓ Take minimal parameters unique to each usage")
                        text("✓ Components should load their own data")
                        text("✓ Write one outer view, so a caller's modifiers apply to the whole component")
                    }
                }

                space()

                titledSection("Basic Component") {
                    text("Create a simple component with parameters:")

                    space()
                    example("""
                        fun ElementWriter.CanAddTheme.userCard(name: String, email: String) = card.col {
                            h4(name)
                            subtext(email)
                        }

                        // Usage
                        col {
                            userCard("Alice", "alice@example.com")
                            userCard("Bob", "bob@example.com")
                        }
                    """.trimIndent()) {
                        fun ElementWriter.CanAddTheme.userCard(name: String, email: String) = card.col {
                            h4(name)
                            subtext(email)
                        }

                        col {
                            userCard("Alice", "alice@example.com")
                            userCard("Bob", "bob@example.com")
                        }
                    }
                }

                space()

                titledSection("Components with State") {
                    text("Components can manage their own internal state:")

                    space()
                    example("""
                        fun ElementWriter.CanAddTheme.counter(initialValue: Int = 0) = card.row {
                            val count = Signal(initialValue)
                            button {
                                text("-")
                                onClick { count.value-- }
                            }
                            centered.expanding.text {
                                ::content { count().toString() }
                            }
                            button {
                                text("+")
                                onClick { count.value++ }
                            }
                        }

                        // Usage
                        col {
                            counter()
                            counter(initialValue = 10)
                        }
                    """.trimIndent()) {
                        fun ElementWriter.CanAddTheme.counter(initialValue: Int = 0) = card.row {
                            val count = Signal(initialValue)
                            button {
                                text("-")
                                onClick { count.value-- }
                            }
                            centered.expanding.text {
                                ::content { count().toString() }
                            }
                            button {
                                text("+")
                                onClick { count.value++ }
                            }
                        }

                        col {
                            counter()
                            counter(initialValue = 10)
                        }
                    }
                }

                space()

                titledSection("Components with Callbacks") {
                    text("Pass suspend callbacks for user interactions; errors they throw are shown automatically:")

                    space()
                    example("""
                        fun ElementWriter.CanAddTheme.deleteRow(name: String, onDelete: suspend () -> Unit) = card.row {
                            expanding.text(name)
                            danger.button {
                                text("Delete")
                                onClick {
                                    context.confirmDanger("Delete ${'$'}name", "This can't be undone.", actionName = "Delete") {
                                        onDelete()
                                    }
                                }
                            }
                        }

                        // Usage
                        deleteRow("Report.pdf") {
                            context.toast("Deleted")
                        }
                    """.trimIndent()) {
                        fun ElementWriter.CanAddTheme.deleteRow(name: String, onDelete: suspend () -> Unit) = card.row {
                            expanding.text(name)
                            danger.button {
                                text("Delete")
                                onClick {
                                    context.confirmDanger("Delete $name", "This can't be undone.", actionName = "Delete") {
                                        onDelete()
                                    }
                                }
                            }
                        }

                        col {
                            deleteRow("Report.pdf") {
                                context.toast("Deleted")
                            }
                        }
                    }
                }

                space()

                titledSection("Components with Children") {
                    text("Accept child content using lambda parameters:")

                    space()
                    example("""
                        fun ElementWriter.CanAddTheme.section(
                            title: String,
                            content: ViewWriter.() -> Unit,
                        ) = card.col {
                            h4(title)
                            separator()
                            content()
                        }

                        // Usage
                        section("User Information") {
                            text("Name: John Doe")
                            text("Email: john@example.com")
                        }
                    """.trimIndent()) {
                        fun ElementWriter.CanAddTheme.section(
                            title: String,
                            content: ViewWriter.() -> Unit,
                        ) = card.col {
                            h4(title)
                            separator()
                            content()
                        }

                        section("User Information") {
                            text("Name: John Doe")
                            text("Email: john@example.com")
                        }
                    }
                }

                space()

                titledSection("Data-Loading Components") {
                    text("Components that load their own data are easier to debug:")

                    space()
                    code {
                        content = """
                            fun ElementWriter.CanAddTheme.userProfile(userId: Int) = card.col {
                                val user = rememberSuspending { fetchUser(userId) }   // shows loading automatically
                                h3 { ::content { user().name } }
                                text { ::content { user().email } }
                            }

                            // Usage - just pass the ID
                            userProfile(userId = 123)
                        """.trimIndent()
                    }

                    space()
                    text("This is better than making every caller load the data and pass it in.")
                }

                space()

                titledSection("Modifier Order") {
                    text("The compiler enforces modifier order: alignment → weight → shownWhen → sizing → theme → scrolling → element.")

                    space()
                    code {
                        content = """
                            centered.sizeConstraints(maxWidth = 40.rem).card.scrolling.col {
                                text("Content")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Component Composition") {
                    text("Build complex components from simpler ones:")

                    space()
                    code {
                        content = """
                            fun ElementWriter.CanAddTheme.avatar(name: String) = card.text(name.take(2).uppercase())

                            fun ElementWriter.CanAddTheme.userListItem(user: User) = card.row {
                                centered.sizeConstraints(width = 3.rem, height = 3.rem).avatar(user.name)
                                expanding.col {
                                    text(user.name)
                                    subtext(user.email)
                                }
                                button {
                                    text("View")
                                    onClick { context.pageNavigator.navigate(UserPage(user.id)) }
                                }
                            }

                            // Usage
                            col {
                                for (user in users) userListItem(user)
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Common Patterns") {
                    card.col {
                        h5("Simple Display Component:")
                        code {
                            content = """
                                fun ElementWriter.CanAddTheme.statusBadge(status: String) = themed(
                                    when (status) {
                                        "active" -> AffirmativeSemantic
                                        "error" -> DangerSemantic
                                        else -> CardSemantic
                                    }
                                ).text(status)
                            """.trimIndent()
                        }

                        space()
                        h5("Interactive Component:")
                        code {
                            content = """
                                fun ElementWriter.CanAddTheme.labeledCheckbox(label: String, checked: Signal<Boolean>) = row {
                                    centered.checkbox { this.checked bind checked }
                                    centered.expanding.text(label)
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Container Component:")
                        code {
                            content = """
                                fun ElementWriter.CanAddTheme.panel(
                                    title: String,
                                    content: ViewWriter.() -> Unit,
                                ) = card.col {
                                    h4(title)
                                    content()
                                }
                            """.trimIndent()
                        }
                    }
                }

                space()

                titledSection("When NOT to Create a Component") {
                    text("Avoid creating components for:")
                    card.col {
                        text("• One-time use cases - just inline the code")
                        text("• Overly specific scenarios - keep components general")
                        text("• Trivial wrappers that add no value")
                    }

                    space()
                    text("Example of what NOT to do:")
                    code {
                        content = """
                            // DON'T - this is too specific and won't be reused
                            fun ElementWriter.CanAddTheme.loginPageTitle() = h1("Login to MyApp")

                            // DO - just inline it
                            h1("Login to MyApp")
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices Summary") {
                    card.col {
                        text("✓ Create components for reusable UI patterns")
                        text("✓ Use ElementWriter.CanAddTheme as the receiver")
                        text("✓ Keep parameters minimal and focused")
                        text("✓ Let components load their own data")
                        text("✓ Compose complex components from simple ones")
                        text("✓ Inline single-use code")
                    }
                }
            }
        }
    }
}
