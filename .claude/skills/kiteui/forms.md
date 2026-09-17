# KiteUI Forms

## Labeled Input Fields

**⚠️ `field` requires an explicit import** — `import com.lightningkite.kiteui.views.l2.field`. The `l2.*` star-import does not reliably resolve it. Two verified patterns:

```kotlin
// Pattern A — simple, no import needed (used in ChannelListScreen dialogs)
col {
    subtext("Email")
    textInput {
        hint = "your@email.com"
        keyboardHints = KeyboardHints.email
        content bind email
    }
}

// Pattern B — field() applies fieldTheme + accessibility automatically
import com.lightningkite.kiteui.views.l2.field

field("Email") {
    // Do NOT call fieldTheme here — field() already applies it
    textInput {
        hint = "your@email.com"
        keyboardHints = KeyboardHints.email
        content bind email
    }
}
```

## Basic Form

```kotlin
val email = Signal("")
val password = Signal("")

col {
    col {
        subtext("Email")
        textInput {
            hint = "your@email.com"
            keyboardHints = KeyboardHints.email
            content bind email
        }
    }

    col {
        subtext("Password")
        textInput {
            hint = "Password"
            keyboardHints = KeyboardHints.password
            content bind password
        }
    }

    important.button {
        text("Sign In")
        onClick {
            signIn(email(), password())
        }
    }
}
```

## Form State Management with Suspend Functions

**Important:** Property setters and binding callbacks cannot call suspend functions. When building forms that save to the server, use local Signals and an Action:

```kotlin
// Local state for form editing
val localIsPrivate = Signal(false)
val localMuteNotifications = Signal(false)

// Initialize from server data when dialog opens
reactive {
    if (showDialog()) {
        localIsPrivate.value = room().isPrivate
        localMuteNotifications.value = room().muteNotifications
    }
}

// Save action
val saveSettings = Action("Save Settings") {
    val s = currentSession() ?: return@Action
    s.api.room.update(room().copy(
        isPrivate = localIsPrivate.value,
        muteNotifications = localMuteNotifications.value
    ))
    showDialog.value = false
}

// In UI
switch { checked bind localIsPrivate }
checkbox { checked bind localMuteNotifications }
button {
    text("Save")
    action = saveSettings
}
```

## Form Validation Patterns

### Pattern 1: Simple Validation with Inline Errors

```kotlin
val username = Signal("")
val usernameError = Signal<String?>(null)

field("Username") {
    textInput {
        content bind username
        // Clear error when user types
        reactive { content(); usernameError.value = null }
    }
}

reactive {
    usernameError()?.let { error ->
        danger.subtext { content = error }
    }
}

button {
    action = Action("Create Account") {
        usernameError.value = null

        if (username().length < 3) {
            usernameError.value = "Username must be at least 3 characters"
            return@Action
        }

        api.createAccount(username())
    }
}
```

### Pattern 2: Multi-Field Validation

```kotlin
data class FormErrors(
    val email: String? = null,
    val password: String? = null,
    val confirmPassword: String? = null
)

val errors = Signal(FormErrors())

fun validate(): Boolean {
    val newErrors = FormErrors(
        email = when {
            email().isBlank() -> "Email required"
            !email().contains("@") -> "Invalid email"
            else -> null
        },
        password = when {
            password().length < 8 -> "Password must be 8+ characters"
            else -> null
        },
        confirmPassword = when {
            confirmPassword() != password() -> "Passwords don't match"
            else -> null
        }
    )

    errors.value = newErrors
    return newErrors.run { email == null && password == null && confirmPassword == null }
}

col {
    field("Email") {
        textInput { content bind email }
    }
    reactive {
        errors().email?.let { danger.subtext { content = it } }
    }

    field("Password") {
        textInput {
            keyboardHints = KeyboardHints.password
            content bind password
        }
    }
    reactive {
        errors().password?.let { danger.subtext { content = it } }
    }

    field("Confirm Password") {
        textInput {
            keyboardHints = KeyboardHints.password
            content bind confirmPassword
        }
    }
    reactive {
        errors().confirmPassword?.let { danger.subtext { content = it } }
    }

    button {
        text("Sign Up")
        action = Action("Sign Up") {
            if (!validate()) {
                throw PlainTextException("Please fix the errors above", "Validation Failed")
            }
            api.signUp(email(), password())
        }
    }
}
```

### Pattern 3: Async Validation (Check Availability)

```kotlin
val username = Signal("")
val isCheckingAvailability = Signal(false)
val availabilityMessage = Signal<String?>(null)

textInput {
    content bind username
}

reactive {
    val name = username()
    if (name.length >= 3) {
        isCheckingAvailability.value = true
        launch {
            delay(500)  // Debounce
            try {
                val available = api.checkUsernameAvailable(name)
                availabilityMessage.value = if (available) {
                    "✓ Username available"
                } else {
                    "✗ Username taken"
                }
            } catch (e: Exception) {
                availabilityMessage.value = null
            } finally {
                isCheckingAvailability.value = false
            }
        }
    } else {
        availabilityMessage.value = null
    }
}

reactive {
    availabilityMessage()?.let { msg ->
        val isAvailable = msg.startsWith("✓")
        if (isAvailable) {
            subtext { content = msg }
        } else {
            danger.subtext { content = msg }
        }
    }
}
```

### Pattern 4: Server-Driven Validation Errors

When the server returns field-specific errors:

```kotlin
data class ValidationErrors(
    val fieldErrors: Map<String, String> = emptyMap(),
    val globalError: String? = null
)

val errors = Signal(ValidationErrors())

col {
    // Add custom handler for validation errors
    this += ExceptionToMessage<LsErrorException> { exception ->
        // Server returns 400 with field errors in response
        if (exception.status == 400) {
            try {
                val errorData = Json.decodeFromString<ValidationErrors>(exception.error.message)
                errors.value = errorData

                ExceptionMessage(
                    title = "Validation Failed",
                    body = errorData.globalError ?: "Please fix the errors below"
                )
            } catch (e: Exception) {
                null  // Pass to default handler
            }
        } else null
    }

    field("Email") {
        textInput { content bind email }
    }
    reactive {
        errors().fieldErrors["email"]?.let { error ->
            danger.subtext { content = error }
        }
    }

    field("Username") {
        textInput { content bind username }
    }
    reactive {
        errors().fieldErrors["username"]?.let { error ->
            danger.subtext { content = error }
        }
    }

    button {
        text("Submit")
        action = Action("Submit") {
            errors.value = ValidationErrors()  // Clear previous errors
            api.submitForm(email(), username())
            // Server might throw LsErrorException with field errors
        }
    }
}
```

## Local Validation vs Server Validation

**Client-Side (Local) Validation** - For UX:
- Instant feedback before server call
- Check formatting, required fields, basic rules
- Prevent unnecessary network requests
- User-friendly error messages

**Server-Side Validation** - For Security:
- NEVER trust client input
- Enforce business rules and permissions
- Validate data integrity and constraints
- Protect against malicious clients

```kotlin
button {
    text("Sign Up")
    action = Action("Sign Up") {
        // LOCAL VALIDATION (UX - instant feedback)
        val emailValue = email().trim()
        val passwordValue = password()

        if (emailValue.isBlank()) {
            throw PlainTextException("Email is required", "Validation Error")
        }

        if (!emailValue.contains("@")) {
            throw PlainTextException("Please enter a valid email address", "Invalid Email")
        }

        if (passwordValue.length < 8) {
            throw PlainTextException("Password must be at least 8 characters", "Weak Password")
        }

        // SERVER VALIDATION (Security - never trust client)
        // Server will re-validate everything and enforce business rules
        val result = api.auth.signUp(emailValue, passwordValue)

        // Server might throw:
        // - 400: Email format invalid (server's stricter rules)
        // - 409: Email already exists
        // - 403: Registration closed
        // - 500: Database error

        pageNavigator.navigate(HomePage)
    }
}
```

## Best Practices

1. **Always validate locally before server calls** - Better UX, less load
2. **Never skip server validation** - Security is non-negotiable
3. **Clear errors when user starts typing** - Feels more responsive
4. **Use Actions for all user interactions** - Automatic error handling
5. **Provide specific error messages** - "Email is required" not "Invalid input"
6. **Show errors near related fields** - Easier for users to fix
