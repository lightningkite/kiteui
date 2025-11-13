# JavaScript Snapshot Testing

## Overview

JavaScript snapshot testing in KiteUI uses `html2canvas` to capture DOM elements as PNG images. Due to the asynchronous nature of JavaScript and browser APIs, the snapshot functionality works differently than on Android.

## How It Works

### Android (Roborazzi)
- Uses Roborazzi library for synchronous screenshot capture
- Screenshots are saved directly to `library/build/outputs/roborazzi/`
- Tests can assert on screenshot data immediately

### JavaScript (html2canvas)
- Uses html2canvas library for asynchronous DOM-to-image conversion
- Screenshots are captured in the background
- Visual output appears in the Karma test browser for manual inspection and download

## Usage

### Basic Test with Screenshots

```kotlin
@Test
fun testMyComponent() = withTestHarness { harness ->
    val root = harness.render {
        col {
            text("Hello, World!")
            button { text("Click Me") }
        }
    }

    // Trigger screenshot capture (async in JS)
    harness.screenshot("my-component-test")

    // Note: In JS, the screenshot will appear in the browser window
    // You can download it manually or view it inline
}
```

## Limitations

Due to JavaScript's single-threaded, non-blocking nature:

1. **Async Only**: Screenshots are captured asynchronously
2. **Manual Download**: Screenshots appear in the Karma browser window with download links
3. **No Direct ByteArray Return**: The JS implementation returns `null` immediately since it can't block
4. **Browser Required**: Tests must run in a real browser environment (Karma with Chrome/Firefox)

## Viewing Screenshots

When running `gradle :library:jsTest`:

1. Karma will open a browser window (Chrome Headless or Firefox)
2. Screenshots will be rendered as canvas elements in the browser
3. Each screenshot has a download link
4. Look for console messages like:
   ```
   Starting screenshot capture for: my-test
   ✓ Screenshot 'my-test' captured successfully (12345 bytes)
   ```

## Future Improvements

Potential enhancements:

1. **Suspend Functions**: Add suspend version of screenshot functions for better async support
2. **Automated Saving**: Use FileSystem API or Node.js backend to save screenshots automatically
3. **Visual Regression**: Integrate with Percy, Chromatic, or similar services
4. **Playwright Integration**: Use Playwright for more robust screenshot testing

## Dependencies

- `html2canvas` v1.4.1 (added via npm in build.gradle.kts)
- Runs in Karma test environment with Chrome Headless and Firefox

## Example Output

When a screenshot is captured, you'll see:

```
Starting screenshot capture for: button-test
✓ Screenshot 'button-test' captured successfully (15234 bytes)
Screenshot canvas and download link added to page
You can right-click the canvas or use the download link to save the image
```

The browser will show:
- A green-bordered canvas with the rendered screenshot
- A download link to save the PNG file
