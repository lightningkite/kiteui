# KiteUI Testing Quick Reference

## One-Page Summary

KiteUI has a **mature, cross-platform testing framework** using Kotlin Multiplatform's expect/actual pattern. The framework provides a unified API for UI testing across Android, iOS, JavaScript, and JVM SSR platforms.

---

## Core API

### Test Harness
```kotlin
// Simple usage - automatic cleanup
@Test
fun myTest() = withTestHarness { harness ->
    val root = harness.render {
        button {
            debugName = "submit"
            text("Submit")
        }
    }
    
    // Find views
    val button = root.findByDebugName("submit")
    
    // Interact
    button?.click()
    
    // Screenshot
    val image = harness.screenshot("my-test")
}
```

### View Discovery
```kotlin
// By debugName
val view = root.findByDebugName("my-button")

// By predicate
val buttons = root.findAll { it is ButtonView }
```

### Interactions
```kotlin
// Click any view
view.click()
```

---

## Platform Support Matrix

| Feature | Android | iOS | JavaScript | JVM SSR |
|---------|---------|-----|--------------|---------|
| Render | ✓ | ✓ | ✓ | ✗ |
| Click | ✓ | ✓ | ✓ | ✗ |
| Screenshot | ✓ | ✓ | ✗ TODO | - |
| Layout Test | ✓ | ✓ | ✓ | ✗ |

---

## Key Characteristics

**Strengths:**
- Unified cross-platform API
- No test boilerplate (expect/actual pattern)
- Fast feedback (especially Android with Robolectric)
- Screenshot support for visual testing
- View discovery by debugName (semantic approach)

**Limitations:**
- JS screenshots not yet implemented
- JVM SSR not supported
- Interaction testing limited to simple clicks
- No built-in visual regression testing

---

## File Locations

### Core Framework
- Common interface: `library/src/commonTest/kotlin/.../testing/`
  - `TestHarness.kt` - expect interface
  - `Interactions.kt` - expect interface
  - `ViewFinder.kt` - common utilities

### Platform Implementations
- Android: `library/src/androidUnitTest/kotlin/.../testing/`
- iOS: `library/src/iosTest/kotlin/.../testing/`
- JavaScript: `library/src/jsTest/kotlin/.../testing/`
- JVM SSR: `library/src/jvmSsrTest/kotlin/.../testing/`

### Test Examples
- Component tests: `library/src/*/kotlin/.../components/`
  - ButtonTest.kt, RowTest.kt, TextViewTest.kt
- Layout tests: `library/src/*/kotlin/LayoutTest.kt`
- Screenshot tests: `library/src/*/kotlin/.../testing/ScreenshotTest.kt`

---

## Quick Start: Writing Tests

### 1. Create a test file
```kotlin
// library/src/*/kotlin/.../components/MyComponentTest.kt
package com.lightningkite.kiteui.components

import com.lightningkite.kiteui.testing.withTestHarness
import com.lightningkite.kiteui.testing.findByDebugName
import kotlin.test.Test

class MyComponentTest {
    @Test
    fun testMyComponent() = withTestHarness { harness ->
        // Your test here
    }
}
```

### 2. Render your UI
```kotlin
val root = harness.render {
    myComponent()
}
```

### 3. Find and interact
```kotlin
val element = root.findByDebugName("my-element")
element?.click()
```

### 4. Assert
```kotlin
assertEquals("expected", actualValue)
```

---

## Running Tests

```bash
# All platforms
./gradlew allTests

# Specific platform
./gradlew :library:androidUnitTest
./gradlew :library:iosX64Test
./gradlew :library:jsTest
./gradlew :library:jvmSsrTest  # Will be skipped

# Watch mode (not supported by default)
# For development, run in IDE or use watch plugin
```

---

## Critical Implementation Details

### Android (Robolectric)
- Must initialize `AndroidAppContext.applicationCtx` before view creation
- Uses `@GraphicsMode(GraphicsMode.Mode.NATIVE)` for graphics tests
- Requires `@RunWith(RobolectricTestRunner::class)`
- Screenshots via Roborazzi

### iOS (Kotlin Native)
- Do NOT call `makeKeyAndVisible()` - causes SIGTRAP
- Do NOT call `setup()` inside `viewDidLoad()` - causes SIGTRAP
- Must manually call `setNeedsLayout()` + `layoutIfNeeded()`
- Screenshots via UIGraphics context rendering

### JavaScript (Compiled to JS)
- Uses `root(Theme) {}` initialization
- Full DOM access via `view.native`
- Clicks invoke actions directly (no DOM events)
- Screenshots: NOT YET IMPLEMENTED

### JVM SSR
- Marked as unsupported (`supported = false`)
- Tests automatically skip
- Use for SSR-specific unit tests only

---

## Best Practices

1. **Always set debugName for test discovery**
   ```kotlin
   myView.apply { debugName = "my-component" }
   ```

2. **Use withTestHarness for automatic cleanup**
   ```kotlin
   @Test
   fun test() = withTestHarness { harness ->
       // Automatic cleanup after block
   }
   ```

3. **Separate UI test logic from assertion**
   ```kotlin
   val root = harness.render { /* UI */ }
   val element = root.findByDebugName("element")
   assertNotNull(element)
   ```

4. **Use screenshots for visual regression**
   ```kotlin
   val image = harness.screenshot("component-state")
   assertNotNull(image)
   ```

5. **Test interaction with click**
   ```kotlin
   val button = root.findByDebugName("submit")
   button?.click()
   // Assert side effects
   ```

---

## Common Patterns

### Testing Button Clicks
```kotlin
var clickCount = 0
val root = harness.render {
    button { onClick { clickCount++ } }
}
root.findByDebugName("button")?.click()
assertEquals(1, clickCount)
```

### Testing Text Changes
```kotlin
val text = Property("initial")
val root = harness.render {
    text { ::content { text() } }
}
text.value = "updated"
// Verify UI update
```

### Testing Layout
```kotlin
val root = harness.render {
    col {
        text("Item 1")
        text("Item 2")
    }
}
val children = root.children
assertEquals(2, children.size)
```

### Capturing Screenshots
```kotlin
val screenshot = harness.screenshot("my-test")
assertNotNull(screenshot)
// Screenshot saved to library/local/screenshots/{platform}/
```

---

## Troubleshooting

**"Test skipped on this platform"**
- Platform's `TestHarness.supported = false`
- Check: JVM SSR tests always skip; JS screenshots not implemented

**"No root view found"**
- Ensure `harness.render()` is called and properly set up
- Check platform-specific initialization (Android context, iOS layout)

**"View not found by debugName"**
- Verify debugName is set on the view
- Check view hierarchy (use findAll to debug)
- Ensure view is inside the rendered tree

**"Click has no effect"**
- Android: view must be clickable (button, etc)
- iOS: view must be UIControl
- JS: view must have action set

**Screenshot returns null**
- Android/iOS: Roborazzi not installed or graphics mode issue
- JS: Screenshot support not yet implemented
- Check build.gradle.kts for roborazzi dependency

---

## Screenshot Locations

```
library/local/screenshots/
├── android/
│   ├── basic-screenshot.png
│   ├── complex-layout.png
│   └── ...
└── ios/
    ├── basic-screenshot-ios.png
    └── ...
```

---

## Next: Implementing JS Screenshots

The only major feature gap is JavaScript screenshot support. To implement:

1. Use `html2canvas` or similar canvas library
2. Access root view's DOM element
3. Render canvas to PNG data
4. Save or return ByteArray

See `/Users/jivie/Projects/kiteui/library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.js.kt` for the TODO comment.

---

## References

**Key Documentation Files:**
- `library/src/commonTest/kotlin/.../testing/TestHarness.kt` - Core interface with docs
- `library/src/commonTest/kotlin/.../testing/ViewFinder.kt` - View discovery utilities
- `library/src/commonTest/kotlin/.../testing/Interactions.kt` - Interaction interface

**Example Tests:**
- `library/src/androidUnitTest/kotlin/.../testing/ScreenshotTest.kt`
- `library/src/androidUnitTest/kotlin/components/ButtonTest.kt`
- `library/src/jsTest/kotlin/LayoutTest.kt`

**Full Documentation:**
- See `TESTING_INFRASTRUCTURE.md` for detailed breakdown
- See `TESTING_DIRECTORY_STRUCTURE.md` for file organization
