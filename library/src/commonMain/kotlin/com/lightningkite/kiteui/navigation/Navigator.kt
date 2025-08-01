package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.rContextAddonInit
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
typealias KiteUiNavigator = PageNavigator
@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
typealias ScreenStack = PageNavigator
class PageNavigator(private val routesGetter: ()->Routes) {
    val routes: Routes by lazy { routesGetter() }

    fun navigateUrlLikePath(path: String) = routes.parse(UrlLikePath.fromUrlString(path))?.let { navigate(it) }
    fun resetUrlLikePath(path: String) = routes.parse(UrlLikePath.fromUrlString(path))?.let { reset(it) }

    val stack: Signal<List<Page>> = Signal(listOf())
    fun wrap(screen: Page): Page = screen
    
    val currentPage: Reactive<Page?> = remember { stack().lastOrNull() }
    val canGoBack: Reactive<Boolean> = remember { stack().size > 1 }
    
    fun navigate(screen: Page) = navigateRaw(wrap(screen))
    fun replace(screen: Page) = replaceRaw(wrap(screen))
    fun reset(screen: Page) = resetRaw(wrap(screen))

    fun navigateRaw(screen: Page) {
        stack.value += screen
    }
    fun replaceRaw(screen: Page) {
        stack.value = stack.value.dropLast(1) + screen
    }
    fun resetRaw(screen: Page) {
        stack.value = listOf(screen)
    }

    fun goBack(): Boolean {
        if(stack.value.size <= 1)
            return false
        stack.value = stack.value.dropLast(1)
        return true
    }

    fun dismiss(): Boolean {
        if(stack.value.isEmpty())
            return false
        stack.value = stack.value.dropLast(1)
        return true
    }
    fun clear() {
        stack.value = listOf()
    }
    fun isStackEmpty(): Boolean = stack.value.isEmpty()

    companion object {
        @Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator.routes", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
        val mainRoutes: Routes get() = TODO()
        @Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
        val main: PageNavigator get() = TODO()
        @Deprecated("Use navigator properly", ReplaceWith("dialogPageNavigator", "com.lightningkite.kiteui.navigation.dialogPageNavigator"), level = DeprecationLevel.ERROR)
        val dialog: PageNavigator get() = TODO()
    }
    @Deprecated("Use navigator properly", ReplaceWith("dialogPageNavigator", "com.lightningkite.kiteui.navigation.dialogPageNavigator"), level = DeprecationLevel.ERROR)
    val dialog: PageNavigator get() = TODO()
}

expect fun PageNavigator.bindToPlatform(context: RContext)

var ViewWriter.pageNavigator by rContextAddonInit<PageNavigator>()
var ViewWriter.mainPageNavigator by rContextAddonInit<PageNavigator>()
var ViewWriter.dialogPageNavigator by rContextAddonInit<PageNavigator>()

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
val PlatformNavigator: PageNavigator get() = TODO()