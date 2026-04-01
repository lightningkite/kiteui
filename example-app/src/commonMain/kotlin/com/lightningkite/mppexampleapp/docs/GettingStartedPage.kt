package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.core.*

@Routable("docs/getting-started")
object GettingStartedPage : DocPage {
    override val covers: List<String> = listOf(
        "getting started", "quickstart", "introduction", "setup", "installation",
        "hello world", "first app", "beginner", "tutorial"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Getting Started with KiteUI") {
                text("Welcome to KiteUI! This guide will walk you through creating your first KiteUI application.")

                space()

                titledSection("What is KiteUI?") {
                    text("KiteUI is a Kotlin Multiplatform UI framework inspired by Solid.js that uses native view components on each platform (Android, iOS, JVM, JS/Web).")

                    space()
                    h4("Key Features:")
                    card.col {
                        text("• Fine-grained reactivity - Only updates what needs to be updated")
                        text("• Semantic theming - Style by meaning, not appearance")
                        text("• URL-based navigation - Web-friendly routing")
                        text("• Small bundle sizes - Efficient for web deployment")
                        text("• Native components - Uses platform-native views")
                    }
                }

                space()

                titledSection("Installation") {
                    text("Add KiteUI to your Kotlin Multiplatform project:")

                    space()
                    h5("1. Add the Maven repository:")
                    code {
                        content = """
                            repositories {
                                maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
                            }
                        """.trimIndent()
                    }

                    space()
                    h5("2. Add the dependency:")
                    code {
                        content = """
                            kotlin {
                                sourceSets {
                                    commonMain.dependencies {
                                        api("com.lightningkite.kiteui:library:<version>")
                                    }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    h5("3. Apply the KiteUI Gradle plugin:")
                    code {
                        content = """
                            plugins {
                                id("com.lightningkite.kiteui")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Your First Page") {
                    text("Pages in KiteUI are objects that implement the Page interface. Let's create a simple page:")

                    space()
                    example("""
                        // Annotation: Routable("hello")
                        object HelloWorldPage : Page {
                            override fun ElementWriter.CanAddTheme.render(): Unit = run {
                                col {
                                    h1("Hello, KiteUI!")
                                    text("Welcome to your first page")
                                }
                            }
                        }
                    """.trimIndent()) {
                        card.col {
                            h1("Hello, KiteUI!")
                            text("Welcome to your first page")
                        }
                    }

                    space()
                    text("The @Routable annotation defines the URL path. This page would be accessible at /hello")
                }

                space()

                titledSection("Understanding ViewWriter") {
                    text("ViewWriter is the context in which you build your UI. It provides access to all view-building functions.")

                    space()
                    h5("Common View Functions:")
                    card.col {
                        text("• text() - Display text")
                        text("• button {} - Create a button")
                        text("• col {} - Vertical container")
                        text("• row {} - Horizontal container")
                        text("• textInput {} - Text input field")
                    }

                    space()
                    example("""
                        col {
                            h2("Container Example")
                            row {
                                text("Left")
                                text("Right")
                            }
                            button {
                                text("Click Me")
                            }
                        }
                    """.trimIndent()) {
                        card.col {
                            h2("Container Example")
                            row {
                                card.text("Left")
                                card.text("Right")
                            }
                            button {
                                text("Click Me")
                            }
                        }
                    }
                }

                space()

                titledSection("Adding Reactivity") {
                    text("KiteUI uses fine-grained reactivity. Create reactive state with Signal:")

                    space()
                    example("""
                        val count = Signal(0)

                        col {
                            text {
                                ::content { "Count: ${'$'}{count()}" }
                            }
                            button {
                                text("Increment")
                                onClick {
                                    count.value++
                                }
                            }
                        }
                    """.trimIndent()) {
                        card.col {
                            val count = Signal(0)

                            text {
                                ::content { "Count: ${count()}" }
                            }
                            button {
                                text("Increment")
                                onClick {
                                    count.value++
                                }
                            }
                        }
                    }

                    space()
                    text("Key points about reactivity:")
                    card.col {
                        text("• Signal creates reactive state")
                        text("• Read with count() - this tracks dependencies")
                        text("• Write with count.value = newValue")
                        text("• Use ::property { } syntax for reactive expressions")
                    }
                }

                space()

                titledSection("Using Modifiers") {
                    text("Modifiers style and position your views. Apply them with the - operator:")

                    space()
                    example("""
                        col {
                            card - text("I'm in a card")
                            important - button { text("Important!") }
                            centered - text("Centered text")
                        }
                    """.trimIndent()) {
                        col {
                            card.text("I'm in a card")
                            important.button { text("Important!") }
                            centered.text("Centered text")
                        }
                    }

                    space()
                    text("Modifier order matters: Position > Visibility > Scroll > Theme")
                }

                space()

                titledSection("Form Inputs") {
                    text("Create interactive forms by binding inputs to reactive state:")

                    space()
                    example("""
                        val name = Signal("")

                        col {
                            field("Your Name") {
                                textInput {
                                    content bind name
                                    hint = "Enter your name"
                                }
                            }
                            text {
                                ::content { "Hello, ${'$'}{name()}!" }
                            }
                        }
                    """.trimIndent()) {
                        card.col {
                            val name = Signal("")

                            field("Your Name") {
                                textInput {
                                    content bind name
                                    hint = "Enter your name"
                                }
                            }
                            text {
                                ::content {
                                    if (name().isNotBlank()) "Hello, ${name()}!"
                                    else "Enter your name above"
                                }
                            }
                        }
                    }

                    space()
                    text("The bind keyword creates a two-way binding between the input and the Signal.")
                }

                space()

                titledSection("Navigation") {
                    text("Navigate between pages using the pageNavigator:")

                    space()
                    code {
                        content = """
                            button {
                                text("Go to Cheat Sheet")
                                onClick {
                                    pageNavigator.navigate(CheatSheet)
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("You can also create links:")
                    code {
                        content = """
                            link {
                                text("Go to Cheat Sheet")
                                to = { CheatSheet }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Pages with Parameters") {
                    text("Create pages that accept URL parameters:")

                    space()
                    code {
                        content = """
                            // Annotation: Routable("user/{id}")
                            class UserPage(val id: String) : Page {
                                override fun ElementWriter.CanAddTheme.render(): Unit = run {
                                    col {
                                        h1("User: ${'$'}id")
                                        text("Viewing profile for user ${'$'}id")
                                    }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("Navigate to parametrized pages:")
                    code {
                        content = """
                            // Navigate with parameter
                            pageNavigator.navigate(UserDetailPage("123"))
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Next Steps") {
                    text("Now that you understand the basics, explore these topics:")

                    space()
                    card.col {
                        text("• Check out the CheatSheet for all available components")
                        text("• Learn about theming to style your app")
                        text("• Explore reactive patterns for complex state management")
                        text("• Read about view modifiers for layouts")
                        text("• Learn form validation patterns")
                    }
                }

                space()

                titledSection("Complete Example") {
                    text("Here's a complete simple app combining what we've learned:")

                    space()
                    code {
                        content = """
                            // Annotation: Routable("todo")
                            object TodoPage : Page {
                                override fun ElementWriter.CanAddTheme.render(): Unit = run {
                                    val todos = Signal(listOf<String>())
                                    val newTodo = Signal("")

                                    col {
                                        h1("My Todo List")

                                        field("New Todo") {
                                            row {
                                                expanding.textInput {
                                                    content bind newTodo
                                                    hint = "What needs to be done?"
                                                }
                                                important.button {
                                                    text("Add")
                                                    onClick {
                                                        if (newTodo().isNotBlank()) {
                                                            todos.value = todos() + newTodo()
                                                            newTodo.value = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        forEach(todos) { todo ->
                                            card.row {
                                                expanding.text { content = todo }
                                                danger.button {
                                                    text("Delete")
                                                    onClick {
                                                        todos.value = todos() - todo
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Tips for Success") {
                    card.col {
                        text("✓ Start simple - build complexity gradually")
                        text("✓ Use reactive state (Signal) for anything that changes")
                        text("✓ Apply semantic modifiers (important, danger) instead of direct styling")
                        text("✓ Keep components small and focused")
                        text("✓ Use the browser dev tools to inspect theme classes")
                        text("✓ Experiment in the example app to see how things work")
                    }
                }
            }
        }
    }
}
