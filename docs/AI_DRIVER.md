# KiteUI AI Driver

The AI Driver enables LLM-driven UI automation and programmatic testing of KiteUI apps across all platforms (Android, iOS, Web, JVM SSR). It uses a plain-text command protocol over WebSocket, with a relay server that bridges between apps and CLI/test clients.

## Architecture

```
┌──────────┐    WebSocket     ┌──────────────┐    HTTP POST    ┌──────────┐
│ KiteUI   │ ──────────────── │  Relay       │ ←───────────── │  CLI /   │
│ App      │  ws://host:7474  │  Server      │  http://host:  │  Tests   │
│          │  (auto-reconnect)│  (:7474)     │  7474          │          │
└──────────┘                  └──────────────┘                └──────────┘
```

- **App** connects outbound via WebSocket to the relay server
- **CLI/tests** send HTTP POST requests to the same server
- **Server** routes commands to the correct app and returns responses
- Multiple apps can connect simultaneously; each gets a stable ID like `example-web-ABC` (random 3-letter postfix per app instance, stable across reconnects)

## Setup

### 1. Install the CLI

```bash
./gradlew :ai-driver-server:installDriver
```

This installs:
- `~/.kiteui/ai-driver/kiteui-ai-driver.jar` — the relay server
- `~/.kiteui/bin/kiteui-drive` — the CLI wrapper script

Add `~/.kiteui/bin` to your `PATH`.

### 2. Connect your app

In your shared `app()` function:

```kotlin
import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.isDevelopment
import com.lightningkite.kiteui.views.AiDriver
import com.lightningkite.kiteui.views.produceOne

fun ViewWriter.app(navigator: PageNavigator, dialog: PageNavigator) {
    val rootView = produceOne {
        // your UI here
    }

    if (Platform.isDevelopment) {
        AiDriver.connect(
            appName = "myapp",
            platform = Platform.current.name.lowercase(),
            rootView = { rootView },
            navigator = { navigator },
        )
    }
}
```

The driver only activates in development builds:
- **Android**: debug builds (`FLAG_DEBUGGABLE`)
- **iOS**: debug binaries (`Platform.isDebugBinary`)
- **Web**: localhost/127.0.0.1
- **JVM SSR**: always (SSR is always dev)

### 3. Start the server

The server auto-starts when you run `kiteui-drive` commands. Or manually:

```bash
kiteui-drive start          # Start daemon
kiteui-drive stop           # Stop daemon
KITEUI_DRIVER_PORT=8080 kiteui-drive start  # Custom port
```

### 4. Android ADB

The server automatically sets up `adb reverse` for connected Android devices, so the app can reach `localhost:7474` from the device.

## CLI Reference

```bash
kiteui-drive ls                                    # List connected apps
kiteui-drive <app> root snapshot                   # Get view tree snapshot
kiteui-drive <app> root snapshot --interactive     # Only interactive elements
kiteui-drive <app> root snapshot --hidden          # Include hidden views
kiteui-drive <app> root screenshot                 # Base64 PNG screenshot
kiteui-drive <app> <path> click                    # Click a button/link
kiteui-drive <app> <path> setValue "hello world"   # Set text input value (spaces ok)
kiteui-drive <app> <path> toggle                   # Toggle checkbox/switch
kiteui-drive <app> <path> select                   # Select radio button
kiteui-drive <app> <path> submit                   # Submit text input action
kiteui-drive <app> <path> scroll 0 100             # Scroll by dx/dy
kiteui-drive <app> <path> scrollIntoView           # Scroll view into viewport
kiteui-drive <app> root find "query"               # Find views by name/value/type/action
kiteui-drive <app> <path> snapshot                 # Snapshot a single element subtree
kiteui-drive <app> navigate /some/route            # Navigate to URL path
kiteui-drive <app> back back                       # Go back in navigation
kiteui-drive <app> logs 50                         # Get recent log entries
```

The `<app>` parameter supports prefix matching: `myapp` matches `myapp-web-ABC`.

## Command Protocol

Commands use **tab separation** (`\t`) between fields so values containing spaces are preserved exactly.

Format: `appId\ttarget\taction\targ1\targ2`

The bash CLI script handles this automatically — shell-quoted arguments are joined with tabs before sending.

## Path Resolution

View paths are `/`-separated segments resolved against the view tree.

- `root` — the root view
- `myButton` — deep-searches from root for a view with `debugName = "myButton"`
- `form/email` — finds `form`, then its child named `email`
- `form/0` — finds `form`, then its first child (by index)
- `myButton/..` — finds `myButton`, then navigates to its parent
- `..` segments can appear anywhere: `form/email/../password`

### Naming views

```kotlin
button {
    debugName = "submitBtn"
    // ...
}

textInput {
    debugName = "email"
    content bind emailProperty
}
```

Views with `debugName` are directly addressable. Views without one use their numeric child index.

## Snapshot Format

The `snapshot` command returns a text tree showing the view hierarchy:

```
submitBtn: Button [click]
  0: TextView = "Submit"
form: RowOrCol
  email: TextInput = "user@example.com" [setValue, submit]
  password: TextInput = "" [setValue, submit]
  0: Checkbox = "false" [toggle, setValue]
```

Each line shows: `name: Type = "value" [actions] (flags)`

- **name**: `debugName` or child index
- **Type**: the Kotlin class name (Button, TextInput, RowOrCol, etc.)
- **value**: the view's current value (text content, checked state, etc.) — only for interactive views
- **actions**: available driver actions in brackets (excluding base actions)
- **flags**: `(hidden)`, `(invisible)` for non-visible views (with `--hidden`)

### Snapshot options

- `snapshot` — full tree from the target view
- `snapshot --interactive` — prune structural-only containers, showing only views with actions, values, or names
- `snapshot --hidden` — include hidden/invisible views
- `snapshot --themes` — include theme derivation info

You can snapshot any subtarget: `kiteui-drive <app> myForm snapshot` shows only `myForm` and its children.

## Find (Search)

The `find` command searches the view subtree by:
- **debugName**: `find "email"` matches views named "email"
- **driverValue**: `find "hello"` matches views whose current value contains "hello"
- **Widget type**: `find "TextInput"` matches all TextInput elements
- **Action names**: `find "toggle"` matches all views with a `toggle` action (Checkbox, Switch, ToggleButton)

Results include full path and display info:

```
navigatorView/0/14/2: 2: TextInput = "text" [setValue]
navigatorView/0/14/3: 3: TextInput = "text" [setValue]
```

## Available Actions

| View Type | Actions | Value |
|-----------|---------|-------|
| Button | `click`, `longClick` | — |
| TextInput | `setValue`, `submit` | current text |
| TextArea | `setValue`, `submit` | current text |
| Checkbox | `toggle`, `setValue` (true/false) | `true`/`false` |
| RadioButton | `select` | `true`/`false` |
| Switch | `toggle`, `setValue` (true/false) | `true`/`false` |
| ToggleButton | `toggle`, `setValue` (true/false) | `true`/`false` |
| Slider | `setValue` (number) | current number |
| Select | `setValue` | selected display text |
| Link | `click` | — |
| Any view | `snapshot`, `screenshot`, `find`, `scroll`, `scrollIntoView` | — |
| View with dragData | `getDragData` | — |
| View with dropTarget | `drop` (base64 drag data) | — |

## Error Handling

Driver actions throw `DriverActionException` on failure. The WebSocket handler catches these and returns `"ERROR: ClassName: message"` to the client. Common errors:

- `"ERROR: DriverActionException: view 'X' not found"` — path didn't resolve
- `"ERROR: DriverActionException: Unknown action 'X' on Y. Available: ..."` — wrong action for this view type
- `"ERROR: DriverActionException: no navigator available"` — navigate/back without navigator
- `"ERROR: DriverActionException: expected true/false argument"` — wrong argument type

## Testing API

### Local testing (in-process, fastest)

```kotlin
@Test
fun myTest() = uiTest(
    content = {
        col {
            textInput { debugName = "email" }
            button { debugName = "submit"; text("Go"); onClick { /* ... */ } }
        }
    }
) {
    setValue("email", "test@example.com")
    assertValue("email", "test@example.com")
    click("submit")

    val tree = snapshot()
    assertTrue(tree.contains("submit"))
}
```

Run with: `./gradlew :library:jvmSsrTest --tests "*.MyTest"` (fastest, no device/browser needed)

### Remote testing (against running app)

```kotlin
@Test
fun remoteTest() = remoteUiTest("myapp-web") {
    navigate("/login")
    setValue("email", "test@example.com")
    setValue("password", "secret")
    click("loginBtn")
    waitForText("Dashboard")
    assertVisible("welcomeMsg")
}
```

Requires: relay server running + app connected. Same `UiTestScope` API — tests work identically local or remote.

### UiTestScope API

| Method | Description |
|--------|-------------|
| `snapshot(target)` | Get text snapshot of view tree |
| `interactiveSnapshot(target)` | Snapshot with only interactive elements |
| `screenshot(target)` | Get base64 PNG screenshot |
| `click(target)` | Click a button or link |
| `longClick(target)` | Long-click a view |
| `setValue(target, value)` | Set input value (spaces preserved) |
| `toggle(target)` | Toggle checkbox/switch |
| `select(target)` | Select radio button |
| `submit(target)` | Submit text input action |
| `scroll(target, dx, dy)` | Scroll by offset |
| `scrollIntoView(target)` | Scroll view into viewport |
| `getDragData(target)` | Get base64 drag data from view |
| `drop(target, data)` | Drop base64 drag data onto view |
| `navigate(route)` | Navigate to URL path |
| `back()` | Go back in navigation |
| `find(query, target)` | Search for views by name/value/type/action |
| `logs(count)` | Get recent log entries |
| `raw(command)` | Send raw command string |
| `assertValue(target, expected)` | Assert view has expected value |
| `assertVisible(target)` | Assert view exists and is visible |
| `assertNotVisible(target)` | Assert view is hidden or missing |
| `waitForText(text, target, timeoutMs)` | Wait for text to appear |
| `waitFor(timeoutMs, condition)` | Wait for arbitrary condition |

## Screenshots

Screenshots are supported on all interactive platforms:
- **Android**: Uses `View.draw()` to render to bitmap
- **iOS**: Uses `UIGraphicsBeginImageContextWithOptions` + `renderInContext`
- **Web**: Uses `modern-screenshot` library (`domToPng`)
- **JVM SSR**: Not supported (throws `DriverActionException`)

## Workflow: AI Agent Driving a KiteUI App

Recommended workflow for an AI agent automating a KiteUI app:

1. **List apps**: `kiteui-drive ls` to find connected app ID
2. **Orient**: `kiteui-drive <app> root snapshot --interactive` to see the UI state
3. **Find targets**: `kiteui-drive <app> root find "ButtonText"` or `find "TextInput"` to locate elements
4. **Interact**: `click`, `setValue`, `toggle`, etc. on specific paths
5. **Verify**: `snapshot` a specific element to check its value changed
6. **Navigate**: Use named links, `navigate /path`, or `back` to move between pages

### Tips

- Use `--interactive` snapshot to cut noise — only shows elements you can interact with
- Use `find` with widget types (`TextInput`, `Button`, `Checkbox`) to discover all interactive elements of a kind
- Use `find` with action names (`click`, `toggle`, `setValue`) to discover what's actionable
- Snapshot a single element (`<path> snapshot`) to check its value without getting the whole tree
- Named views (`debugName`) give stable paths; numeric indices shift when siblings change
- `back` and `navigate` work at the KiteUI PageNavigator level, not browser-level
- App ID supports prefix matching — `myapp` matches `myapp-web-ABC`
- App IDs are stable across reconnects (same browser tab keeps the same ID)
