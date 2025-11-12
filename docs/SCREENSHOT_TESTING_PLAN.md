# Screenshot Testing Implementation Plan

## Overview

Adding screenshot capabilities to the test harness for visual regression testing and documentation.

## Platform-Specific Approaches

### Android (Robolectric)

**Best Option: Roborazzi**
- Modern screenshot testing library built for Robolectric
- Native graphics support via Robolectric Native Graphics
- Widely adopted in 2024 (presented at DroidKaigi 2024)
- Fast - runs on JVM without emulators

**Implementation**:
```kotlin
// Add dependency
testImplementation("io.github.takahirom.roborazzi:roborazzi:1.29.0")

// Test configuration
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])

// Capture screenshot
view.captureRoboImage()
```

**Alternative**: PixelCopy API
- Built into Robolectric
- Lower-level API
- Requires more manual setup

### iOS

**Best Option: UIGraphics Rendering**
- Built into UIKit, no external dependencies
- Use `UIGraphicsImageRenderer` (iOS 10+)
- Render view hierarchy to PNG

**Implementation**:
```kotlin
fun UIView.captureScreenshot(): ByteArray {
    val renderer = UIGraphicsImageRenderer(this.bounds.size)
    val image = renderer.imageWithActions { context ->
        this.drawHierarchy(in = this.bounds, afterScreenUpdates = true)
    }
    return UIImagePNGRepresentation(image)!!
}
```

**Limitations**:
- Won't capture video players (black frames)
- OpenGL views may not render
- For tests, this is acceptable

### JS/Web

**Best Option: DOM Rendering to Canvas**
- Native browser APIs
- No external dependencies needed
- Direct canvas export

**Implementation**:
```kotlin
fun HTMLElement.captureScreenshot(): ByteArray {
    val canvas = document.createElement("canvas") as HTMLCanvasElement
    canvas.width = this.offsetWidth
    canvas.height = this.offsetHeight

    val ctx = canvas.getContext("2d") as CanvasRenderingContext2D
    // Use html2canvas or similar for DOM->Canvas

    return canvas.toDataURL("image/png").toByteArray()
}
```

**Alternatives**:
- html2canvas library (experimental, not recommended for production)
- html-to-image (modern, faster alternative)
- Server-side rendering (Puppeteer, Playwright) - too heavy for unit tests

**Note**: For JS tests, we may want to use native canvas rendering directly rather than external libraries.

## Proposed API Design

### Test Harness API
```kotlin
// In TestHarness
expect class TestHarness {
    fun screenshot(name: String = "screenshot"): ByteArray
    fun screenshotView(view: RView, name: String = "screenshot"): ByteArray
}

// Usage in tests
@Test
fun testButtonAppearance() = withTestHarness { harness ->
    val root = harness.render {
        button {
            debugName = "my-button"
            text("Click Me")
        }
    }

    // Capture screenshot
    harness.screenshot("button-initial")

    // Or screenshot specific view
    val button = root.findByDebugName("my-button")!!
    harness.screenshotView(button, "button-detail")
}
```

### Storage Strategy

**Development**: Save to `./local/screenshots/{platform}/{testName}/{screenshotName}.png`
- Git-ignored directory
- Easy to review during development

**CI/CD**: Compare against baseline images
- Store baselines in `library/src/test/resources/screenshots/`
- Generate diff images on mismatch
- Fail build if differences exceed threshold

## Implementation Phases

### Phase 1: Basic Screenshot Capture (MVP)
- [ ] Add screenshot method to TestHarness interface
- [ ] Implement Android version with Roborazzi
- [ ] Implement iOS version with UIGraphicsImageRenderer
- [ ] Implement JS version with canvas rendering
- [ ] Save to local filesystem during tests

### Phase 2: Comparison & Verification
- [ ] Store baseline screenshots
- [ ] Implement pixel-by-pixel comparison
- [ ] Generate diff images
- [ ] Add tolerance threshold for minor differences

### Phase 3: Visual Regression Testing
- [ ] Integrate with CI/CD
- [ ] Automatic baseline updates
- [ ] Screenshot review workflow
- [ ] Visual diff reports

## Challenges & Solutions

### Challenge 1: Platform Differences
Different platforms render UI differently (fonts, anti-aliasing, spacing).

**Solution**:
- Separate baseline images per platform
- Higher tolerance thresholds
- Focus on layout/structure rather than pixel-perfect matching

### Challenge 2: File Size
Screenshots can be large, especially for full screens.

**Solution**:
- Use PNG with compression
- Only screenshot specific views, not full screen
- Git LFS for baseline storage if needed

### Challenge 3: JS Test Environment
Browser-based tests in Karma may have security restrictions.

**Solution**:
- Use Node.js canvas implementation for headless tests
- Or use browser's built-in screenshot APIs
- May need to write to test report directory

### Challenge 4: Roborazzi Dependency
Adds new dependency to the project.

**Solution**:
- Start with basic PixelCopy if dependency is a concern
- Roborazzi is well-maintained and widely used
- Optional: make it a test-only dependency

## Recommended Approach

**Start Simple**:
1. Implement basic screenshot capture on Android (Roborazzi)
2. Implement iOS with UIGraphics
3. Implement JS with canvas (or defer if complex)
4. Save screenshots to local directory
5. Manual visual inspection initially
6. Add automated comparison later

**Benefits**:
- Visual documentation of components
- Catch visual regressions
- Useful for design reviews
- Platform-specific rendering differences visible

## Dependencies Needed

### Android
```gradle
testImplementation("io.github.takahirom.roborazzi:roborazzi:1.29.0")
testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.29.0")
```

### iOS
- No dependencies (uses UIKit)

### JS
- Consider: `canvas` package for Node.js (if headless)
- Or use browser APIs directly

## Next Steps

1. Add Roborazzi dependency to build.gradle
2. Implement `screenshot()` method in TestHarness.android.kt
3. Test with a simple component
4. Expand to other platforms
5. Create screenshot viewer/comparison tool
