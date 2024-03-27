package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.plus
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.WindowInfo
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon
import org.w3c.dom.HTMLElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NTwoPane(override val js: HTMLElement): NView2<HTMLElement>()

@ViewDsl
actual fun ViewWriter.twoPane(
    setup: TwoPane.() -> Unit,
    leftPaneSize: Dimension,
    rightPaneMinSize: Dimension,
    right: ContainingView.() -> Unit,
    left: ContainingView.() -> Unit
) {
    suspend fun tooSmall() = WindowInfo.await().width < leftPaneSize + rightPaneMinSize
    row {
        val leftPane = Property(false)
        sizeConstraints(width = leftPaneSize) - stack {
            ::exists { !tooSmall() || leftPane.await() }
            left(this)
        }
        button {
            centered - icon(Icon.menu, "Toggle Panes")
            ::exists { tooSmall() }
            onClick { leftPane set !leftPane.await() }
        }
        expanding - stack {
            right(this)
        }
        setup(TwoPane(NTwoPane(native.js)))
    }
}