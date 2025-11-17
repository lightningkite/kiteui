# Web Snapshot Testing

The web testing framework captures HTML snapshots with all styles embedded for visual inspection and comparison.

## How to Use

### 1. Start the Snapshot Server

In one terminal:

```bash
node local/snapshot-server.js
```

You should see:
```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📸 KiteUI Snapshot Server
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🌐 Listening on http://localhost:3001
📁 Saving snapshots to: /Users/jivie/Projects/kiteui/local/screenshots/js

Ready to receive snapshots from tests...
```

### 2. Run Tests

In another terminal:

```bash
./gradlew :library:jsTest
```

As tests run, you'll see snapshots being saved in the server terminal:

```
✅ Saved: test-snapshot.html (5437 chars)
✅ Saved: row-snapshot.html (4070 chars)
✅ Saved: complex-layout.html (15265 chars)
```

### 3. View Snapshots

Open the HTML files in any browser:

```bash
open local/screenshots/js/test-snapshot.html
```

The files contain:
- Complete HTML document with `<!DOCTYPE>` and proper structure
- All computed styles embedded as inline CSS
- No external dependencies - just open and view

## Snapshot Files

Snapshots are saved to: `library/local/screenshots/js/`

Each snapshot is a standalone HTML file that can be:
- Opened directly in any browser
- Committed to version control for regression testing
- Compared programmatically with previous versions
- Inspected with browser dev tools

## Writing Tests

```kotlin
@Test
fun testMyComponent() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("Hello")
            text("World")
        }
    }

    // Captures HTML snapshot with embedded styles
    val snapshot = harness.screenshot("my-component")
    assertNotNull(snapshot)

    // Snapshot is automatically saved to:
    // library/local/screenshots/js/my-component.html
}
```

## Notes

- The server must be running before tests, or snapshots won't be saved to disk
- Tests will still pass without the server - snapshots just won't be saved
- Each browser (Chrome, Firefox) generates its own snapshot
- Later snapshots overwrite earlier ones (last browser wins)
