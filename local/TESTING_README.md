# KiteUI Testing Infrastructure - Complete Documentation

This directory contains comprehensive documentation of the KiteUI testing framework.

## Documentation Files

### 1. TESTING_QUICK_REFERENCE.md (START HERE)
- One-page quick reference for developers
- Core API examples
- Platform support matrix
- Common patterns and best practices
- Troubleshooting guide
- **Read this first** for a quick overview

### 2. TESTING_INFRASTRUCTURE.md (DETAILED GUIDE)
- Complete architectural breakdown
- Detailed platform-specific implementations
- Current limitations and opportunities
- Key design decisions
- Links to all source files with absolute paths
- Comprehensive reference for architecture questions

### 3. TESTING_DIRECTORY_STRUCTURE.md (FILE ORGANIZATION)
- Complete directory tree with file locations
- Legend explaining file types
- Test execution flow diagrams for each platform
- Module dependency graph
- Helper for finding specific test files

---

## Quick Start

1. **First time?** Read `TESTING_QUICK_REFERENCE.md`
2. **Need details?** Check `TESTING_INFRASTRUCTURE.md`
3. **Looking for files?** See `TESTING_DIRECTORY_STRUCTURE.md`

---

## Key Files to Know

**Core Framework** (platform-independent):
- `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt`
- `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/Interactions.kt`
- `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/ViewFinder.kt`

**Platform Implementations**:
- Android: `/Users/jivie/Projects/kiteui/library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/`
- iOS: `/Users/jivie/Projects/kiteui/library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/`
- JavaScript: `/Users/jivie/Projects/kiteui/library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/`
- JVM SSR: `/Users/jivie/Projects/kiteui/library/src/jvmSsrTest/kotlin/com/lightningkite/kiteui/testing/`

**Example Tests**:
- Component tests: `/Users/jivie/Projects/kiteui/library/src/*/kotlin/com/lightningkite/kiteui/components/ButtonTest.kt`
- Screenshot tests: `/Users/jivie/Projects/kiteui/library/src/*/kotlin/com/lightningkite/kiteui/testing/ScreenshotTest.kt`

---

## Quick Facts

**Testing Framework:**
- Uses Kotlin Multiplatform expect/actual pattern
- Unified cross-platform API
- Platform-specific implementations for Android, iOS, JS, and JVM SSR

**Platform Support:**
- Android: Fully supported (Robolectric + Roborazzi)
- iOS: Fully supported (UIKit + UIGraphics)
- JavaScript: Partially supported (rendering works, screenshots TODO)
- JVM SSR: Not supported (marked as unsupported, tests skipped)

**Screenshot Support:**
- Android: Yes (Roborazzi)
- iOS: Yes (UIGraphics)
- JavaScript: TODO (not yet implemented)
- JVM SSR: N/A

**View Discovery:**
- By debugName (semantic approach)
- By predicate (custom filtering)
- Cross-platform via ViewFinder utility

**Interactions:**
- Click/tap on any view
- Platform-specific: Android (performClick), iOS (sendActionsForControlEvents), JS (direct action)

---

## Common Tasks

### Write a new test
See: `TESTING_QUICK_REFERENCE.md` → Quick Start section

### Add screenshot test
See: `TESTING_INFRASTRUCTURE.md` → Existing Test Suite → Screenshot Tests

### Find where platform-specific code is
See: `TESTING_DIRECTORY_STRUCTURE.md` → Complete Testing File Organization

### Understand test execution flow
See: `TESTING_DIRECTORY_STRUCTURE.md` → Test Execution Flow

### Implement JS screenshots
See: `TESTING_QUICK_REFERENCE.md` → Next: Implementing JS Screenshots
And: `TESTING_INFRASTRUCTURE.md` → JavaScript Implementation (TODO section)

### Troubleshoot test issues
See: `TESTING_QUICK_REFERENCE.md` → Troubleshooting section

---

## Platform-Specific Information

### Android
- Framework: Robolectric
- Screenshot library: Roborazzi
- Test runner: RobolectricTestRunner
- Key note: Must initialize AndroidAppContext before view creation
- Output: `library/local/screenshots/android/`

### iOS
- Framework: XCTest (native iOS testing)
- UI access: Kotlin Native interop to UIKit
- Key note: Do NOT call makeKeyAndVisible() or setup() in viewDidLoad()
- Output: `library/local/screenshots/ios/` and `/tmp/screenshots/ios/`

### JavaScript
- Framework: Kotlin test (runs on Node.js)
- Initialization: `root(Theme) {}`
- Key note: Screenshots not yet implemented
- Action triggering: Direct action invocation (no DOM events needed)

### JVM SSR
- Status: Unsupported
- Reason: No full view hierarchy in test environment
- Tests: Automatically skipped (supported = false)

---

## Screenshot Workflow

1. Test calls `harness.screenshot("name")`
2. Platform-specific implementation captures view
3. Android: Roborazzi handles capture
4. iOS: UIGraphics context rendering
5. Output saved to `library/local/screenshots/{platform}/`

---

## View Discovery Workflow

1. Set `debugName` on your views in test code
2. Call `root.findByDebugName("my-view")`
3. ViewFinder performs depth-first search
4. Returns matching view or null

---

## Interaction Workflow

1. Find view: `val button = root.findByDebugName("submit")`
2. Interact: `button?.click()`
3. Platform-specific implementation:
   - Android: `view.native.performClick()`
   - iOS: `UIControl.sendActionsForControlEvents()`
   - JS: Direct action invocation
4. Test assertions verify results

---

## Example Test Structure

```kotlin
@Test
fun componentTest() = withTestHarness { harness ->
    // Setup
    val root = harness.render {
        myComponent()
    }
    
    // Discovery
    val element = root.findByDebugName("element-id")
    
    // Interaction
    element?.click()
    
    // Assertions
    assertEquals(expected, actual)
    
    // Screenshot (optional)
    val image = harness.screenshot("component-state")
    assertNotNull(image)
}
// Automatic cleanup via withTestHarness
```

---

## Test Execution

```bash
# Run all tests across all platforms
./gradlew allTests

# Run specific platform
./gradlew :library:androidUnitTest    # Android only
./gradlew :library:iosX64Test         # iOS only
./gradlew :library:jsTest             # JavaScript only
./gradlew :library:jvmSsrTest         # JVM SSR (skipped)
```

---

## Key Architectural Decisions

1. **Expect/Actual Pattern**: Each platform has its own implementation of the test API
2. **Unified Interface**: Same test code works across all platforms
3. **Automatic Cleanup**: withTestHarness ensures resources are cleaned up
4. **Semantic View Discovery**: Uses debugName instead of resource IDs
5. **Native View Access**: Direct access to platform-native views via view.native
6. **Screenshot Support**: Only Android and iOS fully implemented

---

## Open Issues & TODOs

1. **JavaScript Screenshots**: Currently returns null, needs canvas-based implementation
2. **Visual Regression Testing**: No built-in comparison tooling
3. **iOS Gesture Testing**: Limited to UIControl (buttons); no swipe, long-press, etc.
4. **JVM SSR Testing**: Currently unsupported; unclear if/how it should work

---

## Testing Philosophy

KiteUI's testing approach emphasizes:
- Cross-platform compatibility from the start
- Semantic view discovery (debugName) over brittle selectors
- Fast feedback loops (especially via Robolectric)
- Visual validation via screenshots
- Minimal boilerplate via expect/actual pattern

---

## Related Files

- Project CLAUDE.md: `/Users/jivie/Projects/kiteui/CLAUDE.md`
- Test dependencies: `/Users/jivie/Projects/kiteui/library/build.gradle.kts`
- Core TestHarness: `/Users/jivie/Projects/kiteui/library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt`

---

Generated: 2025-11-12
For current state of testing infrastructure in KiteUI project
