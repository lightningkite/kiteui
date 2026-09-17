# KiteUI Components

## Text Components

```kotlin
h1("Main Heading")
h2("Subheading")
h3("Smaller heading")
h4("Even smaller")
h5("Very small")
h6("Smallest")
text("Regular text")
subtext("Smaller, muted text")
```

### Text with Dynamic Content
```kotlin
val name = Signal("World")
text { ::content { "Hello, ${name()}!" } }
```

## Buttons

```kotlin
// Basic button
button {
    text("Click Me")
    onClick {
        println("Clicked!")
    }
}

// Button with action (preferred for async operations)
button {
    text("Save")
    action = Action("Save", Icon.save) {
        delay(1000)  // Simulated async work
        saveData()
    }
}

// Themed button
important.button {
    text("Important Action")
    onClick { /* ... */ }
}

danger.button {
    text("Delete")
    onClick { /* ... */ }
}
```

### Actions with Loading

Actions work on both **buttons** and **text fields** (runs on Enter key):

```kotlin
// Button action
button {
    text("Load Data")
    action = Action("Load", Icon.download) {
        // Button automatically shows loading state
        delay(2000)
        val result = fetchData()
        toast("Data loaded successfully!")
    }
}

// Action with API call and toast feedback
button {
    row {
        icon { source = AppIcons.lockOpen }
        text { content = "Unlock Door" }
    }
    action = Action("Unlock Door") {
        val session = currentSession.await() ?: return@Action
        session.api.reservation.unlockDoor(reservationId, doorId, "")
        toast("Door unlocked!")
    }
}
```

**Key points:**
- Action blocks are suspend functions - can call async APIs directly
- Automatic loading state on buttons while action runs
- Use `toast()` for immediate user feedback after actions
- Return early with `return@Action` if preconditions aren't met

## Text Inputs

```kotlin
val email = Signal("")

textInput {
    hint = "Enter your email"
    keyboardHints = KeyboardHints.email
    content bind email
}

// With action (runs on Enter key)
textInput {
    hint = "Search"
    content bind searchQuery
    action = Action("Search", Icon.search) {
        performSearch(searchQuery())
    }
}
```

### Text Area (Multi-line)
```kotlin
val notes = Signal("")

textArea {
    hint = "Enter notes"
    content bind notes
}
```

## Checkbox

```kotlin
val isChecked = Signal(false)

checkbox {
    checked bind isChecked
}

// With label
field("Accept Terms") {
    checkbox {
        checked bind acceptedTerms
    }
}
```

## Switch (Toggle)

```kotlin
val isEnabled = Signal(false)

switch {
    checked bind isEnabled  // Note: uses 'checked' not 'enabled'
}
```

## Radio Buttons

Use the `.equalTo()` extension for clean radio button binding:

```kotlin
val selectedOption = Signal<Int?>(null)

val options = listOf(
    null to "Never",
    1 to "After 1 day",
    7 to "After 7 days"
)

options.forEach { (value, label) ->
    button {
        row {
            radioButton {
                // ✅ Best: Use .equalTo() for automatic bidirectional binding
                checked bind selectedOption.equalTo(value)
            }
            space()
            text(label)
        }
        onClick {
            selectedOption.value = value
        }
    }
}
```

**How `.equalTo()` works:**
- Returns `true` when `selectedOption() == value`
- When set to `true`, automatically sets `selectedOption.value = value`
- Much cleaner than manual `remember { }.withWrite { }` pattern

### Radio Toggle Buttons (Preferred)

For styled radio selection groups, use `radioToggleButton` instead of manual button+radioButton combinations:

```kotlin
val selectedActivity = Signal(0)
val activities = listOf("Endurance", "Hill Climb", "HIIT", "Recovery")

row {
    activities.forEachIndexed { index, label ->
        expanding.radioToggleButton {
            centered.text { content = label }
            checked bind selectedActivity.equalTo(index)
        }
    }
}
```

**With icons:**
```kotlin
val activities = listOf(
    AppIcons.bike to "Endurance",
    AppIcons.mountain to "Hill Climb",
    AppIcons.bolt to "HIIT",
    AppIcons.fitness to "Recovery"
)

row {
    activities.forEachIndexed { index, (icon, label) ->
        expanding.radioToggleButton {
            col {
                centered.icon { source = icon }
                centered.subtext { content = label }
            }
            checked bind selectedActivity.equalTo(index)
        }
    }
}
```

**Key Points:**
- `radioToggleButton` automatically uses `SelectedSemantic` when checked, `UnselectedSemantic` when unchecked
- Style via theme overrides on `SelectedSemantic` and `UnselectedSemantic`
- Use `expanding` modifier for equal-width buttons in a row
- Cannot be deselected by clicking again (radio behavior)
- For toggleable buttons (checkbox behavior), use `toggleButton` instead

## Select (Dropdown)

```kotlin
val options = listOf("Option 1", "Option 2", "Option 3")
val selected = Signal("Option 1")

select {
    bind(selected, options) { it }
}
```

## Images

```kotlin
image {
    source = Resources.myImage
    scaleType = ImageScaleType.Crop
    description = "Alt text for accessibility"
}
```

## Icons

```kotlin
// Pattern 1: Direct icon with description (most common)
icon(Icon.home, "Home")
icon(Icon.settings, "Settings")

// Pattern 2: DSL block with properties
icon {
    source = Icon.home
    description = "Home"
}

// Pattern 3: Icon in a row with text
row {
    icon { source = Icon.settings }
    text { content = "Settings" }
}
```

### Custom Icons from SVG Paths

Create custom icons using Material Design SVG paths. Icons use a viewBox typically `0, -960, 960, 960`:

```kotlin
object AppIcons {
    // Use built-in icons when available
    val arrowBack = Icon.arrowBack
    val settings = Icon.settings
    val check = Icon.done

    // Create custom icons from Material Design SVG paths
    val lockOpen = Icon(
        1.5.rem, 1.5.rem, 0, -960, 960, 960,
        listOf("M240-640h360v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85h-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640Z")
    )

    val camera = Icon(
        1.5.rem, 1.5.rem, 0, -960, 960, 960,
        listOf("M480-260q75 0 127.5-52.5T660-440q0-75-52.5-127.5T480-620q-75 0-127.5 52.5T300-440q0 75 52.5 127.5T480-260Z")
    )
}

// Usage
icon { source = AppIcons.lockOpen }
```

**Best Practice**: Centralize custom icons in an `AppIcons` object for consistency and reusability.

## Links

```kotlin
// Internal navigation link
link {
    text("Go to Settings")
    to = { SettingsPage }
}

// External link
externalLink {
    text("Visit Website")
    to = "https://example.com"
}
```

## Separators and Space

```kotlin
col {
    text("Above")
    separator()  // Horizontal line
    text("Below")
    space()      // Empty space
}
```

## Reusable Components (Custom Widgets)

Create reusable components as extension functions on `ViewWriter`:

```kotlin
fun ViewWriter.userCard(user: User) = card.row {
    gap = 1.rem
    image {
        source = user.avatarUrl
        sizeConstraints(width = 3.rem, height = 3.rem)
    }
    col {
        text(user.name)
        subtext(user.email)
    }
}

// Usage
col {
    forEach(users) { user ->
        userCard(user)
    }
}
```

### Component that Loads its Own Data

```kotlin
fun ViewWriter.userDetails(userId: String) = col {
    val userData = LateInitProperty<UserData>()

    // Load data when component is created
    launch {
        userData.value = api.fetchUser(userId)
    }

    // UI automatically shows loading state
    text { ::content { "Name: ${userData().name}" } }
    text { ::content { "Email: ${userData().email}" } }
}
```

### Reusable Components with Bottom Sheets

For components used across multiple screens, create dedicated files:

```kotlin
// UnlockDoorsSheet.kt
package com.example.screens

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*

/**
 * Reusable unlock doors bottom sheet content.
 */
fun ViewWriter.UnlockDoorsSheetContent() {
    col {
        h3 { content = "Unlock Doors" }
        subtext { content = "Select which doors to unlock:" }

        button {
            row {
                icon { source = AppIcons.lockOpen }
                text { content = "Main Entry Door" }
            }
            onClick { /* TODO: Unlock main door */ }
        }
        button {
            row {
                icon { source = AppIcons.lockOpen }
                text { content = "Equipment Room" }
            }
            onClick { /* TODO: Unlock equipment room */ }
        }
    }
}

/**
 * Reusable button that opens the unlock doors bottom sheet.
 */
fun ViewWriter.UnlockDoorsButton() {
    button {
        row {
            icon { source = AppIcons.lockOpen }
            text { content = "Unlock Doors" }
        }
        onClick {
            openBottomSheet {
                UnlockDoorsSheetContent()
            }
        }
    }
}
```

**Usage across screens**:
```kotlin
// In any screen
col {
    // ... other content ...
    UnlockDoorsButton()  // Just call the function
}
```

**Key Points**:
- Extension functions on `ViewWriter` are automatically available in DSL context
- Functions in the same package are accessible without imports
- Separate content from trigger (e.g., `SheetContent` vs `Button`) for flexibility
