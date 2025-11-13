# Screenshot Testing - Investigation Findings

## Summary

I implemented screenshot APIs across all platforms and discovered important platform-specific requirements.

## What Was Implemented ✅

### API Design
```kotlin
// In TestHarness (expect/actual)
fun screenshot(name: String = "screenshot"): ByteArray?
fun screenshotView(view: RView, name: String = "screenshot"): ByteArray?
```

### Platform Implementations

**Android**: Added Roborazzi dependency, implemented with `captureRoboImage()`
**iOS**: Implemented with `UIGraphicsImageRenderer` and `drawViewHierarchyInRect()`
**JS**: Stub (returns null)
**JVM SSR**: Stub (returns null)

## Key Findings

### Android - Robolectric Rendering Issue

**Problem**: Basic Canvas rendering produces blank screenshots
- Created 320x470 PNG files
- Files exist but are completely blank/transparent
- Robolectric's default rendering doesn't actually draw pixels

**Root Cause**: Robolectric needs Native Graphics Mode
- Must use `@GraphicsMode(GraphicsMode.Mode.NATIVE)` annotation
- Requires Roborazzi library for actual pixel rendering
- Need Roborazzi Gradle plugin configuration

**Solution Attempted**:
```kotlin
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)  // Required!
class ScreenshotTest {
    // ...
    view.captureRoboImage(filePath.absolutePath)
}
```

**Status**: ⚠️ Partially working
- Dependency added: `roborazzi:1.29.0`
- API integrated
- Tests compile
- Screenshot capture returns null (needs Gradle plugin setup)

**Next Steps for Android**:
1. Add Roborazzi Gradle plugin to root build.gradle
2. Configure recording/verification tasks
3. May need to use `recordRoborazziDebug` gradle task
4. Alternative: Use Screenshot Testing Library or Paparazzi

### iOS - Implementation Works, Path Issue

**Implementation**:
```kotlin
private fun captureViewScreenshot(view: UIView): ByteArray? {
    val renderer = UIGraphicsImageRenderer(view.bounds)
    val image = renderer.imageWithActions { context ->
        view.drawViewHierarchyInRect(view.bounds, afterScreenUpdates = true)
    }
    val pngData = UIImagePNGRepresentation(image)
    return pngData?.toByteArray()
}
```

**Status**: ✅ API works, ⚠️ file saving issue
- Screenshot capture succeeds
- Returns valid ByteArray
- File path resolution differs in iOS test environment
- `NSFileManager.currentDirectoryPath` may not be project root

**Solution for iOS**:
```kotlin
// Use absolute path or environment variable
val projectDir = ProcessInfo.processInfo.environment["PROJECT_DIR"]
val screenshotPath = "$projectDir/library/local/screenshots/ios/$name.png"
```

Or simpler: Just return the ByteArray and let tests save them

### JS - Not Implemented

**Approach**: Canvas API or html2canvas
```javascript
// Potential implementation
const canvas = document.createElement('canvas');
const ctx = canvas.getContext('2d');
// Draw DOM element to canvas
// Convert to PNG via canvas.toDataURL()
```

**Challenges**:
- Karma test environment may have security restrictions
- html2canvas is "experimental" according to author
- May need Node.js canvas package for headless tests

**Recommendation**: Defer JS screenshots until there's a clear need

## Recommendations

### For Android Screenshots

**Option 1: Roborazzi (Recommended)**
- Modern, well-maintained
- Native graphics support
- Requires Gradle plugin setup
- Steps:
  1. Add plugin to root build.gradle.kts
  2. Apply to library module
  3. Use `./gradlew recordRoborazziDebug` tasks

**Option 2: Paparazzi**
- Snapshot testing library from Square/Cash App
- Uses Layoutlib (Android's layout rendering engine)
- No Robolectric needed
- May be simpler setup

**Option 3: Android Screenshot Testing Library**
- Facebook's screenshot testing
- Record/verify workflow
- Mature but less active maintenance

### For iOS Screenshots

**Fix File Saving**:
```kotlin
// Option 1: Use test output directory
val testOutputDir = NSTemporaryDirectory()
val screenshotPath = "$testOutputDir/screenshots/$name.png"

// Option 2: Return ByteArray, test saves it
@Test
fun testScreenshot() {
    val bytes = harness.screenshot()
    File("library/local/screenshots/ios/test.png").writeBytes(bytes!!)
}
```

### General Screenshot Strategy

**Pragmatic Approach**:
1. **Android**: Complete Roborazzi setup properly
   - Add Gradle plugin
   - Use record/verify workflow
   - Store baselines in `src/test/resources/roborazzi/`

2. **iOS**: Fix file path, use NSTemporaryDirectory or test-controlled saving

3. **JS**: Skip for now unless specifically needed

4. **Cross-platform**: Accept that screenshots will differ
   - Different rendering engines
   - Different fonts/anti-aliasing
   - Focus on layout/structure, not pixel-perfect matching

## Files Modified

- `TestHarness.kt` - Added screenshot API methods
- `TestHarness.android.kt` - Roborazzi implementation
- `TestHarness.ios.kt` - UIGraphicsImageRenderer implementation
- `TestHarness.js.kt` - Stub implementation
- `TestHarness.jvmSsr.kt` - Stub implementation
- `ScreenshotTest.kt` - Test files for Android & iOS
- `build.gradle.kts` - Added Roborazzi dependency

## Conclusion

**Screenshot testing is definitely possible** on Android and iOS!

The implementation is ~80% complete:
- ✅ API design is solid
- ✅ iOS works (just needs path fix)
- ⚠️ Android needs proper Roborazzi configuration
- ❌ JS deferred

**Estimated effort to complete**:
- Android: 30-60 minutes (Gradle plugin setup)
- iOS: 15 minutes (fix file path)
- **Total**: 1-2 hours to fully working screenshots

**Value**: HIGH
- Visual regression testing
- Component documentation
- Bug investigation
- Design review

**Recommendation**: Complete the Roborazzi setup for Android, it's a well-supported solution that many teams use successfully in production.
