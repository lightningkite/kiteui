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
        "component design", "ViewWriter", "extensions"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Custom Components Guide") {
                text("Learn how to create reusable, composable components in KiteUI.")

                space()

                titledSection("What are Components?") {
                    text("In KiteUI, components are extension functions on ViewWriter that return Unit. They encapsulate reusable UI logic.")

                    space()
                    code {
                        content = """
                            fun ViewWriter.myComponent(): Unit = run {
                                card - col {
                                    h3("My Component")
                                    text("Component content here")
                                }
                            }

                            // Usage
                            col {
                                myComponent()
                                myComponent()  // Reuse
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Component Design Principles") {
                    text("From GoodKiteuiCode.md - follow these guidelines:")

                    space()
                    card.col {
                        text("✓ Components should be reusable (used more than once)")
                        text("✓ Single-use components should be inlined")
                        text("✓ Take minimal parameters unique to each usage")
                        text("✓ Components should load their own data")
                        text("✓ Keep components focused and small")
                    }
                }

                space()

                titledSection("Basic Component") {
                    text("Create a simple component with parameters:")

                    space()
                    example("""
                        fun ViewWriter.userCard(name: String, email: String): Unit = run {
                            card - col {
                                h4(name)
                                subtext(email)
                            }
                        }

                        // Usage
                        col {
                            userCard("Alice", "alice@example.com")
                            userCard("Bob", "bob@example.com")
                        }
                    """.trimIndent()) {
                        fun ViewWriter.userCard(name: String, email: String): Unit = run {
                            card.col {
                                h4(name)
                                subtext(email)
                            }
                        }

                        card.col {
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
                        fun ViewWriter.counter(initialValue: Int = 0): Unit = run {
                            val count = Signal(initialValue)

                            card - row {
                                button {
                                    text("-")
                                    onClick { count.value-- }
                                }
                                centered - expanding - text {
                                    ::content { count().toString() }
                                }
                                button {
                                    text("+")
                                    onClick { count.value++ }
                                }
                            }
                        }

                        // Usage
                        col {
                            counter()
                            counter(initialValue = 10)
                        }
                    """.trimIndent()) {
                        fun ViewWriter.counter(initialValue: Int = 0): Unit = run {
                            val count = Signal(initialValue)

                            card.row {
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
                        }

                        col {
                            counter()
                            counter(initialValue = 10)
                        }
                    }
                }

                space()

                titledSection("Components with Callbacks") {
                    text("Pass callbacks for user interactions:")

                    space()
                    example("""
                        fun ViewWriter.confirmDialog(
                            message: String,
                            onConfirm: () -> Unit,
                            onCancel: () -> Unit
                        ): Unit = run {
                            card - col {
                                text(message)
                                row {
                                    expanding - button {
                                        text("Cancel")
                                        onClick { onCancel() }
                                    }
                                    expanding - important - button {
                                        text("Confirm")
                                        onClick { onConfirm() }
                                    }
                                }
                            }
                        }

                        // Usage
                        val showDialog = Signal(false)

                        button {
                            text("Show Dialog")
                            onClick { showDialog.value = true }
                        }

                        shownWhen { showDialog() } - confirmDialog(
                            message = "Are you sure?",
                            onConfirm = {
                                toast("Confirmed!")
                                showDialog.value = false
                            },
                            onCancel = {
                                showDialog.value = false
                            }
                        )
                    """.trimIndent()) {

                        fun ElementWriter.CanAddTheme.confirmDialog(
                            message: String,
                            onConfirm: () -> Unit,
                            onCancel: () -> Unit
                        ) = card.col {
                            text(message)
                            row {
                                expanding.button {
                                    text("Cancel")
                                    onClick { onCancel() }
                                }
                                expanding.important.button {
                                    text("Confirm")
                                    onClick { onConfirm() }
                                }
                            }
                        }

                        card.col {
                            val showDialog = Signal(false)

                            button {
                                text("Show Dialog")
                                onClick { showDialog.value = true }
                            }

                            shownWhen { showDialog() }.confirmDialog(
                                message = "Are you sure?",
                                onConfirm = {
                                    toast("Confirmed!")
                                    showDialog.value = false
                                },
                                onCancel = {
                                    showDialog.value = false
                                }
                            )
                        }
                    }
                }

                space()

                titledSection("Components with Children") {
                    text("Accept child content using lambda parameters:")

                    space()
                    example("""
                        fun ViewWriter.section(
                            title: String,
                            content: ViewWriter.() -> Unit
                        ): Unit = run {
                            card - col {
                                h4(title)
                                separator()
                                content()
                            }
                        }

                        // Usage
                        section("User Information") {
                            text("Name: John Doe")
                            text("Email: john@example.com")
                        }
                    """.trimIndent()) {
                        fun ElementWriter.CanAddTheme.section(
                            title: String,
                            content: ViewWriter.() -> Unit
                        ): Unit = run {
                            card.col {
                                h4(title)
                                separator()
                                content()
                            }
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
                            fun ViewWriter.userProfile(userId: Int): Unit = run {
                                val user = rememberSuspending {
                                    // Component loads its own data
                                    fetchUser(userId)
                                }

                                card - col {
                                    h3 { ::content { user().name } }
                                    text { ::content { user().email } }
                                }
                            }

                            // Usage - just pass the ID
                            userProfile(userId = 123)
                        """.trimIndent()
                    }

                    space()
                    text("This is better than:")
                    code {
                        content = """
                            // DON'T DO THIS - harder to debug
                            fun ViewWriter.userProfile(user: User): Unit = run {
                                card - col {
                                    h3(user.name)
                                    text(user.email)
                                }
                            }

                            // Caller has to manage data loading
                            val user = rememberSuspending { fetchUser(123) }
                            userProfile(user())
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Modifier Order") {
                    text("Remember: Position > Visibility > Scroll > Theme")

                    space()
                    code {
                        content = """
                            fun ViewWriter.myComponent(): Unit = run {
                                // Correct order
                                centered - scrolling - card - col {
                                    text("Content")
                                }
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
                            fun ViewWriter.avatar(name: String, size: Dimension = 40.dp): Unit = run {
                                sizeConstraints(width = size, height = size) - card - centered - text {
                                    content = name.take(2).uppercase()
                                }
                            }

                            fun ViewWriter.userListItem(
                                name: String,
                                email: String,
                                onClick: () -> Unit
                            ): Unit = run {
                                card - row {
                                    avatar(name)
                                    expanding - col {
                                        text(name)
                                        subtext(email)
                                    }
                                    button {
                                        text("View")
                                        onClick { onClick() }
                                    }
                                }
                            }

                            fun ViewWriter.userList(users: List<User>): Unit = run {
                                col {
                                    for (user in users) {
                                        userListItem(
                                            name = user.name,
                                            email = user.email,
                                            onClick = { pageNavigator.navigate(UserPage(user.id)) }
                                        )
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Testing Components") {
                    text("Keep components testable by:")
                    card.col {
                        text("• Accepting data as parameters rather than hardcoding")
                        text("• Using callbacks for side effects")
                        text("• Keeping components pure when possible")
                        text("• Avoiding global state")
                    }
                }

                space()

                titledSection("Common Patterns") {
                    card.col {
                        h5("Simple Display Component:")
                        code {
                            content = """
                                fun ViewWriter.statusBadge(status: String): Unit = run {
                                    val semantic = when(status) {
                                        "active" -> ::affirmative
                                        "error" -> ::danger
                                        else -> ::card
                                    }
                                    semantic() - text(status)
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Interactive Component:")
                        code {
                            content = """
                                fun ViewWriter.toggle(
                                    label: String,
                                    checked: Signal<Boolean>
                                ): Unit = run {
                                    row {
                                        centered - checkbox { this.checked bind checked }
                                        centered - expanding - text(label)
                                    }
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Container Component:")
                        code {
                            content = """
                                fun ViewWriter.panel(
                                    title: String,
                                    content: ViewWriter.() -> Unit
                                ): Unit = run {
                                    card - col {
                                        bar - h4(title)
                                        content()
                                    }
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
                            fun ViewWriter.loginPageTitle(): Unit = run {
                                h1("Login to MyApp")
                            }

                            // DO - just inline it
                            h1("Login to MyApp")
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices Summary") {
                    card.col {
                        text("✓ Create components for reusable UI patterns")
                        text("✓ Keep parameters minimal and focused")
                        text("✓ Let components load their own data")
                        text("✓ Use callbacks for interactions")
                        text("✓ Follow modifier ordering rules")
                        text("✓ Compose complex components from simple ones")
                        text("✓ Make components testable")
                        text("✓ Inline single-use code")
                    }
                }
            }
        }
    }
}
