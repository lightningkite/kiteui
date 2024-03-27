package com.lightningkite.rock.reactive

import com.lightningkite.rock.models.Dimension
import com.lightningkite.rock.models.WindowStatistics
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.events.Event

actual object SoftInputOpen : Readable<Boolean> by Constant(false)


actual object AnimationFrame: Listenable {
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
actual object WindowInfo: Readable<WindowStatistics> by (Property(
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
})
actual object InForeground: Readable<Boolean> {
    override val state: ReadableState<Boolean>
        get() = ReadableState((document.asDynamic().visibilityState as? String) != "hidden")

    override fun addListener(listener: () -> Unit): () -> Unit {
        val l = { _: Event -> listener(); Unit }
        document.addEventListener("visibilitychange", l)
        return { document.removeEventListener("visibilitychange", l) }
    }
}