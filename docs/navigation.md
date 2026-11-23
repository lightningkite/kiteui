# Navigation

A KiteUI app is a stack of `Page`s held by a `PageNavigator`.  Annotate a page with `@Routable` and the KiteUI Gradle plugin generates `AutoRoutes`, which maps URLs to pages and back.  On the web the stack follows the address bar and browser history; on Android and iOS it is driven by the back button and deep links.

## Minimal example

```kotlin
@Routable("/")
object HomePage : Page {
    override fun ElementWriter.CanAddTheme.render() {
        col {
            h1("Home")
            link {
                text("Open user 42")
                to = { UserPage("42") }
            }
        }
    }
}

@Routable("users/{userId}")
class UserPage(val userId: String) : Page {
    override val title: Reactive<String> = Constant("User $userId")

    override fun ElementWriter.CanAddTheme.render() {
        h1("User $userId")
    }
}
```

The navigator is created once, from the generated routes, and handed to the root of the app:

```kotlin
val navigator = PageNavigator { AutoRoutes }

fun ViewWriter.app(navigator: PageNavigator) {
    appBase(navigator) {
        navigatorView(navigator)   // renders the current page
    }
}
```

`appBase` sets `context.pageNavigator` and the overlay frame that [dialogs](dialogs.md) open in.  How `app` gets called is per platform; see [Getting started](getting-started.md).

## Common tasks

### Navigate

```kotlin
button {
    text("Settings")
    onClick { context.pageNavigator.navigate(SettingsPage) }
}
```

| Call | Effect |
|---|---|
| `navigate(page)` | Push `page`. |
| `replace(page)` | Swap the top page for `page`. |
| `reset(page)` | Make `page` the only page, e.g. after sign-in or sign-out. |
| `goBack()` | Pop, unless only one page is left.  Returns whether it did. |
| `dismiss()` | Pop, even the last page. |

`currentPage`, `canGoBack` and `stack` are reactive, so `::shown { context.pageNavigator.canGoBack() }` works.

### Path parameters

```kotlin
@Routable("orders/{orderId}")
class OrderPage(val orderId: Int) : Page { /* ... */ }
```

Each `{name}` must match a constructor parameter of the same name.  Any serializable type works (`String`, `Int`, a `@Serializable` value class); it is encoded into the URL segment.  A page can carry several `@Routable` annotations.

### Query parameters

```kotlin
@Routable("search")
object SearchPage : Page {
    @QueryParameter val query = Signal("")
    @QueryParameter val page = Signal(1)

    override fun ElementWriter.CanAddTheme.render() {
        col {
            textInput { hint = "Search"; keyboardHints = KeyboardHints.title; content bind query }
            text { ::content { "Results for '${query()}', page ${page()}" } }
        }
    }
}
```

A `@QueryParameter` property is a `Signal` on the page.  It is read from the URL when the page is parsed and written back to the URL whenever it changes, so `/search?query=kite&page=2` is shareable.

### Links

```kotlin
link {
    text("Profile")
    to = { UserPage("me") }
}
link {
    text("Start over")
    to = { HomePage }
    resetsStack = true
}
externalLink {
    text("KiteUI on GitHub")
    to = "https://github.com/lightningkite/kiteui"
    newTab = true
}
```

Prefer `link` to a button that navigates: on the web it is a real `<a href>`, so it can be opened in a new tab, copied and crawled.

### Page title

`title` defaults to the class name, split into words, without a trailing `Page` or `Screen` (`UserProfilePage` → "User Profile").  Override it with a `Reactive<String>`; the nav shells show it in the app bar and the web uses it as the document title.

### App shells

The `nav*` functions build a complete shell: app bar, navigation, and the page area.

```kotlin
@OptIn(ExperimentalKiteUi::class, EvolvingAppearance::class)
fun ViewWriter.app(navigator: PageNavigator) {
    appBase(navigator) {
        navSidebar(
            appLogo = Icon.home.toImageSource(Color.white),
            appName = "My App",
            menuItems = {
                listOf(
                    Nav.Link(title = "Home", icon = Icon.home) { HomePage },
                    Nav.Link(title = "Search", icon = Icon.search) { SearchPage },
                    Nav.Group(title = "Admin", icon = Icon.settings, children = listOf(
                        Nav.Link(title = "Settings", icon = Icon.settings) { SettingsPage },
                    )),
                    Nav.External(title = "Docs", icon = Icon.externalLink, to = "https://example.com"),
                )
            },
            footerItems = {
                listOf(Nav.Action(title = "Sign out", icon = Icon.logout) { navigator.reset(HomePage) })
            },
        )
    }
}
```

| Shell | Shape |
|---|---|
| `navSidebar` | Labeled rail, narrow rail, then a drawer as the window shrinks.  For apps with many or grouped destinations. |
| `navTopBar` | Links in the top bar, collapsing into a drawer. |
| `navTabs` | Bottom tab bar on narrow screens, rail on wide ones. |
| `navDrawer` | A drawer at every width. |

The item lists are reactive lambdas: read a signal inside them (the signed-in user's role, say) and the nav rebuilds.  `showNav = { navigator.currentPage() !is LoginPage }` hides the nav on chosen pages.  `count` on an item shows a badge (`0` is a dot).  To lay the parts out yourself, construct a `Nav` and place `appBar`, `rail`, `tabBar` and `drawer`; the shells above are written exactly that way and are short.

These are `@ExperimentalKiteUi` and `@EvolvingAppearance`: their API is stable, their look is refined between releases, so don't screenshot-test them.  The older `appNav(navigator) { navItems = listOf(NavLink(...)) }` with `appNavFactory` still works.

### Warn before leaving

```kotlin
@Routable("editor")
object EditorPage : Page, CanBlockBack {
    val draft = Signal("")
    override fun onNavigateAwayAttempt(): Boolean = draft.value.isEmpty()
    /* ... */
}
```

Returning `false` makes the navigator ask before leaving.  Only the web asks (a `confirm` prompt, and the browser's own prompt on tab close); other platforms navigate anyway.

### A page for unknown URLs

```kotlin
@FallbackRoute
@Routable("not-found")
class NotFoundPage : Page { /* ... */ }
```

### Deep links

On the web every route is a URL already.  On iOS, KiteUI's app delegate passes an opened URL to `navigator.navigateUrlLikePath(path)`, which returns `false` if no route matches.  `resetUrlLikePath` does the same but replaces the stack.

## Gotchas

- **`render()` runs each time the page is shown, but an `object` page keeps its properties.**  Signals declared inside `render()` start fresh every visit; signals declared on the page object (including `@QueryParameter`s) survive navigating away and back.  Use a `class` page when every visit should start clean.
- **Two pages with the same `@Routable` path** compile, and one silently wins.  Search for the path before adding a page.
- **Routes are generated at build time** by scanning source text.  Use a string literal in `@Routable`, not a constant.
- **`@QueryParameter("q")` renames the URL key**: `@QueryParameter("q") val query = Signal("")` reads and writes `?q=`.  Without an argument the key is the property name.
- **Don't build tabs from sibling `shownWhen` blocks.**  Make each tab a page, which is URL-addressable and only exists while shown, or use `swapping(current = { tab() }) { }`.
- `context.pageNavigator` is the current API; the bare `pageNavigator`, `mainPageNavigator` and `screenNavigator` are deprecated aliases.

## See also

- [Dialogs](dialogs.md): dialogs close on back before the page does
- [Example app source](../example-app/src/commonMain/kotlin/com/lightningkite/mppexampleapp/App.kt): `navSidebar`, `navTopBar`, `navTabs` and `navDrawer` switchable at runtime
