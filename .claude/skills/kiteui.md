# KiteUI Version 7 Development Skill

You are an expert in KiteUI, a Kotlin Multiplatform UI framework that uses native view components and fine-grained reactivity inspired by Solid.js.

The information here is for KiteUI 7 - KiteUI 6 is very similar, but it uses ` - ` between modifiers and their views, like this: `card - col {}`.  V6 also differs in that pages are expected to return the views they created, whereas V7 does not.

## Core Principles

### Design Philosophy
- **Web-first**: Generates small binaries (~0.77MB vs Compose's ~12MB) with excellent web performance
- **Native views**: Uses platform's native UI components (not canvas-based rendering)
- **Fine-grained reactivity**: Only updates what needs to be updated, no full-tree reconciliation
- **Semantic theming**: Style based on meaning (important, danger, warning) not direct colors
- **URL-based navigation**: Deep linking and routing built-in via `@Routable` annotations
- **Beautiful by default**: Ugliness should take effort, not prettiness

### Supported Platforms
- Android
- iOS
- Web (JavaScript)
- JVM (in progress)

## Project Structure

### Typical KiteUI Project Layout
```
my-app/
├── build.gradle.kts
├── src/
│   ├── commonMain/
│   │   └── kotlin/
│   │       └── com/example/myapp/
│   │           ├── App.kt              # Main application entry
│   │           ├── pages/              # Page components
│   │           ├── widgets/            # Reusable components
│   │           └── models/             # Data models
│   ├── androidMain/
│   ├── iosMain/
│   └── jsMain/
└── settings.gradle.kts
```

### Dependencies
```kotlin
// In build.gradle.kts
repositories {
    maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")
    mavenCentral()
}

dependencies {
    api("com.lightningkite.kiteui:library:<version>")
}
```

## Creating Pages

### Basic Page
```kotlin
@Routable("my-page")
object MyPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            h1("My Page Title")
            text("Content goes here")
        }
    }
}
```

### Page with Parameters
```kotlin
@Routable("user/{userId}")
class UserProfilePage(val userId: String) : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        col {
            h1("User Profile")
            text("User ID: $userId")
        }
    }
}
```

### Page with State
```kotlin
@Routable("counter")
object CounterPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val count = Signal(0)

        col {
            h1("Counter")
            text { ::content { "Count: ${count()}" } }
            button {
                text("Increment")
                action = Action("Increment", Icon.add) {
                    count.value++
                }
            }
        }
    }
}
```

## Layout Containers

### Column (Vertical Layout)
```kotlin
col {
    gap = 1.rem  // Space between children
    text("First")
    text("Second")
    text("Third")
}
```

### Row (Horizontal Layout)
```kotlin
row {
    gap = 0.5.rem
    card.text("Left")
    expanding.card.text("Center (expands)")
    card.text("Right")
}
```

### Frame (Stacked Layout)
```kotlin
frame {
    // Children are stacked on top of each other
    image { source = Resources.background }
    centered.text("Overlay Text")
}
```

### Responsive Layout
```kotlin
// Becomes column when screen width < 70rem
rowCollapsingToColumn(70.rem) {
    weight(1f).card.text("Sidebar")
    weight(3f).card.text("Main Content")
}
```

## Common Components

### Text Components
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

### Buttons
```kotlin
// Basic button
button {
    text("Click Me")
    onClick {
        println("Clicked!")
    }
}

// Button with action
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
```

### Text Inputs
```kotlin
val email = Signal("")

textInput {
    hint = "Enter your email"
    keyboardHints = KeyboardHints.email
    content bind email
}

// With action (like pressing Enter)
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

### Checkbox
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

### Switch (Toggle)
```kotlin
val isEnabled = Signal(false)

switch {
    enabled bind isEnabled
}
```

### Select (Dropdown)
```kotlin
val options = listOf("Option 1", "Option 2", "Option 3")
val selected = Signal("Option 1")

select {
    bind(selected, options) { it }
}
```

### Images
```kotlin
image {
    source = Resources.myImage
    scaleType = ImageScaleType.Crop
    description = "Alt text for accessibility"
}
```

### Icons
```kotlin
icon {
    source = Icon.home
}
```

### Links
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

### Separators and Space
```kotlin
col {
    text("Above")
    separator()  // Horizontal line
    text("Below")
    space()      // Empty space
}
```

## Modifiers

Modifiers are applied using the `-` operator and chain from left to right.

### Sizing
```kotlin
// Set specific size
sizeConstraints(width = 20.rem, height = 10.rem).text("Fixed size")

// Set only width or height
sizeConstraints(width = 15.rem).text("Fixed width")

// Min/max constraints
sizeConstraints(minWidth = 10.rem, maxWidth = 30.rem).text("Constrained")
```

### Layout Positioning
```kotlin
// In a row
expanding.text("Takes available space")
weight(2f).text("Takes 2x space relative to weight(1f)")

// In a frame
centered.text("Centered")
atTop.text("Top")
atBottom.text("Bottom")
atStart.text("Start (left in LTR)")
atEnd.text("End (right in LTR)")
atTopStart.text("Top left")
atBottomEnd.text("Bottom right")
```

### Scrolling
```kotlin
// Vertical scrolling
scrolling.col {
    repeat(100) { text("Item $it") }
}

// Horizontal scrolling
scrollingHorizontally.row {
    repeat(100) { card.text("$it") }
}
```

### Padding
```kotlin
padded.text("Has default padding")
```

### Visibility
```kotlin
val show = Signal(true)

col {
    reactiveScope {
        if (show()) {
            text("Conditionally shown")
        }
    }
}
```

### Themes (Semantic Styling)
```kotlin
// Common semantics
card.text("In a card")
important.button { text("Important action") }
danger.button { text("Destructive action") }
warning.text("Warning message")
critical.button { text("Critical action") }
fieldTheme.textInput { /* ... */ }
```

## State Management (Reactivity)

### Signal - Mutable Reactive State
```kotlin
val count = Signal(0)

// Read value (in reactive context)
text { ::content { "Count: ${count()}" } }

// Write value
count.value = 5
count.value++
```

### Property - Another name for Signal
```kotlin
val email = Property("")
textInput { content bind email }
```

### Constant - Immutable Reactive Value
```kotlin
val title: Reactive<String> = Constant("Welcome")
```

### Shared - Computed Reactive Value
```kotlin
val firstName = Signal("John")
val lastName = Signal("Doe")
val fullName = shared { "${firstName()} ${lastName()}" }

text { ::content { fullName() } }
```

### LazyProperty - Computed with Override
```kotlin
val calculatedValue = LazyProperty { baseValue() * 2 }

// Can override
calculatedValue.value = 100

// Reset to calculation
calculatedValue.reset()
```

### LateInitProperty - Initially Unset
```kotlin
val userData = LateInitProperty<UserData>()

// Components show loading until set
userData.value = fetchedUserData

// Can unset
userData.unset()
```

### Reactive Scope
```kotlin
text {
    // Automatically re-runs when dependencies change
    ::content { "Total: ${price() * quantity()}" }
}
```

### ReactiveScope Block
```kotlin
reactiveScope {
    // Re-runs when signals inside change
    if (showAdvanced()) {
        advancedSettings()
    }
}
```

### ForEach - Reactive Lists
```kotlin
val items = Signal(listOf("A", "B", "C"))

col {
    forEach(items) { item ->
        card.text(item)
    }
}
```

### Two-way Binding
```kotlin
val text = Signal("")

textInput {
    content bind text  // Bidirectional binding
}
```

## Navigation

### Navigate to Page
```kotlin
button {
    text("Go to Settings")
    onClick {
        pageNavigator.navigate(SettingsPage)
    }
}
```

### Navigate with Parameters
```kotlin
onClick {
    pageNavigator.navigate(UserProfilePage("user123"))
}
```

### Navigate Back
```kotlin
onClick {
    pageNavigator.goBack()
}
```

### Replace Current Page
```kotlin
onClick {
    pageNavigator.replace(LoginPage)
}
```

### Using Links
```kotlin
link {
    text("Settings")
    to = { SettingsPage }
}
```

## Theming

### Using Built-in Themes
```kotlin
// Available themes
Theme.clean()
Theme.flat()
Theme.flat2()
Theme.m3()
Theme.shadCnLike("theme-name")
```

### Applying Semantics
```kotlin
// These are modifiers that apply semantic meaning
important.button { text("Save") }      // Important action
danger.button { text("Delete") }       // Destructive action
warning.text("Warning message")        // Warning
critical.button { text("Override") }   // Critical action
card.col { /* ... */ }                 // Card background
fieldTheme.textInput { /* ... */ }     // Field styling
```

### Theme Rules
1. Switching themes causes a background/card
2. Switching to the *same* theme doesn't create a card (use `card` explicitly)
3. Apply theme switches to containers, not individual elements
4. Typical places: `button`, `col`, `row`, `frame`

### Custom Semantic
```kotlin
data object CustomSemantic : Semantic("custom") {
    override fun default(theme: Theme): ThemeAndBack = theme.withoutBack(
        foreground = Color.blue,
        // ... other customizations
    )
}

// Usage
CustomSemantic.onNext.text("Custom themed")
```

## Working with Forms

### Basic Form
```kotlin
val email = Signal("")
val password = Signal("")

col {
    field("Email") {
        fieldTheme.textInput {
            hint = "your@email.com"
            keyboardHints = KeyboardHints.email
            content bind email
        }
    }

    field("Password") {
        fieldTheme.textInput {
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

### Field with Label
```kotlin
field("Username") {
    textInput {
        hint = "Enter username"
        content bind username
    }
}
```

## Dialogs and Overlays

### Toast Notification
```kotlin
onClick {
    toast("Operation successful!")
}
```

### Alert Dialog
```kotlin
onClick {
    alert("Are you sure you want to delete this?")
}
```

### Confirm Dialog
```kotlin
onClick {
    val confirmed = confirm("Delete this item?")
    if (confirmed) {
        deleteItem()
    }
}
```

### Custom Dialog
```kotlin
onClick {
    dialog {
        card.col {
            h2("Custom Dialog")
            text("Dialog content here")
            row {
                button {
                    text("Cancel")
                    onClick { close() }
                }
                important.button {
                    text("Confirm")
                    onClick {
                        // Do something
                        close()
                    }
                }
            }
        }
    }
}
```

### Bottom Sheet
```kotlin
onClick {
    bottomSheet {
        col {
            h2("Options")
            button {
                text("Option 1")
                onClick { /* ... */ close() }
            }
            button {
                text("Option 2")
                onClick { /* ... */ close() }
            }
        }
    }
}
```

## Advanced Patterns

### Loading States
```kotlin
val data = LateInitProperty<MyData>()

col {
    // Automatically shows loading indicator while data is unset
    text { ::content { data().displayName } }
}

// Later, set the data
launch {
    data.value = fetchData()
}
```

### Actions with Loading
```kotlin
button {
    text("Load Data")
    action = Action("Load", Icon.download) {
        // Button automatically shows loading state
        delay(2000)
        val result = fetchData()
        // Loading state automatically clears when done
    }
}
```

### Conditional Rendering
```kotlin
val showAdvanced = Signal(false)

col {
    checkbox {
        checked bind showAdvanced
    }

    reactiveScope {
        if (showAdvanced()) {
            card.col {
                h3("Advanced Options")
                // ... advanced controls
            }
        }
    }
}
```

### Custom Components (Reusable Widgets)
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

## Units and Dimensions

```kotlin
// Rem units (relative to root font size) - preferred for responsive design
1.rem
2.5.rem

// Pixels (absolute)
100.px
50.px

// DP (density-independent pixels, Android concept)
16.dp

// Percentages
50.percent
```

## Best Practices

### Component Design
1. Components should take minimal parameters unique per usage
2. Components should load their own data (makes them easy to debug)
3. Components should be used more than once; single-use components should be inlined
4. Keep view hierarchy shallow to minimize theme recalculations

### Modifier Order
**Position > Visibility > Scroll > Theme**

Example:
```kotlin
expanding.scrolling.card.col {
    // Position first (expanding)
    // Then scroll (scrolling)
    // Then theme (card)
}
```

### State Management
1. Use `Signal` for mutable state
2. Use `shared` for computed values
3. Use `LateInitProperty` for async-loaded data
4. Prefer reactive functions (`::content { }`) over manual updates

### Theming
1. Use semantic themes (`important`, `danger`) over direct colors
2. Apply themes to containers, not individual elements
3. Let themes cascade from parent to children
4. Use `card` for explicit backgrounds when needed

### Navigation
1. Use `@Routable` annotations for all pages
2. Make deep linking easy with URL parameters
3. Use typed navigation (`navigate(MyPage)`) over string URLs

### Performance
1. Use `forEach` for dynamic lists instead of manual child management
2. Use `Recycler2` for long lists that need virtualization
3. Keep view hierarchy shallow
4. Batch child operations when possible
5. Use `shared` to avoid redundant calculations

## Common Patterns

### Login Screen
```kotlin
@Routable("login")
object LoginPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val email = Signal("")
        val password = Signal("")

        frame {
            // Background image
            image {
                source = Resources.loginBackground
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }

            // Login form
            padded.scrolling.col {
                expanding.space()
                centered.sizeConstraints(maxWidth = 30.rem).card.col {
                    h1("Welcome")

                    field("Email") {
                        fieldTheme.textInput {
                            hint = "your@email.com"
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }

                    field("Password") {
                        fieldTheme.textInput {
                            hint = "Password"
                            keyboardHints = KeyboardHints.password
                            content bind password
                            action = Action("Sign In", Icon.login) {
                                signIn(email(), password())
                            }
                        }
                    }

                    important.button {
                        text("Sign In")
                        onClick {
                            signIn(email(), password())
                        }
                    }
                }
                expanding.space()
            }
        }
    }

    private suspend fun ViewWriter.signIn(email: String, password: String) {
        // Login logic
        pageNavigator.navigate(HomePage)
    }
}
```

### List Page with Search
```kotlin
@Routable("users")
object UsersPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val searchQuery = Signal("")
        val users = Signal(listOf<User>())
        val filteredUsers = shared {
            val query = searchQuery().lowercase()
            users().filter { it.name.lowercase().contains(query) }
        }

        launch {
            users.value = api.fetchUsers()
        }

        col {
            card.row {
                expanding.textInput {
                    hint = "Search users"
                    content bind searchQuery
                }
            }

            scrolling.col {
                forEach(filteredUsers) { user ->
                    userCard(user)
                }
            }
        }
    }
}
```

### Detail Page with Loading
```kotlin
@Routable("product/{id}")
class ProductPage(val id: String) : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val product = LateInitProperty<Product>()

        launch {
            product.value = api.fetchProduct(id)
        }

        scrolling.col {
            // Automatically shows loading while product is unset
            image {
                ::source { product().imageUrl }
                scaleType = ImageScaleType.Crop
            }

            padded.col {
                h1 { ::content { product().name } }
                text { ::content { product().description } }
                text { ::content { "$${product().price}" } }

                important.button {
                    text("Add to Cart")
                    onClick {
                        addToCart(product())
                    }
                }
            }
        }
    }
}
```

## Common Issues and Solutions

### Issue: Theme not applying
**Solution**: Ensure the view has been added to parent and `postSetup()` has been called. Themes cascade from parent.

### Issue: State not updating UI
**Solution**: Use reactive scope with `::content { }` or `reactiveScope { }` to make UI respond to state changes.

### Issue: Memory leaks
**Solution**: Enable leak detection in development: `RViewHelper.leakDetection = true`. Check for strong references in closures and verify `shutdown()` is called.

### Issue: Views not appearing
**Solution**: Check that parent view is actually visible and has size. Use `sizeConstraints` if needed.

## Resources

- **Example App**: https://kiteui.cs.lightningkite.com/
- **Maven Repository**: https://lightningkite-maven.s3.us-west-2.amazonaws.com
- **KDoc**: https://lightningkite-maven.s3.us-west-2.amazonaws.com/com/lightningkite/kiteui/library/docs/index.html

## Key Takeaways

1. **KiteUI is web-first** - Small bundles, fast performance, native HTML elements
2. **Use semantic theming** - Style by meaning, not color
3. **Embrace reactivity** - Use Signals and reactive scopes for dynamic UIs
4. **URL-based navigation** - Deep linking is first-class
5. **Native views** - Platform components, not canvas rendering
6. **Beautiful by default** - Good-looking UIs without manual CSS

## Documentation Development Notes

### Reactive Async Data Loading
```kotlin
// rememberSuspending - correct pattern for async data
val data = rememberSuspending {
    delay(1000)
    fetchData()  // Async operation
}

// Access with reactive binding
text { ::content { data().toString() } }

// Automatic loading states - no manual handling needed
```

### Error Handling with Async Data
```kotlin
// Use try-catch around the USAGE, not inside rememberSuspending
try {
    text { ::content { data().name } }
} catch (e: Exception) {
    danger.text("Failed to load: ${e.message}")
}
```

### Lists and Iteration
```kotlin
// For reactive Signal lists - use forEach
val items = Signal(listOf("A", "B", "C"))
forEach(items) { item ->
    text(item)
}

// For static lists - use for loop
for (item in listOf("A", "B", "C")) {
    text(item)
}

// For long lists - use recyclerView
recyclerView {
    children(items, id = { it }) { item ->
        card.text { ::content { item() } }
    }
}
```

### Updating Lists Correctly
```kotlin
// WRONG - mutating won't trigger UI updates
items().add("D")

// CORRECT - replace entire list
items.value = items() + "D"
```

### Conditional Display Pattern
```kotlin
// Use shownWhen modifier
val visible = Signal(true)
shownWhen { visible() }.text("Conditional")

// Can chain with other modifiers
shownWhen { visible() }.card.col {
    text("Content")
}
```

### Documentation Page Patterns
```kotlin
// Simple example without live demo
code {
    content = """
        val count = Signal(0)
        button { text { ::content { count().toString() } } }
    """.trimIndent()
}

// Example with live demo
example("""
    val count = Signal(0)
    button { text { ::content { count().toString() } } }
""".trimIndent()) {
    card.col {
        val count = Signal(0)
        button {
            text { ::content { count().toString() } }
            onClick { count.value++ }
        }
    }
}
```

### Autogeneration Gotchas
- The @Routable annotation processor parses all source files
- Avoid putting @Routable in code string examples
- Use comment format instead: `// Annotation: Routable("path")`
- Clean and regenerate if routes are corrupted: `rm -rf build/generated && ./gradlew generateAutoRoutes`

### Testing Strategy for Docs
1. Create ONE page at a time
2. Test compilation after EACH page
3. Keep examples simple and focused
4. Use actual working reactive patterns
5. Avoid complex async patterns in examples
6. Test in actual running app when possible
