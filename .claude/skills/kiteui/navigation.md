# KiteUI Navigation & Pages

Full chapter: [docs/navigation.md](../../../docs/navigation.md).

## Pages

```kotlin
@Routable("users/{userId}")                       // {name} must match a constructor param
class UserPage(val userId: String) : Page {        // any serializable type: Int, value class...
    override val title: Reactive<String> = Constant("User $userId")   // optional; default from class name

    @QueryParameter val tab = Signal("posts")      // ?tab=... read from and written to the URL

    override fun ElementWriter.CanAddTheme.render() {
        col { h1("User $userId") }
    }
}
```

- `render` receiver is `ElementWriter.CanAddTheme` (not `ViewWriter`).
- `object` pages keep their properties across visits; signals created inside `render()` reset each visit.
- `@FallbackRoute` marks the not-found page.  `@QueryParameter("q") val query` uses `?q=`; with no argument the key is the property name.
- Routes are generated (`AutoRoutes`) by scanning source: string literals only; duplicate paths silently pick one.  `grep -r '@Routable("path")'` before adding.

## App root

```kotlin
val navigator = PageNavigator { AutoRoutes }
appBase(navigator) { navigatorView(navigator) }   // sets context.pageNavigator + overlay frame
```

Shells (`@OptIn(ExperimentalKiteUi::class, EvolvingAppearance::class)`), used inside `appBase`:

```kotlin
navSidebar(                     // or navTopBar / navTabs / navDrawer
    appLogo = Icon.home.toImageSource(Color.white), appName = "My App",
    showNav = { navigator.currentPage() !is LoginPage },
    menuItems = { listOf(
        Nav.Link(title = "Home", icon = Icon.home) { HomePage },
        Nav.Group(title = "Admin", icon = Icon.settings, children = listOf(/* ... */)),
        Nav.External(title = "Docs", icon = Icon.externalLink, to = "https://..."),
    ) },
    footerItems = { listOf(Nav.Action(title = "Sign out", icon = Icon.logout) { signOut() }) },
)
```

Item lambdas are reactive.  Older `appNav(navigator) { navItems = listOf(NavLink(...)) }` still works.

## Navigating

Always through `context.pageNavigator` (bare `pageNavigator` / `mainPageNavigator` are deprecated):

```kotlin
onClick { context.pageNavigator.navigate(UserPage("42")) }
context.pageNavigator.replace(page)   // swap top
context.pageNavigator.reset(page)     // only page (login/logout)
context.pageNavigator.goBack()        // false if at root
```

`currentPage`, `canGoBack`, `stack` are reactive.

## Links (prefer over navigating buttons; real `<a href>` on web)

```kotlin
link { text("Profile"); to = { UserPage("me") } }            // resetsStack = true to reset
externalLink { text("Docs"); to = "https://example.com"; newTab = true }
```

## Blocking navigation

`Page, CanBlockBack` with `override fun onNavigateAwayAttempt() = !dirty.value`.  Only the web prompts; other platforms navigate anyway.

## Tabs

Don't build tabs from sibling `shownWhen` branches (all stay alive; `expanding` children collapse).  Make each tab a `@Routable` page, or use `swapping(current = { tab() }) { }`.
