package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.rContextAddonInit

@Deprecated("Use ScreenNavigator directly instead", ReplaceWith("ScreenNavigator", "com.lightningkite.kiteui.navigation.ScreenNavigator"))
typealias KiteUiNavigator = ScreenNavigator
@Deprecated("Use ScreenNavigator directly instead", ReplaceWith("ScreenNavigator", "com.lightningkite.kiteui.navigation.ScreenNavigator"))
typealias ScreenStack = ScreenNavigator
class ScreenNavigator(private val routesGetter: ()->Routes) {
    val routes: Routes by lazy { routesGetter() }
    
    val stack: Property<List<Page>> = Property(listOf())
    fun wrap(screen: Page): Page = screen
    
    val currentScreen: Readable<Page?> = shared { stack().lastOrNull() }
    val canGoBack: Readable<Boolean> = shared { stack().size > 1 }
    
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
        @Deprecated("Use navigator properly", ReplaceWith("mainScreenNavigator.routes", "com.lightningkite.kiteui.navigation.mainScreenNavigator"), level = DeprecationLevel.ERROR)
        val mainRoutes: Routes get() = TODO()
        @Deprecated("Use navigator properly", ReplaceWith("mainScreenNavigator", "com.lightningkite.kiteui.navigation.mainScreenNavigator"), level = DeprecationLevel.ERROR)
        val main: ScreenNavigator get() = TODO()
        @Deprecated("Use navigator properly", ReplaceWith("dialogScreenNavigator", "com.lightningkite.kiteui.navigation.dialogScreenNavigator"), level = DeprecationLevel.ERROR)
        val dialog: ScreenNavigator get() = TODO()
    }
    @Deprecated("Use navigator properly", ReplaceWith("dialogScreenNavigator", "com.lightningkite.kiteui.navigation.dialogScreenNavigator"), level = DeprecationLevel.ERROR)
    val dialog: ScreenNavigator get() = TODO()
}

expect fun ScreenNavigator.bindToPlatform(context: RContext)

var ViewWriter.screenNavigator by rContextAddonInit<ScreenNavigator>()
var ViewWriter.mainScreenNavigator by rContextAddonInit<ScreenNavigator>()
var ViewWriter.dialogScreenNavigator by rContextAddonInit<ScreenNavigator>()

@Deprecated("Use navigator properly", ReplaceWith("mainScreenNavigator", "com.lightningkite.kiteui.navigation.mainScreenNavigator"), level = DeprecationLevel.ERROR)
val PlatformNavigator: ScreenNavigator get() = TODO()