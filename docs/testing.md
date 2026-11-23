# Testing

`test-utilities` runs UI tests written once in `commonTest` on every platform: JVM (`jvmSsr`), Android (Robolectric), iOS (simulator), and the browser.  `uiTest` builds a view tree in-process and drives it with the same text commands as the [AI driver](ai-driver.md): you name views with `debugName`, then click, type, and read them by name.

```kotlin
// build.gradle.kts
kotlin {
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation("com.lightningkite.kiteui:test-utilities:<version>")
    }
}
```

## Minimal example

```kotlin
@JUnitRunWith(RobolectricTestRunner::class)  // Android needs Robolectric; a no-op elsewhere
@RobolectricConfig
class CounterTest {
    @Test
    fun clickingIncrements() = uiTest(
        content = {
            val count = Signal(0)
            col {
                text { debugName = "label"; ::content { "Count: ${count()}" } }
                button {
                    debugName = "add"
                    text("Add")
                    onClick { count.value++ }
                }
            }
        }
    ) {
        assertTextVisible("Count: 0")
        click("add")
        assertTextVisible("Count: 1", target = "label")
    }
}
```

`content` builds the UI; the second lambda is the test, run in a `UiTestScope`.

## Common tasks

### Address a view

```kotlin
click("add")                  // the view whose debugName is "add", searched from the root
setValue("form/email", "a@b.c")  // "email" inside "form"
snapshot("form/0")            // the first child of "form"
```

Paths are `/`-separated names or child indexes, and `..` means the parent.  Name what a test touches; indexes shift when the layout changes.  The path syntax is described fully in the [AI driver](ai-driver.md#paths) reference.

### Test a page

```kotlin
@Test
fun logsIn() = uiTest(content = { with(LoginPage) { render() } }) {
    setValue("email", "a@example.com")
    assertValue("email", "a@example.com")
    val logIn = findClickable("Log in").firstOrNull() ?: error("No 'Log in' button")
    click(logIn.path)
    waitForText("Welcome, a@example.com")
}
```

`findClickable` finds views by text, name, type or action and walks each match up to the nearest clickable ancestor, so a button can be found by its label.  `findAll` returns every match as a `FindResult` (`path`, `name`, `type`, `value`, `actions`).

### Wait for asynchronous work

```kotlin
click("save")                 // returns once the action has started, not finished
waitForText("Saved")          // polls the snapshot; fails after 5 seconds
waitForId("confirmDialog")
waitFor(description = "three rows") { findAll("Row").size == 3 }
```

Synchronous updates (a signal set in `onClick`) are visible as soon as the command returns.  Anything that suspends (a `delay`, a fetch, an `Action` that awaits) needs a `waitFor…`.

### Navigate between pages

```kotlin
@Test
fun navigates() {
    lateinit var navigator: PageNavigator
    uiTest(content = {
        navigator = PageNavigator { AutoRoutes }
        navigator.navigate(LoginPage)
        navigatorView(navigator)
    }) {
        assertIdExists("email")
    }
}
```

`uiTest` has no navigator of its own, so the scope's `navigate`, `reset`, `url` and `back` fail with "no navigator available".  Show the pages with `navigatorView` and drive the `PageNavigator` you created.

### Mock the file picker and location

```kotlin
mockFile("hello".encodeToByteArray(), "text/plain", "hello.txt")  // next requestFile() gets this
mockFileCancel()                                                   // ...or the user cancels
mockGeolocation(45.0, -111.0)
click("upload")
assertTrue(mockCalls().contains("RequestFile"))
```

Or install a prepared `MockExternalServices`:

```kotlin
val mock = MockExternalServices()
mock.pendingFileResponses.addLast(createFileReferenceFromBytes(bytes, "text/plain", "file.txt"))
uiTest(mockExternalServices = mock, content = { /* ... */ }) {
    // ...
    assertTrue(mock.calls.any { it is MockExternalServices.Call.RequestFile })
}
```

### Drive a running app

```kotlin
@Test
fun remoteLogin() = remoteUiTest("myapp-web", startRoute = "/login") {
    setValue("email", "a@example.com")
    click("logIn")
    waitForText("Dashboard")
}
```

`remoteUiTest` (JVM only) sends the same commands through the AI driver relay to an app connected with `AiDriver.connect`, so the same test body can run in-process or against a real device.  `startRoute` resets the navigator first.  See [AI driver](ai-driver.md).

### Screenshots

```kotlin
saveScreenshot("build/screenshots/login.png")  // JVM tests driving a remote app
val png: String = screenshot("form")           // base64 PNG of one view
```

The JVM in-process target has no renderer, so screenshots there throw; take them in Android, iOS or browser tests, or remotely.  For direct access to the elements and Roborazzi screenshots on Android, use `withTestHarness { harness -> harness.render { ... } }` (Android, iOS and JS only).

### Run tests

```bash
./gradlew :app:jvmSsrTest                  # fastest; no device or browser
./gradlew :app:testAndroidHostTest         # Robolectric
./gradlew :app:iosSimulatorArm64Test       # needs a simulator
./gradlew :app:jsBrowserTest               # needs Chrome
./gradlew :app:allTests
./gradlew :app:jvmSsrTest --tests "*.CounterTest"
```

## Gotchas

- Clicking the same button twice within 500 ms counts once, and a click while its action is still running is ignored: `onClick` and `Action` default to `frequencyCap = 500.milliseconds` and `ignoreRetryWhileRunning = true`.  Wait for the action's result before clicking again.
- A command on a missing view or an unknown action throws `DriverActionException` naming what was available.  The `assert…` helpers throw `AssertionError` with the snapshot.
- `assertValue` compares the view's driver value: text for inputs, `true`/`false` for toggles, the number for sliders.  Password fields (`KeyboardHints.password` / `newPassword`) read as `••••••••` when filled.
- `assertEnabled(query)` / `assertDisabled(query)` check whether a matching view has a clickable ancestor.  A button with no `onClick` or `action` has none.
- `find` and `findClickable` skip hidden views unless you pass `includeHidden = true`.
- On Android, every test class that renders needs `@JUnitRunWith(RobolectricTestRunner::class)` and `@RobolectricConfig`.  Add `@GraphicsMode(GraphicsModeEnum.NATIVE)` for screenshots.

## Testing KiteUI itself

Network tests for `fetch` and `webSocket` live in `library/src/commonTest` so every platform's implementation is held to the same contract.  Write them with `networkTest { }`, not `runTest`, whose virtual clock expires real-socket timeouts instantly:

```kotlin
@Test
fun readsBody() = networkTest {
    val response = fetch("${TestServer.http}/hello")
    assertEquals(TestServer.helloBody, response.text())
}
```

The tests talk to a local echo server (`:test-server`) that `library/build.gradle.kts` starts once per build and every test task shares.  `TestServer` lists its addresses: `http`, `ws`, `refused` (nothing listening), and `httpWithoutCors` (for `jsTest`).  To add an endpoint, add it to `test-server/.../EchoServer.kt` and mirror any constant the tests assert on in `TestServer`.  If port 18787 is taken, pass `-Pkiteui.testServerPort=<port>`.

## See also

- [AI driver](ai-driver.md): the command protocol behind `UiTestScope`
- Example tests: `library/src/jvmSsrTest/` (one file per element), `example-app/src/commonInteractiveTest/`
