# KiteUI AI Driver — Testing & Debugging with `kiteui-drive`

Copy this file to your project's `.claude/skills/kiteui-drive.md` to give LLM agents
full knowledge of how to interactively test, debug, and write automated tests for your
KiteUI app.

## Prerequisites

```bash
# Install (once per KiteUI version)
./gradlew aiDriverInstall

# Ensure ~/.kiteui/bin is on your PATH
export PATH="$HOME/.kiteui/bin:$PATH"

# Start the daemon
kiteui-drive start
```

On JVM (including test runs), `AiDriver.connect()` auto-starts the daemon if installed.

## Architecture

```
┌─────────────┐   WebSocket    ┌────────────┐    HTTP     ┌──────────────────┐
│  KiteUI App │ ◄──────────► │   Daemon   │ ◄──────────► │ CLI (kiteui-drive) │
│ (browser,   │  ws://:7474   │ (JVM proc) │  http://:7475│  (you run these)  │
│  Android,   │               │            │              └──────────────────┘
│  iOS)       │               └────────────┘
└─────────────┘
```

- **App** connects outbound to daemon on `ws://localhost:7474/app`
- **Daemon** bridges apps and CLI commands
- **CLI** sends commands via HTTP to `:7475/cli`

Your app must call `AiDriver.connect()` during startup (debug/QA builds) to enable the driver.

---

# Part 1: Interactive Testing & Debugging

Use these commands via the Bash tool to explore, test, and debug a running app.

## Exploring the App

```bash
kiteui-drive list                            # See connected apps and their IDs
kiteui-drive snapshot <app>                  # Full UI tree
kiteui-drive snapshot <app> --component nav  # Scope to a subtree
kiteui-drive snapshot <app> --search "error" # Filter by value text
```

Snapshot output shows the component tree with IDs, types, values, and actions:
```
Page: HomePage ()
[navigatorView] SwapView (value="navigatorView")
  [navigatorView/0] Frame
    [counter] Text (value="0")
    [increment] Button (value="increment", actions=[click,longClick])
```

**Component IDs** use shortened paths:
- Named views (with `debugName`) reset the path: `counter` not `root/0/3/counter`
- Unnamed views use index from nearest named ancestor: `navigatorView/0/2`

## Performing Actions

```bash
kiteui-drive perform <app> click <id>
kiteui-drive perform <app> setValue <id> <value>
kiteui-drive perform <app> navigate <route>
kiteui-drive perform <app> back
kiteui-drive perform <app> forward
kiteui-drive perform <app> scroll <id> --dy 500
kiteui-drive perform <app> longClick <id>
kiteui-drive perform <app> custom <command> --data "payload"
```

## Waiting for State Changes

```bash
kiteui-drive wait <app> --page DashboardPage
kiteui-drive wait <app> --component results
kiteui-drive wait <app> --componentGone spinner
kiteui-drive wait <app> --enabled submitBtn
kiteui-drive wait <app> --change                    # Any state change
kiteui-drive wait <app> --timeout 30000             # Custom timeout (ms)
```

Multiple conditions can be combined — all must be met simultaneously.

## Debugging

```bash
# What's on screen?
kiteui-drive snapshot <app>

# Any errors?
kiteui-drive logs <app> --level Warn

# Visual state
kiteui-drive screenshot <app> --path debug.png

# Full log context
kiteui-drive logs <app>
kiteui-drive logs <app> --lines 50 --tag "network"
```

## Mocking External Services

```bash
kiteui-drive mock <app> file /path/to/photo.jpg
kiteui-drive mock <app> file /path/to/doc.pdf --mimeType application/pdf
kiteui-drive mock <app> geolocation 37.7749 -122.4194 --accuracy 5.0
```

Mocks are FIFO — the next call to the corresponding service dequeues the mock.

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

## Setup

Add test dependency in your module's `build.gradle.kts`:
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

Run tests (JVM SSR is fastest — no emulator/browser needed):
```bash
./gradlew :<module>:jvmSsrTest
./gradlew :<module>:jvmSsrTest --tests "*.MyTestClass"
```

## Test Structure

```kotlin
import com.lightningkite.kiteui.testing.*
import kotlin.test.Test

class MyFeatureTest {
    @Test
    fun myTest() = uiTest(
        config = UiTestConfig(/* optional */),
        content = { /* ViewWriter DSL — build the UI to test */ }
    ) {
        /* UiTestScope — interact and assert */
    }
}
```

### UiTestConfig Options

```kotlin
UiTestConfig(
    theme = myTheme,                              // optional
    navigator = PageNavigator { AutoRoutes },     // optional, for navigation tests
    externalServices = MockExternalServices(),     // optional, for file/geo mocking
    customCommands = mapOf("reset" to { root, data -> null }), // optional
)
```

## Test Patterns

### Component Test — Render a Page Directly

```kotlin
private fun myPageTest(block: suspend UiTestScope.() -> Unit) {
    uiTest(content = { with(MyPage()) { render() } }, block = block)
}

@Test
fun incrementWorks() = myPageTest {
    assertValue("counter", "0")
    click("increment")
    assertValue("counter", "1")
}
```

### Inline UI Test — Build UI in the Test

```kotlin
@Test
fun formValidation() = uiTest(content = {
    val email = Signal("")
    textInput { debugName = "email"; content bind email }
    text { debugName = "error"; ::content { if ("@" !in email()) "Invalid" else "" } }
    button {
        debugName = "submit"
        text("Submit")
        ::enabled { "@" in email() }
    }
}) {
    assertDisabled("submit")
    assertValue("error", "Invalid")
    setValue("email", "user@example.com")
    assertEnabled("submit")
    assertValue("error", "")
}
```

### Navigation Test

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
fun navigateAndBack() = navTest {
    navigate("/items/123")
    assertPage("ItemDetailPage")
    back()
    assertPage("HomePage")
}
```

### Mock External Services Test

```kotlin
@Test
fun fileUpload() = uiTest(
    config = UiTestConfig(
        externalServices = MockExternalServices().apply {
            pendingFileResponses.add(
                createFileReferenceFromBytes("data".encodeToByteArray(), "image/png", "photo.png")
            )
        }
    ),
    content = { with(UploadPage()) { render() } }
) {
    click("uploadBtn")
    waitForComponent("uploadedFileName")
    assertValue("uploadedFileName", "photo.png")
}
```

### Log Verification Test

```kotlin
@Test
fun logsAction() = uiTest(content = {
    button {
        debugName = "action"
        text("Do Thing")
        onClick(frequencyCap = null) { Log.tag("myFeature").info("triggered") }
    }
}) {
    click("action")
    val entries = logs()
    assert(entries.any { it.tag == "myFeature" && "triggered" in it.message })
}
```

## UiTestScope API Reference

### Read State
| Method | Returns | Description |
|--------|---------|-------------|
| `snapshot()` | `UiSnapshot` | Full UI tree |
| `find(id)` | `UiComponent?` | Find by ID (null if missing) |
| `require(id)` | `UiComponent` | Find or throw with snapshot dump |
| `dumpSnapshot()` | — | Print tree to stdout for debugging |
| `logs(lines)` | `List<LogEntry>` | Read app log buffer |

### Actions
| Method | Description |
|--------|-------------|
| `click(id)` | Click a component |
| `setValue(id, value)` | Set text input / select value |
| `navigate(route)` | Navigate to URL route |
| `back()` | Go back in navigation |
| `custom(command, data?)` | Invoke custom command handler |

### Wait (default 5s timeout)
| Method | Description |
|--------|-------------|
| `waitFor(timeout, desc) { snap -> bool }` | Generic condition |
| `waitForPage(name, timeout)` | Page name match |
| `waitForComponent(id, timeout)` | Component appears |
| `waitForComponentGone(id, timeout)` | Component disappears |
| `waitForEnabled(id, timeout)` | Component becomes enabled |

### Assertions (throw AssertionError with snapshot dump)
| Method | Description |
|--------|-------------|
| `assertPage(expected)` | Current page name |
| `assertValue(id, expected)` | Component value |
| `assertVisible(id)` | Component visible |
| `assertEnabled(id)` | Component enabled |
| `assertDisabled(id)` | Component disabled |

---

# Part 2b: Remote UI Testing (Against a Live App)

Run the same `UiTestScope` API against a **live, already-running app** connected to the daemon. Useful for integration testing against real servers or testing on actual devices.

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
    }

    @Test
    fun loginFlow() = remoteUiTest(appId = "web") {
        navigate("/login")
        waitForComponent("emailInput")
        setValue("emailInput", "user@example.com")
        setValue("passwordInput", "password123")
        click("loginButton")
        waitForPage("HomePage")
    }
}
```

Run with:
```bash
./gradlew :<module>:jvmSsrTest --tests "*.LiveAppTest"
```

## Parameters

```kotlin
remoteUiTest(
    appId = "web",           // Required — from `kiteui-drive list`
    host = "localhost",      // Optional — daemon host
    port = 7475,             // Optional — daemon CLI port
) { /* UiTestScope — same API as uiTest() */ }
```

## Screenshots

Capture screenshots from the live app and save to disk — useful for app store screenshots.

```kotlin
@Test
fun captureHome() = remoteUiTest(appId = "android-1") {
    navigate("/home")
    waitForPage("HomePage")
    screenshotToFile("screenshots/home-android.png")
}
```

| Method | Returns | Description |
|--------|---------|-------------|
| `screenshot()` | `ByteArray?` | Raw PNG bytes (null if unsupported) |
| `screenshotToFile(path)` | — | Save PNG to disk (JVM extension) |

## Differences from Local `uiTest()`

| | `uiTest()` | `remoteUiTest()` |
|--|-----------|-----------------|
| UI rendering | In-process | External app |
| Speed | Milliseconds | Network round-trip |
| Screenshots | Not supported | `screenshot()` / `screenshotToFile()` |
| `mockExternalServices` | Available | null |
| Platform | All | JVM SSR only |
| Server | Mocked/local | Real |

Use longer timeouts for `waitFor*` calls due to network + server latency.

---

# Part 3: Explore-then-Test Workflow

The most effective way to write tests: **explore interactively, then translate to code**.

## Step 1: Explore with CLI

```bash
kiteui-drive list
kiteui-drive snapshot <app>
```

Identify component IDs, page names, and available actions.

## Step 2: Interact Manually

```bash
kiteui-drive perform <app> setValue email test@example.com
kiteui-drive perform <app> click submit
kiteui-drive wait <app> --page SuccessPage
kiteui-drive snapshot <app>
```

## Step 3: Translate to Test Code

| CLI Command | Test Code |
|------------|-----------|
| `snapshot <app>` | `snapshot()` or `dumpSnapshot()` |
| `snapshot --component X` | `find("X")` or `require("X")` |
| `perform click X` | `click("X")` |
| `perform setValue X val` | `setValue("X", "val")` |
| `perform navigate /path` | `navigate("/path")` |
| `perform back` | `back()` |
| `wait --page Name` | `waitForPage("Name")` |
| `wait --component X` | `waitForComponent("X")` |
| `wait --componentGone X` | `waitForComponentGone("X")` |
| `wait --enabled X` | `waitForEnabled("X")` |
| `logs` | `logs()` |

## Step 4: Or Use Recording

```bash
kiteui-drive record <app> start
# interact manually...
kiteui-drive record <app> stop
kiteui-drive record <app> export-kotlin   # Paste into test, refine
```

---

# Troubleshooting

| Problem | Solution |
|---------|----------|
| `kiteui-drive` not found | `./gradlew aiDriverInstall` then add `~/.kiteui/bin` to PATH |
| Daemon not running | `kiteui-drive start` or `./gradlew aiDriverStart` |
| No apps connected | App must call `AiDriver.connect()` and be running |
| App ID not found | `kiteui-drive list` to see current IDs |
| Component not in snapshot | Add `debugName` to the view: `view { debugName = "myId" }` |
| Stale after version upgrade | `./gradlew aiDriverInstall && kiteui-drive stop && kiteui-drive start` |
| Test timeout | Increase timeout param; use `dumpSnapshot()` to debug |

# Daemon Management (Gradle Tasks)

```bash
./gradlew aiDriverInstall    # Download fat JAR, install to ~/.kiteui/
./gradlew aiDriverStart      # Start daemon if not running
./gradlew aiDriverStop       # Stop daemon
```
