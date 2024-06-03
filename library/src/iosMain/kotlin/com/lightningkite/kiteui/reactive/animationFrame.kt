package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.WindowStatistics
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIScreen

actual object AnimationFrame: Listenable {
    override fun addListener(listener: () -> Unit): () -> Unit {
        var end = false
        var sub: (Double) -> Unit = {}
        sub = label@{
            listener()
            if (end) {
                // Done!
            } else {
                afterTimeout(16) { sub(0.0016) }
            }
        }
        afterTimeout(16) { sub(0.0016) }
        return {
            end = true
        }
    }
}
@OptIn(ExperimentalForeignApi::class)
actual object WindowInfo: ImmediateReadable<WindowStatistics> by Property(
    WindowStatistics(
        width = Dimension(UIScreen.mainScreen.bounds.useContents { size.width }),
        height = Dimension(UIScreen.mainScreen.bounds.useContents { size.height }),
        density = UIScreen.mainScreen.scale.toFloat()
    )
)
internal val InForegroundProperty = Property(true)
actual object InForeground: ImmediateReadable<Boolean> by InForegroundProperty
