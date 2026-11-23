# Forms

A form is ordinary state plus inputs: a `Signal` (or a lens into one) per field, `field(label) { input }` to label each input, and one `Action` to submit.  Validation comes from the Reactive library's `lensing.validation` package: wrap a value so it reports issues, then show those issues next to the input.

## Minimal example

```kotlin
val email = Signal("")
val password = Signal("")
val signIn = Action("Sign in") { api.signIn(email(), password()) }

col {
    field("Email") {
        textInput {
            hint = "you@example.com"
            keyboardHints = KeyboardHints.email
            content bind email
        }
    }
    field("Password") {
        textInput {
            keyboardHints = KeyboardHints.password
            content bind password
            action = signIn                 // Enter submits
        }
    }
    important.button {
        text("Sign in")
        action = signIn
    }
}
```

## Common tasks

### Label an input

`field(label) { ... }` (in `com.lightningkite.kiteui.views.l2`) puts a label above its content, applies `fieldTheme` to each view you write directly inside it, links the label to the first input for screen readers, and shows below the input any error thrown inside it.

`label(label) { ... }` does the same layout and accessibility, without `fieldTheme` or error text.  Use it for switches and checkboxes, or when you want to place the issue text yourself (below).

```kotlin
sizeConstraints(width = 20.rem).field("Email") {     // modifiers go outside
    textInput { content bind email }
}
label("Notifications") {
    switch { checked bind notifications }
}
```

### Reject bad input on submit

The simplest validation is a check inside the action.  A `PlainTextException` is shown to the user as-is, with its title:

```kotlin
important.button {
    text("Continue")
    action = Action("Continue") {
        if ("@" !in email()) throw PlainTextException("Enter a valid email address.", "Check your email")
        api.start(email())
    }
}
```

### Validate fields as the user types

`validate`, `assert` and `assertNotBlank` turn a `MutableReactive` into a `MutableValidated` that reports an `Issue` whenever its value fails the check.  Bind inputs to the validated value, then:

- `validate(value).` (a modifier) gives the input the invalid theme while it has issues,
- `issueText(value.issues)` shows the issue summaries, and is announced by screen readers.

Both take an optional condition; use it to stay quiet until the user first tries to submit.

```kotlin
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
```

An issue doesn't block the write by default: the signal still gets what the user typed.  Pass `setOnIssue = false` to keep the last valid value instead.

### Edit a copy, then save or revert

A `Draft` buffers edits to a value until you `publish()` them; `cancel()` reverts and `changesMade` says whether anything differs.  Validate the draft once, then lens each field out of it: every field's issues roll up into the draft's `issues`.

```kotlin
data class Profile(val name: String = "", val age: Int? = null)

val saved = Signal(Profile("Ada", 36))           // or any MutableReactive, e.g. a server-backed one
val form = Draft(saved).validated()
val name = form.lens(get = { it.name }, modify = { p, v -> p.copy(name = v) })
    .assertNotBlank("Name is required")
val age = form.lens(get = { it.age }, modify = { p, v -> p.copy(age = v) })

col {
    label("Name") {
        validate(name).fieldTheme.textInput { content bind name }
        issueText(name.issues)
    }
    field("Age") {
        numberInput { content bind age.asDouble() }
    }
    row {
        button {
            text("Revert")
            ::enabled { form.changesMade() }
            onClick { form.cancel() }
        }
        important.button {
            text("Save")
            ::enabled { form.changesMade() && form.issues().isEmpty() }
            action = Action("Save") { form.publish() }
        }
    }
}
```

With Service Abstractions models you don't write the lenses by hand: `com.lightningkite.optics.lens` takes the generated optic, as in `draft.lens(User.name)`.

### Show server errors

Let the exception propagate out of the action.  It is shown by the nearest exception handler: a dialog by default, or the error text of the enclosing `field`.  To turn specific server errors into friendly messages, see [Error handling](error-handling.md).

## Gotchas

- **Inside `field { }` the receiver only takes theme modifiers.**  `expanding`, `sizeConstraints` and `shownWhen` go on the `field` itself, and `issueText` needs a `label { }` or a container.
- **`field` already applies `fieldTheme`.**  Don't add it again inside; do add it yourself inside `label`.
- **Validation runs immediately.**  An empty required field has an issue before the user touches it; gate `validate(...)` and `issueText(...)` on a condition if that's not what you want.
- **Bind the validated value, not the original signal.**  The issue only updates while something is listening to the validated value, and `setOnIssue = false` only filters writes made through it.
- **Share one `Action`** between the last input and the submit button, so Enter and the button can't run it twice at once.
- **Client checks are for convenience.**  The server must validate again.

## See also

- [Components](components.md): every input and what it binds to
- [Reactivity](reactivity.md): `Signal`, lenses and `bind`
- [Error handling](error-handling.md): `Action`, `PlainTextException` and exception handlers
