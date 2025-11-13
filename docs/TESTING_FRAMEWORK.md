# KiteUI Testing Framework

Complete guide to testing KiteUI applications with unit tests, snapshots, and interactions.

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [TestHarness](#testharness)
4. [Finding Views](#finding-views)
5. [User Interactions](#user-interactions)
6. [Screenshot Testing](#screenshot-testing)
7. [Platform Support](#platform-support)
8. [Best Practices](#best-practices)
9. [Examples](#examples)

## Overview

The KiteUI Testing Framework provides a cross-platform API for testing UI components with:

- **TestHarness**: Set up and render UI in tests
- **ViewFinder**: Locate views in the hierarchy
- **Interactions**: Simulate user actions (clicks, text input)
- **Screenshots**: Capture visual snapshots for regression testing

All testing APIs work across Android, iOS, and JavaScript platforms using the expect/actual pattern.

## Getting Started

### Basic Test Structure

```kotlin
import com.lightningkite.kiteui.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class MyComponentTest {
    @Test
    fun testMyComponent() = withTestHarness { harness ->
        // 1. Render your UI
        val root = harness.render {
            text("Hello, World!")
        }

        // 2. Find views and interact
        // 3. Assert expected behavior
    }
}
```

### Platform Configuration

**Android**: Uses Robolectric for unit tests

```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AndroidTest {
    // Tests here
}
```

**iOS**: No special configuration needed

```kotlin
class IOSTest {
    // Tests here
}
```

**JavaScript**: Uses Karma with browser testing

```kotlin
class JSTest {
    // Tests here
}
```

## TestHarness

The `TestHarness` provides the foundation for setting up UI tests.

### Rendering UI

```kotlin
@Test
fun testRendering() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("Title")
            text("Subtitle")
        }
    }
    // root is now the RView for the col
}
```

### Custom Themes

```kotlin
@Test
fun testWithTheme() = withTestHarness { harness ->
    val theme = Theme(
        id = "dark",
        background = Color.black,
        foreground = Color.white
    )

    val root = harness.render(theme) {
        text("Dark mode text")
    }
}
```

### Cleanup

The `withTestHarness` helper automatically cleans up after tests. For manual cleanup:

```kotlin
@Test
fun manualCleanup() {
    val harness = TestHarness()
    try {
        val root = harness.render { /* UI here */ }
        // Test code
    } finally {
        harness.cleanup()
    }
}
```

## Finding Views

Use `debugName` to mark views for testing, then locate them with `findByDebugName()`.

### Basic Finding

```kotlin
@Test
fun testFindView() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("Item 1").apply { debugName = "first" }
            text("Item 2").apply { debugName = "second" }
        }
    }

    val firstText = root.findByDebugName("first")
    assertNotNull(firstText)
}
```

### Finding Multiple Views

```kotlin
@Test
fun testFindAll() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("A")
            text("B")
            text("C")
        }
    }

    // Find all text views
    val allTexts = root.findAll { view ->
        view is TextView
    }
    assertEquals(3, allTexts.size)
}
```

### View Hierarchy Navigation

```kotlin
@Test
fun testHierarchy() = withTestHarness { harness ->
    val root = harness.render {
        col {
            debugName = "container"
            row {
                text("Left")
                text("Right")
            }
        }
    }

    val container = root.findByDebugName("container")
    assertNotNull(container)
    assertEquals(1, container.children.size) // 1 row child
}
```

## User Interactions

Simulate user actions with the `Interactions` API.

### Clicking

```kotlin
@Test
fun testButtonClick() = withTestHarness { harness ->
    var clicked = false

    val root = harness.render {
        button {
            debugName = "my-button"
            text("Click Me")
            onClick { clicked = true }
        }
    }

    // Method 1: Using extension function
    root.findByDebugName("my-button")?.click()

    // Method 2: Using Interactions object
    val button = root.findByDebugName("my-button")!!
    Interactions.click(button)

    assertTrue(clicked)
}
```

### Text Input

```kotlin
@Test
fun testTextInput() = withTestHarness { harness ->
    val emailProperty = Property("")

    val root = harness.render {
        textInput {
            debugName = "email"
            content bind emailProperty
        }
    }

    // Type into input field
    root.findByDebugName("email")?.typeText("user@example.com")

    assertEquals("user@example.com", emailProperty.value)
}
```

### Appending Text

```kotlin
@Test
fun testAppendText() = withTestHarness { harness ->
    val textProperty = Property("Hello ")

    val root = harness.render {
        textInput {
            debugName = "message"
            content bind textProperty
        }
    }

    // Append to existing text
    root.findByDebugName("message")?.typeText("World!", append = true)

    assertEquals("Hello World!", textProperty.value)
}
```

### Form Testing

```kotlin
@Test
fun testLoginForm() = withTestHarness { harness ->
    var submitted = false
    var submittedUsername = ""
    var submittedPassword = ""

    val usernameProperty = Property("")
    val passwordProperty = Property("")

    val root = harness.render {
        col {
            textInput {
                debugName = "username"
                hint = "Username"
                content bind usernameProperty
            }

            textInput {
                debugName = "password"
                hint = "Password"
                content bind passwordProperty
            }

            button {
                debugName = "submit"
                text("Login")
                onClick {
                    submitted = true
                    submittedUsername = usernameProperty.value
                    submittedPassword = passwordProperty.value
                }
            }
        }
    }

    // Fill in form
    root.findByDebugName("username")?.typeText("alice")
    root.findByDebugName("password")?.typeText("secret123")
    root.findByDebugName("submit")?.click()

    // Verify submission
    assertTrue(submitted)
    assertEquals("alice", submittedUsername)
    assertEquals("secret123", submittedPassword)
}
```

## Screenshot Testing

Capture visual snapshots for regression testing and debugging.

### Platform Support

| Platform | Library | Status | Output Location |
|----------|---------|--------|-----------------|
| Android | Roborazzi | ✅ Fully supported | `library/build/outputs/roborazzi/` |
| iOS | UIKit | ✅ Fully supported | `NSTemporaryDirectory()/screenshots/` |
| JS | html2canvas | ✅ Async support | Browser window with download link |
| JVM SSR | N/A | ❌ Not supported | N/A |

### Basic Screenshots

```kotlin
@Test
fun testScreenshot() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("Screenshot Test")
            text("This will be captured")
        }
    }

    // Capture screenshot of entire view
    val screenshot = harness.screenshot("my-test-screenshot")
    assertNotNull(screenshot) // Android and iOS return ByteArray
}
```

### Component Screenshots

```kotlin
@Test
fun testComponentScreenshot() = withTestHarness { harness ->
    val root = harness.render {
        col {
            row {
                debugName = "header"
                text("Logo")
                text("Menu")
            }
            text("Body content")
        }
    }

    // Capture just the header row
    val header = root.findByDebugName("header")!!
    val screenshot = harness.screenshotView(header, "header-only")
    assertNotNull(screenshot)
}
```

### Saving Screenshots (Android/iOS)

```kotlin
private fun saveScreenshot(data: ByteArray, name: String) {
    val file = File("local/screenshots/$name.png")
    file.parentFile?.mkdirs()
    file.writeBytes(data)
    println("Screenshot saved: ${file.absolutePath}")
}

@Test
fun testWithSaving() = withTestHarness { harness ->
    val root = harness.render {
        text("Test")
    }

    val screenshot = harness.screenshot("test")
    if (screenshot != null) {
        saveScreenshot(screenshot, "my-component")
    }
}
```

### JavaScript Screenshots

JavaScript screenshots work asynchronously. They appear in the browser window with download links:

```kotlin
@Test
fun testJSScreenshot() = withTestHarness { harness ->
    val root = harness.render {
        text("JS Test")
    }

    // Triggers async screenshot capture
    harness.screenshot("js-test")
    // Returns null immediately, but screenshot appears in browser

    // Look for:
    // - Canvas element with green border
    // - Download link below the canvas
    // - Console messages about screenshot completion
}
```

## Platform Support

### Android (Robolectric + Roborazzi)

**Features:**
- ✅ Full rendering support
- ✅ Screenshot testing with Roborazzi
- ✅ Click interactions
- ✅ Text input
- ✅ Synchronous operations

**Run Tests:**
```bash
./gradlew :library:testDebugUnitTest
```

### iOS

**Features:**
- ✅ Full rendering support with UIKit
- ✅ Screenshot testing with layer rendering
- ✅ Click interactions (UIControl)
- ✅ Text input
- ✅ Synchronous operations

**Run Tests:**
```bash
./gradlew :library:iosX64Test
```

### JavaScript (Karma)

**Features:**
- ✅ Full rendering support in browser
- ✅ Screenshot testing with html2canvas (async)
- ✅ Click interactions
- ✅ Text input
- ⚠️ Async screenshots only

**Run Tests:**
```bash
./gradlew :library:jsTest
```

### JVM SSR

**Status:** Not supported (no UI rendering)

## Best Practices

### 1. Use Debug Names

Always use `debugName` for views you'll interact with in tests:

```kotlin
button {
    debugName = "submit-button"  // Good
    text("Submit")
}

button {
    text("Submit")  // Bad - can't find easily
}
```

### 2. Test Behavior, Not Implementation

```kotlin
// Good: Test user-visible behavior
@Test
fun testCounter() = withTestHarness { harness ->
    val root = harness.render { CounterComponent() }
    root.findByDebugName("increment")?.click()
    val display = root.findByDebugName("count-display")
    // Assert display shows "1"
}

// Bad: Test internal state
@Test
fun testCounterImplementation() {
    val counter = CounterViewModel()
    counter.increment()
    assertEquals(1, counter.count.value)
}
```

### 3. Keep Tests Focused

One test should verify one behavior:

```kotlin
// Good
@Test
fun testIncrementButton() { /* ... */ }

@Test
fun testDecrementButton() { /* ... */ }

@Test
fun testResetButton() { /* ... */ }

// Bad
@Test
fun testAllButtons() {
    // Tests increment, decrement, reset, edge cases, etc.
}
```

### 4. Use Property Bindings

Bind test properties to verify reactive updates:

```kotlin
@Test
fun testReactiveUpdate() = withTestHarness { harness ->
    val nameProperty = Property("Alice")

    val root = harness.render {
        text {
            ::content { "Hello, ${nameProperty()}" }
        }
    }

    nameProperty.value = "Bob"
    // Reactive system updates the text automatically
    // Verify the change was reflected
}
```

### 5. Clean Test Data

Use `withTestHarness` for automatic cleanup:

```kotlin
@Test
fun testWithCleanup() = withTestHarness { harness ->
    // Test code
    // Automatic cleanup happens here
}
```

### 6. Screenshot Naming

Use descriptive names for screenshots:

```kotlin
// Good
harness.screenshot("login-form-empty-state")
harness.screenshot("login-form-with-errors")
harness.screenshot("login-form-success")

// Bad
harness.screenshot("test1")
harness.screenshot("screenshot")
```

## Examples

### Complete Login Form Test

```kotlin
class LoginFormTest {
    @Test
    fun testLoginFlow() = withTestHarness { harness ->
        // Set up test data
        var loginAttempted = false
        var username = ""
        var password = ""

        val usernameProperty = Property("")
        val passwordProperty = Property("")

        // Render UI
        val root = harness.render {
            col {
                textInput {
                    debugName = "username-input"
                    hint = "Username"
                    content bind usernameProperty
                }

                textInput {
                    debugName = "password-input"
                    hint = "Password"
                    content bind passwordProperty
                }

                button {
                    debugName = "login-button"
                    text("Login")
                    onClick {
                        loginAttempted = true
                        username = usernameProperty.value
                        password = passwordProperty.value
                    }
                }
            }
        }

        // Take initial screenshot
        harness.screenshot("login-form-initial")

        // Fill in form
        root.findByDebugName("username-input")?.typeText("alice")
        root.findByDebugName("password-input")?.typeText("secret123")

        // Screenshot with filled form
        harness.screenshot("login-form-filled")

        // Submit
        root.findByDebugName("login-button")?.click()

        // Verify
        assertTrue(loginAttempted, "Login should be attempted")
        assertEquals("alice", username)
        assertEquals("secret123", password)
    }
}
```

### Counter Component Test

```kotlin
class CounterTest {
    @Test
    fun testCounterIncrement() = withTestHarness { harness ->
        val count = Property(0)

        val root = harness.render {
            col {
                text {
                    debugName = "count-display"
                    ::content { "Count: ${count()}" }
                }

                button {
                    debugName = "increment"
                    text("+1")
                    onClick { count.value++ }
                }
            }
        }

        // Initial state
        assertEquals(0, count.value)

        // Click increment
        root.findByDebugName("increment")?.click()
        assertEquals(1, count.value)

        // Click again
        root.findByDebugName("increment")?.click()
        assertEquals(2, count.value)
    }
}
```

### Form Validation Test

```kotlin
class FormValidationTest {
    @Test
    fun testEmailValidation() = withTestHarness { harness ->
        val email = Property("")
        val emailError = Property<String?>(null)

        val root = harness.render {
            col {
                textInput {
                    debugName = "email-input"
                    hint = "Email"
                    content bind email
                }

                text {
                    debugName = "error-message"
                    ::content { emailError() ?: "" }
                }

                button {
                    debugName = "validate"
                    text("Validate")
                    onClick {
                        emailError.value = when {
                            email.value.isEmpty() -> "Email required"
                            !email.value.contains("@") -> "Invalid email"
                            else -> null
                        }
                    }
                }
            }
        }

        // Test empty validation
        root.findByDebugName("validate")?.click()
        assertEquals("Email required", emailError.value)

        // Test invalid email
        root.findByDebugName("email-input")?.typeText("notanemail")
        root.findByDebugName("validate")?.click()
        assertEquals("Invalid email", emailError.value)

        // Test valid email
        root.findByDebugName("email-input")?.typeText("user@example.com")
        root.findByDebugName("validate")?.click()
        assertNull(emailError.value)
    }
}
```

## Troubleshooting

### Android: "AndroidAppContext not initialized"

Make sure Robolectric is configured:

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyTest { /* ... */ }
```

### iOS: "SIGTRAP in test environment"

Don't call `makeKeyAndVisible()` on windows in tests. The TestHarness handles this.

### JavaScript: "Screenshots not appearing"

JS screenshots are async. Check:
1. Browser console for completion messages
2. Look for canvas elements in the DOM
3. Check for download links

### View Not Found

```kotlin
val view = root.findByDebugName("my-view")
if (view == null) {
    // Print hierarchy for debugging
    println(root.debugHierarchy())
}
```

## Related Documentation

- [JavaScript Snapshot Testing](../library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/README-JS-SNAPSHOTS.md)
- [TESTING_GUIDE.md](./TESTING_GUIDE.md) - Detailed testing patterns
- [TestHarness.kt](../library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt) - API reference

## Contributing

To add new interaction types:

1. Add method to `Interactions` expect object in `commonTest`
2. Implement in each platform's actual object
3. Add extension function for convenience
4. Create test examples
5. Update documentation

Example structure:

```kotlin
// commonTest/Interactions.kt
expect object Interactions {
    fun newInteraction(view: RView)
}

fun RView.newInteraction() {
    Interactions.newInteraction(this)
}

// androidUnitTest/Interactions.android.kt
actual object Interactions {
    actual fun newInteraction(view: RView) {
        // Android implementation
    }
}
```
