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

    override fun ViewWriter.render(): Unit = run {
        article {
            titledSection("Forms and Validation") {
                text("Learn how to build robust, validated forms in KiteUI.")

                space()

                titledSection("Basic Text Input") {
                    text("The simplest form is a single input bound to a Signal:")

                    space()
                    code {
                        content = """
                            val name = Signal("")

                            field("Your Name") {
                                textInput {
                                    content bind name
                                    hint = "Enter your name"
                                }
                            }

                            text {
                                ::content { "Hello, ${'$'}{name()}!" }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("The bind keyword creates two-way data binding between the input and the Signal.")
                }

                space()

                titledSection("Multiple Form Fields") {
                    text("Use separate Signals for each form field:")

                    space()
                    code {
                        content = """
                            val username = Signal("")
                            val email = Signal("")
                            val password = Signal("")

                            col {
                                field("Username") {
                                    textInput {
                                        content bind username
                                    }
                                }

                                field("Email") {
                                    textInput {
                                        content bind email
                                        keyboardHints = KeyboardHints.email
                                    }
                                }

                                field("Password") {
                                    textInput {
                                        content bind password
                                        keyboardHints = KeyboardHints.password
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Keyboard Hints") {
                    text("Use keyboardHints to optimize mobile input:")

                    space()
                    code {
                        content = """
                            // For email addresses
                            textInput {
                                content bind email
                                keyboardHints = KeyboardHints.email
                            }

                            // For passwords
                            textInput {
                                content bind password
                                keyboardHints = KeyboardHints.password
                            }

                            // For phone numbers
                            textInput {
                                content bind phone
                                keyboardHints = KeyboardHints.phone
                            }

                            // For URLs
                            textInput {
                                content bind website
                                keyboardHints = KeyboardHints.url
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Number Input") {
                    text("Use numberInput for numeric values:")

                    space()
                    code {
                        content = """
                            val age = Signal<Int?>(null)
                            val price = Signal<Double?>(null)

                            field("Age") {
                                numberInput {
                                    content bind age
                                    hint = "Enter age"
                                }
                            }

                            field("Price") {
                                numberInput {
                                    content bind price
                                    hint = "0.00"
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Basic Validation") {
                    text("Validate input using reactive expressions:")

                    space()
                    code {
                        content = """
                            val email = Signal("")
                            val emailError = remember {
                                val value = email()
                                when {
                                    value.isEmpty() -> "Email is required"
                                    !value.contains("@") -> "Invalid email format"
                                    else -> null
                                }
                            }

                            col {
                                field("Email") {
                                    textInput {
                                        content bind email
                                        keyboardHints = KeyboardHints.email
                                    }
                                }

                                // Show error message if present
                                emailError()?.let { error ->
                                    danger.text(error)
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Field-Level Validation") {
                    text("Create reusable validation functions:")

                    space()
                    code {
                        content = """
                            fun validateEmail(email: String): String? {
                                return when {
                                    email.isEmpty() -> "Email is required"
                                    !email.contains("@") -> "Invalid email"
                                    !email.contains(".") -> "Invalid email"
                                    else -> null
                                }
                            }

                            fun validatePassword(password: String): String? {
                                return when {
                                    password.isEmpty() -> "Password is required"
                                    password.length < 8 -> "Password must be at least 8 characters"
                                    else -> null
                                }
                            }

                            val email = Signal("")
                            val password = Signal("")

                            val emailError = remember { validateEmail(email()) }
                            val passwordError = remember { validatePassword(password()) }

                            col {
                                field("Email") {
                                    textInput { content bind email }
                                }
                                emailError()?.let { danger.text(it) }

                                field("Password") {
                                    textInput {
                                        content bind password
                                        keyboardHints = KeyboardHints.password
                                    }
                                }
                                passwordError()?.let { danger.text(it) }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Form-Level Validation") {
                    text("Validate the entire form before submission:")

                    space()
                    code {
                        content = """
                            val username = Signal("")
                            val email = Signal("")
                            val password = Signal("")

                            val isValid = remember {
                                username().isNotBlank() &&
                                email().contains("@") &&
                                password().length >= 8
                            }

                            col {
                                field("Username") {
                                    textInput { content bind username }
                                }

                                field("Email") {
                                    textInput {
                                        content bind email
                                        keyboardHints = KeyboardHints.email
                                    }
                                }

                                field("Password") {
                                    textInput {
                                        content bind password
                                        keyboardHints = KeyboardHints.password
                                    }
                                }

                                important.button {
                                    text("Submit")
                                    enabled = isValid
                                    onClick {
                                        // Submit form
                                        submitRegistration(
                                            username(),
                                            email(),
                                            password()
                                        )
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Checkboxes") {
                    text("Use checkbox for boolean values:")

                    space()
                    code {
                        content = """
                            val agreedToTerms = Signal(false)
                            val subscribe = Signal(true)

                            col {
                                row {
                                    checkbox {
                                        checked bind agreedToTerms
                                    }
                                    text("I agree to the terms and conditions")
                                }

                                row {
                                    checkbox {
                                        checked bind subscribe
                                    }
                                    text("Subscribe to newsletter")
                                }

                                button {
                                    text("Continue")
                                    enabled = agreedToTerms
                                    onClick { /* ... */ }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Select Dropdown") {
                    text("Use select for choosing from options:")

                    space()
                    code {
                        content = """
                            val country = Signal("US")
                            val countries = listOf("US", "CA", "UK", "AU", "DE")

                            field("Country") {
                                select {
                                    bind(country, countries) { it }
                                }
                            }

                            text {
                                ::content { "Selected: ${'$'}{country()}" }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("With custom display:")
                    code {
                        content = """
                            data class Country(val code: String, val name: String)

                            val selected = Signal<Country?>(null)
                            val countries = listOf(
                                Country("US", "United States"),
                                Country("CA", "Canada"),
                                Country("UK", "United Kingdom")
                            )

                            field("Country") {
                                select {
                                    bind(selected, countries) { it.name }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Radio Buttons") {
                    text("Create radio button groups:")

                    space()
                    code {
                        content = """
                            val plan = Signal("basic")

                            col {
                                h4("Choose Your Plan")

                                radio {
                                    checked = remember { plan() == "basic" }
                                    onCheckedChange = { if (it) plan.value = "basic" }
                                }
                                text("Basic - Free")

                                radio {
                                    checked = remember { plan() == "pro" }
                                    onCheckedChange = { if (it) plan.value = "pro" }
                                }
                                text("Pro - ${'$'}10/month")

                                radio {
                                    checked = remember { plan() == "enterprise" }
                                    onCheckedChange = { if (it) plan.value = "enterprise" }
                                }
                                text("Enterprise - ${'$'}50/month")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Date and Time Input") {
                    text("Use specialized pickers for dates and times:")

                    space()
                    code {
                        content = """
                            import kotlinx.datetime.*

                            val birthDate = Signal<LocalDate?>(null)
                            val appointmentTime = Signal<LocalTime?>(null)

                            field("Birth Date") {
                                datePicker {
                                    date bind birthDate
                                }
                            }

                            field("Appointment Time") {
                                timePicker {
                                    time bind appointmentTime
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Form Submission") {
                    text("Handle form submission with validation:")

                    space()
                    code {
                        content = """
                            val name = Signal("")
                            val email = Signal("")
                            val submitting = Signal(false)

                            val canSubmit = remember {
                                name().isNotBlank() &&
                                email().contains("@") &&
                                !submitting()
                            }

                            col {
                                field("Name") {
                                    textInput { content bind name }
                                }

                                field("Email") {
                                    textInput {
                                        content bind email
                                        keyboardHints = KeyboardHints.email
                                    }
                                }

                                important.button {
                                    text {
                                        ::content {
                                            if (submitting()) "Submitting..."
                                            else "Submit"
                                        }
                                    }
                                    enabled = canSubmit
                                    onClick {
                                        launch {
                                            submitting.value = true
                                            try {
                                                submitForm(name(), email())
                                                toast("Form submitted successfully!")
                                            } catch (e: Exception) {
                                                toast("Error: ${'$'}{e.message}")
                                            } finally {
                                                submitting.value = false
                                            }
                                        }
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Async Validation") {
                    text("Validate against server (e.g., check username availability):")

                    space()
                    code {
                        content = """
                            val username = Signal("")
                            val checking = Signal(false)
                            val isAvailable = Signal<Boolean?>(null)

                            // Debounce username check
                            var checkJob: Job? = null

                            reactiveScope {
                                val currentUsername = username()
                                checkJob?.cancel()

                                if (currentUsername.length >= 3) {
                                    checkJob = launch {
                                        checking.value = true
                                        delay(500) // Debounce
                                        try {
                                            isAvailable.value = checkUsernameAvailable(currentUsername)
                                        } finally {
                                            checking.value = false
                                        }
                                    }
                                }
                            }

                            field("Username") {
                                textInput { content bind username }
                            }

                            when {
                                checking() -> text("Checking...")
                                isAvailable() == true -> affirmative.text("Available!")
                                isAvailable() == false -> danger.text("Username taken")
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Use Signal for all form state")
                        text("✓ Bind inputs with the bind keyword")
                        text("✓ Set appropriate keyboardHints for mobile")
                        text("✓ Validate on change, not just on submit")
                        text("✓ Show validation errors near the relevant field")
                        text("✓ Disable submit button until form is valid")
                        text("✓ Show loading state during submission")
                        text("✓ Provide clear error messages")
                        text("✓ Use semantic modifiers (danger for errors, affirmative for success)")
                    }
                }

                space()

                titledSection("Common Patterns Summary") {
                    card.col {
                        h5("Simple Input:")
                        code {
                            content = """
                                val text = Signal("")
                                textInput { content bind text }
                            """.trimIndent()
                        }

                        space()
                        h5("With Validation:")
                        code {
                            content = """
                                val email = Signal("")
                                val error = remember {
                                    if (!email().contains("@")) "Invalid" else null
                                }
                                textInput { content bind email }
                                error()?.let { danger.text(it) }
                            """.trimIndent()
                        }

                        space()
                        h5("Checkbox:")
                        code {
                            content = """
                                val checked = Signal(false)
                                checkbox { checked bind this.checked }
                            """.trimIndent()
                        }

                        space()
                        h5("Select:")
                        code {
                            content = """
                                val choice = Signal("A")
                                select { bind(choice, listOf("A", "B", "C")) { it } }
                            """.trimIndent()
                        }
                    }
                }
            }
        }
    }
}
