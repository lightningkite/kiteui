package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

actual object AppState {
    actual val animationFrame: Listenable
        get() = _AnimationFrame
    internal val _windowInfo = Property(
        WindowStatistics(
            width = Dimension(window.innerWidth.toString() + "px"),
            height = Dimension(window.innerHeight.toString() + "px"),
            density = 1f
        )
    ).also {
        window.addEventListener("resize", { ev ->
            val newwidth = Dimension(window.innerWidth.toString() + "px")
            val newheight = Dimension(window.innerHeight.toString() + "px")
            if(it.value.width != newwidth || it.value.height != newheight) {
                it.value = WindowStatistics(
                    width = newwidth,
                    height = newheight,
                    density = 1f
                )
            }
        })
    }
    actual val windowInfo: ImmediateReadable<WindowStatistics>
        get() = _windowInfo
    actual val inForeground: ImmediateReadable<Boolean>
        get() = _InForeground
    internal val _softInputOpen = Property(false)
    actual val softInputOpen: ImmediateReadable<Boolean>
        get() = _softInputOpen
}

private object _AnimationFrame: Listenable {
    override fun addListener(listener: () -> Unit): () -> Unit {
        var end = false
        var sub: (Double) -> Unit = {}
        sub = label@{
            listener()
            if (end) {
                // Done!
            } else {
                window.requestAnimationFrame(sub)
            }
        }
        window.requestAnimationFrame(sub)
        return {
            end = true
        }
    }
}

private object _InForeground: ImmediateReadable<Boolean> {
    override val value: Boolean
        get() = (document.asDynamic().visibilityState as? String) != "hidden"

    override fun addListener(listener: () -> Unit): () -> Unit {
        val l = { _: Event -> listener(); Unit }
        document.addEventListener("visibilitychange", l)
        return { document.removeEventListener("visibilitychange", l) }
    }
}