# KiteUI: Creating Pages and Components

This document provides a guide on how to create pages and components in KiteUI based on the example app.

## Table of Contents
1. [General Tips](#general-tips)
2. [Creating Pages](#creating-pages)
3. [Page Structure](#page-structure)
4. [Creating Components](#creating-components)
5. [Using Modifiers](#using-modifiers)
6. [State Management](#state-management)
7. [Navigation](#navigation)
8. [Theming](#theming)
9. [Examples from the Cheat Sheet](#examples-from-the-cheat-sheet)

## General Tips

- Components should:
  - Take a minimal number of parameters that are unique per usage and directly related to the component.
  - Should load their own data. This makes them easy to debug and work with independently.
  - Should be used more than once. Single-use components can usually just be inlined for better readability.
- Modifier Order:
  - Position > Visibility > Scroll > Theme

## Creating Pages

In KiteUI, pages are objects that implement the `Page` interface. Here's how to create a basic page:

```kotlin
@Routable("your/path")
object YourPage : Page {
    override fun ViewWriter.render(): Unit = run {
        // Your UI code here
    }
}
```

The `@Routable` annotation defines the URL path for the page. The `render()` function is where you define your UI.

If you want parameters in your page, you can do so like this:

```kotlin
@Routable("some-collection/{id}")
class YourPage(val id: String) : Page {
    override fun ViewWriter.render(): Unit = run {
        // Your UI code here
    }
}
```

Parameter types can be any KotlinX Serialization serializable type.

### Example: Simple Login Page

```kotlin
@Routable("sample/login")
object SampleLogInPage : Page {
    override fun ViewWriter.render(): Unit = run {
        val email = Signal("")
        val password = Signal("")
        frame {
            gap = 0.rem
            image {
                source = Resources.imagesSolera
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }
            padded - scrolling - col {
                expanding - space()
                centered - sizeConstraints(maxWidth = 50.rem) - card - col {
                    h1 { content = "My App" }
                    sizeConstraints(width = 20.rem) - field("Email") {
                        fieldTheme - textInput {
                            hint = "Email"
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }
                    sizeConstraints(width = 20.rem) - field("Password") {
                        fieldTheme - textInput {
                            hint = "Password"
                            keyboardHints = KeyboardHints.password
                            content bind password
                            action = Action(
                                title = "Log In",
                                icon = Icon.login,
                            ) {
                                fakeLogin(email)
                            }
                        }
                    }
                    centered - sizeConstraints(width = 15.rem) - important - button {
                        h6 { content = "Log In" }
                        onClick {
                            delay(1000)
                            fakeLogin(email)
                        }
                    }
                }
                expanding - space()
            }
        }
    }

    private suspend fun ViewWriter.fakeLogin(email: Signal<String>) {
        fetch("fake-login/${email()}")
        pageNavigator.navigate(ControlsPage)
    }
}
```

## Page Structure

A typical KiteUI page consists of:

1. **State Management**: Using `Signal` objects to manage state
2. **Layout Containers**: Such as `frame`, `col`, `row`, etc.
3. **UI Components**: Such as `text`, `button`, `textInput`, etc.
4. **Modifiers**: Applied using the `-` operator
5. **Event Handlers**: Such as `onClick`, `onSubmit`, etc.

### Documentation Pages

For documentation pages, you can implement the `DocPage` interface:

```kotlin
object YourDocPage : DocPage {
    override val covers: List<String> = listOf("topic1", "topic2")

    override fun ViewWriter.render(): Unit = run {
        article {
            h1("Your Topic")
            text("Description")
            // Examples and content
        }
    }
}
```

## Creating Components

KiteUI provides a rich set of components that you can use to build your UI. Here are some of the most common ones:

### Containers
- `row`: Horizontal layout
- `col`: Vertical layout
- `frame`: Stacked layout
- `rowCollapsingToColumn`: Responsive layout that collapses to a column on smaller screens

### Display Components
- `text`, `h1`-`h6`, `subtext`: Text components
- `icon`: Icon component
- `image`: Image component
- `video`: Video component
- `separator`: Line separator
- `space`: Empty space

### Interactive Components
- `button`: Button component
- `link`: Internal link
- `externalLink`: External link
- `menuButton`: Button that opens a menu

### Form Controls
- `textInput`: Text input field
- `textArea`: Multi-line text input
- `checkbox`: Checkbox component
- `radioButton`: Radio button component
- `switch`: Toggle switch
- `select`: Dropdown selection
- `field`: Wraps an input with a label

## Using Modifiers

Modifiers are applied to components using the `-` operator. They change the appearance or behavior of a component.

```kotlin
centered - card - text("Centered text in a card")
```

Common modifiers include:

- `centered`: Centers the component
- `expanding`: Makes the component expand to fill available space
- `weight(float)`: Assigns a weight to the component for flex layout
- `sizeConstraints`: Sets size constraints for the component
- `padded`: Adds padding to the component
- `scrolling`: Makes the component scrollable
- `card`, `fieldTheme`, `important`, etc.: Applies a theme to the component

## State Management

KiteUI uses reactive programming for state management. This section covers the basic and advanced reactive tools available in KiteUI.

### Basic Reactive Tools

#### Signal

The most basic reactive container is `Signal`. It holds a value and notifies its listeners when that value changes.

```kotlin
val email = Signal("")
```

You can update a property's value directly:

```kotlin
email.value = "user@example.com"
```

#### Binding Properties

You can bind a property to a component using the `bind` keyword:

```kotlin
textInput {
    content bind email
}
```

This creates a two-way binding where changes to the input field update the property and vice versa.

#### Reactive Functions

You can use reactive functions to update the UI based on state changes:

```kotlin
text {
    ::content { "Email: ${email()}" }
}
```

The UI will automatically update whenever the email property changes.

#### Remembered Calculations

The `remember` function creates a dependency-tracking calculation. If any of its dependencies change, the calculation is re-evaluated and listeners are notified.

```kotlin
val fullName = remember { "${firstName()} ${lastName()}" }
```

This is more efficient than recalculating the same value in multiple places.

### Advanced Reactive Tools

#### MutableRemember

`MutableRemember` is similar to `remember`, but allows you to override the calculated value and reset it back to the calculation if needed.

```kotlin
val calculatedValue = MutableRemember { baseValue() * multiplier() }

// Override the calculated value
calculatedValue.value = 100.0

// Reset to use the calculation again
calculatedValue.reset()
```

#### LateInitSignal

`LateInitSignal` is useful for values that aren't available at declaration time. It starts in a "not ready" state until a value is set.

```kotlin
val userData = LateInitSignal<UserData>()

// Set a value later
userData.value = fetchedUserData

// Remove the value
userData.unset()
```

Components that depend on a `LateInitSignal` will show loading states until a value is available.

#### Creating Custom Mutable Reactives with withWrite

You can create a custom mutable reactive from a read-only one using the `withWrite` function:

```kotlin
val customMutable = someReactive.withWrite { newValue ->
    // Custom logic to handle setting the new value
    // This might update multiple other signals
}
```

### Using Reactive Values Outside of Reactive Scopes

#### Getting Current Values

To get the current value of a reactive value:

```kotlin
// Suspending function that waits for a value
val value = someReactive.awaitOnce()

// Get the current state immediately (may be not ready or error)
val state = someReactive.state
```

#### Handling States

Reactive values can be in different states:
- Ready: A value is available
- Not Ready: No value is available yet (loading)
- Error: An error occurred while calculating the value

## Navigation

Navigation in KiteUI is handled through the `pageNavigator` object:

```kotlin
pageNavigator.navigate(AnotherPage)
```

You can also use the `link` component for declarative navigation:

```kotlin
link {
    text { content = "Go to Another Page" }
    to = { AnotherPage }
}
```

## Theming

KiteUI uses themes for styling components. You can apply a theme to a component using modifiers:

```kotlin
card - text("Text with card theme")
important - button { text("Important button") }
```

You can also create custom themes by implementing the `Semantic` interface:

```kotlin
data object CustomSemantic : Semantic("custom") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        // Theme customizations
    )
}

// Usage
CustomSemantic.onNext - text("Custom themed text")
```

## Examples from the Cheat Sheet

The example app includes a comprehensive cheat sheet that demonstrates all available components and modifiers. Here are some examples:

### Basic Layout

```kotlin
row {
    card - text("A")
    weight(1f) - card - text("B")
    card - text("C")
}
```

### Form with Validation

```kotlin
val email = Signal("")
val password = Signal("")

col {
    field("Email") {
        textInput {
            hint = "Email"
            keyboardHints = KeyboardHints.email
            content bind email
        }
    }
    field("Password") {
        textInput {
            hint = "Password"
            keyboardHints = KeyboardHints.password
            content bind password
        }
    }
    button {
        text("Log In")
        onClick {
            // Validation and login logic
        }
    }
}
```

### Dynamic Content

```kotlin
val items = Signal(listOf("Item 1", "Item 2", "Item 3"))

col {
    forEach(items) { item ->
        card - text(item)
    }
}
```

### Responsive Design

```kotlin
rowCollapsingToColumn(70.rem) {
    card - text("A")
    weight(1f) - card - text("B")
    card - text("C")
}
```

For more examples, refer to the CheatSheet.kt file in the example app, which can be found at:
`example-app/src/commonMain/kotlin/com/lightningkite/mppexampleapp/docs/CheatSheet.kt`

The cheat sheet provides examples for:
- Containers (row, col, frame, etc.)
- Display components (text, icon, image, etc.)
- Interactive elements (button, link, etc.)
- Form controls (textInput, checkbox, select, etc.)
- View modifiers (scrolling, centered, weight, etc.)
- Advanced components (canvas, recyclerView, etc.)
- Dialogs (toast, alert, confirm, etc.)
