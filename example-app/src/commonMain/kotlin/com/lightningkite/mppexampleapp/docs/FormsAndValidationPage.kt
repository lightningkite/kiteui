package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code
import com.lightningkite.reactive.core.*

@Routable("docs/forms-validation")
object FormsAndValidationPage : DocPage {
    override val covers: List<String> = listOf(
        "forms", "validation", "input", "fields", "form validation",
        "text input", "form handling", "user input", "bind"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Forms and Validation") {
                text("A form is ordinary state plus inputs: a Signal per field, field(label) { input } to label each input, and one Action to submit.")

                space()

                titledSection("Basic Text Input") {
                    text("The simplest form is a single input bound to a Signal:")

                    space()
                    code {
                        content = """
                            val name = Signal("")

                            field("Your Name") {
                                textInput {
                                    hint = "Enter your name"
                                    keyboardHints = KeyboardHints.title
                                    content bind name
                                }
                            }

                            text {
                                ::content { "Hello, ${'$'}{name()}!" }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("bind creates a two-way binding between the input and the Signal. field applies fieldTheme, links the label for screen readers, and shows errors thrown inside it.")
                }

                space()

                titledSection("Multiple Form Fields") {
                    text("Use a Signal for each field, and share one Action between the last input and the submit button:")

                    space()
                    code {
                        content = """
                            val username = Signal("")
                            val email = Signal("")
                            val password = Signal("")
                            val signUp = Action("Sign up") { api.signUp(username(), email(), password()) }

                            col {
                                field("Username") {
                                    textInput {
                                        keyboardHints = KeyboardHints.id
                                        content bind username
                                    }
                                }
                                field("Email") {
                                    textInput {
                                        keyboardHints = KeyboardHints.email
                                        content bind email
                                    }
                                }
                                field("Password") {
                                    textInput {
                                        keyboardHints = KeyboardHints.newPassword
                                        content bind password
                                        action = signUp          // Enter submits
                                    }
                                }
                                important.button {
                                    text("Sign up")
                                    action = signUp
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Keyboard Hints") {
                    text("Always set keyboardHints: it picks the mobile keyboard and autofill:")

                    space()
                    code {
                        content = """
                            textInput { keyboardHints = KeyboardHints.email; content bind email }
                            textInput { keyboardHints = KeyboardHints.password; content bind password }
                            textInput { keyboardHints = KeyboardHints.newPassword; content bind newPassword }
                            textInput { keyboardHints = KeyboardHints.phone; content bind phone }
                            textInput { keyboardHints = KeyboardHints.oneTimeCode; content bind code }
                            textArea { keyboardHints = KeyboardHints.paragraph; content bind notes }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Number Input") {
                    text("numberInput's content is a Double?. Bind a Signal<Double?>, or convert an Int? with asDouble():")

                    space()
                    code {
                        content = """
                            val age = Signal<Int?>(null)
                            val price = Signal<Double?>(null)

                            field("Age") {
                                numberInput {
                                    hint = "Enter age"
                                    keyboardHints = KeyboardHints.integer
                                    content bind age.asDouble()
                                }
                            }

                            field("Price") {
                                numberInput {
                                    hint = "0.00"
                                    keyboardHints = KeyboardHints.decimal
                                    content bind price
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Reject Bad Input on Submit") {
                    text("The simplest validation is a check inside the action. A PlainTextException is shown to the user as-is:")

                    space()
                    code {
                        content = """
                            important.button {
                                text("Continue")
                                action = Action("Continue") {
                                    if ("@" !in email()) throw PlainTextException("Enter a valid email address.", "Check your email")
                                    api.start(email())
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Validate as the User Types") {
                    text("validate, assert and assertNotBlank wrap a Signal so it reports issues. Bind the input to the validated value, theme it with the validate(...) modifier, and show the issues with issueText:")

                    space()
                    code {
                        content = """
                            val email = Signal("").validate { if ("@" in it) null else "Enter a valid email address" }
                            val password = Signal("").assert("At least 8 characters") { it.length >= 8 }
                            val submitted = Signal(false)

                            col {
                                label("Email") {
                                    validate(email) { submitted() }.fieldTheme.textInput {
                                        keyboardHints = KeyboardHints.email
                                        content bind email
                                    }
                                    issueText(email.issues) { submitted() }
                                }
                                label("Password") {
                                    validate(password) { submitted() }.fieldTheme.textInput {
                                        keyboardHints = KeyboardHints.newPassword
                                        content bind password
                                    }
                                    issueText(password.issues) { submitted() }
                                }
                                important.button {
                                    text("Create account")
                                    action = Action("Create account") {
                                        submitted.value = true
                                        if (email.issues().isNotEmpty() || password.issues().isNotEmpty())
                                            throw PlainTextException("Fix the highlighted fields.", "Can't create account")
                                        api.signUp(email(), password())
                                    }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("The condition ({ submitted() }) keeps the form quiet until the first submit attempt.")
                }

                space()

                titledSection("Enable a Button Reactively") {
                    text("Bind enabled with ::enabled { }:")

                    space()
                    code {
                        content = """
                            val agreedToTerms = Signal(false)

                            col {
                                label("I agree to the terms and conditions") {
                                    checkbox { checked bind agreedToTerms }
                                }
                                important.button {
                                    text("Continue")
                                    ::enabled { agreedToTerms() }
                                    onClick { /* ... */ }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Select Dropdown") {
                    text("select binds a MutableReactive<T> to a Reactive<List<T>> of options; wrap a fixed list in Constant:")

                    space()
                    code {
                        content = """
                            val country = Signal("US")

                            field("Country") {
                                select {
                                    bind(country, Constant(listOf("US", "CA", "UK", "AU", "DE"))) { it }
                                }
                            }

                            text {
                                ::content { "Selected: ${'$'}{country()}" }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("With custom display and no initial choice:")
                    code {
                        content = """
                            data class Country(val code: String, val name: String)

                            val selected = Signal<Country?>(null)
                            val countries = listOf(
                                null,
                                Country("US", "United States"),
                                Country("CA", "Canada"),
                                Country("UK", "United Kingdom"),
                            )

                            field("Country") {
                                select {
                                    bind(selected, Constant(countries)) { it?.name ?: "Choose a country" }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Radio Buttons") {
                    text("Bind each radio button to equalTo on one Signal:")

                    space()
                    code {
                        content = """
                            val plan = Signal("basic")

                            col {
                                h4("Choose Your Plan")
                                label("Basic - Free") {
                                    radioButton { checked bind plan.equalTo("basic") }
                                }
                                label("Pro - ${'$'}10/month") {
                                    radioButton { checked bind plan.equalTo("pro") }
                                }
                                label("Enterprise - ${'$'}50/month") {
                                    radioButton { checked bind plan.equalTo("enterprise") }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Date and Time Input") {
                    text("localDateField, localTimeField and localDateTimeField bind to kotlinx.datetime values:")

                    space()
                    code {
                        content = """
                            val birthDate = Signal<LocalDate?>(null)
                            val appointmentTime = Signal<LocalTime?>(null)

                            field("Birth Date") {
                                localDateField { content bind birthDate }
                            }

                            field("Appointment Time") {
                                localTimeField { content bind appointmentTime }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Form Submission") {
                    text("An Action shows a working state while it runs and shows any error it throws; you don't track submitting state yourself:")

                    space()
                    code {
                        content = """
                            val name = Signal("")
                            val email = Signal("")

                            col {
                                field("Name") {
                                    textInput {
                                        keyboardHints = KeyboardHints.title
                                        content bind name
                                    }
                                }
                                field("Email") {
                                    textInput {
                                        keyboardHints = KeyboardHints.email
                                        content bind email
                                    }
                                }
                                important.button {
                                    text("Submit")
                                    ::enabled { name().isNotBlank() && "@" in email() }
                                    action = Action("Submit") {
                                        submitForm(name(), email())
                                        context.toast("Form submitted successfully!")
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Async Validation") {
                    text("Check against the server as the user types, with debounce and rememberSuspending. The text shows a loading state while the check runs:")

                    space()
                    code {
                        content = """
                            val username = Signal("")
                            val debounced = username.debounce(500.milliseconds)
                            val available = rememberSuspending {
                                val name = debounced()
                                if (name.length < 3) null else checkUsernameAvailable(name)
                            }

                            field("Username") {
                                textInput {
                                    keyboardHints = KeyboardHints.id
                                    content bind username
                                }
                            }
                            subtext {
                                ::content {
                                    when (available()) {
                                        null -> ""
                                        true -> "Available!"
                                        false -> "Username taken"
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Use a Signal (or a lens into one) for each field")
                        text("✓ Set keyboardHints on every text input")
                        text("✓ Use field(label) { } so labels and errors are wired up")
                        text("✓ Submit with one Action shared by the last input and the button")
                        text("✓ Throw PlainTextException for user-facing validation messages")
                        text("✓ Validate on the server too: client checks are for convenience")
                    }
                }

                space()

                titledSection("Common Patterns Summary") {
                    card.col {
                        h5("Simple Input:")
                        code {
                            content = """
                                val text = Signal("")
                                textInput { keyboardHints = KeyboardHints.title; content bind text }
                            """.trimIndent()
                        }

                        space()
                        h5("With Validation:")
                        code {
                            content = """
                                val email = Signal("").validate { if ("@" in it) null else "Invalid" }
                                label("Email") {
                                    validate(email).fieldTheme.textInput { keyboardHints = KeyboardHints.email; content bind email }
                                    issueText(email.issues)
                                }
                            """.trimIndent()
                        }

                        space()
                        h5("Checkbox:")
                        code {
                            content = """
                                val agreed = Signal(false)
                                checkbox { checked bind agreed }
                            """.trimIndent()
                        }

                        space()
                        h5("Select:")
                        code {
                            content = """
                                val choice = Signal("A")
                                select { bind(choice, Constant(listOf("A", "B", "C"))) { it } }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
