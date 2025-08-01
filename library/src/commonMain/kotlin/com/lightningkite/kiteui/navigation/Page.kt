package com.lightningkite.kiteui.navigation

import com.lightningkite.signal.Constant
import com.lightningkite.signal.Readable
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.space

public interface Page {
    public val title: Readable<String>
        get() = Constant(
            this::class.simpleName.toString().camelToHuman().removeSuffix(" Screen").removeSuffix(" Page")
        )
    public fun ViewWriter.render(): ViewModifiable
    public object Empty: Page {
        public override fun ViewWriter.render(): ViewModifiable = space {}
    }
    public open class Direct(title: String = "", public val render: ViewWriter.()->ViewModifiable): Page {
        public override fun ViewWriter.render(): ViewModifiable = this@Direct.render(this)
        public override val title: Readable<String> = Constant(title)
    }
}

private val camelRegex = Regex("([a-z])([A-Z]+)")
private fun String.camelToHuman(): String = this.replace(camelRegex) { it.groupValues[1] + " " + it.groupValues[2] }