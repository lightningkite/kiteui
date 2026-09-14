package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.lateInitContextAddon
import com.lightningkite.kiteui.views.lazyContextAddon
import com.lightningkite.reactive.core.Reactive
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.core.remember

public class PageNavigator(private val routesGetter: ()->Routes) {
    public val routes: Routes by lazy { routesGetter() }

    // These take a raw URL string, which in practice arrives from outside the application
    // (an OS deep link, a restored navigation stack), so parsing must not be able to throw.
    // Returning false lets the caller decide what to do; crashing is never the right answer
    // for input the application did not produce.
    public fun navigateUrlLikePath(path: String): Boolean = routes.parse(UrlLikePath.fromUrlString(path))?.let { navigate(it); true } ?: false
    public fun resetUrlLikePath(path: String): Boolean = routes.parse(UrlLikePath.fromUrlString(path))?.let { reset(it); true } ?: false

    public val stack: Signal<List<Page>> = Signal(listOf())

    // reentrancyLimit>0: a page whose render() re-entrantly navigates (the "redirect page"
    // pattern - show a spinner, then replace()/navigate() from a coroutine that doesn't actually
    // suspend) mutates `stack` while this calculation may still be unwinding an earlier mutation's
    // listener cascade - most visibly the very first time something reads currentPage/canGoBack
    // and activates this remember{}, since that activation is itself nested inside the stack
    // mutation that triggered it. Without a positive limit here, that shows up as a permanently
    // stuck currentPage (a swallowed ReactiveReentrancyException) even though `stack` itself
    // updated correctly and the redirect wrote to it in good faith. See SwapView.swapping()'s
    // matching reentrancyLimit, which handles the same pattern one layer up but does not, by
    // itself, cover this one - the two were found and fixed together.
    public val currentPage: Reactive<Page?> = remember(reentrancyLimit = 8) { stack().lastOrNull() }
    public val canGoBack: Reactive<Boolean> = remember(reentrancyLimit = 8) { stack().size > 1 }

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

public var ElementContext.pageNavigator: PageNavigator by lazyContextAddon {
    throw IllegalStateException(
        "'ElementContext.pageNavigator' has not been initialized. " +
                "It is recommended to call 'appBase { ... }' at the root of your app to properly initialize pageNavigator, as well as many other properties."
    )
}
