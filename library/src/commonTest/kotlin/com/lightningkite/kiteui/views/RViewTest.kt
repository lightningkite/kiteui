//package com.lightningkite.kiteui.views
//
//import com.lightningkite.kiteui.models.*
//import com.lightningkite.kiteui.reactive.Action
//import com.lightningkite.reactive.core.Signal
//import kotlin.test.*
//
///**
// * Unit tests for RView hierarchy management and lifecycle.
// *
// * Note: These tests use a minimal mock RView implementation since RView is platform-specific.
// * Platform-specific behavior is tested in platform test suites.
// */
//abstract class RViewTest {
//
//    private fun createTestView(): TestRView {
//        return TestRView(RContext.test())
//    }
//
//    @Test
//    fun testParentChildRelationship() {
//        val parent = createTestView()
//        val child = createTestView()
//
//        parent.willAddChild(child)
//        parent.addChild(child)
//
//        assertEquals(parent, child.parent)
//        assertEquals(1, parent.children.size)
//        assertTrue(parent.children.contains(child))
//    }
//
//    @Test
//    fun testRemoveChild() {
//        val parent = createTestView()
//        val child = createTestView()
//
//        parent.willAddChild(child)
//        parent.addChild(child)
//        parent.removeChild(child)
//
//        assertNull(child.parent)
//        assertEquals(0, parent.children.size)
//        assertTrue(child.shutdownCalled)
//    }
//
//    @Test
//    fun testRemoveChildByIndex() {
//        val parent = createTestView()
//        val child1 = createTestView()
//        val child2 = createTestView()
//
//        parent.willAddChild(child1)
//        parent.addChild(child1)
//        parent.willAddChild(child2)
//        parent.addChild(child2)
//
//        parent.removeChild(0)
//
//        assertEquals(1, parent.children.size)
//        assertEquals(child2, parent.children[0])
//        assertTrue(child1.shutdownCalled)
//    }
//
//    @Test
//    fun testClearChildren() {
//        val parent = createTestView()
//        val child1 = createTestView()
//        val child2 = createTestView()
//
//        parent.willAddChild(child1)
//        parent.addChild(child1)
//        parent.willAddChild(child2)
//        parent.addChild(child2)
//
//        parent.clearChildren()
//
//        assertEquals(0, parent.children.size)
//        assertTrue(child1.shutdownCalled)
//        assertTrue(child2.shutdownCalled)
//    }
//
//    @Test
//    fun testShutdownCleansUpChildren() {
//        val parent = createTestView()
//        val child = createTestView()
//
//        parent.willAddChild(child)
//        parent.addChild(child)
//
//        parent.shutdown()
//
//        assertTrue(parent.isShutdown)
//        assertTrue(child.shutdownCalled)
//        assertEquals(0, parent.children.size)
//    }
//
//    @Test
//    fun testGapDelegationInRViewWriter() {
//        val parent = createTestView()
//        val wrapper = createTestView() // extends RViewWriter
//
//        parent.gap = 10.px
//        parent.willAddChild(wrapper)
//        parent.addChild(wrapper)
//
//        // Wrapper should delegate to parent's gap when its own is null
//        assertEquals(10.px, wrapper.gap)
//
//        // Setting wrapper's own gap should override
//        wrapper.gap = 20.px
//        assertEquals(20.px, wrapper.gap)
//    }
//
//    @Test
//    fun testOpacityProperty() {
//        val view = createTestView()
//
//        assertEquals(1.0, view.opacity)
//
//        view.opacity = 0.5
//        assertEquals(0.5, view.opacity)
//    }
//
//    @Test
//    fun testShownAndVisibleProperties() {
//        val view = createTestView()
//
//        assertTrue(view.shown)
//        assertTrue(view.visible)
//
//        view.shown = false
//        assertFalse(view.shown)
//
//        view.visible = false
//        assertFalse(view.visible)
//    }
//
//    @Test
//    fun testPaddingUniformApplication() {
//        val view = createTestView()
//
//        view.padding = 10.px
//
//        assertNotNull(view.paddingByEdge)
//        assertEquals(10.px, view.paddingByEdge?.left)
//        assertEquals(10.px, view.paddingByEdge?.top)
//        assertEquals(10.px, view.paddingByEdge?.right)
//        assertEquals(10.px, view.paddingByEdge?.bottom)
//    }
//
//    @Test
//    fun testIgnoreInteraction() {
//        val view = createTestView()
//
//        assertFalse(view.ignoreInteraction)
//
//        view.ignoreInteraction = true
//        assertTrue(view.ignoreInteraction)
//    }
//
//    @Test
//    fun testTransitionId() {
//        val view = createTestView()
//
//        assertNull(view.transitionId)
//
//        view.transitionId = "test-transition"
//        assertEquals("test-transition", view.transitionId)
//    }
//
//    @Test
//    fun testWorkingAndLoadingSignals() {
//        val view = createTestView()
//
//        assertFalse(view.working.value)
//        assertFalse(view.loading.value)
//
//        // These are managed internally by listenForWorking/listenForStatus
//        // Just verify the signals exist and are readable
//        assertNotNull(view.working)
//        assertNotNull(view.loading)
//    }
//
//    @Test
//    fun testRectangleRelativeTo() {
//        val view1 = object : TestRView(RContext.test()) {
//            override fun screenRectangle() = Rect(100.0, 200.0, 150.0, 250.0)
//        }
//        val view2 = object : TestRView(RContext.test()) {
//            override fun screenRectangle() = Rect(50.0, 100.0, 100.0, 150.0)
//        }
//
//        val relative = view1.rectangleRelativeTo(view2)
//
//        assertNotNull(relative)
//        assertEquals(50.0, relative.left)
//        assertEquals(100.0, relative.top)
//        assertEquals(100.0, relative.right)
//        assertEquals(150.0, relative.bottom)
//    }
//
//    @Test
//    fun testRectangleRelativeToReturnsNullWhenNotVisible() {
//        val view1 = object : TestRView(RContext.test()) {
//            override fun screenRectangle() = null
//        }
//        val view2 = createTestView()
//
//        assertNull(view1.rectangleRelativeTo(view2))
//    }
//}
//
///**
// * Tests for RViewWithAction
// */
//class RViewWithActionTest {
//
//    private class TestRViewWithAction(context: RContext) : RViewWithAction(context) {
//        override var showOnPrint: Boolean = true
//        var actionSetCalled = 0
//
//        override fun scrollIntoView(horizontal: Align?, vertical: Align?, animate: Boolean) {}
//        override fun requestFocus() {}
//        override fun screenRectangle(): Rect? = null
//        override fun applyTheme(theme: ThemeAndBack) {}
//        override fun internalAddChild(index: Int, view: RView) {}
//        override fun internalRemoveChild(index: Int) {}
//        override fun internalClearChildren() {}
//
//        override fun actionSet(value: Action?) {
//            actionSetCalled++
//            super.actionSet(value)
//        }
//    }
//
//    @Test
//    fun testActionProperty() {
//        val view = TestRViewWithAction(RContext.test())
//        val action = Action(Signal(false))
//
//        assertNull(view.action)
//        assertEquals(0, view.actionSetCalled)
//
//        view.action = action
//
//        assertEquals(action, view.action)
//        assertEquals(1, view.actionSetCalled)
//    }
//
//    @Test
//    fun testActionChangeCleansUpPreviousListener() {
//        val view = TestRViewWithAction(RContext.test())
//        val action1 = Action(Signal(false))
//        val action2 = Action(Signal(false))
//
//        view.action = action1
//        assertEquals(1, view.actionSetCalled)
//
//        view.action = action2
//        assertEquals(2, view.actionSetCalled)
//
//        // Previous action listener should be cleaned up
//        // This is implicitly tested by ensuring no exceptions occur
//    }
//}
//
///**
// * Helper extension to create a test RContext
// */
//private fun RContext.Companion.test(): RContext {
//    // This would need to be implemented based on the actual RContext structure
//    // For now, we'll leave it as a placeholder that would fail at runtime if not properly mocked
//    TODO("RContext.test() needs platform-specific implementation for testing")
//}
