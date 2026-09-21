package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that the [InteractiveElement] accessibility state properties produce the expected ARIA
 * attributes on web.
 */
class InteractiveA11yTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    @Test
    fun expandedAndOpensDialogProduceAriaAttributes() {
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            button {
                accessibleExpanded = true
                accessibleOpensDialog = true
            }
        }

        val html = buildString { root.native.render(this) }
        assertTrue(html.contains("aria-expanded='true'"), "expanded state should map to aria-expanded")
        assertTrue(html.contains("aria-haspopup='dialog'"), "opens-dialog should map to aria-haspopup")
        assertFalse(html.contains("aria-openspopup"), "aria-openspopup is not a real ARIA attribute")
    }

    @Test
    fun collapsedStateProducesAriaExpandedFalse() {
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            button {
                accessibleExpanded = false
            }
        }

        val html = buildString { root.native.render(this) }
        assertTrue(html.contains("aria-expanded='false'"), "collapsed state should map to aria-expanded='false'")
    }

    @Test
    fun clearingExpandedRemovesAttribute() {
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            button {
                accessibleExpanded = true
                accessibleExpanded = null
            }
        }

        val html = buildString { root.native.render(this) }
        assertFalse(html.contains("aria-expanded"), "clearing expanded state should remove the attribute")
    }
}
