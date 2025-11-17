# KiteUI Testing Infrastructure Summary

## Overview

KiteUI has a well-structured cross-platform testing framework using the **expect/actual** pattern for platform-specific implementations. The testing infrastructure supports Android, iOS, JavaScript/Web, and JVM SSR platforms with a unified test harness API.

---

## Core Testing Components

### 1. TestHarness (Expect/Actual Classes)

**Location:** `library/src/*/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt`

**Files:**
- `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt` (Common Interface)
- `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.android.kt` (Android)
- `/Users/jivie/Projects/kiteui/library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.ios.kt` (iOS)
- `/Users/jivie/Projects/kiteui/library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.js.kt` (JavaScript)
- `/Users/jivie/Projects/kiteui/library/src/jvmSsrTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.jvmSsr.kt` (JVM SSR)

**API:**
```kotlin
class TestHarness() {
    val supported: Boolean  // Indicates if platform supports testing
    
    fun render(theme: Theme, content: ViewWriter.() -> Unit): RView
    fun screenshot(name: String = "screenshot"): ByteArray?
    fun screenshotView(view: RView, name: String = "screenshot"): ByteArray?
    fun cleanup()
}

// Helper function for automatic cleanup
inline fun withTestHarness(block: (TestHarness) -> Unit)
```

**Key Features:**
- Platform-independent rendering API
- Automatic cleanup via `withTestHarness` helper
- Screenshot support (varies by platform)
- Skips unsupported platforms automatically

---

## Screenshot Functionality

### Android Implementation
- **Technology:** Roborazzi + Robolectric
- **Approach:** Uses `captureRoboImage()` from Roborazzi library
- **Output Path:** `library/local/screenshots/android/`
- **Status:** Fully Implemented
- **File:** `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.android.kt`

### iOS Implementation
- **Technology:** UIGraphics context rendering
- **Approach:** Uses `UIGraphicsBeginImageContext` + `layer.renderInContext()`
- **Output Path:** `library/local/screenshots/ios/` + temp directory
- **Status:** Fully Implemented
- **File:** `/Users/jivie/Projects/kiteui/library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.ios.kt`

### JavaScript Implementation
- **Status:** TODO - Not yet implemented
- **File:** `/Users/jivie/Projects/kiteui/library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.js.kt`
- **Notes:** Stub returns null; canvas-based implementation needed

### JVM SSR Implementation
- **Status:** Not Supported
- **Reason:** JVM SSR doesn't have full view hierarchy in tests
- **File:** `/Users/jivie/Projects/kiteui/library/src/jvmSsrTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.jvmSsr.kt`

---

## Interaction Testing

### Interactions (Expect/Actual Classes)

**Location:** `library/src/*/kotlin/com/lightningkite/kiteui/testing/Interactions.kt`

**Files:**
- `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/Interactions.kt` (Common Interface)
- `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/Interactions.android.kt`
- `/Users/jivie/Projects/kiteui/library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/Interactions.ios.kt`
- `/Users/jivie/Projects/kiteui/library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/Interactions.js.kt`
- `/Users/jivie/Projects/kiteui/library/src/jvmSsrTest/kotlin/com/lightningkite/kiteui/testing/Interactions.jvmSsr.kt`

**API:**
```kotlin
object Interactions {
    fun click(view: RView)
}

// Extension function
fun RView.click()
```

**Platform-Specific Implementations:**

| Platform | Implementation |
|----------|-----------------|
| Android | `view.native.performClick()` |
| iOS | `UIControl.sendActionsForControlEvents(UIControlEventTouchUpInside)` |
| JavaScript | Direct action invocation via `RViewWithAction.action?.startAction()` |
| JVM SSR | Throws `UnsupportedOperationException` |

---

## View Finder

**Location:** `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/ViewFinder.kt`

**API:**
```kotlin
object ViewFinder {
    fun findByDebugName(root: RView, debugName: String): RView?
    fun findAll(root: RView, predicate: (RView) -> Boolean): List<RView>
}

// Extension functions
fun RView.findByDebugName(debugName: String): RView?
fun RView.findAll(predicate: (RView) -> Boolean): List<RView>
```

**Features:**
- Depth-first search through view hierarchy
- Matches by `debugName` property
- Generic predicate-based search
- Cross-platform (works on all platforms)

---

## Existing Test Suite

### Screenshot Tests

**Android:** `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/ScreenshotTest.kt`
```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {
    @Test
    fun testBasicScreenshot()
    
    @Test
    fun testViewScreenshot()  // Specific view screenshot
    
    @Test
    fun testComplexLayoutScreenshot()
}
```

**iOS:** `/Users/jivie/Projects/kiteui/library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/ScreenshotTest.kt`
- Similar structure to Android tests
- Saves to both temp directory and project directory
- Includes debug printing with emoji indicators

### Component Tests

**ButtonTest (Android):** `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/components/ButtonTest.kt`
```kotlin
@RunWith(RobolectricTestRunner::class)
class ButtonTest {
    @Test
    fun testButtonClick()  // Interaction test
    
    @Test
    fun testButtonWithReactiveState()
}
```

**ButtonTest (JavaScript):** `/Users/jivie/Projects/kiteui/library/src/jsTest/kotlin/com/lightningkite/kiteui/components/ButtonTest.kt`
- Identical structure to Android
- Works with WebDriver/Playwright tests

**Other Component Tests:**
- `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/components/RowTest.kt`
- `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/components/TextViewTest.kt`
- Similar on iOS and JS platforms

### Harness Tests

**Android:** `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/TestHarnessTest.android.kt`
```kotlin
@RunWith(RobolectricTestRunner::class)
class AndroidTestHarnessTest {
    @Test
    fun testBasicRender()
    
    @Test
    fun testFindByDebugName()
}
```

**iOS & JavaScript:** Similar test files demonstrating platform-specific harness functionality

### Other Tests

- **LayoutTest.kt** (JS, Android)
  - Uses the `root()` function to set up test environment
  - Tests layout calculations and view hierarchy
  - Location: `library/src/jsTest/kotlin/LayoutTest.kt`, `library/src/androidUnitTest/kotlin/LayoutTest.kt`

- **Recycler2Test.kt** (JS)
  - Tests recycler/list view functionality
  - Location: `library/src/jsTest/kotlin/Recycler2Test.kt`

---

## Platform-Specific Details

### Android Testing
- **Framework:** Robolectric (unit tests)
- **Screenshot Library:** Roborazzi
- **Activity Harness:** Uses custom `TestActivity` extending `KiteUiActivity`
- **Context Setup:** Requires `AndroidAppContext.applicationCtx` initialization before view creation
- **Limitations:** Must initialize context before `onCreate()` to avoid crashes
- **Gradle Plugin:** `roborazzi` plugin configured in `build.gradle.kts`

### iOS Testing
- **Framework:** XCTest (native iOS testing)
- **UI Framework:** UIKit directly via Kotlin Native interop
- **Window Setup:** Creates `UIWindow` + `UIViewController` manually
- **Critical Notes:**
  - Do NOT call `makeKeyAndVisible()` - causes SIGTRAP in tests
  - Do NOT call `setup()` inside `viewDidLoad()` - causes SIGTRAP
  - Must call layout methods manually: `setNeedsLayout()` + `layoutIfNeeded()`
- **Screenshot Path:** `/tmp/screenshots/ios/` and `library/local/screenshots/ios/`

### JavaScript Testing
- **Framework:** Kotlin test (runs on Node.js via Dukat/Webpack)
- **Rendering:** Uses `root(Theme) {}` initialization pattern
- **View Hierarchy:** Full access to DOM elements via `view.native`
- **Screenshot Support:** Not yet implemented (TODO)
- **Action Triggering:** Direct action invocation without DOM events

### JVM SSR Testing
- **Status:** Mostly not supported
- **Reason:** SSR doesn't render full UI hierarchy in test environment
- **TestHarness:** `supported = false` (tests are skipped)
- **Interactions:** Throws `UnsupportedOperationException`

---

## Test Dependencies

**From `library/build.gradle.kts`:**
```gradle
// Testing libraries
androidUnitTest {
    implementation(libs.robolectric)
    implementation(libs.roborazzi)
}

// Plugins
plugins {
    alias(libs.plugins.roborazzi)
}
```

---

## Running Tests

```bash
# Run all platform tests
./gradlew allTests

# Run platform-specific tests
./gradlew :library:androidUnitTest          # Android tests
./gradlew :library:iosX64Test               # iOS tests
./gradlew :library:jsTest                   # JavaScript tests
./gradlew :library:jvmSsrTest               # JVM SSR tests (will be skipped)
```

---

## Screenshot Output Locations

| Platform | Location |
|----------|----------|
| Android | `library/local/screenshots/android/` |
| iOS | `library/local/screenshots/ios/` + `/tmp/screenshots/ios/` |
| JavaScript | Not yet implemented |
| JVM SSR | N/A |

---

## Example Test Usage

```kotlin
// Basic rendering test
@Test
fun myTest() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("Hello").apply { debugName = "title" }
            button {
                debugName = "submit"
                text("Submit")
                onClick { /* action */ }
            }
        }
    }
    
    // Find and interact with views
    val title = root.findByDebugName("title")
    val submitBtn = root.findByDebugName("submit")
    
    // Simulate interaction
    submitBtn?.click()
    
    // Capture screenshot
    val screenshot = harness.screenshot("my-test")
    assertNotNull(screenshot)
}
```

---

## Key Architectural Decisions

1. **Expect/Actual Pattern:** All platform-specific code uses Kotlin Multiplatform's expect/actual pattern
2. **Cross-Platform API:** Common interface allows same test code to run on all platforms
3. **Automatic Cleanup:** `withTestHarness` ensures proper resource cleanup
4. **Debug Names:** Views must set `debugName` for test discovery (semantic approach)
5. **Native View Access:** Tests access platform-native views via `view.native`
6. **Screenshot Support:** Only Android and iOS fully implemented; JS is TODO

---

## Current Limitations

| Platform | Feature | Status |
|----------|---------|--------|
| JS | Screenshot capture | TODO - needs canvas implementation |
| JVM SSR | Full testing | Not supported |
| iOS | Non-UIControl interactions | Limited (only buttons tested) |
| JS | Visual screenshot comparison | Not implemented |

---

## Files Summary

**Core Testing Framework:**
- `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt` - Common interface
- `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/Interactions.kt` - Common interaction API
- `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/ViewFinder.kt` - View discovery

**Platform Implementations:**
- Android: `TestHarness.android.kt`, `Interactions.android.kt`
- iOS: `TestHarness.ios.kt`, `Interactions.ios.kt`
- JavaScript: `TestHarness.js.kt`, `Interactions.js.kt`
- JVM SSR: `TestHarness.jvmSsr.kt`, `Interactions.jvmSsr.kt`

**Test Suites:**
- Screenshots: `ScreenshotTest.kt` (Android, iOS)
- Components: `ButtonTest.kt`, `RowTest.kt`, `TextViewTest.kt` (Android, iOS, JS)
- Harness: `TestHarnessTest.*.kt` (Android, iOS, JS)
- Layout: `LayoutTest.kt` (Android, JS)

---

## Next Steps / Opportunities

1. **JavaScript Screenshot Implementation:** Implement canvas-based screenshot capture
2. **Visual Regression Testing:** Set up image comparison tooling for automated visual checks
3. **iOS Gesture Testing:** Extend iOS interaction support beyond UIControl (swipe, long-press, etc.)
4. **JVM SSR Testing:** Consider what testing is possible for SSR rendering
5. **Test Coverage Metrics:** Add code coverage reporting
6. **Performance Benchmarking:** Add performance testing utilities to test harness
