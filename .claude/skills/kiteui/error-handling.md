# KiteUI Error Handling

KiteUI includes a powerful automatic error handling system that catches exceptions from Actions and displays them to users with minimal boilerplate.

## Core Concept: Actions and Automatic Error Handling

**Actions** are KiteUI's way of handling user interactions (like button clicks) with built-in loading states and error handling. When an exception occurs inside an Action, it's automatically caught and displayed to the user.

```kotlin
button {
    text("Save")
    action = Action("Save") {
        // Any exception thrown here is automatically caught
        val result = api.saveData(userInput())
        toast("Saved successfully!")
    }
}
```

**What happens when an error occurs:**
1. The Action catches the exception
2. Error propagates up the view hierarchy through `ExceptionHandler` chain
3. `ExceptionToMessage` converters transform the exception into user-friendly text
4. Error is displayed (dialog by default, or custom handler)
5. Working/loading state automatically clears

## ExceptionHandler - Controls How Errors Are Displayed

`ExceptionHandler` determines what happens when an error occurs (show dialog, toast, inline error, etc.):

```kotlin
col {
    // Add custom error handler to this view and all children
    this += object : ExceptionHandler {
        override val priority: Float = 10f  // Higher priority = checked first

        override fun handle(view: RView, working: Boolean, exception: Exception): (() -> Unit)? {
            // Return null to pass to next handler
            // Return a cleanup function if you handled it

            if (exception is ValidationException) {
                // Show inline error instead of dialog
                toast(exception.message ?: "Validation failed")
                return {} // Return cleanup function (empty in this case)
            }

            return null // Pass to next handler
        }
    }

    // Children inherit this handler
    button {
        action = Action("Submit") {
            throw ValidationException("Email is required")
        }
    }
}
```

## ExceptionToMessage - Converts Exceptions to User-Friendly Text

`ExceptionToMessage` transforms technical exceptions into messages users can understand:

```kotlin
col {
    // Add custom message converter
    this += ExceptionToMessage<NetworkException>(priority = 5f) {
        ExceptionMessage(
            title = "Connection Error",
            body = "Could not connect to server. Check your internet connection.",
            actions = listOf(
                Action("Retry") { retryLastAction() }
            )
        )
    }

    button {
        action = Action("Load Data") {
            // If this throws NetworkException, the converter above handles it
            api.fetchData()
        }
    }
}
```

**Built-in converters** (from `LsErrorHandlers.kt`):
- `LsErrorException` - Server errors from Lightning Server
  - 400 → "Incorrectly formed information was sent."
  - 401 → "You're not authenticated properly."
  - 403 → "You're not allowed to do this."
  - 500 → "Something's wrong with the server."

**Adding Lightning Server error handling:**
```kotlin
// In your app initialization
ExceptionToMessages.root.installLsError()
```

## Displaying Errors: Global vs Field-Level

### Global Errors (Toast/Dialog)

**Default behavior** - Errors show in a dialog:

```kotlin
button {
    action = Action("Delete") {
        api.deleteItem(itemId())
        // Error shows in dialog automatically
    }
}
```

**Toast for non-critical errors:**

```kotlin
col {
    // Custom handler that shows toasts instead of dialogs
    this += object : ExceptionHandler {
        override val priority: Float = 10f
        override fun handle(view: RView, working: Boolean, exception: Exception): (() -> Unit)? {
            if (exception is MinorException) {
                view.toast(exception.message ?: "An error occurred")
                return {}
            }
            return null  // Pass to default dialog handler
        }
    }
}
```

### Field-Level Errors (Inline)

Show errors next to specific fields for better UX:

```kotlin
val email = Signal("")
val emailError = Signal<String?>(null)

col {
    field("Email") {
        textInput {
            hint = "your@email.com"
            content bind email
        }
    }

    // Error message below field
    reactive {
        emailError()?.let { error ->
            danger.text { content = error }
        }
    }

    button {
        text("Submit")
        action = Action("Submit") {
            emailError.value = null  // Clear previous errors

            // Validate
            if (!email().contains("@")) {
                emailError.value = "Invalid email format"
                throw PlainTextException("Please fix the errors above", "Validation Failed")
            }

            try {
                api.submit(email())
                emailError.value = null
            } catch (e: LsErrorException) {
                if (e.status == 409) {
                    emailError.value = "This email is already registered"
                }
                throw e  // Re-throw for dialog
            }
        }
    }
}
```

## Error Types: Automatic vs Manual Handling

### Automatically Handled Errors

These errors are caught and displayed automatically when thrown from an Action:

```kotlin
// ✅ Automatic - Exception caught and shown in dialog
button {
    action = Action("Load") {
        throw Exception("Something went wrong")  // Automatically shown
    }
}

// ✅ Automatic - Server errors
button {
    action = Action("Save") {
        api.user.update(user())  // LsErrorException automatically converted to message
    }
}

// ✅ Automatic - Custom exceptions
button {
    action = Action("Process") {
        throw PlainTextException(
            "Unable to process payment",
            title = "Payment Failed",
            actions = listOf(Action("Retry") { retryPayment() })
        )
    }
}
```

### Manually Handled Errors

Errors outside of Actions need explicit handling:

```kotlin
// ❌ NOT automatic - No Action wrapping this
launch {
    try {
        val data = api.fetchData()
    } catch (e: Exception) {
        // Must handle manually
        toast("Error: ${e.message}")
    }
}

// ✅ Manual handling with helper
launch {
    try {
        val data = api.fetchData()
    } catch (e: Exception) {
        // Convert to user-friendly message
        val message = exceptionToMessage(e)
        dialog { close ->
            card.col {
                h2(message?.title ?: "Error")
                text(message?.body ?: "An error occurred")
                button {
                    text("OK")
                    onClick { close() }
                }
            }
        }
    }
}
```

## The Error Handling Flow

```
User clicks button with Action
    ↓
Action executes (suspend function)
    ↓
Exception thrown (validation, network, etc.)
    ↓
StatusListener catches exception
    ↓
Walk up view hierarchy looking for ExceptionHandler
    ↓
If found: Use ExceptionToMessage to convert exception
    ↓
Display to user (dialog, toast, inline)
    ↓
Clear working/loading state
```

## Best Practices

1. **Always validate locally before server calls** - Better UX, less load
2. **Never skip server validation** - Security is non-negotiable
3. **Clear errors when user starts typing** - Feels more responsive
4. **Use Actions for all user interactions** - Automatic error handling
5. **Provide specific error messages** - "Email is required" not "Invalid input"
6. **Show errors near related fields** - Easier for users to fix
7. **Add retry actions for transient errors** - Network issues, timeouts
8. **Use PlainTextException for custom errors** - Clean, user-friendly messages

**Key Insight**: You rarely need try-catch in Actions. Just throw exceptions and let KiteUI handle them. Focus on clear error messages and good UX patterns.
