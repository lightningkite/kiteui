package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

interface Page {
    val title: Reactive<String>
        get() = Constant(
            this::class.simpleName.toString().camelToHuman().removeSuffix(" Screen").removeSuffix(" Page")
        )
    public fun ViewWriter.render(): ViewModifiable
    public object Empty: Page {
        public override fun ViewWriter.render(): ViewModifiable = space {}
    }
    open class Direct(title: String = "", val render: ViewWriter.()->ViewModifiable): Page {
        override fun ViewWriter.render(): ViewModifiable = this@Direct.render(this)
        override val title: Reactive<String> = Constant(title)
    }
}

private val camelRegex = Regex("([a-z])([A-Z]+)")
private fun String.camelToHuman(): String = this.replace(camelRegex) { it.groupValues[1] + " " + it.groupValues[2] }