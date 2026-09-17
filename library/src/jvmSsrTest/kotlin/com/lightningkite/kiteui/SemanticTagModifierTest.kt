@file:OptIn(ExperimentalCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the semantic-tag modifiers (`asNavigation`, `asList`, ...) on the web target.
 *
 * The interesting case is when the tagged element is NOT a plain div/span: it gets wrapped in a
 * [PassthroughContainer]. That wrapper must be layout-transparent — it must carry the parent-facing
 * footprint (weight/alignment) AND let its single child fill it — otherwise weight/stretch applied
 * to a tagged element silently does nothing.
 */
class SemanticTagModifierTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    @Test
    fun weightAndStretchFlowThroughTagWrapper() {
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            col {
                // button's tag can't be relabelled, so this wraps it in a <nav> PassthroughContainer
                expanding.asNavigation.button { }
            }
        }

        val col = root.children[0]
        val wrapper = col.native.children[0]

        // The wrapper carries the semantic tag and is a flex box (so its child can fill it)
        assertEquals("nav", wrapper.tag)
        assertEquals("flex", wrapper.style["display"], "wrapper must be a flex box")

        // The weight (expanding) landed on the WRAPPER, so the parent flex sizes it correctly
        assertEquals("1.0", wrapper.style["flex-grow"], "weight must land on the wrapper")

        // The single child grows along the main axis; cross-axis fill is flex's default align-items.
        val inner = wrapper.children[0]
        assertEquals("button", inner.tag)
        assertEquals("1", inner.style["flex-grow"], "child must grow to fill the wrapper's main axis")
    }

    @Test
    fun tagWrapperHasNoLeftoverBlockDisplay() {
        // Regression guard: the old implementation forced `display: block`, which is what broke
        // stretch (a block child won't fill a stretched/weighted wrapper's height).
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            col { asMain.button { } }
        }
        val wrapper = root.children[0].native.children[0]
        assertEquals("main", wrapper.tag)
        assertEquals("flex", wrapper.style["display"])
    }
}
