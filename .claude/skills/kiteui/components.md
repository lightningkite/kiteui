# KiteUI Components

Condensed from [docs/components.md](../../../docs/components.md); read that for more examples.

Builders: `com.lightningkite.kiteui.views.direct.*`.  `field`/`label`/`toast`: `views.l2.*`.  `Icon`, `KeyboardHints`, `rem`, `Align`: `kiteui.models.*`.

## Text

```kotlin
h1("Title") … h6("…"); text("Body"); subtext("Muted"); text("Centered", align = Align.Center)
text { ::content { "Count: ${count()}" } }           // reactive
text { content = s; lineClamp = 2 }                   // also ellipsis, wraps, wordBreak
bold.text("…"); italic.text("…")                      // underline, strikethrough, allCaps, textSize(x)
```

## Buttons and actions

```kotlin
button {
    text("Save")                     // a button has NO text property; nest views
    onClick { api.save() }           // suspending; shows loading; errors go to exception handlers; 500 ms tap cap
}
button { text("x"); onLongClick { } }
important.button { }; danger.button { }   // critical, warning, affirmative, card
button { text("Next"); ::enabled { agreed() }; onClick { } }

// Shared action: Enter in the input and the button run the same Action
val search = Action("Search", Icon.search) { runSearch(query()) }
textInput { content bind query; action = search }
button { icon(Icon.search, "Search"); action = search }
```

`onClick { }` and `Action(...) { }` lambdas have no receiver of their own; `context` resolves to the enclosing element's or `render()`'s, so `context.toast(...)` and `context.pageNavigator.navigate(...)` work inside them.  Use `context.toast`, not bare `toast` (deprecated).

## Links

```kotlin
link { text("Settings"); to = { SettingsPage } }          // <a href> on web
externalLink { text("Docs"); to = "https://…"; newTab = true }
```

## Inputs

```kotlin
textInput { hint = "Email"; keyboardHints = KeyboardHints.email; content bind email }   // String
textArea { keyboardHints = KeyboardHints.paragraph; content bind notes }
textInput { content bind nullableString.nullToBlank() }
numberInput { range = 0.0..100.0; content bind price }          // Double?  (Int? → .asDouble())
formattedTextInput { content bind raw; format = TextFormat(isRawData = { it.isDigit() }, format = { it }) }
phoneNumberInput { content bind phone }
autoCompleteTextField { suggestions = listOf("A", "B"); content bind s }
localDateField { content bind date }        // LocalDate?   also range = a..b
localTimeField { content bind time }        // LocalTime?
localDateTimeField { content bind at }      // LocalDateTime?
```

`KeyboardHints`: `paragraph`, `title`, `id`, `integer`, `integerWithNegative`, `decimal`, `decimalWithNegative`, `phone`, `email`, `password`, `newPassword`, `oneTimeCode`.  Always set one.  No `url`: build `KeyboardHints(case, type, autocomplete, autocorrect = false)`.

Dates are `kotlinx.datetime`; display with `renderToString()` from `com.lightningkite.kiteui.locale` (LocalDate, LocalTime, LocalDateTime, Instant).

## Choices

```kotlin
checkbox { checked bind flag }; switch { checked bind flag }
radioButton { checked bind size.equalTo(Size.Small) }
expanding.radioToggleButton { centered.text("Small"); checked bind size.equalTo(Size.Small) }  // segmented
toggleButton { text("Bold"); checked bind isBold }
checkbox { checked bind tags.contains("urgent") }                 // Set/List membership
select { bind(size, Constant(Size.entries)) { it.name } }          // list MUST be Reactive: wrap in Constant
slider { range(0f, 1f, 0.1f); value bind volume }                  // Float
progressBar(ratio); circularProgress { ::ratio { r() } }; activityIndicator()
```

## Media

```kotlin
image { source = Resources.myImage; scaleType = ImageScaleType.Crop; description = "…" }  // Fit, Crop, Stretch, NoScale
image { source = ImageRemote(url); description = "…" }
icon(Icon.home, "Home"); icon(Icon.home.resize(3.rem), "Home")
video { source = VideoRemote(url); showControls = true }
webView { load(WebViewSource.Url(url)) }        // or WebViewSource.Html(html); permissions denied by default
menuButton { icon(Icon.moreVert, "More"); requireClick = true; opensMenu { col { button { … } } } }
separator(); space(); expanding.space()
```

Built-in icons ONLY: `add arrowBack block certification chat chevronDown chevronLeft chevronRight close collapse copy darkMode delete deleteForever done dot download email expand externalLink filterList group help home info lightMode list login logout menu moreHoriz moreVert notification notificationFilled passkey person remove search send settings sort star starFilled sync upload warning`.  No `save`, `edit`, `calendar`, `event`.  Custom: `Icon(1.5.rem, 1.5.rem, 0, -960, 960, 960, listOf("<material symbol path>"))`.

## Custom components

```kotlin
fun ElementWriter.CanAddTheme.userCard(user: Reactive<User>) {     // NOT plain ElementWriter: card etc. won't resolve
    card.row {
        icon(Icon.person, "User")
        expanding.col {
            text { ::content { user().name } }
            subtext { ::content { user().email } }
        }
    }
}
// callers can still add modifiers: expanding.userCard(user)
```

Only extract a component when it is reused; inline markup is fine.
