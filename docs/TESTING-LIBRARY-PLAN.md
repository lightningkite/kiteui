# KiteUI Testing Library - Incremental Implementation Plan

## Overview
Building a cross-platform UI testing library for KiteUI that works on JavaScript/Web, Android, iOS, and JVM platforms. The goal is to write tests in **common code** that run on all platforms, and create comprehensive test coverage for all built-in components.

## Key Principles
- ✅ Tests written in **commonTest** (not platform-specific test directories)
- ✅ Use standard kotlin.test assertions (no custom assertion library)
- ✅ Use existing `debugName` property (no special syntax needed)
- ✅ Small, incremental steps - one component at a time
- ✅ Each component fully tested before moving to the next

---

## ✅ Phase 1: Foundation - Test Harness (COMPLETED)

### Goal
Get UI rendering working in tests on each platform using their native patterns.

### Steps
1. ✅ Create common `TestHarness` expect class in `commonTest`
2. ✅ Implement JS version using `root(Theme) {}` pattern
3. ✅ Test JS implementation works
4. ✅ Implement Android version using Robolectric + KiteUiActivity
5. ✅ Test Android implementation works
6. ✅ Implement iOS version using UIWindow + UIViewController + setup()
   - Avoid `makeKeyAndVisible()` (causes SIGTRAP)
   - Avoid calling setup in `viewDidLoad()` (causes SIGTRAP)
7. ✅ Test iOS implementation works
8. ✅ Add JVM SSR implementation
9. ✅ Create `withTestHarness` helper function for automatic cleanup
10. ✅ Add `supported` flag to skip tests on unsupported platforms

### Files Created
- `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.kt`
- `library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.js.kt`
- `library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.android.kt`
- `library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/TestHarness.ios.kt`
- Test files for each platform

### API
```kotlin
@Test
fun myTest() = withTestHarness { harness ->
    val root = harness.render {
        text("Hello, World!")
    }
    // Test assertions here
}
```

---

## ✅ Phase 2: View Finding (COMPLETED)

### Goal
Find views in the hierarchy using their `debugName` property.

### Steps
1. ✅ Create `ViewFinder` object with recursive search logic
2. ✅ Add `findByDebugName(name: String)` method
3. ✅ Add `findAll(predicate)` method for flexible searching
4. ✅ Add extension functions on RView for convenience
5. ✅ Test on JS platform
6. ✅ Test on Android platform
7. ✅ Test on iOS platform

### Files Created
- `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/ViewFinder.kt`

### API
```kotlin
val root = harness.render {
    text("Hello").apply { debugName = "greeting" }
}

val found = root.findByDebugName("greeting")
val allTexts = root.findAll { it is TextView }
```

---

## ✅ Phase 2.5: Enable Common Test Code (COMPLETED)

### Goal
Allow writing tests in commonTest that run on all platforms.

### Steps
1. ✅ Add `supported` property to TestHarness
2. ✅ Update `withTestHarness` to skip if not supported
3. ✅ Move test files from platform-specific directories to commonTest
4. ✅ Verify tests run on all supported platforms

---

## 🔄 Phase 3: Click Interactions (NEXT)

### Goal
Simulate user clicks/taps on views.

### Steps
1. Create common `Interactions` object
2. Add `click(view: RView)` function signature
3. Implement JS version
   - Find native DOM element
   - Dispatch click event
   - Test with button
4. Implement Android version
   - Call `performClick()` on native view
   - Test with button
5. Implement iOS version
   - Send touch events to UIView
   - Test with button
6. Add convenience extension: `RView.click()`
7. Test all platforms with example

### Files to Create
- `library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/Interactions.kt`
- `library/src/jsTest/kotlin/com/lightningkite/kiteui/testing/Interactions.js.kt`
- `library/src/androidUnitTest/kotlin/com/lightningkite/kiteui/testing/Interactions.android.kt`
- `library/src/iosTest/kotlin/com/lightningkite/kiteui/testing/Interactions.ios.kt`

### API
```kotlin
val button = root.findByDebugName("submit-button")!!
button.click()
```

---

## 🔲 Phase 4: Component Tests - Basic Components

### Goal
Write comprehensive tests for basic built-in components in **commonTest**.

### Components to Test (in order)
1. **TextView** - Display text
   - Render text
   - Find by debugName
   - Verify content

2. **Button** - Clickable button
   - Render button
   - Click interaction
   - Verify click handler called
   - Test enabled/disabled state

3. **TextInput** - Single-line text input
   - Render input
   - Type text
   - Clear text
   - Verify Property binding works

4. **Switch** - Toggle switch
   - Render switch
   - Toggle on/off
   - Verify state changes

5. **Checkbox** - Checkbox input
   - Render checkbox
   - Check/uncheck
   - Verify state changes

6. **Select** - Dropdown selection
   - Render select
   - Choose option
   - Verify selection

### For Each Component
1. Create test file in `commonTest/kotlin/com/lightningkite/kiteui/components/`
2. Test basic rendering
3. Test interactions
4. Test Property bindings
5. Test state changes
6. Run on all platforms
7. Move to next component

---

## 🔲 Phase 5: Component Tests - Layout Components

### Goal
Test layout containers and positioning.

### Components to Test
1. **Stack** - Overlapping layers
2. **Row** - Horizontal layout
3. **Col** - Vertical layout
4. **Frame** - Single child container
5. **Space** - Empty space
6. **Separator** - Visual divider

### For Each Component
1. Test layout behavior
2. Test with multiple children
3. Test spacing/gaps
4. Verify screen rectangles

---

## 🔲 Phase 6: Component Tests - Advanced Components

### Goal
Test more complex interactive components.

### Components to Test
1. **Link** - Navigation link
2. **ExternalLink** - External URL link
3. **Image** - Image display
4. **Video** - Video player
5. **WebView** - Embedded web content
6. **Canvas** - Custom drawing

---

## 🔲 Phase 7: Component Tests - Form Components

### Goal
Test form-related components and validation.

### Components to Test
1. **IntInput** - Integer input
2. **NumberInput** - Decimal number input
3. **LocalDateInput** - Date picker
4. **LocalTimeInput** - Time picker
5. **LocalDateTimeInput** - Date+time picker
6. **Slider** - Range slider
7. **RadioButton** - Radio button group

---

## 🔲 Phase 8: The Big One - Recycler2

### Goal
Comprehensively test the Recycler2 component (list/grid rendering).

### Test Areas
1. **Basic List Rendering**
   - Render list of items
   - Verify all items present
   - Test empty list

2. **Item Interaction**
   - Click on items
   - Verify selection
   - Test item actions

3. **Dynamic Updates**
   - Add items
   - Remove items
   - Update items
   - Verify list updates correctly

4. **Scrolling** (if possible in test environment)
   - Scroll to item
   - Verify virtualization

5. **Search/Filter**
   - Apply filter
   - Verify filtered results
   - Clear filter

6. **Sorting**
   - Sort ascending/descending
   - Verify order

### Test File
- `commonTest/kotlin/com/lightningkite/kiteui/components/Recycler2Test.kt`

---

## 🔲 Phase 9: Text Input Interactions (Moved from Phase 4)

### Goal
Simulate typing text into input fields.

### Steps
1. Add `typeText(view: RView, text: String)` to Interactions
2. Add `clearText(view: RView)` to Interactions
3. Implement JS version
   - Set value on input element
   - Dispatch input/change events
   - Test with textInput
4. Implement Android version
   - Set text on EditText
   - Trigger listeners
   - Test with textInput
5. Implement iOS version
   - Set text on UITextField
   - Send appropriate notifications
   - Test with textInput
6. Add extension functions: `RView.typeText()`, `RView.clearText()`
7. Test all platforms

### API
```kotlin
val emailField = root.findByDebugName("email-input")!!
emailField.clearText()
emailField.typeText("user@example.com")
```

---

## 🔲 Phase 10: Documentation

### Goal
Create user-facing documentation for the testing library.

### Steps
1. Create quick start guide
   - Installation (it's built-in!)
   - First test example
   - Running tests
2. Create API reference
   - TestHarness usage
   - Finding views
   - Interactions (click, type)
   - Assertions
3. Create best practices guide
   - When to use debugName
   - Test organization
   - Common patterns
4. Add troubleshooting section
   - Platform-specific issues
   - Common errors

### Files to Create
- `docs/TESTING-QUICK-START.md`
- `docs/TESTING-API-REFERENCE.md`
- `docs/TESTING-BEST-PRACTICES.md`

---

## 🔲 Phase 11: Advanced Features (Optional)

### Future enhancements to consider:

1. **Wait/Retry Logic**
   - `waitFor(timeout) { condition }`
   - `waitForView(debugName, timeout)`
   - Handle async operations

2. **Gesture Support**
   - `swipe(direction)`
   - `longClick()`
   - `scroll()`

3. **Screenshot Testing**
   - Capture view screenshots
   - Compare with baseline
   - Platform-specific implementation

4. **Performance Testing**
   - Measure render time
   - Measure interaction time
   - Track memory usage

5. **Accessibility Testing**
   - Check content descriptions
   - Verify focus order
   - Test screen reader support

---

## Testing Strategy

### For Each Feature/Component
1. ✅ Write test in **commonTest**
2. ✅ Implement any needed platform-specific interaction helpers
3. ✅ Run on JS - must pass
4. ✅ Run on Android - must pass
5. ✅ Run on iOS - must pass
6. ✅ Run on JVM (if supported) - must pass
7. ✅ Only then move to next component

### Test Commands
```bash
# JavaScript
./gradlew :library:jsTest

# Android
./gradlew :library:testDebugUnitTest

# iOS
./gradlew :library:iosSimulatorArm64Test

# JVM SSR
./gradlew :library:jvmSsrTest

# All platforms
./gradlew :library:allTests
```

### File Structure
```
library/src/
  commonTest/kotlin/com/lightningkite/kiteui/
    testing/
      TestHarness.kt         # Common API
      ViewFinder.kt          # View finding
      Interactions.kt        # Click, type, etc.
    components/
      TextViewTest.kt        # Tests for TextView
      ButtonTest.kt          # Tests for Button
      TextInputTest.kt       # Tests for TextInput
      SwitchTest.kt          # Tests for Switch
      ...
      Recycler2Test.kt       # Tests for Recycler2

  jsTest/kotlin/com/lightningkite/kiteui/testing/
    TestHarness.js.kt        # JS implementation
    Interactions.js.kt       # JS-specific interactions

  androidUnitTest/kotlin/com/lightningkite/kiteui/testing/
    TestHarness.android.kt   # Android implementation
    Interactions.android.kt  # Android-specific interactions

  iosTest/kotlin/com/lightningkite/kiteui/testing/
    TestHarness.ios.kt       # iOS implementation
    Interactions.ios.kt      # iOS-specific interactions

  jvmSsrTest/kotlin/com/lightningkite/kiteui/testing/
    TestHarness.jvmSsr.kt    # JVM implementation
    Interactions.jvmSsr.kt   # JVM-specific interactions
```

---

## Success Criteria

### Minimum Viable Product (MVP)
- ✅ Test harness works on all platforms
- ✅ Can find views by debugName
- ✅ Tests can be written in commonTest
- 🔲 Can click/tap views
- 🔲 Can type text into inputs
- 🔲 All basic components have tests
- 🔲 All layout components have tests
- 🔲 All advanced components have tests
- 🔲 Recycler2 comprehensively tested

### Nice to Have
- 🔲 Comprehensive documentation
- 🔲 Advanced features (wait, gestures, screenshots)
- 🔲 Performance testing
- 🔲 Accessibility testing

---

## Current Status

**Completed:** Phases 1-2.5 (Test Harness + View Finding + Common Test Support)

**Next Up:** Phase 3 (Click Interactions)

**All tests passing:** ✅ JS, ✅ Android, ✅ iOS, ✅ JVM

**Priority:** Write tests in **commonTest** for every built-in component, culminating in comprehensive Recycler2 tests.
