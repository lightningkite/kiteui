# KiteUI Forms

Condensed from [docs/forms.md](../../../docs/forms.md).

## Labeled fields

```kotlin
field("Email") {                       // l2: label + fieldTheme on each direct child + error text + labelFor
    textInput { keyboardHints = KeyboardHints.email; content bind email }
}
sizeConstraints(width = 20.rem).field("Email") { … }   // modifiers go OUTSIDE field
label("Notify me") { switch { checked bind notify } }  // label only: no fieldTheme, no error text
```

- `field` resolves with `import com.lightningkite.kiteui.views.l2.*`; no special import needed.
- Inside `field { }` the receiver is `CanAddTheme`: no `expanding`, `sizeConstraints`, `shownWhen`, `issueText` there.
- Don't add `fieldTheme` inside `field`; do add it inside `label`.

## Submit

```kotlin
val signIn = Action("Sign in") {
    if ("@" !in email()) throw PlainTextException("Enter a valid email address.", "Check your email")
    api.signIn(email(), password())
}
textInput { content bind password; keyboardHints = KeyboardHints.password; action = signIn }  // Enter
important.button { text("Sign in"); action = signIn }
```

One shared `Action`: one loading/error state, no double submit.  `PlainTextException(message, title)` (`kiteui.exceptions`) is shown to the user verbatim.  Other exceptions go to exception handlers (default: a dialog; inside `field`, its error text).

## Validation (`com.lightningkite.reactive.lensing.validation.*` + `kiteui.reactive.*`)

```kotlin
val email = Signal("").validate { if ("@" in it) null else "Enter a valid email address" }   // message or null
val password = Signal("").assert("At least 8 characters") { it.length >= 8 }                 // also assertNotBlank()
val submitted = Signal(false)

label("Email") {
    validate(email) { submitted() }.fieldTheme.textInput { content bind email }   // invalid theme while it has issues
    issueText(email.issues) { submitted() }                                       // shows summaries, live region
}
important.button {
    text("Create")
    action = Action("Create") {
        submitted.value = true
        if (email.issues().isNotEmpty() || password.issues().isNotEmpty())
            throw PlainTextException("Fix the highlighted fields.", "Can't create account")
        api.create(email(), password())
    }
}
```

- Validation runs immediately (an empty required field has an issue at once): gate display on a condition.
- Issues don't block writes unless `setOnIssue = false`.
- Bind inputs to the validated value, not the original signal.
- Put `issueText` inside the field's own `label { }`: it links to the first input in its parent for screen readers.

## Draft: edit a copy, save or revert

```kotlin
val form = Draft(saved).validated()          // saved: any MutableReactive<Profile>
val name = form.lens(get = { it.name }, modify = { p, v -> p.copy(name = v) }).assertNotBlank("Name is required")
// field issues roll up into form.issues
button { text("Revert"); ::enabled { form.changesMade() }; onClick { form.cancel() } }
important.button {
    text("Save")
    ::enabled { form.changesMade() && form.issues().isEmpty() }
    action = Action("Save") { form.publish() }
}
```

With Service Abstractions models use the generated optics: `draft.lens(User.name)` (`com.lightningkite.optics.lens`), `.asString()` for typed strings like `EmailAddress`.

Client checks are UX only; the server must validate too.
