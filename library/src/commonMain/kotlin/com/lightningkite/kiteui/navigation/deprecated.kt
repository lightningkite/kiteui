package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ContainerElement
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.kiteui.views.lateInitContextAddon
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive


@Deprecated("Move to using 'Page'", ReplaceWith("Page", "com.lightningkite.kiteui.navigation.Page"))
public interface Screen: Page {
    override fun ElementWriter.CanAddTheme.render() {
        frame { render() }
    }

    public fun ViewWriter.renderOld()

    @Suppress("Deprecation")
    public object Empty: Screen {

        @Deprecated("Use render2", ReplaceWith("render2()"))
        override fun ViewWriter.renderOld() {
            space {}
        }
    }

    @Suppress("Deprecation")
    public open class Direct(title: String = "", public val render: ViewWriter.()->Unit): Screen {
        override fun ViewWriter.renderOld(): Unit = this@Direct.render(this)
        override val title: Reactive<String> = Constant(title)
    }
}

@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"), DeprecationLevel.HIDDEN)
public typealias KiteUiNavigator = PageNavigator
@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"), DeprecationLevel.HIDDEN)
public typealias ScreenStack = PageNavigator
@Deprecated("Renamed to PageNavigator", ReplaceWith("PageNavigator"))
public typealias ScreenNavigator = PageNavigator


@Deprecated("Use the new popover system instead")
public var ElementContext.dialogPageNavigator by lateInitContextAddon<PageNavigator>()

@Deprecated("Use directly through context", ReplaceWith("context.pageNavigator"))
public var ElementWriter.pageNavigator
    get() = context.pageNavigator
    set(value) { context.pageNavigator = value }

@Deprecated("Use directly through context", ReplaceWith("context.mainPageNavigator"))
public var ElementWriter.mainPageNavigator
    get() = context.mainPageNavigator
    set(value) { context.mainPageNavigator = value }

@Deprecated("Use directly through context", ReplaceWith("context.dialogPageNavigator"))
public var ElementWriter.dialogPageNavigator
    get() = context.dialogPageNavigator
    set(value) { context.dialogPageNavigator = value }

@Deprecated("Use directly through context", ReplaceWith("context.pageNavigator"))
public var Element.pageNavigator
    get() = context.pageNavigator
    set(value) { context.pageNavigator = value }

@Deprecated("Use directly through context", ReplaceWith("context.mainPageNavigator"))
public var Element.mainPageNavigator
    get() = context.mainPageNavigator
    set(value) { context.mainPageNavigator = value }

@Deprecated("Use new popover system, or at least directly through context", ReplaceWith("context.dialogPageNavigator"))
public var Element.dialogPageNavigator
    get() = context.dialogPageNavigator
    set(value) { context.dialogPageNavigator = value }

@Deprecated("Use directly through context", ReplaceWith("context.pageNavigator"))
public var ContainerElement.pageNavigator
    get() = context.pageNavigator
    set(value) { context.pageNavigator = value }

@Deprecated("Use directly through context", ReplaceWith("context.mainPageNavigator"))
public var ContainerElement.mainPageNavigator
    get() = context.mainPageNavigator
    set(value) { context.mainPageNavigator = value }

@Deprecated("Use new popover system, or at least directly through context", ReplaceWith("context.dialogPageNavigator"))
public var ContainerElement.dialogPageNavigator
    get() = context.dialogPageNavigator
    set(value) { context.dialogPageNavigator = value }

@Deprecated("Renamed to pageNavigator", ReplaceWith("context.pageNavigator")) public var ViewWriter.screenNavigator by ViewWriter::pageNavigator
@Deprecated("Renamed to mainPageNavigator", ReplaceWith("context.mainPageNavigator")) public var ViewWriter.mainScreenNavigator by ViewWriter::mainPageNavigator
@Deprecated("Renamed to dialogPageNavigator", ReplaceWith("context.dialogPageNavigator")) public var ViewWriter.dialogScreenNavigator by ViewWriter::dialogPageNavigator

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator.routes", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.HIDDEN)
public val PageNavigator.Companion.mainRoutes: Routes get() = TODO()
@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.HIDDEN)
public val PageNavigator.Companion.main: PageNavigator get() = TODO()
@Deprecated("Use navigator properly", ReplaceWith("dialogPageNavigator", "com.lightningkite.kiteui.navigation.dialogPageNavigator"), level = DeprecationLevel.HIDDEN)
public val PageNavigator.Companion.dialog: PageNavigator get() = TODO()

@Deprecated("Use navigator properly", ReplaceWith("dialogPageNavigator", "com.lightningkite.kiteui.navigation.dialogPageNavigator"), level = DeprecationLevel.HIDDEN)
public val PageNavigator.dialog: PageNavigator get() = TODO()
