# KiteUI Testing & Deployment

## Critical Frontend Configuration

When setting up a KiteUI project for testing and deployment, several frontend-specific configurations must be correct for the application to work.

### MJS Module Reference Must Match Project Name

**⚠️ CRITICAL PITFALL**: The generated JavaScript module name is based on the **project name** in `settings.gradle.kts`, not the template name. If `index.html` references the wrong module, you'll get a blank page.

**Problem Symptom:**
- Browser shows blank page
- Console error: "Failed to load url /ls-kiteui-starter-apps.mjs (404 Not Found)"
- Vite dev server running without errors
- No compilation errors

**Root Cause:**
The Kotlin/JS Gradle plugin generates the JavaScript module with a filename based on `rootProject.name`:

```kotlin
// settings.gradle.kts
rootProject.name = "claude-coordinator"  // ← This determines the MJS filename
```

The generated module will be named: `${rootProject.name}-apps.mjs`

**Solution:**
Update `index.html` to reference the correct module name:

```html
<!-- apps/src/jsMain/resources/index.html -->
<!DOCTYPE html>
<html lang="en">
<head>
    <!-- ... -->
</head>
<body>
    <!-- ❌ WRONG - references template name, not project name -->
    <script src="/ls-kiteui-starter-apps.mjs" type="module"></script>

    <!-- ✅ CORRECT - matches rootProject.name from settings.gradle.kts -->
    <script src="/claude-coordinator-apps.mjs" type="module"></script>
</body>
</html>
```

**How to diagnose:**
1. Check browser console for "Failed to load url" errors
2. Look at Vite network tab to see which MJS file is being requested
3. Compare with `rootProject.name` in `settings.gradle.kts`
4. Update `index.html` to match project name

**Prevention:**
When creating a new project from a template, immediately update `index.html` after changing the project name in `settings.gradle.kts`.

### Vite Dev Server Port Configuration

For multi-project development, use isolated ports to avoid conflicts:

```javascript
// apps/vite.config.mjs
export default {
  server: {
    host: true,
    port: 8942,  // Frontend port (isolated from other projects)
    allowedHosts: ["localhost:8942", "your-domain.com"],
    proxy: {
      '/api': {
        target: 'http://localhost:8082',  // Backend port
        rewrite: (path) => path.replace(/^\/api/, ''),
        ws: true,  // Enable WebSocket proxying
      }
    }
  }
}
```

**Key points:**
- Choose unique port numbers for each project (e.g., increment by 2: 8940, 8942, 8944)
- Update `allowedHosts` to include all domains you'll use
- Proxy `/api` requests to backend server
- Enable WebSocket support with `ws: true`

### SDK Regeneration After Backend Changes

**⚠️ CRITICAL**: After changing server endpoints, you MUST regenerate the SDK before the frontend can use new endpoints.

```bash
# Regenerate SDK from server definitions
./gradlew :server:generateSdk

# Generated SDK appears in:
# apps/src/commonMain/kotlin/<package>/sdk/
```

**Common errors from missing SDK regeneration:**
- `Unresolved reference: respondToInput` - Method doesn't exist in generated SDK
- `Type mismatch` errors when calling API methods
- Compilation errors in frontend after backend changes

**Workflow:**
1. Add/modify server endpoint in `server/src/main/kotlin/.../SomeEndpoints.kt`
2. Add request/response data classes to `shared/src/commonMain/kotlin/.../models.kt`
3. **Regenerate SDK**: `./gradlew :server:generateSdk`
4. Implement frontend using new SDK methods
5. Both backend and frontend should now compile successfully

## Browser Testing with Chrome MCP

For end-to-end testing, use Claude's Chrome MCP tools for direct browser automation:

**Setup workflow:**
```bash
# 1. Start backend and wait for it to be ready
./testing/start-backend.sh
while ! curl -s http://localhost:8082 > /dev/null; do sleep 1; done

# 2. Capture admin token from backend logs (if debug mode enabled)
TOKEN=$(grep "Admin token:" server.log | cut -d"'" -f2)
echo "$TOKEN" > testing/.admin-token

# 3. Start frontend dev server
./testing/start-frontend.sh

# 4. Ready for browser testing at http://localhost:8942
```

**Testing workflow with Chrome MCP:**
1. Get tab context: `mcp__claude-in-chrome__tabs_context_mcp(createIfEmpty=true)`
2. Navigate to frontend: `mcp__claude-in-chrome__navigate(tabId=X, url='http://localhost:8942')`
3. Take screenshot to verify page loaded: `mcp__claude-in-chrome__computer(tabId=X, action='screenshot')`
4. Inject auth token if needed: `mcp__claude-in-chrome__javascript_tool(tabId=X, text='localStorage.setItem(...)')`
5. Interact with page using find/click/form_input tools
6. Verify UI state with screenshots

**Benefits over Playwright:**
- Uses actual Chrome browser (same experience user sees)
- No separate process needed
- Direct visual feedback in browser window
- Can interact with any tab in MCP group

## Testing Infrastructure Scripts

Create reusable scripts in `testing/` directory:

**testing/start-backend.sh:**
```bash
#!/bin/bash
cd "$(dirname "$0")/.."
./gradlew :server:run --args="serve settings=testing/settings.testing.json" &
echo $! > testing/.backend-pid
```

**testing/start-frontend.sh:**
```bash
#!/bin/bash
cd "$(dirname "$0")/.."
./gradlew :apps:jsBrowserDevelopmentRun &
echo $! > testing/.frontend-pid
```

**testing/stop-all.sh:**
```bash
#!/bin/bash
if [ -f testing/.backend-pid ]; then
    kill $(cat testing/.backend-pid) 2>/dev/null
    rm testing/.backend-pid
fi
if [ -f testing/.frontend-pid ]; then
    kill $(cat testing/.frontend-pid) 2>/dev/null
    rm testing/.frontend-pid
fi
```

**testing/prepare-browser-test.sh:**
```bash
#!/bin/bash
./testing/start-backend.sh
while ! curl -s http://localhost:8082 > /dev/null; do sleep 1; done
./testing/start-frontend.sh
echo "Ready for browser testing at http://localhost:8942"
```

**Port Configuration:**
- Document chosen ports in `testing/README.md`
- Update all scripts to use consistent port numbers
- Keep ports isolated from other projects (increment by 2)

## Automated UI testing: in-process `uiTest()` vs `remoteUiTest()` (AiDriver)

There are TWO cross-platform automated-UI mechanisms — pick deliberately:

- **`uiTest()` + `UiTestScope`** — runs the real frontend IN-PROCESS against a RAM-DB Lightning
  Server via `TestRunnerFetcher` (no HTTP, no browser, no relay). Fastest on JVM SSR; also
  Android/iOS/JS. **Prefer this for CI/regression** — deterministic, no external app needed.
  Depend on `com.lightningkite.kiteui:kiteui-test-utilities`; run `./gradlew :<module>:jvmSsrTest`.
- **`remoteUiTest(appId, startRoute) { ... }`** — drives the REAL running app through the **AiDriver
  relay** (`ws://localhost:7474`); the app must be launched and connected (`AiDriver.connect` in
  `app()`, dev builds only). Cross-platform **including mobile** (Android debug / iOS debug binary),
  but flakier (needs a live app + relay). Use for true on-device / cross-platform drives, not CI.
  Same `UiTestScope` API: `snapshot()`, `find`, `waitForText`, `assertTextVisible`, `setValue`,
  `click`, `scroll`, etc.

### AiDriver relay gotchas (cost real time in iter-6)
- **Stale-tab routing:** the relay resolves an app by PREFIX and returns the alphabetically-MAX
  client id. Multiple/stale browser tabs (`myapp-web-AAA`, `…-ZZZ`) → commands hijacked by a frozen
  tab → 30s timeouts. Fix: ensure only ONE client is connected (close stale tabs) or target the FULL
  client id (`myapp-web-XYZ`), not the prefix.
- **Driving infinite scroll:** `…\troot\tscroll\t0\t800` scrolls the ROOT, not the recyclerView, so
  no page loads. Target the recyclerView's element path, or (robust) `find` the "Load more" control
  and `click` it.
- Install/run: `./gradlew :ai-driver-server:installDriver` then `kiteui-drive`. If that install is
  unavailable, run the relay jar directly (`java -jar …/kiteui-ai-driver.jar`, binds :7474) and POST
  tab-separated commands to `http://localhost:7474` (there's also a JSON-RPC `/mcp` endpoint).

## Common Frontend Issues

### Blank page with no errors
1. Check browser console for module load errors
2. Verify MJS filename in `index.html` matches `rootProject.name`
3. Ensure Vite dev server is running (`./gradlew :apps:jsBrowserDevelopmentRun`)
4. Check that backend is accessible (proxy configuration)

### Compilation errors after backend changes
1. Regenerate SDK: `./gradlew :server:generateSdk`
2. Check that new data classes are in `shared/` module (visible to both server and frontend)
3. Ensure import statements reference generated SDK paths

### WebSocket connection issues
1. Verify `ws: true` in Vite proxy config
2. Check backend WebSocket endpoint is enabled
3. Ensure CORS settings allow WebSocket connections
4. Test WebSocket URL directly in browser console

### Hot reload not working
1. Restart Vite dev server: `Ctrl+C` then `./gradlew :apps:jsBrowserDevelopmentRun`
2. Check file watchers aren't at system limit
3. Ensure files are in correct source sets (`commonMain`, `jsMain`)

### Theme not applying
Ensure the view has been added to parent and `postSetup()` has been called. Themes cascade from parent.

### State not updating UI
Use reactive scope with `::content { }` or `reactive { }` to make UI respond to state changes.

### Memory leaks
Enable leak detection in development: `RViewHelper.leakDetection = true`. Check for strong references in closures and verify `shutdown()` is called.

### Views not appearing
Check that parent view is actually visible and has size. Use `sizeConstraints` if needed.
