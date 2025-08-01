package com.lightningkite.kiteui.navigation

import com.lightningkite.signal.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.rContextAddonInit

@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
public typealias KiteUiNavigator = PageNavigator
@Deprecated("Use PageNavigator directly instead", ReplaceWith("PageNavigator", "com.lightningkite.kiteui.navigation.PageNavigator"))
public typealias ScreenStack = PageNavigator
public class PageNavigator(private val routesGetter: ()->Routes) {
    public val routes: Routes by lazy { routesGetter() }

    public fun navigateUrlLikePath(path: String): Unit? = routes.parse(UrlLikePath.fromUrlString(path))?.let { navigate(it) }
    public fun resetUrlLikePath(path: String): Unit? = routes.parse(UrlLikePath.fromUrlString(path))?.let { reset(it) }

    public val stack: Property<List<Page>> = Property(listOf())
    public fun wrap(screen: Page): Page = screen
    
    public val currentPage: Readable<Page?> = shared { stack().lastOrNull() }
    public val canGoBack: Readable<Boolean> = shared { stack().size > 1 }
    
    public fun navigate(screen: Page): Unit = navigateRaw(wrap(screen))
    public fun replace(screen: Page): Unit = replaceRaw(wrap(screen))
    public fun reset(screen: Page): Unit = resetRaw(wrap(screen))

    public fun navigateRaw(screen: Page) {
        stack.value += screen
    }
    public fun replaceRaw(screen: Page) {
        stack.value = stack.value.dropLast(1) + screen
    }
    public fun resetRaw(screen: Page) {
        stack.value = listOf(screen)
    }

    public fun goBack(): Boolean {
        if(stack.value.size <= 1)
            return false
        stack.value = stack.value.dropLast(1)
        return true
    }

    public fun dismiss(): Boolean {
        if(stack.value.isEmpty())
            return false
        stack.value = stack.value.dropLast(1)
        return true
    }
    public fun clear() {
        stack.value = listOf()
    }
    public fun isStackEmpty(): Boolean = stack.value.isEmpty()

    public companion object {
        @Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator.routes", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
        public val mainRoutes: Routes get() = TODO()
        @Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
        public val main: PageNavigator get() = TODO()
        @Deprecated("Use navigator properly", ReplaceWith("dialogPageNavigator", "com.lightningkite.kiteui.navigation.dialogPageNavigator"), level = DeprecationLevel.ERROR)
        public val dialog: PageNavigator get() = TODO()
    }
    @Deprecated("Use navigator properly", ReplaceWith("dialogPageNavigator", "com.lightningkite.kiteui.navigation.dialogPageNavigator"), level = DeprecationLevel.ERROR)
    public val dialog: PageNavigator get() = TODO()
}

public expect fun PageNavigator.bindToPlatform(context: RContext)

public var ViewWriter.pageNavigator by rContextAddonInit<PageNavigator>()
public var ViewWriter.mainPageNavigator by rContextAddonInit<PageNavigator>()
public var ViewWriter.dialogPageNavigator by rContextAddonInit<PageNavigator>()

@Deprecated("Use navigator properly", ReplaceWith("mainPageNavigator", "com.lightningkite.kiteui.navigation.mainPageNavigator"), level = DeprecationLevel.ERROR)
public val PlatformNavigator: PageNavigator get() = TODO()