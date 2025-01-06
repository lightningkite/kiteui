package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.BaseListenable
import com.lightningkite.kiteui.reactive.Listenable
import com.lightningkite.kiteui.views.HtmlElementLike

actual fun HtmlElementLike.resizeObserver(): Listenable {
    return object: BaseListenable() {
        var observer: ResizeObserver? = null
        override fun activate() {
            observer = ResizeObserver({ _, _->
                invokeAllListeners()
            }).apply {
                this@resizeObserver.onElement {
                    this.observe(it)
                }
            }
        }

        override fun deactivate() {
            observer?.disconnect()
            observer = null
        }
    }
}