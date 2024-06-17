package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics

actual object AnimationFrame : Listenable {
    fun frame() {
        listeners.toList().forEach { it() }
    }

    private val listeners = ArrayList<()->Unit>()
    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return { listeners.remove(listener) }
    }
}

actual object WindowInfo: ImmediateReadable<WindowStatistics> by _WindowInfo
internal val _WindowInfo = Property(WindowStatistics(Dimension(1920f), Dimension(1080f), 1f))

actual object InForeground: ImmediateReadable<Boolean> by _InForeground
internal val _InForeground = Property(true)

actual object SoftInputOpen : ImmediateReadable<Boolean> by _SoftInputOpen
internal val _SoftInputOpen = Property(false)