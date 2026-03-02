# Running KiteUI Tests

Practical guide for running tests across all platforms. For the testing *API* (how to write tests), see [TESTING_GUIDE.md](TESTING_GUIDE.md) and [library/TESTING.md](../library/TESTING.md).

## Quick Reference

| Platform | Command | Prerequisites |
|----------|---------|---------------|
| **JVM SSR** | `./gradlew :example-app:jvmSsrTest` | None (fastest) |
| **Android** | `./gradlew :example-app:testDebugUnitTest` | None (uses Robolectric) |
| **iOS** | `./gradlew :example-app:iosSimulatorArm64Test` | iOS Simulator running |
| **JS/Web** | `./gradlew :example-app:jsBrowserTest` | Chrome installed |
| **All** | `./gradlew :example-app:allTests` | All of the above |

Same commands work for `:library:` — substitute the module name.

## Platform Details

### JVM SSR (Fastest — Start Here)

No external tools needed. Uses `Dispatchers.Unconfined` so all coroutines run synchronously. Best for rapid iteration.

```bash
# All JVM SSR tests
./gradlew :example-app:jvmSsrTest

# Specific test class
./gradlew :example-app:jvmSsrTest --tests "com.lightningkite.mppexampleapp.MockExternalServicesUiTest"

# Specific test method
./gradlew :example-app:jvmSsrTest --tests "*.MockExternalServicesUiTest.openLinkRecorded"
```

### Android (Robolectric)

Uses Robolectric — no emulator/device needed. Robolectric JARs download on first run (~1 min).

```bash
# All Android unit tests
./gradlew :example-app:testDebugUnitTest

# Specific test class
./gradlew :example-app:testDebugUnitTest --tests "com.lightningkite.mppexampleapp.MockExternalServicesUiTest"
```

**Cross-platform tests in `commonTest`** that need Robolectric must be annotated:

```kotlin
@JUnitRunWith(RobolectricTestRunner::class)
class MyUiTest { ... }
```

Both `JUnitRunWith` and `RobolectricTestRunner` are provided by the `test-utilities` module — they're no-ops on non-Android platforms.

### iOS

Requires a running iOS Simulator. Open one in Xcode or:

```bash
# List available simulators
xcrun simctl list devices available | head -20

# Boot one if needed
xcrun simctl boot "iPhone 16"

# Run tests (Apple Silicon Mac)
./gradlew :example-app:iosSimulatorArm64Test

# Run tests (Intel Mac)
./gradlew :example-app:iosX64Test
```

**Note:** `--tests` filtering is not supported for iOS native tests.

### JS/Web (Karma + ChromeHeadless)

Requires Chrome installed at a standard location. Karma finds it automatically on macOS (`/Applications/Google Chrome.app`). On Linux, set `CHROME_BIN`:

```bash
# macOS — just run it
./gradlew :example-app:jsBrowserTest

# Linux — set CHROME_BIN if Chrome isn't on PATH
CHROME_BIN=/usr/bin/google-chrome-stable ./gradlew :example-app:jsBrowserTest
```

**Note:** `--tests` filtering is not supported for Karma-based JS tests. All tests in the test source set run together.

## Test Source Sets

Tests live in these source sets (under `src/`):

| Source Set | Compiles For | Use When |
|---|---|---|
| `commonTest` | All platforms | Tests that should run everywhere |
| `commonInteractiveTest` | Android, iOS, JS (not JVM SSR) | Tests needing real view rendering |
| `jvmSsrTest` | JVM SSR only | SSR-specific tests |
| `androidUnitTest` | Android only | Android-specific tests |
| `iosTest` | iOS only | iOS-specific tests |
| `jsTest` | JS/Web only | Browser-specific tests |

**Prefer `commonTest`** for new tests. Use platform-specific source sets only when testing platform-specific behavior.

## Writing a Test (Minimal Example)

```kotlin
// src/commonTest/kotlin/com/example/MyTest.kt
package com.example

import com.lightningkite.kiteui.testing.*
import kotlin.test.Test

@JUnitRunWith(RobolectricTestRunner::class)  // Required for Android
class MyTest {
    @Test
    fun buttonClick() = uiTest(
        content = { with(MyPage) { render() } }
    ) {
        click("myButton")           // by debugName
        assertValue("output", "42") // check a text view's content
    }
}
```

Key APIs on `UiTestScope`: `click(id)`, `setValue(id, value)`, `assertValue(id, expected)`, `snapshot()`, `waitFor { condition }`, `dumpSnapshot()`.

For mocking external services (file pickers, geolocation, etc.):

```kotlin
@Test
fun filePick() {
    val mock = MockExternalServices()
    mock.pendingFileResponses.add(createFileReferenceFromBytes(bytes, "image/png", "photo.png"))
    uiTest(
        config = UiTestConfig(externalServices = mock),
        content = { with(MyPage) { render() } }
    ) {
        click("uploadButton")
        // Check mock.calls for recorded interactions
    }
}
```

## Gotchas

### `MutableList.removeFirst()` — avoid on shared code
Kotlin's `removeFirst()` resolves to JDK 21's `java.util.List.removeFirst()` which doesn't exist in Android's runtime. Use `removeAt(0)` instead in any code that runs on Android.

### Android tests need `@JUnitRunWith`
Tests in `commonTest` won't use Robolectric unless annotated with `@JUnitRunWith(RobolectricTestRunner::class)`. Without it, Android context isn't initialized and `TestHarness.supported` will be `false`.

### `UiTestConfig.theme` is nullable
Don't construct `Theme(...)` at the call site of `uiTest()` — on Android, `Theme.<clinit>` accesses Android resources before Robolectric is ready. Leave `theme` as `null` (the default) and each platform actual resolves a safe default.

### onClick with `frequencyCap`
Button `onClick` defaults to `frequencyCap = 500.milliseconds`. In tests, rapid sequential clicks on the same button may be suppressed. Use `onClick(frequencyCap = null) { ... }` in test pages if you need to click repeatedly.

### JS and iOS don't support `--tests` filtering
Karma (JS) and XCTest (iOS) run all tests in the source set. To run a subset, either use JVM SSR / Android for filtered runs, or temporarily `@Ignore` other tests.
