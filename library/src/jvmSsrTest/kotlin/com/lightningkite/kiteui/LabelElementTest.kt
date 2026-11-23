package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import kotlin.test.Test
import kotlin.test.assertTrue

class LabelElementTest {
    // Both star imports are what app code uses; the deprecated views.direct.label used to make this call ambiguous.
    @Test
    fun labelResolvesWithDirectAndL2StarImports() = uiTest(
        content = {
            col {
                label("Nickname") {
                    textInput { hint = "nick" }
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("Nickname"), "Snapshot should contain the label: $snap")
    }
}
