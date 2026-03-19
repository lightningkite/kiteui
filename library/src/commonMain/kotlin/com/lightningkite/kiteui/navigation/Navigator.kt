package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.lateInitContextAddon
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember


@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
typealias KiteUiNavigator = PageNavigator
@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
typealias ScreenStack = PageNavigator
class PageNavigator(private val routesGetter: ()->Routes) {
    val routes: Routes by lazy { routesGetter() }

    fun navigateUrlLikePath(path: String) = routes.parse(UrlLikePath.fromUrlString(path))?.let { navigate(it) }
    fun resetUrlLikePath(path: String) = routes.parse(UrlLikePath.fromUrlString(path))?.let { reset(it) }

    val stack: Signal<List<Page>> = Signal(listOf())

    val currentPage: Reactive<Page?> = remember { stack().lastOrNull() }
    val canGoBack: Reactive<Boolean> = remember { stack().size > 1 }

    private val allowNavigate get(): Boolean {
        val notBlocked = (stack.value.lastOrNull() as? CanBlockBack)?.onNavigateAwayAttempt() ?: true
        return if (notBlocked) true else askForConfirmNavigateAway()
    }

    fun navigate(screen: Page) {
        if (allowNavigate) {
            stack.value += screen
        }
    }

    fun replace(screen: Page) {
        if (allowNavigate) {
            stack.value = stack.value.dropLast(1) + screen
        }
    }

    fun reset(screen: Page) {
        if (allowNavigate) {
            stack.value = listOf(screen)
        }
    }

    fun goBack(): Boolean {
        if(stack.value.size <= 1 || !allowNavigate) {
            return false
        }
        stack.value = stack.value.dropLast(1)
        return true
    }

    fun dismiss(): Boolean {
        if(stack.value.isEmpty() || !allowNavigate) {
            return false
        }
        stack.value = stack.value.dropLast(1)
        return true
    }
    fun clear() {
        if (allowNavigate) {
            stack.value = listOf()
        }
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

expect fun PageNavigator.bindToPlatform(context: ElementContext)

internal expect fun PageNavigator.askForConfirmNavigateAway(): Boolean

var ViewWriter.pageNavigator by lateInitContextAddon<PageNavigator>()
var ViewWriter.mainPageNavigator by lateInitContextAddon<PageNavigator>()
var ViewWriter.dialogPageNavigator by lateInitContextAddon<PageNavigator>()

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
val PlatformNavigator: PageNavigator get() = TODO()