# AI driver

The AI driver lets a CLI, a test, or an AI agent operate a running KiteUI app on any platform by plain-text commands: read the view tree as text, click, type, navigate, take screenshots, mock the file picker.  The app connects out to a local relay server; clients send it HTTP POSTs.

```
App ──WebSocket──► relay (127.0.0.1:7474) ◄──HTTP POST── kiteui-drive CLI, remoteUiTest, MCP client
```

In-process tests (`uiTest`, see [Testing](testing.md)) use the same commands without the relay.

## Minimal example

Connect the app, in your shared `app()`:

```kotlin
fun ViewWriter.app(navigator: PageNavigator) {
    val root = produceExactlyOneElement {
        navigatorView(navigator)  // or your appNav { } shell
    }
    if (Platform.isDevelopment) {
        AiDriver.connect(
            appName = "myapp",
            rootView = { root },
            navigator = { navigator },
        )
    }
}
```

Then drive it:

```bash
kiteui-drive ls                                      # myapp-web-QZK
kiteui-drive myapp root snapshot --interactive
kiteui-drive myapp email setValue "a@example.com"
kiteui-drive myapp root findClickable "Log in"       # logIn: logIn: Button [click]
kiteui-drive myapp logIn click
```

## Setup

Install the relay and the `kiteui-drive` CLI into `~/.kiteui/`, then add `~/.kiteui/bin` to your `PATH`:

```bash
./gradlew aiDriverInstall                     # in a project using the com.lightningkite.kiteui plugin
./gradlew :ai-driver-server:installDriver     # inside the KiteUI repo
```

`kiteui-drive` starts the relay on first use; `kiteui-drive start` / `stop` control it, and `KITEUI_DRIVER_PORT` changes the port.  The plugin also adds `aiDriverStart` and `aiDriverStop` tasks.  A JVM app starts the relay itself if it's installed.  For Android devices the relay runs `adb reverse`, so `localhost:7474` on the device reaches it.

`Platform.isDevelopment` is true for debuggable Android builds, iOS debug binaries, web pages served from `localhost`/`127.0.0.1`/`0.0.0.0`, and JVM processes with `KITEUI_DEVELOPMENT=true`.  The relay binds to loopback only and trusts whatever connects, so never connect release builds.

Each connection is named `<appName>-<platform>-<3 random letters>`.  The letters are chosen per `connect` call, so they survive reconnects but change on every launch or page reload.

## Paths

Commands target a view by a `/`-separated path:

| Path | Resolves to |
|---|---|
| `root` | the root view |
| `email` | the first view, searched depth-first, with `debugName = "email"` |
| `form/email` | `form`, then its direct child named `email` |
| `form/0` | `form`, then its first child |
| `email/..` | `email`'s parent; `..` can appear anywhere |

```kotlin
col {
    debugName = "form"
    textInput { debugName = "email"; content bind email }
    button { debugName = "submit"; text("Submit"); onClick { } }
}
```

Name every view a script or test touches.  Indexes shift when siblings change.

## Snapshots

`snapshot` prints the subtree under its target, one view per line:

```
form: form: RowOrCol
  email: email: TextInput = "a@example.com" [setValue, submit]
  submit: submit: Button [click]
    submit/0: 0: TextView = "Submit"
```

Each line is `path: name: Type = "value" [actions]`.  The path works as given in any command; the name is the `debugName` or child index; the value appears for text and inputs; the actions are those beyond the ones every view has.

| Option | Effect |
|---|---|
| `--interactive` | Leave out views with no name, value, or action (their children still show) |
| `--hidden` | Include hidden views, flagged `(hidden)` or `(invisible)` |
| `--themes` | Show each view's theme derivation |

## Finding views

```bash
kiteui-drive myapp root find "email"            # by debugName, value, type, or action, case-insensitive
kiteui-drive myapp root find "TextInput"
kiteui-drive myapp root find "toggle"           # everything with a toggle action
kiteui-drive myapp root findClickable "Log in"  # each match's nearest clickable ancestor
```

`find "Log in"` matches the `TextView` inside a button; `findClickable` walks up to the button.  Both skip hidden views unless given `--hidden`.

## Commands

View commands are `<path> <action> [args]`:

| View | Actions | Value |
|---|---|---|
| Any | `snapshot`, `screenshot`, `find`, `findClickable`, `scrollIntoView` | |
| `Button` | `click` (if it has an action), `longClick` (if it has a secondary action) | |
| `Link` | `click` | |
| `ExternalLink` | `click`: reports the URL, doesn't open it | |
| `MenuButton` | `click`: opens the menu and returns its snapshot | |
| `TextInput`, `TextArea`, `FormattedTextInput`, `AutoCompleteTextField` | `setValue <text>`, `submit` (if it has an action) | the text |
| `NumberInput` | `setValue <number>` (clamped to its range), `submit` | the number |
| `LocalDateField`, `LocalTimeField`, `LocalDateTimeField` | `setValue <ISO value>`, `submit` | ISO value |
| `Checkbox`, `Switch`, `ToggleButton` | `toggle`, `setValue true\|false` | `true`/`false` |
| `RadioButton`, `RadioToggleButton` | `select` | `true`/`false` |
| `Slider` | `setValue <number>` (clamped) | the number |
| `Select` | `setValue <option text>` | selected option |
| A dialog's background, an open menu's backdrop | `dismiss` | |
| A view with `dragData` / a drop target | `getDragData` / `drop <base64>` | |

App commands take no path:

| Command | Does |
|---|---|
| `navigate <route>` | Push the page at a URL path, such as `/users/42` |
| `reset <route>` | Replace the whole stack with that page |
| `url` | The URL the web would show: the topmost page that has a route |
| `back back` | Close the top dialog, else go back |
| `logs [count]` | Recent log lines (default 50) |
| `mock file <base64> <mime> <name>` | Queue a file for the next `requestFile`/`requestFiles`/`requestCapture…`.  `requestFiles` takes every queued file. |
| `mock fileNull` | Queue a cancellation |
| `mock geolocation <lat> <lon> [accuracy]` | Queue a location for the next `getCurrentPosition` |
| `mock calls` / `mock clearCalls` | Show or clear the recorded external-service calls |

The first `mock` command replaces the app's external services with a `MockExternalServices` that answers from these queues and passes everything else through.

Successful commands answer `OK` or the requested text; failures answer `ERROR: <Exception>: <message>`, such as `view 'x' not found`, `Unknown action 'y' on Button. Available: ...`, or `no navigator available`.

## Protocol

Every request is one HTTP POST to `http://127.0.0.1:7474` whose body is tab-separated: `<appId>\t<path>\t<action>\t<arg>...`.  Tabs, not spaces, so values keep their spaces; `kiteui-drive` joins its arguments with tabs.  `ls` lists connected apps.  An app id may be a prefix: `myapp` matches `myapp-web-QZK`.  A command waits up to 30 seconds for the app.

```bash
printf 'myapp\temail\tsetValue\thello world' | curl -s -X POST http://127.0.0.1:7474 --data-binary @-
```

The relay is also an MCP server at `http://127.0.0.1:7474/mcp` (JSON-RPC), with two tools: `ls`, and `drive`, which takes `app` and `commands` (a list of argument lists, run in order, stopping at the first error; `["delay", "<ms>"]` pauses).  Screenshots taken through `drive` are saved to a file in the relay's temp directory and the path is returned.

From Kotlin, `remoteUiTest(appId) { ... }` gives the [`UiTestScope`](testing.md) API over the relay.

## Screenshots

`screenshot` returns a base64 PNG of the target view.  Android draws the view to a bitmap, iOS renders its layer, the web uses `modern-screenshot`, and Compose desktop crops the window's last frame.  JVM server-side rendering has no pixels and answers an error.

## Gotchas

- **Prefix matches pick one app.**  With several matching connections (a stale browser tab, say), the alphabetically last id wins, which may be a tab that no longer responds.  Close old tabs or use the full id from `ls`.
- **Password fields are redacted.**  Inputs with `KeyboardHints.password` or `newPassword` report `••••••••` when filled, `""` when empty.
- **`setValue` sets the bound value** directly, as typing would end up; it doesn't send key events.
- **`back` and `navigate` use the `PageNavigator`**, not browser history.
- **Everything shown is visible to the client.**  The relay doesn't authenticate; it relies on binding to loopback.

## See also

- [Testing](testing.md): `uiTest` and `remoteUiTest`
- Source: `library/src/commonMain/.../views/AiDriver.kt` (commands), `DriverSupport.kt` (paths, snapshots), `views/direct/DriverOverrides.kt` (per-view actions), `ai-driver-server/` (relay)
