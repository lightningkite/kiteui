# KiteUI AI Driver — Testing & Debugging with `kiteui-drive`

Use this skill when you need to visually test, interact with, debug, or write automated tests for a running KiteUI app. The AI driver lets you take UI snapshots, click buttons, fill forms, navigate pages, read logs, take screenshots, and verify state — all from CLI commands or from automated Kotlin tests.

## Prerequisites

```bash
# Install (once per KiteUI version)
./gradlew aiDriverInstall

# Ensure ~/.kiteui/bin is on your PATH
export PATH="$HOME/.kiteui/bin:$PATH"

# Start the daemon
kiteui-drive start
```

If `kiteui-drive` is not found, the project may need to publish KiteUI to maven local first:
```bash
./gradlew publishToMavenLocal
./gradlew aiDriverInstall
```

On JVM (including tests), the daemon auto-starts when `AiDriver.connect()` is called.

## Architecture

```
┌─────────────┐   WebSocket    ┌────────────┐    HTTP     ┌──────────────────┐
│  KiteUI App │ ◄──────────► │   Daemon   │ ◄──────────► │ CLI (kiteui-drive) │
│ (browser,   │  ws://:7474   │ (JVM proc) │  http://:7475│  (you run these)  │
│  Android,   │               │            │              └──────────────────┘
│  iOS)       │               └────────────┘
└─────────────┘
```

- **App** connects outbound to daemon on `ws://localhost:7474/app` (auto-reconnects)
- **Daemon** bridges between apps and CLI commands
- **CLI** (`kiteui-drive`) sends commands to daemon via HTTP POST to `:7475/cli`

---

# Part 1: Interactive Testing & Debugging with CLI

Use these commands via the Bash tool to explore, test, and debug a running app.

## Getting Started

```bash
kiteui-drive list                    # See connected apps and their IDs
kiteui-drive snapshot <appId>        # See the full UI tree
```

App IDs are set by the app via `AiDriver.connect(appId = "myapp")`. First instance gets the bare name; duplicates get `-2`, `-3` suffixes.

## Reading UI State

### Snapshots
```bash
kiteui-drive snapshot <app>                           # Full tree
kiteui-drive snapshot <app> --component navigatorView # Scope to subtree
kiteui-drive snapshot <app> --search "Welcome"        # Filter by value text
kiteui-drive snapshot <app> --format Json             # Machine-readable JSON
```

Snapshot output looks like:
```
Page: HomePage ()
[navigatorView] SwapView (value="navigatorView")
  [navigatorView/0] Frame
    [navigatorView/0/0] Column
      [navigatorView/0/0/0] Text (value="Welcome")
      [counter] Text (value="0")
      [increment] Button (value="increment", actions=[click,longClick])
```

**Component IDs** use shortened paths:
- **Named views** (with `debugName` or `ariaDescription`) reset the path: just `counter`
- **Unnamed views** accumulate from their nearest named ancestor: `navigatorView/0/0/3`

### Screenshots
```bash
kiteui-drive screenshot <app>                          # Auto-named file
kiteui-drive screenshot <app> --path debug.png         # Specific path
```

### Logs
```bash
kiteui-drive logs <app>                    # Last 200 entries
kiteui-drive logs <app> --lines 50         # Last 50
kiteui-drive logs <app> --level Warn       # Only Warn and Error
kiteui-drive logs <app> --tag "network"    # Filter by tag substring
```

## Performing Actions

```bash
kiteui-drive perform <app> click <targetId>
kiteui-drive perform <app> longClick <targetId>
kiteui-drive perform <app> setValue <targetId> <value>
kiteui-drive perform <app> navigate <route>
kiteui-drive perform <app> back
kiteui-drive perform <app> forward
kiteui-drive perform <app> scroll <targetId> --dy 500
kiteui-drive perform <app> custom <command> --data "payload"
```

## Waiting for Conditions

```bash
kiteui-drive wait <app> --page HomePage
kiteui-drive wait <app> --component loginBtn
kiteui-drive wait <app> --componentGone loadingSpinner
kiteui-drive wait <app> --enabled submitBtn
kiteui-drive wait <app> --change                         # Any state change
kiteui-drive wait <app> --timeout 30000                  # Custom timeout (ms)
```

Multiple conditions can be combined — all must be met simultaneously.

## Mocking External Services

```bash
kiteui-drive mock <app> file /path/to/photo.jpg
kiteui-drive mock <app> file /path/to/doc.pdf --mimeType application/pdf
kiteui-drive mock <app> geolocation 37.7749 -122.4194
kiteui-drive mock <app> geolocation 37.7749 -122.4194 --accuracy 5.0
```

Mocks are FIFO queues — the next call to the corresponding external service dequeues the mock.

## Debugging Workflow

When something isn't working right:

```bash
# 1. What's on screen?
kiteui-drive snapshot <app>

# 2. Any errors in the logs?
kiteui-drive logs <app> --level Warn

# 3. Take a screenshot for visual context
kiteui-drive screenshot <app> --path debug.png

# 4. Try interacting and see what changes
kiteui-drive perform <app> click <someButton>
kiteui-drive snapshot <app> --search "error"

# 5. Check full logs for context
kiteui-drive logs <app>
```

## Explore-then-Act Pattern

When working with an unfamiliar app:

```bash
# 1. Discover what's running
kiteui-drive list

# 2. See the full UI tree to understand layout and IDs
kiteui-drive snapshot <app>

# 3. Navigate to the page you care about
kiteui-drive perform <app> navigate /some/route

# 4. Find specific elements
kiteui-drive snapshot <app> --search "Submit"

# 5. Interact with the UI
kiteui-drive perform <app> setValue emailField user@test.com
kiteui-drive perform <app> click submitBtn

# 6. Verify the result
kiteui-drive wait <app> --page SuccessPage --timeout 10000
kiteui-drive snapshot <app>
```

## Recording Interactions

Record manual interactions and export them as test code:

```bash
kiteui-drive record <app> start
# ... interact with the app manually ...
kiteui-drive record <app> stop
kiteui-drive record <app> export-kotlin   # Generate uiTest code
kiteui-drive record <app> export          # CLI command format
```

---

# Part 2: Writing Automated UI Tests

## Overview

KiteUI tests are written once in common Kotlin and run on all platforms (JVM SSR, Android, iOS, JS). The test framework reuses the same snapshot and action-dispatch primitives as the CLI.

**JVM SSR is the fastest test target** — pure JVM, no emulator/browser/simulator needed.

### Running Tests
```bash
./gradlew :<module>:jvmSsrTest                           # All SSR tests (fastest)
./gradlew :<module>:jvmSsrTest --tests "*.MyTestClass"   # Single class
./gradlew :<module>:testDebugUnitTest                     # Android/Robolectric
./gradlew :<module>:jsBrowserTest                         # JS/browser
./gradlew :<module>:iosSimulatorArm64Test                 # iOS simulator
```

### Dependencies

In your module's `build.gradle.kts`:
```kotlin
kotlin {
    sourceSets {
        commonTest {
            dependencies {
                implementation("com.lightningkite.kiteui:kiteui-test-utilities:<version>")
                implementation(kotlin("test"))
            }
        }
    }
}
```

## Test Structure

```kotlin
import com.lightningkite.kiteui.testing.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test

class MyFeatureTest {
    @Test
    fun myTest() = uiTest(
        config = UiTestConfig(/* optional config */),
        content = {
            // ViewWriter DSL — build the UI to test
        }
    ) {
        // UiTestScope — interact and assert
    }
}
```

### UiTestConfig Options

```kotlin
UiTestConfig(
    theme = myTheme,                              // optional, defaults to Theme(id = "test")
    navigator = PageNavigator { AutoRoutes },     // optional, for navigation tests
    externalServices = MockExternalServices(),     // optional, for file/geo mocking
    customCommands = mapOf(                        // optional, for custom actions
        "reset" to { root, data -> null }
    ),
)
```

## Test Patterns

### Pattern 1: Testing a Component Directly

For testing a single component or page in isolation — no navigator needed.

```kotlin
// Helper that sets up the page for all tests in this class
private fun myPageTest(block: suspend UiTestScope.() -> Unit) {
    uiTest(content = {
        with(MyPage()) { render() }
    }, block = block)
}

@Test
fun displaysInitialValue() = myPageTest {
    assertValue("counter", "0")
}

@Test
fun incrementWorks() = myPageTest {
    click("increment")
    assertValue("counter", "1")
    click("increment")
    assertValue("counter", "2")
}
```

### Pattern 2: Testing with Inline UI

For testing specific reactive behaviors without a pre-built page.

```kotlin
@Test
fun formValidation() = uiTest(content = {
    val email = Signal("")
    textInput { debugName = "email"; content bind email }
    text { debugName = "error"; ::content { if ("@" !in email()) "Invalid email" else "" } }
    button {
        debugName = "submit"
        text("Submit")
        ::enabled { "@" in email() }
    }
}) {
    assertDisabled("submit")
    assertValue("error", "Invalid email")
    setValue("email", "user@example.com")
    assertEnabled("submit")
    assertValue("error", "")
}
```

### Pattern 3: Testing Navigation

For testing page transitions and routing. Requires a `PageNavigator`.

```kotlin
private fun navTest(block: suspend UiTestScope.() -> Unit) {
    val navigator = PageNavigator { AutoRoutes }
    navigator.reset(HomePage())
    uiTest(
        config = UiTestConfig(navigator = navigator),
        content = { navigatorView(navigator) },
        block = block
    )
}

@Test
fun navigateToDetails() = navTest {
    navigate("/items/123")
    assertPage("ItemDetailPage")
    assertVisible("itemTitle")
    back()
    assertPage("HomePage")
}

@Test
fun clickNavigates() = navTest {
    click("itemLink")
    waitForPage("ItemDetailPage")
}
```

### Pattern 4: Testing with Mock External Services

For testing file pickers, camera capture, geolocation, etc.

```kotlin
private fun mockTest(
    setupMock: MockExternalServices.() -> Unit = {},
    block: suspend UiTestScope.() -> Unit
) {
    val mock = MockExternalServices()
    mock.setupMock()
    uiTest(
        config = UiTestConfig(externalServices = mock),
        content = { with(UploadPage()) { render() } },
        block = block
    )
}

@Test
fun fileUpload() = mockTest(
    setupMock = {
        pendingFileResponses.add(
            createFileReferenceFromBytes("test data".encodeToByteArray(), "image/png", "photo.png")
        )
    }
) {
    click("uploadBtn")
    // MockExternalServices returns the queued file to the next requestFile() call
    waitForComponent("uploadedFileName")
    assertValue("uploadedFileName", "photo.png")

    // Verify the app called the right service method
    val mock = mockExternalServices!!
    val call = mock.calls.filterIsInstance<MockExternalServices.Call.RequestFile>().single()
    assertEquals(listOf("image/*"), call.mimeTypes)
}
```

### Pattern 5: Testing with Custom Commands

For resetting state, injecting test data, or other test-specific hooks.

```kotlin
@Test
fun resetCommand() = uiTest(
    config = UiTestConfig(
        customCommands = mapOf(
            "reset" to { root, _ ->
                // Reset app state; return null = success, string = error
                null
            },
            "setUser" to { root, data ->
                // Inject test data
                null
            }
        )
    ),
    content = { with(MyPage()) { render() } }
) {
    custom("setUser", """{"name":"Test User"}""")
    assertValue("userName", "Test User")
    custom("reset")
    assertValue("userName", "")
}
```

### Pattern 6: Using Logs for Verification

When you need to verify that internal behavior occurred (API calls, state changes, etc.).

```kotlin
@Test
fun buttonLogsAction() = uiTest(content = {
    button {
        debugName = "action"
        text("Do Thing")
        onClick(frequencyCap = null) {
            Log.tag("myFeature").info("Action triggered")
        }
    }
}) {
    click("action")
    val entries = logs()
    assert(entries.any { it.tag == "myFeature" && "Action triggered" in it.message })
}
```

## UiTestScope API Reference

### Read State
| Method | Returns | Description |
|--------|---------|-------------|
| `snapshot()` | `UiSnapshot` | Full UI tree |
| `find(id)` | `UiComponent?` | Find component by ID (null if missing) |
| `require(id)` | `UiComponent` | Find component or throw with snapshot dump |
| `dumpSnapshot()` | — | Print snapshot to stdout (debugging) |
| `logs(lines)` | `List<LogEntry>` | Read captured log buffer |

### Perform Actions
| Method | Description |
|--------|-------------|
| `click(id)` | Click a component |
| `setValue(id, value)` | Set text input / select value |
| `navigate(route)` | Navigate to a URL route |
| `back()` | Go back in navigation |
| `custom(command, data?)` | Invoke a custom command handler |
| `perform(UiAction)` | Low-level: dispatch any UiAction |

All action methods throw `AssertionError` on failure.

### Wait for Conditions
| Method | Description |
|--------|-------------|
| `waitFor(timeout, description) { snap -> bool }` | Generic wait with lambda |
| `waitForPage(name, timeout)` | Wait for page name match |
| `waitForComponent(id, timeout)` | Wait for component to appear |
| `waitForComponentGone(id, timeout)` | Wait for component to disappear |
| `waitForEnabled(id, timeout)` | Wait for component to become enabled |

Default timeout: 5 seconds. All throw `AssertionError` on timeout with snapshot dump.

### Assertions
| Method | Description |
|--------|-------------|
| `assertPage(expected)` | Assert current page name |
| `assertValue(id, expected)` | Assert component's value |
| `assertVisible(id)` | Assert component is visible |
| `assertEnabled(id)` | Assert component is enabled |
| `assertDisabled(id)` | Assert component is disabled |

All throw `AssertionError` with descriptive messages including a snapshot dump on failure.

### Snapshot Extensions (on `UiSnapshot`)
| Method | Returns | Description |
|--------|---------|-------------|
| `findById(id)` | `UiComponent?` | Search by full ID or last segment |
| `findByValue(text)` | `UiComponent?` | First component whose value contains text |
| `findByType(type)` | `List<UiComponent>` | All components of a given type |
| `findAll { predicate }` | `List<UiComponent>` | All matching components |
| `renderText()` | `String` | Human-readable text tree |

### UiComponent Fields
```kotlin
data class UiComponent(
    val id: String,           // path ID (e.g. "navigatorView/0/3" or "submitBtn")
    val type: String,         // "Button", "Text", "TextInput", "Column", "Row", etc.
    val value: String?,       // current text/value
    val enabled: Boolean,     // interactive?
    val visible: Boolean,     // on screen?
    val actions: List<String>,// ["click", "setValue", "longClick", "scroll"]
    val children: List<UiComponent>
)
```

---

# Part 2b: Remote UI Testing (Against a Live App)

Run the same `UiTestScope` API against a **live, already-running app** connected to the daemon — useful for integration testing against real servers or testing on actual devices.

## How It Works

Instead of rendering UI in-process, `remoteUiTest()` sends snapshot/perform/logs commands to the daemon's HTTP API, which forwards them to the connected app. The test API is identical to local `uiTest()`.

```
┌──────────────┐    HTTP      ┌────────────┐   WebSocket   ┌─────────────┐
│  JVM Test    │ ──────────► │   Daemon   │ ◄──────────► │  Live App   │
│ (remoteUi    │  :7475/cli   │ (JVM proc) │  ws://:7474   │ (any        │
│  Test)       │              │            │               │  platform)  │
└──────────────┘              └────────────┘               └─────────────┘
```

## Prerequisites

1. Daemon running: `kiteui-drive start`
2. App connected: `AiDriver.connect()` called in the app
3. App visible: `kiteui-drive list` shows the app ID

## Usage

```kotlin
import com.lightningkite.kiteui.testing.*
import kotlin.test.Test

class LiveAppTest {
    @Test
    fun dashboardLoads() = remoteUiTest(appId = "web") {
        navigate("/dashboard")
        waitForPage("DashboardPage")
        assertVisible("dashboardTitle")
        assertValue("dashboardTitle", "Welcome")
    }

    @Test
    fun loginFlow() = remoteUiTest(appId = "web") {
        navigate("/login")
        waitForComponent("emailInput")
        setValue("emailInput", "user@example.com")
        setValue("passwordInput", "password123")
        click("loginButton")
        waitForPage("HomePage")
        assertVisible("homeTitle")
    }
}
```

Run with:
```bash
./gradlew :<module>:jvmSsrTest --tests "*.LiveAppTest"
```

## `remoteUiTest()` Parameters

```kotlin
remoteUiTest(
    appId = "web",           // Required — connected app ID from `kiteui-drive list`
    host = "localhost",      // Optional — daemon host
    port = 7475,             // Optional — daemon CLI port
) {
    // UiTestScope — same API as uiTest()
}
```

## Screenshots

Remote tests can capture screenshots from the live app and save them to disk — useful for app store screenshots, visual regression testing, or documentation.

```kotlin
class AppStoreScreenshots {
    @Test
    fun captureHomeScreen() = remoteUiTest(appId = "android-1") {
        navigate("/home")
        waitForPage("HomePage")
        screenshotToFile("screenshots/home-android.png")
    }

    @Test
    fun captureProfileScreen() = remoteUiTest(appId = "ios-1") {
        navigate("/profile")
        waitForPage("ProfilePage")
        screenshotToFile("screenshots/profile-ios.png")
    }
}
```

| Method | Returns | Description |
|--------|---------|-------------|
| `screenshot()` | `ByteArray?` | Raw PNG bytes (null if unsupported) |
| `screenshotToFile(path)` | — | Save PNG to disk (creates parent dirs) |

`screenshotToFile` is a JVM extension on `UiTestScope` — available in `remoteUiTest` blocks.

## Differences from Local `uiTest()`

| | `uiTest()` (local) | `remoteUiTest()` (remote) |
|--|---------------------|---------------------------|
| UI rendering | In-process | External app |
| Speed | Milliseconds | Network round-trip per call |
| Screenshots | Not supported | `screenshot()` / `screenshotToFile()` |
| `mockExternalServices` | Available | Returns null |
| `customCommands` | Via UiTestConfig | Not available |
| Platform | All (Android, iOS, JS, JVM SSR) | JVM SSR only |
| Server connectivity | Mocked/local | Real (app's actual server) |

## Tips

- Use longer timeouts for `waitFor*` calls (network + real server latency)
- The app must stay connected to the daemon throughout the test
- Remote tests are ideal for smoke tests, integration tests, and app store screenshot capture; use local `uiTest()` for fast unit-level UI tests

---

# Part 3: CLI-to-Test Workflow

The most effective way to write tests is to **explore interactively first**, then **translate to automated tests**.

## Step 1: Explore the App

```bash
kiteui-drive list
kiteui-drive snapshot <app>
```

Identify the component IDs, page names, and actions available.

## Step 2: Interact and Verify Manually

```bash
kiteui-drive perform <app> setValue emailField test@example.com
kiteui-drive perform <app> click submitBtn
kiteui-drive wait <app> --page DashboardPage
kiteui-drive snapshot <app> --search "Welcome"
```

Once you've confirmed the flow works, translate each step into test code.

## Step 3: Write the Test

Map CLI commands directly to `UiTestScope` methods:

| CLI Command | Test Code |
|------------|-----------|
| `kiteui-drive snapshot <app>` | `snapshot()` or `dumpSnapshot()` |
| `kiteui-drive snapshot <app> --component X` | `find("X")` or `require("X")` |
| `kiteui-drive perform <app> click X` | `click("X")` |
| `kiteui-drive perform <app> setValue X value` | `setValue("X", "value")` |
| `kiteui-drive perform <app> navigate /path` | `navigate("/path")` |
| `kiteui-drive perform <app> back` | `back()` |
| `kiteui-drive wait <app> --page Name` | `waitForPage("Name")` |
| `kiteui-drive wait <app> --component X` | `waitForComponent("X")` |
| `kiteui-drive wait <app> --componentGone X` | `waitForComponentGone("X")` |
| `kiteui-drive wait <app> --enabled X` | `waitForEnabled("X")` |
| `kiteui-drive logs <app>` | `logs()` |
| `kiteui-drive screenshot <app>` | (not available in tests) |

## Step 4: Use Recording for Scaffolding

If the interaction is complex, record it and export as Kotlin:

```bash
kiteui-drive record <app> start
# interact manually...
kiteui-drive record <app> stop
kiteui-drive record <app> export-kotlin
```

This generates `UiTestScope` code you can paste into a test and refine.

---

# Troubleshooting

### "kiteui-drive not installed"
```bash
./gradlew aiDriverInstall
```

### "Daemon not running"
```bash
kiteui-drive start
# or
./gradlew aiDriverStart
```

### "No apps connected"
- App must be running with `AiDriver.connect()` wired in
- For web: browser must be open to the app URL
- Wait a few seconds for WebSocket handshake

### "App 'myapp' not found"
Run `kiteui-drive list` to see current app IDs.

### Component not found in snapshot
- Check that the component has a `debugName` or `ariaDescription` set
- Use `kiteui-drive snapshot <app>` to see all available IDs
- Unnamed components use index-based paths like `navigatorView/0/2`

### Stale daemon after version upgrade
```bash
./gradlew aiDriverInstall
kiteui-drive stop && kiteui-drive start
```

### Test timeout
- Increase timeout: `waitForPage("Name", timeout = 15_000)`
- Check that the condition is reachable — use `dumpSnapshot()` for debugging
- For JVM SSR tests, reactive updates are synchronous so timeouts usually mean the condition is wrong

---

# Daemon Management (Gradle Tasks)

These tasks are registered by the KiteUI Gradle plugin on every project that applies it:

```bash
./gradlew aiDriverInstall    # Download fat JAR from Maven, install to ~/.kiteui/
./gradlew aiDriverStart      # Start daemon if not already running
./gradlew aiDriverStop       # Stop daemon
```

---

# Key Source Files (in the kiteui repo)

- `library/src/commonMain/.../aidriver/` — Protocol types, client, shared dispatch, log buffer
- `library/src/commonMain/.../aidriver/AiDriverAutoStart.kt` — Auto-start expect/actual
- `library/src/commonInteractiveMain/.../aidriver/` — Shared snapshot walker (Android, iOS, JS)
- `library/src/jvmSsrMain/.../aidriver/` — SSR snapshot walker + action dispatch + auto-start
- `test-utilities/src/commonMain/.../testing/` — `UiTestBackend`, `LocalUiTestBackend`, `UiTestScope`, `uiTest()`
- `test-utilities/src/jvmSsrMain/.../testing/` — `RemoteUiTestBackend`, `remoteUiTest()`
- `test-utilities/src/{androidMain,iosMain,jsMain,jvmSsrMain}/` — Platform `uiTest()` actuals
- `ai-driver-server/src/main/kotlin/.../server/` — Daemon + CLI server
- `gradle-plugin/src/main/kotlin/aiDriverTasks.kt` — Gradle tasks for install/start/stop
