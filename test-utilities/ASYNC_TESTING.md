# Async Testing Support in KiteUI

This document describes the async testing utilities added to KiteUI's test-utilities module to handle asynchronous operations in tests.

## Overview

The async testing support provides platform-specific mechanisms for:
- **Waiting for async operations to complete** (delays, network calls, UI updates)
- **Advancing virtual time** (on platforms that support it, like Robolectric)
- **Waiting for conditions** with timeout support

## API

### AsyncTestSupport

The `AsyncTestSupport` class provides three main methods:

```kotlin
suspend fun waitUntilIdle()
```
Waits for all pending async operations to complete.
- **Android/Robolectric**: Idles the Looper scheduler (virtual time, instant)
- **iOS**: Processes the run loop 10 times
- **JS**: Flushes microtasks and awaits animation frame
- **JVM Desktop**: Flushes the Swing EDT queue

```kotlin
suspend fun waitFor(timeout: Duration = 5.seconds, condition: () -> Boolean)
```
Waits until a condition becomes true or timeout is reached.
- Polls the condition at regular intervals (50-100ms depending on platform)
- Throws `AssertionError` if timeout is exceeded

```kotlin
suspend fun advanceTimeBy(duration: Duration)
```
Advances time by the specified duration.
- **Android/Robolectric**: Advances virtual time (instant execution of delayed tasks)
- **iOS/JS/JVM**: Uses actual delays (tests will wait in real time)

### TestHarness Integration

The `TestHarness` class provides direct access to async utilities:

```kotlin
val harness = TestHarness()

// Via async property
harness.async.waitUntilIdle()
harness.async.waitFor(timeout = 3.seconds) { someCondition() }
harness.async.advanceTimeBy(500.milliseconds)

// Via convenience methods
harness.waitUntilIdle()
harness.waitFor(timeout = 3.seconds) { someCondition() }
harness.advanceTimeBy(500.milliseconds)
```

## Usage Examples

### Example 1: Wait for Posted Runnable (Android)

```kotlin
@Test
fun testWaitUntilIdleWithPostedRunnable() = runBlocking {
    withTestHarness { harness ->
        val executed = Property(false)

        harness.render {
            col {
                text { ::content { if (executed()) "Done" else "Waiting" } }
            }
        }

        // Post a runnable to the main thread
        Handler(Looper.getMainLooper()).post {
            executed.value = true
        }

        // Wait for all pending operations
        harness.waitUntilIdle()

        assertTrue(executed.value)
    }
}
```

### Example 2: Advance Virtual Time (Android)

```kotlin
@Test
fun testAdvanceTimeByWithDelayedOperation() = runBlocking {
    withTestHarness { harness ->
        val executed = Property(false)

        harness.render {
            col {
                text { ::content { if (executed()) "Done" else "Waiting" } }
            }
        }

        // Post a delayed runnable (500ms)
        Handler(Looper.getMainLooper()).postDelayed({
            executed.value = true
        }, 500)

        // Advance time by 500ms - executes the delayed runnable immediately
        harness.advanceTimeBy(500.milliseconds)

        assertTrue(executed.value)
    }
}
```

### Example 3: Wait for Condition

```kotlin
@Test
fun testWaitForConditionBecomesTrue() = runBlocking {
    withTestHarness { harness ->
        val counter = Property(0)

        harness.render {
            col {
                text { ::content { "Counter: ${counter()}" } }
            }
        }

        // Schedule increments with delays
        Handler(Looper.getMainLooper()).postDelayed({ counter.value = 1 }, 100)
        Handler(Looper.getMainLooper()).postDelayed({ counter.value = 2 }, 200)
        Handler(Looper.getMainLooper()).postDelayed({ counter.value = 3 }, 300)

        // Wait for counter to reach 3
        harness.waitFor(timeout = 1.seconds) {
            counter.value >= 3
        }

        assertEquals(3, counter.value)
    }
}
```

### Example 4: Async Data Loading with Screenshots

```kotlin
@Test
fun testAsyncDataLoadingSimulation() = runBlocking {
    withTestHarness { harness ->
        val isLoading = Property(true)
        val data = Property<String?>(null)

        harness.render {
            col {
                if (isLoading()) {
                    text("Loading...")
                } else {
                    text { ::content { "Data: ${data()}" } }
                }
            }
        }

        // Take screenshot of loading state
        harness.screenshot("async_loading_state")

        // Simulate async data loading
        Handler(Looper.getMainLooper()).postDelayed({
            data.value = "Loaded Data"
            isLoading.value = false
        }, 500)

        // Advance time and wait for loading to complete
        harness.advanceTimeBy(500.milliseconds)
        harness.waitFor { !isLoading.value }

        // Take screenshot of loaded state
        harness.screenshot("async_loaded_state")

        assertEquals("Loaded Data", data.value)
    }
}
```

## Platform-Specific Details

### Android/Robolectric

**Implementation**: Uses Robolectric's scheduler control via `shadowOf(Looper.getMainLooper())`

**Advantages**:
- **Virtual time**: `advanceTimeBy()` is instant - no actual waiting
- **Fast tests**: Can simulate hours of delays in milliseconds
- **Precise control**: Advance time incrementally as needed

**Handles**:
- `Handler.post()` and `Handler.postDelayed()`
- `view.post()` and `view.postDelayed()`
- Coroutines on `Dispatchers.Main` (they use Looper under the hood)
- View invalidation and layout passes

**Location**: `test-utilities/src/androidMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.android.kt`

### iOS

**Implementation**: Uses `NSRunLoop.currentRunLoop().runMode()` to process the run loop

**Advantages**:
- **Real async**: Tests behave like production
- **RunLoop integration**: Properly processes UIKit events

**Limitations**:
- **No virtual time**: `advanceTimeBy()` uses actual `delay()` - tests are slower
- **Real delays**: A 1-second delay actually waits 1 second

**Handles**:
- `DispatchQueue.main.async {}`
- UIKit view updates
- Kotlin coroutines

**Location**: `test-utilities/src/iosMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.ios.kt`

### JS/Web

**Implementation**: Uses `yield()`, `delay()`, and `requestAnimationFrame()`

**Advantages**:
- **Event loop integration**: Flushes microtasks and macrotasks
- **Rendering sync**: `requestAnimationFrame()` ensures DOM updates complete

**Limitations**:
- **No virtual time**: Uses actual delays
- **All methods are suspend**: Must be called from coroutine context

**Handles**:
- Promises and async/await
- DOM updates and rendering
- Timers (`setTimeout`, `setInterval`)

**Location**: `test-utilities/src/jsMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.js.kt`

### JVM Desktop

**Implementation**: Uses Swing's `SwingUtilities.invokeAndWait()` to flush the EDT

**Advantages**:
- **EDT integration**: Properly handles Swing thread safety

**Limitations**:
- **No virtual time**: Uses actual delays
- **Deadlock detection**: Checks if already on EDT before calling `invokeAndWait()`
- **Currently disabled**: Due to Kotlin Multiplatform limitation (can't have multiple JVM targets)

**Handles**:
- Swing EDT operations (`SwingUtilities.invokeLater`, etc.)
- JavaFX operations (could be added in the future)

**Location**: `test-utilities/src/jvmDesktopMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.jvm.kt`

**Note**: The JVM desktop target is currently disabled due to a Kotlin Multiplatform limitation (cannot declare multiple JVM targets like `jvmDesktop` and `jvmSsr` in the same module). The implementation exists but is not compiled.

## Platform Comparison Table

| Feature | Android/Robolectric | iOS | JS | JVM Desktop |
|---------|-------------------|-----|----|----|
| **Virtual Time** | ✅ Yes (instant) | ❌ No (real delays) | ❌ No (real delays) | ❌ No (real delays) |
| **waitUntilIdle** | ✅ Looper.idle() | ✅ RunLoop drain | ✅ yield() + RAF | ✅ EDT flush |
| **waitFor** | ✅ Poll + idle | ✅ Poll + run loop | ✅ Poll + RAF | ✅ Poll + EDT |
| **advanceTimeBy** | ✅ Virtual (instant) | ⚠️ Real delay | ⚠️ Real delay | ⚠️ Real delay |
| **Test Speed** | ⭐⭐⭐ Fast | ⭐⭐ Moderate | ⭐⭐ Moderate | ⭐⭐ Moderate |
| **Status** | ✅ Active | ✅ Active | ✅ Active | ⚠️ Disabled* |

*JVM Desktop is implemented but disabled due to Kotlin Multiplatform build limitations.

## Best Practices

### 1. Use `waitUntilIdle()` After Triggering Async Operations

```kotlin
// Post async work
Handler(Looper.getMainLooper()).post { doSomething() }

// Wait for it to complete
harness.waitUntilIdle()

// Now safe to assert
assertEquals(expectedValue, actualValue)
```

### 2. Use `waitFor()` for Complex Conditions

```kotlin
// Instead of multiple waitUntilIdle() calls, use waitFor()
harness.waitFor(timeout = 5.seconds) {
    allDataLoaded() && uiUpdated() && !isLoading()
}
```

### 3. Prefer Virtual Time (Android) for Fast Tests

```kotlin
// Android: Instant execution (virtual time)
harness.advanceTimeBy(10.seconds) // Takes ~0ms

// iOS/JS: Actual waiting (real time)
harness.advanceTimeBy(10.seconds) // Takes 10 seconds!
```

### 4. Always Use `withTestHarness` for Auto-Cleanup

```kotlin
@Test
fun myTest() = runBlocking {
    withTestHarness { harness ->
        // Your test code
        // cleanup() is called automatically
    }
}
```

### 5. Remember All Methods Are Suspend Functions

```kotlin
// ✅ Correct: In runBlocking or suspend context
@Test
fun myTest() = runBlocking {
    harness.waitUntilIdle()
}

// ❌ Wrong: Not in suspend context
@Test
fun myTest() {
    harness.waitUntilIdle() // Compilation error!
}
```

## Implementation Details

### expect/actual Pattern

The async testing support uses Kotlin Multiplatform's expect/actual mechanism:

- **expect declaration**: `test-utilities/src/commonInteractiveMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.kt`
- **Android actual**: `test-utilities/src/androidMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.android.kt`
- **iOS actual**: `test-utilities/src/iosMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.ios.kt`
- **JS actual**: `test-utilities/src/jsMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.js.kt`
- **JVM actual**: `test-utilities/src/jvmDesktopMain/kotlin/com/lightningkite/kiteui/testing/AsyncTestSupport.jvm.kt` (disabled)

### Source Set Hierarchy

```
commonMain
  └── commonInteractiveMain (for platforms with UI)
       ├── androidMain
       ├── iosMain
       └── jsMain

commonHtmlMain (for web-related code)
  ├── jsMain
  └── jvmSsrMain (server-side rendering, no interactive UI)
```

## Troubleshooting

### "Condition not met within timeout" Error

**Problem**: `waitFor()` times out before condition becomes true.

**Solutions**:
1. Increase timeout: `waitFor(timeout = 10.seconds) { ... }`
2. Check if async operation is actually running
3. Add logging to see condition state: `waitFor { println("state: $x"); x == expected }`

### Tests Hang Indefinitely

**Problem**: `waitUntilIdle()` or `waitFor()` never completes.

**Solutions**:
1. Check for infinite loops in async code
2. Ensure the condition in `waitFor()` can actually become true
3. On Android, check if you have background loopers that never idle

### Virtual Time Not Working (Android)

**Problem**: Time doesn't advance despite calling `advanceTimeBy()`.

**Solutions**:
1. Ensure you're using Robolectric (not instrumentation tests)
2. Check that delayed operations use `Handler` (not `Thread.sleep()`)
3. Call `waitUntilIdle()` after `advanceTimeBy()` to process scheduled tasks

## Future Enhancements

Potential improvements for the async testing support:

1. **JVM Desktop Support**: Resolve the Kotlin Multiplatform limitation to enable JVM desktop testing
2. **Coroutine Test Dispatcher**: Integrate `kotlinx-coroutines-test` for better coroutine time control
3. **Animation Support**: Add utilities for waiting for animations to complete
4. **Network Mocking**: Integration with fake/mock data sources
5. **Detailed Logging**: Option to log all async operations for debugging
6. **Timeout Diagnostics**: Better error messages showing what was pending when timeout occurred

## Migration Guide

If you have existing tests that need async support:

### Before:
```kotlin
@Test
fun myTest() {
    val harness = TestHarness()
    harness.render { text("Hello") }

    // Problem: Test finishes before async operations complete
    Thread.sleep(1000) // Unreliable!

    // Assertion may fail randomly
    assertEquals(expected, actual)
}
```

### After:
```kotlin
@Test
fun myTest() = runBlocking {
    withTestHarness { harness ->
        harness.render { text("Hello") }

        // Wait for async operations properly
        harness.waitUntilIdle()

        // Or wait for specific condition
        harness.waitFor { condition() }

        // Reliable assertion
        assertEquals(expected, actual)
    }
}
```

## Related Documentation

- [test-utilities/README.md](./README.md) - Overview of test utilities
- [library/TESTING.md](../library/TESTING.md) - General testing guide for KiteUI
- [Robolectric Documentation](http://robolectric.org/) - Android testing framework
- [kotlinx-coroutines-test](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/) - Kotlin coroutine testing utilities
