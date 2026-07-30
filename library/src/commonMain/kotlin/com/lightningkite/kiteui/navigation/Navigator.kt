package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.lateInitContextAddon
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember

public class PageNavigator(private val routesGetter: ()->Routes) {
    public val routes: Routes by lazy { routesGetter() }

    public fun navigateUrlLikePath(path: String): Boolean = routes.parse(UrlLikePath.fromUrlString(path))?.let { navigate(it); true } ?: false
    public fun resetUrlLikePath(path: String): Boolean = routes.parse(UrlLikePath.fromUrlString(path))?.let { reset(it); true } ?: false

    public val stack: Signal<List<Page>> = Signal(listOf())

    public val currentPage: Reactive<Page?> = remember { stack().lastOrNull() }
    public val canGoBack: Reactive<Boolean> = remember { stack().size > 1 }

    private val allowNavigate get(): Boolean {
        val notBlocked = (stack.value.lastOrNull() as? CanBlockBack)?.onNavigateAwayAttempt() ?: true
        return if (notBlocked) true else askForConfirmNavigateAway()
    }

    public fun navigate(screen: Page) {
        if (allowNavigate) {
            stack.value += screen
        }
    }

    public fun replace(screen: Page) {
        if (allowNavigate) {
            stack.value = stack.value.dropLast(1) + screen
        }
    }

    public fun reset(screen: Page) {
        if (allowNavigate) {
            stack.value = listOf(screen)
        }
    }

    public fun goBack(): Boolean {
        if(stack.value.size <= 1 || !allowNavigate) {
            return false
        }
        stack.value = stack.value.dropLast(1)
        return true
    }

    public fun dismiss(): Boolean {
        if(stack.value.isEmpty() || !allowNavigate) {
            return false
        }
        stack.value = stack.value.dropLast(1)
        return true
    }
    public fun clear() {
        if (allowNavigate) {
            stack.value = listOf()
        }
    }
    public fun isStackEmpty(): Boolean = stack.value.isEmpty()

    public companion object;
}

public expect fun PageNavigator.bindToPlatform(context: ElementContext)

internal expect fun PageNavigator.askForConfirmNavigateAway(): Boolean

public var ElementContext.pageNavigator: PageNavigator by lateInitContextAddon<PageNavigator>()
public var ElementContext.mainPageNavigator: PageNavigator by lateInitContextAddon<PageNavigator>()
