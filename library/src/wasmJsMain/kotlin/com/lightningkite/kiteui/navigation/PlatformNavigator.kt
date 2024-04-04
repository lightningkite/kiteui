package com.lightningkite.kiteui.navigation

import com.lightningkite.kiteui.FallbackRoute
import com.lightningkite.kiteui.decodeURIComponent
import com.lightningkite.kiteui.encodeURIComponent
import com.lightningkite.kiteui.reactive.*
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.w3c.dom.*
import org.w3c.dom.url.URLSearchParams

@Serializable
data class BaseUrlScript(val baseUrl: String)

actual object PlatformNavigator : KiteUiNavigator {
    var basePath = (document.getElementById("baseUrlLocation") as? HTMLScriptElement)?.innerText?.let {
        Json.decodeFromString<BaseUrlScript>(it).baseUrl
    } ?: "/"

    override fun isStackEmpty(): Boolean = false

    private lateinit var _routes: Routes
    actual override var routes: Routes
        get() = _routes
        set(value) {
            _routes = value
            if(_currentScreen.value is KiteUiScreen.Empty) {
                _currentScreen.value = routes.parse(window.location.urlLike()) ?: routes.fallback
            }
            CalculationContext.NeverEnds.reactiveScope {
                currentScreen.await().let {
                    val rendered = value.render(it)
                    rendered?.listenables?.forEach { rerunOn(it) }
                    rendered?.urlLikePath?.let {
                        if(window.location.urlLike() != it) {
                            window.history.replaceState(currentIndex.toJsNumber(), "", basePath + it.render())
                        }
                    }
                }
            }
        }

    private fun Location.urlLike() = UrlLikePath(
        segments = pathname.removePrefix(basePath).split('/').filter { it.isNotBlank() },
        parameters = search.trimStart('?').split('&').filter { it.isNotBlank() }.associate { it.substringBefore('=') to decodeURIComponent(it.substringAfter('=')) }
    )

    override val dialog: KiteUiNavigator = LocalNavigator({ routes })

    private var nextIndex: Int = 1
    private val currentIndexProp = Property(0)
    private var currentIndex: Int by currentIndexProp

    init {
        window.addEventListener("popstate", { event ->
            event as PopStateEvent
            val index = (event.state as? JsNumber)?.toInt()
            when {
                index == null -> KiteUiNavigator.Direction.Neutral
                index > currentIndex -> direction = KiteUiNavigator.Direction.Forward
                index < currentIndex -> direction = KiteUiNavigator.Direction.Back
                else -> direction = KiteUiNavigator.Direction.Neutral
            }
            currentIndex = index ?: 0
            navigate(window.location.urlLike(), false)
        })
    }

    private val String.asSegments: List<String> get() = split('/').filter { it.isNotBlank() }
    private val _currentScreen = Property<KiteUiScreen>(KiteUiScreen.Empty)
    override val currentScreen: Readable<KiteUiScreen>
        get() = _currentScreen
    override val canGoBack: Readable<Boolean>
        get() = shared {
            currentIndexProp.await() > 0
        }

    override var direction: KiteUiNavigator.Direction? = null
        private set
    private fun navigate(urlLikePath: UrlLikePath, pushState: Boolean) {
        val kiteuiScreen = routes.parse(urlLikePath) ?: routes.fallback
        navigate(urlLikePath, kiteuiScreen, pushState)
    }
    private fun navigate(kiteuiScreen: KiteUiScreen, pushState: Boolean) {
        val path = routes.render(kiteuiScreen)?.urlLikePath ?: return
        navigate(path, kiteuiScreen, pushState)
    }

    private fun navigate(
        path: UrlLikePath,
        kiteuiScreen: KiteUiScreen,
        pushState: Boolean,
    ) {
        if (pushState) {
            currentIndex = nextIndex
            window.history.pushState(
                nextIndex++.toJsNumber(), "", (basePath + path.render()).also { println("pushing state $it") }
            )
            window.history.scrollRestoration = ScrollRestoration.MANUAL
        } else {
            window.history.replaceState(currentIndex.toJsNumber(), "", basePath + path.render())
            window.history.scrollRestoration = ScrollRestoration.MANUAL
        }
        _currentScreen.value = kiteuiScreen
    }

    override fun navigateRaw(screen: KiteUiScreen) {
        direction = KiteUiNavigator.Direction.Forward
        navigate(screen, pushState = true)
    }

    override fun replaceRaw(screen: KiteUiScreen) {
        direction = KiteUiNavigator.Direction.Neutral
        navigate(screen, pushState = false)
    }

    override fun resetRaw(screen: KiteUiScreen) {
        direction = KiteUiNavigator.Direction.Neutral
        navigate(screen, pushState = false)
        currentIndex = 0
    }

    override fun goBack(): Boolean {
        window.history.go(-1)
        return true
    }

    override fun dismiss(): Boolean {
        window.history.go(-1)
        return true
    }

    override fun clear() {
        window.history.go(-1)
    }
}
