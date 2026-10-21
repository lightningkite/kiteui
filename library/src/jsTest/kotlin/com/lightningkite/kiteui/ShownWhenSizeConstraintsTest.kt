@file:OptIn(DelicateCoroutinesApi::class)

package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.native
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.promise
import org.w3c.dom.HTMLElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The shownWhen animation locks its child to pixel sizes while it runs.  It used to finish by
 * setting the child to 100% x 100%, discarding the child's own sizeConstraints: a small unread
 * dot became as big as its row.
 */
class ShownWhenSizeConstraintsTest {

    @Test
    fun sizeConstraintsSurviveShowAndHideAnimations() = GlobalScope.promise {
        val visible = Signal(false)
        lateinit var dot: Element
        root(Theme(id = "unitTest")) {
            row {
                text("Messages")
                shownWhen { visible() }.sizeConstraints(width = 8.px, height = 8.px).frame {
                    dot = this
                }
            }
        }
        val dotElement = dot.native.element as HTMLElement
        val wrapper = dotElement.parentElement as HTMLElement

        suspend fun animateTo(shown: Boolean) {
            visible.value = shown
            delay(100)
            var waited = 0
            while (wrapper.classList.contains("animatingShowHide") && waited < 5000) {
                delay(50); waited += 50
            }
            assertEquals(!shown, wrapper.hidden)
        }

        animateTo(true)
        assertEquals("8px", dotElement.style.width)
        assertEquals("8px", dotElement.style.height)
        assertEquals(8.0, dotElement.getBoundingClientRect().width)
        assertEquals(8.0, dotElement.getBoundingClientRect().height)

        animateTo(false)
        animateTo(true)
        assertEquals(8.0, dotElement.getBoundingClientRect().width)
        assertEquals(8.0, dotElement.getBoundingClientRect().height)
    }
}
