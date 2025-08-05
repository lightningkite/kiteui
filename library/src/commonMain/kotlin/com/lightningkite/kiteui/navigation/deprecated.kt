package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


@Deprecated("Renamed to PageNavigator", ReplaceWith("PageNavigator"))
public typealias ScreenNavigator = PageNavigator

@Deprecated("Move to using 'Page'", ReplaceWith("Page", "com.lightningkite.kiteui.navigation.Page"))
public interface Screen: Page {
    public override fun ViewWriter.render(): ViewModifiable {
        @Suppress("DEPRECATION")
        return frame { render() }
//        return this.lastWrittenView ?: throw IllegalStateException("Screens must create a single view, but you have not created one.")
    }
    public fun ViewWriter.renderOld(): Any?

    @Suppress("Deprecation")
    public object Empty: Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        public override fun ViewWriter.renderOld(): ViewModifiable = space {}
    }

    @Suppress("Deprecation")
    public open class Direct(title: String = "", public val render: ViewWriter.()->ViewModifiable): Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        public override fun ViewWriter.renderOld(): ViewModifiable = this@Direct.render(this)
        public override val title: Reactive<String> = Constant(title)
    }
}
@Deprecated("Renamed to pageNavigator", ReplaceWith("pageNavigator"))
public var ViewWriter.screenNavigator: PageNavigator by ViewWriter::pageNavigator
@Deprecated("Renamed to mainPageNavigator", ReplaceWith("mainPageNavigator"))
public var ViewWriter.mainScreenNavigator: PageNavigator by ViewWriter::mainPageNavigator
@Deprecated("Renamed to dialogPageNavigator", ReplaceWith("dialogPageNavigator"))
public var ViewWriter.dialogScreenNavigator: PageNavigator by ViewWriter::dialogPageNavigator