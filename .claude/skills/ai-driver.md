# AI Driver — LLM-Driven UI Automation for KiteUI

Use this skill when you need to visually test, interact with, or automate a running KiteUI app.
The AI driver lets you take UI snapshots, click buttons, fill forms, navigate pages, and verify
visual state — all from CLI commands via the Bash tool.

## Architecture

```
┌─────────────┐   WebSocket    ┌────────────┐    HTTP     ┌──────────┐
│  KiteUI App │ ◄──────────► │   Daemon   │ ◄──────────► │ CLI (./ui) │
│ (browser,   │  ws://:7474   │ (JVM proc) │  http://:7475│ (you run │
│  Android,   │               │            │              │  these)  │
│  iOS)       │               └────────────┘              └──────────┘
└─────────────┘
```

- **App** connects outbound to daemon on `ws://localhost:7474/app` (auto-reconnects)
- **Daemon** bridges between apps and CLI commands
- **CLI** (`./ui`) sends commands to daemon via HTTP POST to `:7475/cli`

## Setup — Getting Everything Running

### Prerequisites
- The ai-driver-server distribution must be built (one-time)
- A KiteUI app must be running with `AiDriver.connect()` wired in

### Quick Setup (recommended)
```bash
# One command: builds server, starts daemon
./ui-setup
```

### Manual Setup (step by step)
```bash
# 1. Build the server distribution (only needed once, or after server code changes)
./gradlew :ai-driver-server:installDist

# 2. Start the daemon in background
./ui start &

# 3. Verify daemon is running
./ui status

# 4. Start a KiteUI app (e.g., the example app web version)
./gradlew :example-app:viteRun &
# NOTE: User must open their browser to the viteRun URL (usually http://localhost:5173)

# 5. Wait for app to connect, then list connected apps
./ui list
```

### Checking if Already Running
```bash
# Check daemon
./ui status     # Returns "Daemon running. Connected apps: N" if up

# Check connected apps
./ui list       # Lists app IDs like "web-1 (js) - KiteUI Example"
```

## CLI Command Reference

All commands go through `./ui <command> [args] [--options]`.

Required args are positional (in order). Optional args use `--name value` syntax.
Boolean flags use just `--name` (presence = true).

### Daemon Management
```bash
./ui start                              # Start daemon (foreground, blocks)
./ui start &                            # Start daemon (background)
./ui start --port 7474 --cliPort 7475   # Custom ports
./ui status                             # "Daemon running. Connected apps: N"
./ui stop                               # Stop daemon
```

### App Discovery
```bash
./ui list                               # List all connected apps
# Output: "web-1 (js) - KiteUI Example"

./ui info web-1                         # App details
# Output: "App: KiteUI Example\nPlatform: js\nID: web-1"
```

### Snapshots — Reading the UI Tree
```bash
./ui snapshot web-1                     # Text format (human-readable)
./ui snapshot web-1 --format Json       # JSON format (machine-readable)
./ui snapshot web-1 --component path/to/element  # Scope to subtree
```

**Text snapshot output format:**
```
Page: HomePage (/)
[nav] frame
  [nav/homeLink] link (value="Home", actions=[click])
  [nav/docsLink] link (value="Documentation", actions=[click])
[content] col
  [content/title] text (value="Welcome to KiteUI")
  [content/counter] button (value="Count: 0", actions=[click])
  [content/emailField] textInput (value="", actions=[click,setValue])
```

Each line: `[componentId] type (properties)`
- `componentId` — slash-separated path from root (use this for targeting actions)
- `type` — view type (text, button, textInput, link, col, row, frame, etc.)
- `value` — current text/value of the component
- `actions` — what you can do with it (click, setValue, longClick, scroll)
- `disabled` — shown when component is not interactive
- `hidden` — shown when component is not visible

### Performing Actions
```bash
# Click a component
./ui perform web-1 click content/counter

# Set a text input's value
./ui perform web-1 setValue content/emailField hello@example.com

# Navigate to a route
./ui perform web-1 navigate docs/cheat-sheet

# Go back in navigation
./ui perform web-1 back

# Scroll a container
./ui perform web-1 scroll content/list --dy 500

# Long-click
./ui perform web-1 longClick content/item
```

**Action syntax:** `./ui perform <appId> <actionType> [actionArgs] [--optionalArgs]`

| Action | Positional Args | Optional Args |
|--------|----------------|---------------|
| `click` | `<targetId>` | — |
| `longClick` | `<targetId>` | — |
| `setValue` | `<targetId>` `<value>` | — |
| `scroll` | `<targetId>` | `--dx <float>` `--dy <float>` |
| `navigate` | `<route>` | — |
| `back` | — | — |
| `forward` | — | — |

### Waiting for Conditions
```bash
# Wait for a specific page
./ui wait web-1 --page HomePage

# Wait for a component to appear
./ui wait web-1 --component content/loginBtn

# Wait for a component to disappear
./ui wait web-1 --componentGone content/loadingSpinner

# Wait for a component to become enabled
./ui wait web-1 --enabled content/submitBtn

# Wait for any state change
./ui wait web-1 --change

# Wait for a specific URL
./ui wait web-1 --url /dashboard

# Custom timeout (default 10000ms)
./ui wait web-1 --component content/results --timeout 30000
```

Multiple conditions can be combined — all must be met simultaneously.

### Screenshots
```bash
./ui screenshot web-1                           # Auto-named file
./ui screenshot web-1 --path my-screenshot.png  # Specific path
```

### Mocking External Services
```bash
# Queue a mock file pick response (next requestFile() call returns this file)
./ui mock web-1 file /path/to/photo.jpg
./ui mock web-1 file /path/to/doc.pdf --mimeType application/pdf

# Queue a mock geolocation response
./ui mock web-1 geolocation 37.7749 -122.4194
./ui mock web-1 geolocation 37.7749 -122.4194 --accuracy 5.0
```

Mock responses are queued FIFO — the next call to the corresponding external service
method dequeues and returns the mock. When the queue is empty, falls through to the
real platform implementation (or returns null on SSR).

### Recording Interactions
```bash
./ui record web-1 start          # Start recording
# ... perform actions ...
./ui record web-1 stop           # Stop recording
./ui record web-1 export         # Export as CLI commands
./ui record web-1 export-kotlin  # Export as Kotlin test code
```

## Component IDs — How Views Get Their Path

Component IDs in snapshots are built by walking the view tree. Each segment comes from (in priority order):

1. **`debugName`** — Set via `"myName".testId - view { }` modifier
2. **`ariaDescription`** — Set via `"My Description".ariaDescription - view { }`, converted to camelCase
3. **Numeric index** — Fallback: child position among siblings (e.g., `0`, `1`, `2`)

Full path is slash-separated: `parent/child/grandchild`

**Tip:** Views with `testId` or `ariaDescription` get stable, meaningful IDs. Views without them get numeric indices that may shift when the UI changes.

## Common Workflows

### Workflow: Inspect Current Page
```bash
./ui snapshot web-1
# Read the output to understand what's on screen, what components exist, what actions are available
```

### Workflow: Click a Button and Verify
```bash
./ui perform web-1 click content/incrementBtn
./ui wait web-1 --change
./ui snapshot web-1
# Check the snapshot to verify the button click had the expected effect
```

### Workflow: Fill a Form
```bash
./ui perform web-1 setValue form/emailField user@example.com
./ui perform web-1 setValue form/passwordField secret123
./ui perform web-1 click form/submitBtn
./ui wait web-1 --page DashboardPage --timeout 15000
```

### Workflow: Navigate and Verify
```bash
./ui perform web-1 navigate docs/cheat-sheet
./ui wait web-1 --page CheatSheet
./ui snapshot web-1
```

### Workflow: Full Test Sequence
```bash
# Start fresh
./ui snapshot web-1                               # See initial state
./ui perform web-1 click nav/docsLink             # Navigate
./ui wait web-1 --change                          # Wait for navigation
./ui snapshot web-1                               # Verify new page
./ui perform web-1 back                           # Go back
./ui wait web-1 --page HomePage                   # Verify we're back
```

## Troubleshooting

### "Error: AI driver daemon is not running"
The daemon process isn't running. Start it:
```bash
./ui start --daemon &
```
If the binary doesn't exist, rebuild: `./gradlew :ai-driver-server:installDist`

### "No apps connected"
The KiteUI app isn't running or hasn't connected yet.
- Ensure the app is running (e.g., `./gradlew :example-app:viteRun` + browser open)
- The app must have `AiDriver.connect()` wired in (example app already does)
- Wait a few seconds — WebSocket connection takes a moment after app load

### "App 'web-1' not found"
The app disconnected or the ID changed. Run `./ui list` to see current app IDs.
App IDs are generated fresh on each connection (e.g., `web-1`, `web-2`).

### Stale daemon (commands fail unexpectedly)
If you rebuilt the server, the running daemon may be from an old build:
```bash
./ui stop
./gradlew :ai-driver-server:installDist
./ui start --daemon &
```

### "Action 'click' not supported on <ViewType>"
The view class doesn't declare `click` in its `accessibilityActions`. This needs to be added in the platform-specific view implementation.

### Timeout on wait
Increase the timeout or check that the condition is correct:
```bash
./ui wait web-1 --component content/myElement --timeout 30000
```

## Automated Testing — `uiTest()` and `UiTestScope`

The same `buildSnapshot()` and `dispatchAction()` primitives that power the CLI also drive
automated UI tests. Tests are written **once** in common code and run on all platforms.

### CLI ↔ Test Equivalence

| CLI Command | UiTestScope Kotlin |
|---|---|
| `./ui snapshot web-1` | `snapshot()` |
| `./ui perform web-1 click btn` | `click("btn")` |
| `./ui perform web-1 setValue email foo` | `setValue("email", "foo")` |
| `./ui perform web-1 navigate /dashboard` | `navigate("/dashboard")` |
| `./ui perform web-1 back` | `back()` |
| `./ui wait web-1 --page Dashboard` | `waitForPage("Dashboard")` |
| `./ui wait web-1 --component btn` | `waitForComponent("btn")` |
| `./ui wait web-1 --componentGone spinner` | `waitForComponentGone("spinner")` |
| `./ui wait web-1 --enabled submit` | `waitForEnabled("submit")` |

### Writing a Test

```kotlin
@Test
fun loginFlow() = uiTest(content = {
    // Build UI inline — same ViewWriter DSL as production code
    val email = Signal("")
    textInput { debugName = "email"; content bind email }
    button {
        debugName = "submit"
        text("Log In")
        onClick(frequencyCap = null) { /* handle login */ }
    }
}) {
    // UiTestScope methods match CLI commands
    setValue("email", "user@example.com")
    assertValue("email", "user@example.com")
    click("submit")
    // waitForPage("Dashboard")
}
```

### Key APIs (UiTestScope)

- **Read**: `snapshot()`, `find(id)`, `require(id)`
- **Write**: `click(id)`, `setValue(id, value)`, `navigate(route)`, `back()`
- **Wait**: `waitFor(condition)`, `waitForPage(name)`, `waitForComponent(id)`, `waitForComponentGone(id)`, `waitForEnabled(id)`
- **Assert**: `assertPage(name)`, `assertValue(id, expected)`, `assertVisible(id)`, `assertEnabled(id)`, `assertDisabled(id)`
- **Mock**: `mockExternalServices` — accessor for `MockExternalServices` when injected via config
- **Debug**: `dumpSnapshot()`

### Mocking External Services in Tests

```kotlin
@Test
fun testFileUpload() = uiTest(
    config = UiTestConfig(
        externalServices = MockExternalServices().apply {
            pendingFileResponses.add(createFileReferenceFromBytes(
                bytes = "test-content".encodeToByteArray(),
                mimeType = "text/plain",
                fileName = "test.txt"
            ))
        }
    ),
    content = { /* UI with file upload */ }
) {
    click("uploadButton")
    // The next requestFile() call returns the mock file
    val mock = mockExternalServices!!
    assert(mock.calls.any { it is MockExternalServices.Call.RequestFile })
}
```

**MockExternalServices queues**: `pendingFileResponses`, `pendingFilesResponses`,
`pendingCaptureResponses`, `pendingGeolocation`. Each dequeues FIFO when the
corresponding method is called. When empty, delegates to real impl (or returns null on SSR).

### Platform Targets

- **JVM SSR** — Fastest: pure JVM, no emulator/browser. Uses `SsrContext` directly.
- **Android** — Uses Robolectric `TestHarness`
- **JS** — Uses browser DOM `TestHarness`
- **iOS** — Uses native `TestHarness`

JVM SSR is recommended for CI since it has no platform dependencies.

### Running Tests

```bash
./gradlew :library:jvmSsrTest     # SSR (fastest)
./gradlew :library:androidUnitTest # Android/Robolectric
./gradlew :library:jsTest          # JS/browser
```

## Key Source Files

- `library/src/commonMain/.../aidriver/` — Protocol types + client + shared dispatch (`ActionDispatcherCommon.kt`)
- `library/src/commonInteractiveMain/.../aidriver/` — Shared snapshot walker
- `library/src/{android,ios,js}Main/.../aidriver/` — Platform action dispatch (with screenshot support)
- `library/src/jvmSsrMain/.../aidriver/` — SSR snapshot walker + action dispatch
- `test-utilities/src/commonMain/.../testing/` — `UiTestScope`, `UiSnapshotExtensions`, `uiTest()` expect
- `test-utilities/src/{androidMain,iosMain,jsMain,jvmSsrMain}/.../testing/` — Platform `uiTest()` actuals
- `ai-driver-server/src/main/kotlin/.../server/` — Daemon + CLI server
- `./ui` — CLI wrapper script
- `./ui-setup` — Build + start daemon script
