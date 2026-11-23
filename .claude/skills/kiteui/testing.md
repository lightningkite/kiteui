# KiteUI Testing and the AI Driver

Full docs: [docs/testing.md](../../../docs/testing.md), [docs/ai-driver.md](../../../docs/ai-driver.md).

## Two ways to drive UI, one API (`UiTestScope`)

| | `uiTest(content) { }` | `remoteUiTest(appId, startRoute) { }` |
|---|---|---|
| Runs | in-process, in `commonTest` | against a running app, through the relay on `127.0.0.1:7474` |
| Platforms | jvmSsr (fastest), Android (Robolectric), iOS sim, JS browser | JVM test code; the app can be any platform |
| Use for | CI and regression | on-device checks, agent-driven exploration |
| Navigator | none: `navigate`/`reset`/`url`/`back` throw "no navigator available" | the app's |

Dependency: `commonTest.dependencies { implementation("com.lightningkite.kiteui:test-utilities:<version>") }`.

```kotlin
@JUnitRunWith(RobolectricTestRunner::class)  // required on Android, no-op elsewhere
@RobolectricConfig
class LoginTest {
    @Test
    fun logsIn() = uiTest(content = { with(LoginPage) { render() } }) {
        setValue("email", "a@example.com")
        val logIn = findClickable("Log in").firstOrNull() ?: error("No 'Log in' button")  // by label, walked up to the button
        click(logIn.path)
        waitForText("Welcome")                          // anything async needs a waitFor…
    }
}
```

To test navigation in `uiTest`, build `PageNavigator { AutoRoutes }` in `content`, show it with `navigatorView(navigator)`, and call the navigator directly.

## Scope API

- Inspect: `snapshot(target)`, `interactiveSnapshot()`, `find(q)` (text), `findAll(q)` / `findClickable(q)` / `findWithAction(q, action)` (→ `FindResult(path, name, type, value, actions)`), `logs()`, `screenshot()` (base64; throws on jvmSsr), `saveScreenshot(file)` (JVM).
- Act: `click`, `longClick`, `setValue`, `toggle`, `select`, `submit`, `scrollIntoView`, `getDragData`/`drop`, `navigate`, `reset`, `back`, `raw(cmd)`.
- Mock: `mockFile(bytes, mime, name)`, `mockFileCancel()`, `mockGeolocation(lat, lon)`, `mockCalls()`, `mockClearCalls()`; or pass `uiTest(mockExternalServices = MockExternalServices())`.
- Assert/wait: `assertValue`, `assertVisible`, `assertNotVisible`, `assertTextVisible`, `assertIdExists`, `assertEnabled`/`assertDisabled` (clickable ancestor or not), `waitFor { }`, `waitForText`, `waitForId` (5 s default).

## Paths

`debugName = "email"` makes a view addressable as `email` (depth-first search).  `form/email` = child named `email` of `form`; `form/0` = first child; `..` = parent.  Name everything a test touches.

## Gotchas

- `click` returns once the action **starts**; await results with `waitForText`/`waitForId`.
- `onClick`/`Action` default to a 500 ms `frequencyCap` and ignore clicks while running: a second quick click on the same button does nothing.
- Password inputs (`KeyboardHints.password`/`newPassword`) report `••••••••` as their value.
- `find`/`findClickable` skip hidden views unless `includeHidden = true`.
- There is no `scroll` command and `getAlignment()` always fails (no view registers it).
- Relay: an app-id **prefix** resolves to the alphabetically last matching connection, which may be a dead tab: close stale tabs or use the full id from `kiteui-drive ls`.  Ids get new random letters on every launch/reload.
- Driving infinite scroll: `find`/`click` a "Load more" control or `scrollIntoView` the last row; there's no scroll-by-offset.

## Relay / CLI

- Install: `./gradlew aiDriverInstall` (projects with the KiteUI plugin) or `./gradlew :ai-driver-server:installDriver` (KiteUI repo); puts `kiteui-drive` in `~/.kiteui/bin`.
- App: `if (Platform.isDevelopment) AiDriver.connect(appName = "myapp", rootView = { root }, navigator = { navigator })`.  `isDevelopment`: Android debuggable, iOS debug binary, web on localhost, JVM with `KITEUI_DEVELOPMENT=true`.
- CLI: `kiteui-drive ls`, `kiteui-drive myapp root snapshot --interactive`, `kiteui-drive myapp email setValue "a b"`, `kiteui-drive myapp root findClickable "Save"`.
- Raw: POST tab-separated `appId\tpath\taction\targs…` to `http://127.0.0.1:7474`.  MCP: JSON-RPC at `/mcp`, tools `ls` and `drive`.

## Web: blank page

The Kotlin/JS bundle is named `<rootProject.name>-<module>.mjs` (e.g. `kiteui-example-app.mjs`).  If `index.html`'s `<script src>` names another file (common after renaming a project made from a template), the page is blank with a 404 in the console.

## Local tokens in tests

`PersistentProperty` values are stored JSON-encoded: a `String` token is `"\"abc\""` in `localStorage`.  Inject the JSON form when seeding storage from a harness or headless browser.
