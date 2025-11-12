# Working Test Patterns in KiteUI

## Summary
All three platforms (JS, Android, iOS) have working LayoutTest implementations. Here's what I learned from analyzing them.

## Test Execution Results

✅ **JS Tests**: Pass (`./gradlew :library:jsTest`)
✅ **Android Tests**: Pass (`./gradlew :library:testDebugUnitTest`)
✅ **iOS Tests**: Pass (`./gradlew :library:iosX64Test`)

## Platform-Specific Patterns

### JavaScript/Web (`jsTest/kotlin/LayoutTest.kt`)

```kotlin
class LayoutTest {
    @Test
    fun test() {
        val s = LayoutsTestPage()
        lateinit var root: RView
        root(Theme(id = "unitTest")) {
            frame {
                s.render(this)
            }.also { root = it }
        }
        println(root.screenRectangle())
        s.checks.forEach { it() }
    }
}
```

**Key points:**
- Uses `root(Theme) { }` function
- Returns the view from the lambda using `.also { }`
- No special setup needed

### Android (`androidUnitTest/kotlin/LayoutTest.kt`)

```kotlin
@RunWith(RobolectricTestRunner::class)
class LayoutTest {
    class TestActivity: KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator {
            Routes(listOf(), mapOf(), Page.Empty)
        }

        val s = LayoutsTestPage()

        override val theme: ReactiveContext.() -> Theme = {
            Theme(id = "unitTest")
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_Mppexample)
            with(viewWriter) {
                frame {
                    s.render(this)
                }
            }
        }
    }

    @Test
    fun test() {
        Robolectric.buildActivity(TestActivity::class.java).use { controller ->
            controller.setup()
            controller.get().s.checks.forEach { it() }
        }
    }
}
```

**Key points:**
- Requires `@RunWith(RobolectricTestRunner::class)`
- Creates a custom `KiteUiActivity` subclass
- Must override `mainNavigator` and `theme`
- Must call `setTheme(R.style.Theme_Mppexample)` in onCreate
- Uses `with(viewWriter) { }` to build UI
- Uses `Robolectric.buildActivity().use { }` pattern

### iOS (`iosTest/kotlin/LayoutTest.kt`) - CURRENTLY BROKEN

```kotlin
class LayoutTest {
    @Test
    fun test() {
        val s = LayoutsTestPage()
        lateinit var root: RView
        val vc = object: UIViewController(null, null) {
            override fun viewDidLoad() {
                super.viewDidLoad()
                setup(Theme(id = "unitTest")) {
                    frame {
                        s.render(this)
                    }.also { root = it }
                }
            }
        }
        val window = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 1000.0))
        window.makeKeyAndVisible()
        window.rootViewController = vc
        vc.view.setNeedsLayout()
        vc.view.layoutIfNeeded()
        println(root.screenRectangle())
        s.checks.forEach { it() }
    }
}
```

**Current Status: FAILS with NullPointerException**

**What I Learned:**
- Original test called `setup()` inside `viewDidLoad()` which caused SIGTRAP (signal 5)
- Calling `makeKeyAndVisible()` on UIWindow causes SIGTRAP in test environment
- Simple test works without `makeKeyAndVisible()`: just create UIWindow, set rootViewController, setFrame, call setup
- The NPE comes from LayoutsTestPage.kt:19 which uses `parent!!` - views don't have parents set during test init
- `screenRectangle()` requires window.rootViewController.view to exist (line 120 of RView.ios.kt)

**Simplified Working Pattern (without LayoutsTestPage):**
```kotlin
@Test
fun test() {
    val window = UIWindow(frame = CGRectMake(0.0, 0.0, 500.0, 1000.0))
    val vc = UIViewController(null, null)
    window.rootViewController = vc
    vc.view.setFrame(CGRectMake(0.0, 0.0, 500.0, 1000.0))
    lateinit var root: RView
    vc.setup(Theme(id = "unitTest")) {
        frame {
            text("Hello")
        }.also { root = it }
    }
    vc.view.setNeedsLayout()
    vc.view.layoutIfNeeded()
    // Can now use root.screenRectangle(), etc.
}
```

**Key points:**
- Do NOT use `makeKeyAndVisible()` in tests - causes SIGTRAP
- Do NOT call `setup()` inside `viewDidLoad()` - causes SIGTRAP
- Must create UIWindow and set rootViewController for screenRectangle() to work
- Must call `vc.view.setFrame()` to set size
- Must call `setNeedsLayout()` and `layoutIfNeeded()` to trigger layout
- LayoutsTestPage uses `parent!!` which NPEs during test init - needs to be fixed or replaced

## Common Patterns

1. **Test Data Structure**: Use a test page class (like `LayoutsTestPage`) that stores assertion checks
2. **Deferred Assertions**: Collect checks during render, execute after layout
3. **Layout Verification**: All tests use `screenRectangle()` to verify layout
4. **Theme**: All use `Theme(id = "unitTest")`

## What Doesn't Exist (Hallucinations from Before)

❌ `AppScope` - doesn't exist
❌ `extensionData` on RView - doesn't exist
❌ `beforeNextElementSetup` on ViewWriter - need to verify
❌ Test-specific interaction APIs - don't exist yet

## Next Steps for Testing Framework

To build a working testing framework, I should:

1. Start with the `debugName` property (which DOES exist)
2. Use the existing `root()` / `setup()` / `viewWriter` patterns
3. Build helper functions that work WITH these patterns, not against them
4. Test each small addition before moving on
5. Don't hallucinate new APIs - check the code first
