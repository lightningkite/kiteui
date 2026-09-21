package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Regression test: a [TextArea] wraps its actual `<textarea>` in a `<div class="textarea-container">`.
 * A generated `<label for=...>` must point at the inner `<textarea>`, not the wrapper `<div>`,
 * otherwise clicking the label does not focus the control and screen readers don't associate them.
 */
class TextAreaLabelTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }

    private fun FutureElement.findAll(tag: String): List<FutureElement> =
        (if (this.tag == tag) listOf(this) else emptyList()) + children.flatMap { it.findAll(tag) }

    @Test
    fun labelPointsAtInnerTextarea() {
        val context = ElementContext("/")
        val root = Frame(context)
        with(root) {
            field("Description") {
                textArea { }
            }
        }

        val label = root.native.findAll("label").single()
        val textarea = root.native.findAll("textarea").single()
        val wrapper = root.native.findAll("div").single { "textarea-container" in it.classes }

        val forId = label.attributes["for"]
        assertNotNull(forId, "label should have a for attribute")
        assertEquals(textarea.id, forId, "label must point at the inner textarea")
        assertNull(wrapper.id, "the wrapping div must not carry the label target id")
    }
}
