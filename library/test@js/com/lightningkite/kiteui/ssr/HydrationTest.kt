package com.lightningkite.kiteui.ssr

import com.lightningkite.kiteui.views.FutureElement
import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import kotlin.test.*

class HydrationTest {

    @BeforeTest
    fun setUp() {
        // Clear HydrationContext before each test to ensure isolation.
        // clear() deliberately leaves the hydration counters alone (see its KDoc), so tests that
        // assert on them need resetStats() too.
        HydrationContext.clear()
        HydrationContext.resetStats()

        // Remove any leftover __SSR_DATA__ elements from previous tests
        document.getElementById("__SSR_DATA__")?.let { document.body?.removeChild(it) }
    }

    @AfterTest
    fun tearDown() {
        // Clean up after each test
        HydrationContext.clear()
        HydrationContext.resetStats()
        document.getElementById("__SSR_DATA__")?.let { document.body?.removeChild(it) }
    }

    @Test
    fun testBasicHydration() {
        // Create a server-rendered DOM element
        val ssrElement = document.createElement("div") as HTMLElement
        ssrElement.id = "test-div"
        ssrElement.className = "existing-class"
        ssrElement.textContent = "SSR Content"

        // Create a FutureElement that matches
        val future = FutureElement().apply {
            tag = "div"
            id = "test-div"
            classes.add("existing-class")
            content = "SSR Content"
        }

        // Hydrate
        val success = future.hydrate(ssrElement)

        // Verify
        assertTrue(success, "Hydration should succeed with matching tags")
        assertSame(ssrElement, future.element, "FutureElement should reference the SSR element")
        assertEquals("test-div", future.id, "ID should be preserved")
        assertTrue(future.classes.contains("existing-class"), "Classes should be preserved")
    }

    @Test
    fun testTagMismatchFallback() {
        // Create SSR element with different tag
        val ssrElement = document.createElement("span") as HTMLElement

        // Create FutureElement expecting div
        val future = FutureElement().apply {
            tag = "div"
        }

        // Hydrate
        val success = future.hydrate(ssrElement)

        // Verify hydration failed
        assertFalse(success, "Hydration should fail with tag mismatch")
        assertNull(future.element, "FutureElement should not link to mismatched element")
    }

    @Test
    fun testEventListenerAttachment() {
        // Create SSR element
        val ssrElement = document.createElement("button") as HTMLElement

        // Track if event was attached
        var clickHandlerCalled = false

        // Create FutureElement with event listener
        val future = FutureElement().apply {
            tag = "button"
            addEventListener("click") {
                clickHandlerCalled = true
            }
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify event listener was attached
        assertNotNull(ssrElement.asDynamic().onclick, "Click handler should be attached")
    }

    @Test
    fun testStyleSyncing() {
        // Create SSR element with some styles
        val ssrElement = document.createElement("div") as HTMLElement
        ssrElement.style.color = "red"

        // Create FutureElement with additional styles
        val future = FutureElement().apply {
            tag = "div"
            setStyleProperty("background-color", "blue")
            setStyleProperty("font-size", "16px")
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify styles were applied
        assertEquals("blue", ssrElement.style.backgroundColor, "Background color should be applied")
        assertEquals("16px", ssrElement.style.fontSize, "Font size should be applied")
        assertEquals("red", ssrElement.style.color, "Existing color should be preserved")
    }

    @Test
    fun testAttributeSyncing() {
        // Create SSR element with some attributes
        val ssrElement = document.createElement("input") as HTMLElement
        ssrElement.setAttribute("type", "text")
        ssrElement.setAttribute("placeholder", "SSR placeholder")

        // Create FutureElement with additional attributes
        val future = FutureElement().apply {
            tag = "input"
            setAttribute("value", "default value")
            setAttribute("maxlength", "100")
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify attributes were applied
        assertEquals("default value", ssrElement.getAttribute("value"), "Value attribute should be applied")
        assertEquals("100", ssrElement.getAttribute("maxlength"), "Maxlength attribute should be applied")
        assertEquals("text", ssrElement.getAttribute("type"), "Existing type should be preserved")
    }

    @Test
    fun testClassSyncing() {
        // Create SSR element with existing classes
        val ssrElement = document.createElement("div") as HTMLElement
        ssrElement.className = "ssr-class-1 ssr-class-2"

        // Create FutureElement with additional classes
        val future = FutureElement().apply {
            tag = "div"
            classes.add("ssr-class-2") // Already exists
            classes.add("client-class") // New class
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify classes
        assertTrue(ssrElement.classList.contains("ssr-class-1"), "SSR class 1 should remain")
        assertTrue(ssrElement.classList.contains("ssr-class-2"), "SSR class 2 should remain")
        assertTrue(ssrElement.classList.contains("client-class"), "Client class should be added")
    }

    @Test
    fun testRecursiveHydrationSuccess() {
        // Create SSR DOM tree
        val ssrParent = document.createElement("div") as HTMLElement
        val ssrChild1 = document.createElement("span") as HTMLElement
        val ssrChild2 = document.createElement("p") as HTMLElement
        ssrChild1.textContent = "Child 1"
        ssrChild2.textContent = "Child 2"
        ssrParent.appendChild(ssrChild1)
        ssrParent.appendChild(ssrChild2)

        // Create matching FutureElement tree
        val futureParent = FutureElement().apply {
            tag = "div"
        }
        val futureChild1 = FutureElement().apply {
            tag = "span"
            content = "Child 1"
        }
        val futureChild2 = FutureElement().apply {
            tag = "p"
            content = "Child 2"
        }
        futureParent.appendChild(futureChild1)
        futureParent.appendChild(futureChild2)

        // Hydrate recursively
        val success = futureParent.hydrateRecursive(ssrParent)

        // Verify
        assertTrue(success, "Recursive hydration should succeed")
        assertSame(ssrParent, futureParent.element, "Parent should be hydrated")
        assertSame(ssrChild1, futureChild1.element, "Child 1 should be hydrated")
        assertSame(ssrChild2, futureChild2.element, "Child 2 should be hydrated")
    }

    @Test
    fun testRecursiveHydrationWithChildMismatch() {
        // Create SSR DOM with 2 children
        val ssrParent = document.createElement("div") as HTMLElement
        val ssrChild1 = document.createElement("span") as HTMLElement
        val ssrChild2 = document.createElement("p") as HTMLElement
        ssrParent.appendChild(ssrChild1)
        ssrParent.appendChild(ssrChild2)

        // Create FutureElement with 3 children (one extra)
        val futureParent = FutureElement().apply {
            tag = "div"
        }
        val futureChild1 = FutureElement().apply { tag = "span" }
        val futureChild2 = FutureElement().apply { tag = "p" }
        val futureChild3 = FutureElement().apply { tag = "div" }
        futureParent.appendChild(futureChild1)
        futureParent.appendChild(futureChild2)
        futureParent.appendChild(futureChild3)

        // Hydrate recursively
        futureParent.hydrateRecursive(ssrParent)

        // Verify first two children hydrated, third created fresh
        assertSame(ssrChild1, futureChild1.element, "Child 1 should be hydrated")
        assertSame(ssrChild2, futureChild2.element, "Child 2 should be hydrated")
        assertNotNull(futureChild3.element, "Child 3 should be created")
        assertNotSame(ssrChild1, futureChild3.element, "Child 3 should be a new element")
        assertNotSame(ssrChild2, futureChild3.element, "Child 3 should be a new element")
        assertEquals(3, ssrParent.childElementCount, "Parent should have 3 children after hydration")
    }

    @Test
    @Ignore
    fun testRecursiveHydrationWithTagMismatch() {
        // Create SSR DOM
        val ssrParent = document.createElement("div") as HTMLElement
        val ssrChild = document.createElement("button") as HTMLElement
        ssrParent.appendChild(ssrChild)

        // Create FutureElement expecting span
        val futureParent = FutureElement().apply { tag = "div" }
        val futureChild = FutureElement().apply { tag = "span" }
        futureParent.appendChild(futureChild)

        // Hydrate recursively
        futureParent.hydrateRecursive(ssrParent)

        // Verify parent hydrated but child failed
        assertSame(ssrParent, futureParent.element, "Parent should be hydrated")
        // Child hydration should fail silently and leave SSR content
        assertNull(futureChild.element, "Child should not hydrate due to tag mismatch")
    }

    @Test
    fun testHydrationCursor() {
        // Create DOM with multiple children
        val parent = document.createElement("div") as HTMLElement
        val child1 = document.createElement("span") as HTMLElement
        val child2 = document.createElement("p") as HTMLElement
        val child3 = document.createElement("div") as HTMLElement
        parent.appendChild(child1)
        parent.appendChild(child2)
        parent.appendChild(child3)

        // Create cursor
        val cursor = HydrationCursor(parent)

        // Test sequential access
        assertSame(child1, cursor.nextChild(), "First child should be child1")
        assertSame(child2, cursor.nextChild(), "Second child should be child2")
        assertSame(child3, cursor.nextChild(), "Third child should be child3")
        assertNull(cursor.nextChild(), "Fourth call should return null")

        // Test remaining count
        assertEquals(0, cursor.remainingCount(), "Should have no remaining children")
        assertFalse(cursor.hasRemaining(), "Should not have remaining children")
    }

    @Test
    fun testHydrationCursorSubCursor() {
        // Create nested DOM
        val grandparent = document.createElement("div") as HTMLElement
        val parent = document.createElement("div") as HTMLElement
        val child1 = document.createElement("span") as HTMLElement
        val child2 = document.createElement("p") as HTMLElement
        parent.appendChild(child1)
        parent.appendChild(child2)
        grandparent.appendChild(parent)

        // Create cursor for grandparent
        val cursor = HydrationCursor(grandparent)
        val parentElement = cursor.nextChild()
        assertNotNull(parentElement, "Should get parent element")

        // Create sub-cursor
        val subCursor = cursor.forElement(parentElement)
        assertSame(child1, subCursor.nextChild(), "Sub-cursor should access child1")
        assertSame(child2, subCursor.nextChild(), "Sub-cursor should access child2")
        assertTrue(subCursor.remainingCount() == 0, "Sub-cursor should be exhausted")
    }

    @Test
    fun testHydrationContextClearResetsIsHydrating() {
        // Test that clear() resets the isHydrating flag
        assertFalse(HydrationContext.isHydrating, "Should not be hydrating initially")

        // Clear should set it to false
        HydrationContext.clear()
        assertFalse(HydrationContext.isHydrating, "Should still be false after clear()")
    }

    @Test
    fun testCaseInsensitiveTagMatching() {
        // Create SSR element with uppercase tag (browsers normalize to lowercase)
        val ssrElement = document.createElement("DIV") as HTMLElement

        // Create FutureElement with lowercase tag
        val future = FutureElement().apply {
            tag = "div"
        }

        // Hydrate should succeed (case-insensitive)
        val success = future.hydrate(ssrElement)

        assertTrue(success, "Hydration should succeed with case-insensitive tag matching")
        assertSame(ssrElement, future.element, "Element should be hydrated")
    }

    @Test
    fun testOnElementCallbacksDuringHydration() {
        // Create SSR element
        val ssrElement = document.createElement("div") as HTMLElement

        var callbackExecuted = false
        var receivedElement: Element? = null

        // Create FutureElement with pending callback
        val future = FutureElement().apply {
            tag = "div"
            onElement { element ->
                callbackExecuted = true
                receivedElement = element
            }
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify callback was executed
        assertTrue(callbackExecuted, "onElement callback should be executed during hydration")
        assertSame(ssrElement, receivedElement, "Callback should receive the hydrated element")
    }

    @Test
    fun testHydrationPreservesBlankId() {
        // Create SSR element without ID
        val ssrElement = document.createElement("div") as HTMLElement

        // Create FutureElement
        val future = FutureElement().apply {
            tag = "div"
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify ID remains null
        assertNull(future.id, "ID should remain null when SSR element has no ID")
    }

    @Test
    fun testMismatchCounterTracksFailedHydrations() {
        // Create SSR element with a different tag than expected
        val ssrElement = document.createElement("span") as HTMLElement
        val future = FutureElement().apply { tag = "div" }

        assertEquals(0, HydrationContext.mismatchedElements, "Should start with no recorded mismatches")

        future.hydrate(ssrElement)

        assertEquals(1, HydrationContext.mismatchedElements, "A tag mismatch should increment the counter")
    }

    @Test
    fun testMismatchCounterSurvivesClear() {
        // The counter is meant to stay readable (e.g. from devtools) after hydration completes -
        // clear() only tears down the resource cache, not the stats. resetStats() is what a test
        // (or a future page load, via initFromDom()) uses to zero it back out.
        val ssrElement = document.createElement("span") as HTMLElement
        val future = FutureElement().apply { tag = "div" }
        future.hydrate(ssrElement)
        assertEquals(1, HydrationContext.mismatchedElements)

        HydrationContext.clear()
        assertEquals(1, HydrationContext.mismatchedElements, "clear() should not reset the mismatch counter")

        HydrationContext.resetStats()
        assertEquals(0, HydrationContext.mismatchedElements, "resetStats() should reset the mismatch counter")
    }

    @Test
    fun testHydrationWithNonBlankId() {
        // Create SSR element with ID
        val ssrElement = document.createElement("div") as HTMLElement
        ssrElement.id = "ssr-generated-id"

        // Create FutureElement
        val future = FutureElement().apply {
            tag = "div"
        }

        // Hydrate
        future.hydrate(ssrElement)

        // Verify ID is captured
        assertEquals("ssr-generated-id", future.id, "ID should be captured from SSR element")
    }
}
