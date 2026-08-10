# KiteUI Testing Guide

## Overview

KiteUI includes a comprehensive cross-platform testing framework that allows you to write UI tests once and run them on all supported platforms (JavaScript/Web, Android, and iOS).

## Table of Contents

1. [Getting Started](#getting-started)
2. [Basic Concepts](#basic-concepts)
3. [Writing Tests](#writing-tests)
4. [Finding Views](#finding-views)
5. [Interacting with Views](#interacting-with-views)
6. [Assertions](#assertions)
7. [Testing Reactive State](#testing-reactive-state)
8. [Testing Pages](#testing-pages)
9. [Best Practices](#best-practices)
10. [Platform-Specific Considerations](#platform-specific-considerations)

## Getting Started

### Test Structure

Tests are written in `commonTest` and automatically run on all platforms:

```kotlin
import com.lightningkite.kiteui.testing.*
import kotlin.test.Test

class MyComponentTest {
    @Test
    fun myTest() = kiteUiTest {
        // Your test code here
    }
}
```

### Basic Test Example

```kotlin
@Test
fun buttonClickWorks() = kiteUiTest {
    var clicked = false

    val scope = render {
        "my-button".testId.button {
            text("Click Me")
            onClick { clicked = true }
        }
    }

    scope.findByTestId("my-button").click()
    awaitStability()

    assertTrue(clicked)
}
```

## Basic Concepts

### Test Harness

`KiteUiTestHarness` creates an isolated testing environment:

- Creates a test `ElementContext`
- Manages view lifecycle
- Handles cleanup

Use the `kiteUiTest` helper function for automatic setup and teardown.

### Test IDs

Add test IDs to views for reliable querying:

```kotlin
"submit-button".testId.button {
    text("Submit")
}
```

Test IDs are stored in view metadata and don't affect production behavior.

### Await Stability

After interactions or state changes, call `awaitStability()` to wait for reactive updates:

```kotlin
property.value = "new value"
awaitStability()
// Now safe to make assertions
```

## Writing Tests

### Component Tests

Test individual components in isolation:

```kotlin
@Test
fun textInputBindsToSignal() = kiteUiTest {
    val inputValue = Signal("")

    val scope = render {
        "test-input".testId.textInput {
            content bind inputValue
        }
    }

    scope.findByTestId("test-input").typeText("Hello")
    awaitStability()

    assertEquals("Hello", inputValue.value)
}
```

### Page Tests

Test complete pages:

```kotlin
@Test
fun loginPageWorks() = kiteUiTest {
    val page = LoginPage()
    val scope = renderPage(page)

    scope.findByTestId("email").typeText("user@example.com")
    scope.findByTestId("password").typeText("password123")
    scope.findByText("Log In").click()

    awaitStability()

    scope.assertExists { withText("Welcome!") }
}
```

## Finding Views

### By Test ID

Most reliable method:

```kotlin
scope.findByTestId("my-view")
```

### By Text

Find TextViews containing specific text:

```kotlin
scope.findByText("Submit")              // Contains
scope.findByText("Submit", exact = true) // Exact match
```

### By Type

Find views of a specific type:

```kotlin
scope.findByType<Button>()
```

### Custom Criteria

Build complex queries:

```kotlin
scope.findAll {
    ofType<TextView>()
    withVisibility(true)
    matching { it.parent != null }
}
```

## Interacting with Views

### Click

```kotlin
scope.findByTestId("button").click()
```

### Long Click

```kotlin
scope.findByTestId("item").longClick()
```

Note: iOS has limitations with long click simulation.

### Type Text

```kotlin
scope.findByTestId("input").typeText("Hello World")
```

### Clear Text

```kotlin
scope.findByTestId("input").clearText()
```

### Scroll Into View

```kotlin
scope.findByTestId("far-down-item").scrollIntoView()
```

### Swipe

```kotlin
scope.findByTestId("scrollable").swipe(SwipeDirection.Up)
```

Note: iOS has limitations with swipe simulation.

## Assertions

### Existence

```kotlin
scope.assertExists { withTestId("my-view") }
scope.assertNotExists { withText("Error") }
```

### Count

```kotlin
scope.assertCount(3) { withText("Item", exact = false) }
```

### Text Content

```kotlin
scope.findByTestId("label").assertText("Expected Text")
```

### Visibility

```kotlin
scope.findByTestId("view").assertVisible(true)
scope.findByTestId("hidden").assertVisible(false)
```

### Enabled State

```kotlin
scope.findByTestId("button").assertEnabled(true)
```

### Custom Properties

```kotlin
scope.findByTestId("image").assertProperty<ImageView>("scaleType", ImageScaleType.Fit) {
    scaleType
}
```

## Testing Reactive State

### Signal Changes

```kotlin
@Test
fun signalChangeUpdatesUI() = kiteUiTest {
    val message = Signal("Initial")

    val scope = render {
        "label".testId.text {
            ::content { message() }
        }
    }

    scope.findByTestId("label").assertText("Initial")

    message.value = "Updated"
    awaitStability()

    scope.findByTestId("label").assertText("Updated")
}
```

### Remembered Calculations

```kotlin
@Test
fun rememberedCalculationCaches() = kiteUiTest {
    var count = 0
    val base = Signal(5)
    val calculated = remember {
        count++
        base() * 2
    }

    render {
        col {
            text { ::content { "${calculated()}" } }
            text { ::content { "${calculated()}" } }
        }
    }

    awaitStability()
    assertEquals(1, count) // Only calculated once
}
```

### Conditional Rendering

```kotlin
@Test
fun conditionalRendering() = kiteUiTest {
    val show = Signal(false)

    val scope = render {
        col {
            if (show()) {
                "conditional".testId.text("Visible")
            }
        }
    }

    scope.assertNotExists { withTestId("conditional") }

    show.value = true
    awaitStability()

    scope.assertExists { withTestId("conditional") }
}
```

## Testing Pages

### Page Rendering

```kotlin
@Test
fun pageRendersCorrectly() = kiteUiTest {
    val page = MyPage()
    val scope = renderPage(page)

    scope.assertExists { withText("Page Title") }
}
```

### Page Navigation

When testing navigation, you can verify page properties or state:

```kotlin
@Test
fun navigationUpdatesPage() = kiteUiTest {
    val page = DetailPage(itemId = "123")
    val scope = renderPage(page)

    // Verify page loaded correct data
    scope.assertExists { withText("Item 123") }
}
```

### Form Submission

```kotlin
@Test
fun formSubmission() = kiteUiTest {
    val page = FormPage()
    val scope = renderPage(page)

    scope.findByTestId("name-input").typeText("John Doe")
    scope.findByTestId("email-input").typeText("john@example.com")
    scope.findByTestId("submit-button").click()

    awaitStability()

    assertTrue(page.submitted)
    assertEquals("John Doe", page.submittedData.name)
}
```

## Best Practices

### 1. Use Test IDs for Important Elements

```kotlin
// Good
"submit-button".testId.button { text("Submit") }

// Less reliable
button { text("Submit") } // Found only by text
```

### 2. Always Await Stability After Interactions

```kotlin
scope.findByTestId("button").click()
awaitStability() // ← Important!
scope.assertExists { withText("Success") }
```

### 3. Test Behavior, Not Implementation

```kotlin
// Good - tests behavior
scope.findByTestId("counter").click()
scope.assertText("Count: 1")

// Less good - tests internal state
assertEquals(1, component.internalCounter)
```

### 4. Keep Tests Focused

```kotlin
// Good - one thing per test
@Test
fun buttonClickIncrementsCounter() = kiteUiTest { /* ... */ }

@Test
fun buttonIsDisabledWhenMaxReached() = kiteUiTest { /* ... */ }

// Less good - testing multiple things
@Test
fun buttonWorks() = kiteUiTest { /* tests 5 different behaviors */ }
```

### 5. Use Descriptive Test Names

```kotlin
// Good
@Test
fun loginButtonIsDisabledWithEmptyFields() = kiteUiTest { /* ... */ }

// Less good
@Test
fun test1() = kiteUiTest { /* ... */ }
```

### 6. Clean Up Resources

The `kiteUiTest` helper automatically calls `tearDown()`, but if you create a harness manually:

```kotlin
val harness = KiteUiTestHarness()
try {
    // Test code
} finally {
    harness.tearDown()
}
```

### 7. Test Edge Cases

```kotlin
@Test
fun handlesEmptyList() = kiteUiTest { /* ... */ }

@Test
fun handlesVeryLongText() = kiteUiTest { /* ... */ }

@Test
fun handlesNullValues() = kiteUiTest { /* ... */ }
```

## Platform-Specific Considerations

### JavaScript/Web

- Full support for all interactions
- Uses DOM APIs for interaction simulation
- Fast test execution with Karma

### Android

- Uses Robolectric for unit tests
- Full support for all interactions
- May require Android SDK dependencies

### iOS

- **Limited gesture support**: `performLongClick` and `performSwipe` throw `UnsupportedOperationException`
- Click and text input work correctly
- Tests run in iOS simulator/XCTest environment

### Handling Platform Limitations

If you need to test long press or swipe on iOS, test the underlying logic directly:

```kotlin
// Instead of simulating gesture
@Test
fun longPressOpensMenu() = kiteUiTest {
    // Don't use: scope.findByTestId("item").longClick()

    // Instead, test the handler directly
    val page = MyPage()
    page.handleLongPress(myItem)
    assertTrue(page.menuOpen)
}
```

## Running Tests

### Run All Tests

```bash
./gradlew :library:allTests
```

### Run Platform-Specific Tests

```bash
./gradlew :library:jsTest          # JavaScript/Web
./gradlew :library:androidUnitTest # Android
./gradlew :library:iosX64Test      # iOS Simulator
```

### Run Specific Test Class

```bash
./gradlew :library:jvmTest --tests "ButtonTest"
```

## Examples

See the example tests in `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/examples/`:

- `ButtonTest.kt` - Button interaction examples
- `TextInputTest.kt` - Text input examples
- `ReactiveStateTest.kt` - Reactive property examples
- `LoginPageTest.kt` - Complete page testing example

## Troubleshooting

### Tests are flaky

- Ensure you call `awaitStability()` after interactions
- Increase delays if needed for slow operations
- Check that test IDs are unique

### Views not found

- Verify test IDs are set correctly
- Check that views are actually rendered (not hidden by conditions)
- Use `scope.assertExists { withText("some text") }` to debug

### Interactions don't work

- Ensure view is visible and enabled
- Check that you're using the correct view type
- Verify reactive bindings are set up correctly

## Advanced Topics

### Custom Matchers

```kotlin
fun ViewMatcher.withCustomProperty(value: String): ViewMatcher {
    return matching { view ->
        view.extensionData["customProp"] == value
    }
}

scope.assertExists { withCustomProperty("special") }
```

### Testing Async Operations

```kotlin
@Test
fun asyncDataLoading() = kiteUiTest {
    val page = DataPage()
    val scope = renderPage(page)

    // Trigger load
    scope.findByTestId("load-button").click()

    // Wait for async operation
    awaitStability()
    delay(100) // Additional wait if needed

    scope.assertExists { withText("Data loaded") }
}
```

### Screenshot Testing (Future)

```kotlin
@Test
fun visualRegression() = kiteUiTest {
    val scope = render { /* ... */ }

    val screenshot = scope.captureScreenshot()
    screenshot.assertMatchesGolden("login-page", threshold = 0.01)
}
```

## Contributing

When adding new components to KiteUI, please include tests demonstrating their usage and behavior across platforms.
