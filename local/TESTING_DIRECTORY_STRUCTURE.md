# KiteUI Testing Directory Structure

## Complete Testing File Organization

```
library/src/
├── commonTest/kotlin/com/lightningkite/kiteui/testing/
│   ├── TestHarness.kt              [EXPECT] Common interface for test harness
│   ├── Interactions.kt             [EXPECT] Common interface for user interactions
│   └── ViewFinder.kt               [COMMON] Cross-platform view discovery utility
│
├── androidUnitTest/kotlin/com/lightningkite/kiteui/
│   ├── testing/
│   │   ├── TestHarness.android.kt          [ACTUAL] Android implementation using Robolectric
│   │   ├── Interactions.android.kt         [ACTUAL] Android click via performClick()
│   │   ├── ScreenshotTest.kt               [TEST] Screenshot functionality tests
│   │   ├── TestHarnessTest.android.kt      [TEST] Android-specific harness tests
│   │   └── TestHarnessTest.kt              [TEST] Common harness test logic
│   │
│   └── components/
│       ├── ButtonTest.kt           [TEST] Button interaction tests
│       ├── RowTest.kt              [TEST] Row layout tests
│       └── TextViewTest.kt         [TEST] Text view tests
│
├── iosTest/kotlin/com/lightningkite/kiteui/
│   ├── testing/
│   │   ├── TestHarness.ios.kt              [ACTUAL] iOS implementation using UIKit
│   │   ├── Interactions.ios.kt             [ACTUAL] iOS click via sendActionsForControlEvents
│   │   ├── ScreenshotTest.kt               [TEST] Screenshot functionality tests
│   │   ├── TestHarnessTest.ios.kt          [TEST] iOS-specific harness tests
│   │
│   └── components/
│       ├── ButtonTest.kt           [TEST] Button interaction tests
│       ├── RowTest.kt              [TEST] Row layout tests
│       └── TextViewTest.kt         [TEST] Text view tests
│
├── jsTest/kotlin/com/lightningkite/kiteui/
│   ├── testing/
│   │   ├── TestHarness.js.kt               [ACTUAL] JavaScript implementation (render only)
│   │   ├── Interactions.js.kt              [ACTUAL] JS click via action invocation
│   │   ├── TestHarnessTest.js.kt           [TEST] JS-specific harness tests
│   │
│   ├── components/
│   │   ├── ButtonTest.kt           [TEST] Button interaction tests
│   │   ├── RowTest.kt              [TEST] Row layout tests
│   │   └── TextViewTest.kt         [TEST] Text view tests
│   │
│   ├── LayoutTest.kt               [TEST] Layout calculations and hierarchy
│   └── Recycler2Test.kt            [TEST] Recycler/list view tests
│
├── jvmSsrTest/kotlin/com/lightningkite/kiteui/
│   ├── testing/
│   │   ├── TestHarness.jvmSsr.kt           [ACTUAL] JVM SSR (unsupported)
│   │   └── Interactions.jvmSsr.kt          [ACTUAL] JVM SSR interactions (unsupported)
│   │
│   └── [other JVM SSR tests...]
│
├── androidUnitTest/kotlin/
│   └── LayoutTest.kt               [TEST] Android layout tests
│
├── commonTest/kotlin/
│   ├── LayoutsTestPage.kt          [COMMON] Common layout test page utilities
│   ├── RViewTest.kt                [TEST] RView core functionality tests
│   ├── AutoInsertCommaTest.kt      [TEST] Utility tests
│   ├── BlobTest.kt                 [TEST] Blob handling tests
│   ├── ConnectivityGateTest.kt     [TEST] Connectivity tests
│   ├── GeneralFormatTest.kt        [TEST] Formatting tests
│   ├── HashingTest.kt              [TEST] Hashing tests
│   ├── MicroHtmlTests.kt           [TEST] HTML parsing tests
│   └── UrlEncodingTest.kt          [TEST] URL encoding tests
│
└── example-app/
    ├── src/commonMain/kotlin/com/lightningkite/mppexampleapp/internal/
    │   ├── [TestPage.kt files]      [UI TEST PAGES] Interactive test pages for manual testing
    │   ├── AnimationTestPage.kt
    │   ├── AnimationTest2Page.kt
    │   ├── ButtonsTestPage.kt
    │   ├── CoveringTestPage.kt
    │   ├── FormattedInputTests.kt
    │   ├── ImageTestPage.kt
    │   ├── LoadAnimationTestPage.kt
    │   ├── NavigationTestPage.kt
    │   ├── PerformanceTestPage.kt
    │   ├── PopoverTestingPage.kt
    │   ├── ProgrammaticLayoutTestPage.kt
    │   ├── Recycler2TestPage.kt
    │   ├── RecyclerFilterTestPage.kt
    │   ├── RecyclerViewTestPage.kt
    │   ├── ScrollElementTestPage.kt
    │   ├── ScrollIntoViewTest.kt
    │   ├── SpecialScrollTest.kt
    │   ├── TestPage.kt
    │   ├── TestingGroundPage.kt
    │   ├── ViewPagerCenterIndexTestPage.kt
    │   └── VectorsTestPage.kt
    │
    └── src/iosMain/kotlin/com/lightningkite/mppexampleapp/
        └── IosTest.kt              [iOS test page entry point]
```

## Legend
- `[EXPECT]` - Expect declaration (common interface)
- `[ACTUAL]` - Actual implementation (platform-specific)
- `[COMMON]` - Common/shared code (not platform-specific)
- `[TEST]` - Test file
- `[UI TEST PAGES]` - Interactive test pages in example app (manual testing)

## Key Observations

1. **Symmetry Across Platforms:** Android, iOS, and JS all have identical test structure:
   - TestHarness implementation
   - Interactions implementation
   - Component tests (ButtonTest, RowTest, TextViewTest)
   - Harness tests

2. **Android Unique:**
   - ScreenshotTest (Roborazzi-powered)
   - Uses Robolectric test runner
   - Uses GraphicsMode annotation

3. **iOS Unique:**
   - ScreenshotTest (UIGraphics-powered)
   - Native Kotlin/iOS interop

4. **JavaScript Unique:**
   - LayoutTest and Recycler2Test
   - No screenshot support yet (TODO)

5. **JVM SSR Unique:**
   - Marked as unsupported
   - TestHarness.supported = false

6. **Example App:**
   - Contains ~20 manual test pages for interactive testing
   - These are UI demos, not automated tests
   - Accessible through the app's test menu system

## Testing Output Directories

```
library/
├── local/
│   ├── screenshots/
│   │   ├── android/     <- Screenshot test outputs
│   │   └── ios/         <- Screenshot test outputs
│   │
│   ├── TESTING_INFRASTRUCTURE.md      <- This documentation
│   └── TESTING_DIRECTORY_STRUCTURE.md <- File structure
│
└── build/
    └── outputs/
        └── roborazzi/   <- Intermediate Roborazzi outputs
```

## Test Execution Flow

### Android
```
Test Code (e.g., ButtonTest.kt)
    ↓
withTestHarness { harness ->
    ↓
TestHarness.android (Robolectric)
    ↓
TestActivity (Robolectric instrumented)
    ↓
Native Android Views
    ↓
Assertions / Screenshots (Roborazzi)
```

### iOS
```
Test Code (e.g., ButtonTest.kt)
    ↓
withTestHarness { harness ->
    ↓
TestHarness.ios (Kotlin Native)
    ↓
UIWindow + UIViewController
    ↓
Native UIView hierarchy
    ↓
Assertions / Screenshots (UIGraphics)
```

### JavaScript
```
Test Code (e.g., ButtonTest.kt)
    ↓
withTestHarness { harness ->
    ↓
TestHarness.js (Compiled to JS)
    ↓
root(Theme) {} initialization
    ↓
DOM Elements
    ↓
Assertions (Screenshots TODO)
```

### JVM SSR
```
Test Code
    ↓
withTestHarness { harness ->
    ↓
TestHarness.jvmSsr
    ↓
supported = false
    ↓
Test SKIPPED
```

## Module Dependencies

```
Testing Framework:
commonTest/ (expect)
├── depends on: core library code
├── used by: androidUnitTest/, iosTest/, jsTest/, jvmSsrTest/
└── provides: TestHarness interface, ViewFinder, Interactions

Android:
androidUnitTest/ (actual)
├── depends on: Robolectric, Roborazzi, commonTest
└── implements: Android TestHarness, Interactions

iOS:
iosTest/ (actual)
├── depends on: UIKit (via cinterop), commonTest
└── implements: iOS TestHarness, Interactions

JavaScript:
jsTest/ (actual)
├── depends on: commonTest
└── implements: JS TestHarness, Interactions

JVM SSR:
jvmSsrTest/ (actual)
├── depends on: commonTest
└── implements: Stub (unsupported) TestHarness, Interactions
```
