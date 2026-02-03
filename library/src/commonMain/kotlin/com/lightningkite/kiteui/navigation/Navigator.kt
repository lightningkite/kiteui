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
import kotlin.reflect.KProperty



@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
typealias KiteUiNavigator = PageNavigator
@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
typealias ScreenStack = PageNavigator
class PageNavigator(private val routesGetter: ()->Routes) {
    val routes: Routes by lazy { routesGetter() }

    fun navigateUrlLikePath(path: String) = routes.parse(UrlLikePath.fromUrlString(path))?.let { navigate(it) }
    fun resetUrlLikePath(path: String) = routes.parse(UrlLikePath.fromUrlString(path))?.let { reset(it) }

    val stack: MutableReactiveValue<List<Page>> = object : MutableReactiveValue<List<Page>>, BaseReactiveValue<List<Page>>(listOf()) {
        override fun valueSet(value: List<Page>) {
            val canNavigate = (stack.value.last() as? CanBlockBack)?.onNavigateAwayAttempt() ?: true
            val confirmed = if (canNavigate) true else askForConfirmNavigateAway()
            if (confirmed) {
                super.valueSet(value)
            }
        }
    }

    fun wrap(screen: Page): Page = screen
    
    val currentPage: Reactive<Page?> = remember { stack().lastOrNull() }
    val canGoBack: Reactive<Boolean> = remember { stack().size > 1 }
    
    fun navigate(screen: Page) = navigateRaw(wrap(screen))
    fun replace(screen: Page) = replaceRaw(wrap(screen))
    fun reset(screen: Page) = resetRaw(wrap(screen))
    fun navigateRaw(screen: Page) {
        stack.valueSet(stack.value + screen)
    }
    fun replaceRaw(screen: Page) {
        stack.valueSet(stack.value.dropLast(1) + screen)
    }
    fun resetRaw(screen: Page) {
        stack.valueSet(listOf(screen))
    }

    fun goBack(): Boolean {
        if(stack.value.size <= 1)
            return false
        stack.valueSet(stack.value.dropLast(1))
        return true
    }

    fun dismiss(): Boolean {
        if(stack.value.isEmpty())
            return false
        stack.valueSet(stack.value.dropLast(1))
        return true
    }
    fun clear() {
        stack.valueSet(listOf())
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

expect fun PageNavigator.askForConfirmNavigateAway(): Boolean
expect fun ViewWriter.addListenerForNavigateAway(pageNav: PageNavigator)

var ViewWriter.pageNavigator by rContextAddonInit<PageNavigator>()
var ViewWriter.mainPageNavigator by rContextAddonInit<PageNavigator>()
var ViewWriter.dialogPageNavigator by rContextAddonInit<PageNavigator>()

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
val PlatformNavigator: PageNavigator get() = TODO()