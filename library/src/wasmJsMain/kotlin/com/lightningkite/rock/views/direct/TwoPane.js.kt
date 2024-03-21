package com.lightningkite.rock.views.direct

import com.lightningkite.rock.models.Dimension
import com.lightningkite.rock.models.Icon
import com.lightningkite.rock.models.SizeConstraints
import com.lightningkite.rock.models.plus
import com.lightningkite.rock.reactive.Property
import com.lightningkite.rock.reactive.WindowInfo
import com.lightningkite.rock.reactive.await
import com.lightningkite.rock.reactive.invoke
import com.lightningkite.rock.views.*
import com.lightningkite.rock.views.l2.icon
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