# Components

Every view is a function on `ElementWriter`: call it inside a container and it is added there.  The trailing lambda configures the view; set plain properties directly, and bind changing ones with `::property { }` or `bind` (see [Reactivity](reactivity.md)).

Builders live in `com.lightningkite.kiteui.views.direct`; `field`, `label` and `toast` live in `com.lightningkite.kiteui.views.l2`; `Icon`, `KeyboardHints` and `rem` live in `com.lightningkite.kiteui.models`.

## Minimal example

```kotlin
val name = Signal("")

col {
    h1("Hello")
    textInput {
        hint = "Your name"
        keyboardHints = KeyboardHints.title
        content bind name
    }
    text { ::content { "Hello, ${name()}!" } }
    important.button {
        text("Save")
        onClick { save(name()) }
    }
}
```

## Common tasks

### Show text

```kotlin
h1("Title"); h2("Section"); h3("…"); h4("…"); h5("…"); h6("…")
text("Body text")
subtext("Secondary text")
text("Centered", align = Align.Center)

text { ::content { "Count: ${count()}" } }          // reactive
text { content = longText; lineClamp = 2 }           // also: ellipsis, wraps, wordBreak
bold.text("Bold"); italic.subtext("Italic")         // also: underline, strikethrough, allCaps, textSize(…)
```

### Buttons

```kotlin
button {
    text("Refresh")
    onClick { reload() }                 // suspending; the button shows loading until it returns
}

button {
    row { icon(Icon.add, "Add"); text("Add item") }
    onClick { addItem() }
    onLongClick { context.toast("Long press") }
}

important.button { text("Primary"); onClick { } }
danger.button { text("Delete"); onClick { } }   // also: critical, warning, affirmative, card

button {
    text("Continue")
    ::enabled { agreed() }
    onClick { next() }
}
```

A button is a container: put any views inside it.  `onClick` wraps its lambda in an [`Action`](error-handling.md), so a thrown exception is shown to the user, and taps within 500 ms are ignored.

### One action for a button and a text field

A text input runs its `action` when the user presses Enter.  Share one `Action` between the input and a button so both do the same thing, with one loading and error state:

```kotlin
val search = Action("Search", Icon.search) { runSearch(query()) }

row {
    expanding.textInput {
        hint = "Search"
        content bind query
        action = search
    }
    button {
        icon(Icon.search, "Search")
        action = search
    }
}
```

### Links

```kotlin
link {
    text("Settings")
    to = { SettingsPage }        // a Page; see Navigation
}
externalLink {
    text("Kotlin docs")
    to = "https://kotlinlang.org/docs/"
    newTab = true
}
```

Use `link` rather than a button that navigates: on the web it is a real `<a href>`, so it can be opened in a new tab.

### Text input

```kotlin
textInput {
    hint = "you@example.com"
    keyboardHints = KeyboardHints.email
    content bind email                        // MutableReactive<String>
}
textArea {
    hint = "Notes"
    keyboardHints = KeyboardHints.paragraph
    content bind notes
}
textInput { content bind nickname.nullToBlank() }   // String? → String
```

`KeyboardHints` chooses the keyboard, autocapitalization, autocorrect and autofill: `paragraph`, `title`, `id`, `integer`, `integerWithNegative`, `decimal`, `decimalWithNegative`, `phone`, `email`, `password`, `newPassword`, `oneTimeCode`.  For anything else, construct one: `KeyboardHints(case = KeyboardCase.None, type = KeyboardType.Text, autocorrect = false)`.

### Numbers, formatted text, phone numbers, suggestions

```kotlin
numberInput {
    keyboardHints = KeyboardHints.decimal
    range = 0.0..1000.0
    content bind price                        // MutableReactive<Double?>
}
numberInput { content bind quantity.asDouble() }   // Int? → Double?

formattedTextInput {
    content bind code                         // stores the raw characters only
    format = TextFormat(
        isRawData = { it.isLetterOrDigit() },
        format = { clean -> if (clean.length > 3) clean.take(3) + "-" + clean.drop(3) else clean },
    )
}

phoneNumberInput {
    hint = "(555) 555-5555"
    content bind phone                        // format = PhoneNumberFormat.USA by default
}

autoCompleteTextField {
    suggestions = listOf("Apple", "Banana", "Cherry")
    content bind fruit
}
```

### Dates and times

```kotlin
val date = Signal<LocalDate?>(null)

localDateField {
    content bind date
    range = LocalDate(2020, 1, 1)..LocalDate(2030, 12, 31)
}
localTimeField { content bind time }              // LocalTime?
localDateTimeField { content bind startsAt }      // LocalDateTime?

text { ::content { date()?.renderToString() ?: "No date picked" } }
```

The types are `kotlinx.datetime`.  `renderToString()` (in `com.lightningkite.kiteui.locale`) formats `LocalDate`, `LocalTime`, `LocalDateTime` and `Instant` for the user's locale.  To store an `Instant`, convert with `toInstant(TimeZone.currentSystemDefault())`.

### Choices

```kotlin
row {
    checkbox { checked bind notify }
    centered.text("Email me")
}
switch { checked bind notify }

Size.entries.forEach { option ->
    row {
        radioButton { checked bind size.equalTo(option) }
        centered.text(option.name)
    }
}

row {                                          // segmented control
    Size.entries.forEach { option ->
        expanding.radioToggleButton {
            centered.text(option.name)
            checked bind size.equalTo(option)
        }
    }
}
toggleButton { text("Bold"); checked bind isBold }

checkbox { checked bind tags.contains("urgent") }   // membership in a Set or List

select { bind(size, Constant(Size.entries)) { it.name } }
select { bind(country, Constant(listOf(null, "US", "CA"))) { it ?: "Choose a country" } }

slider {
    range(0f, 1f, 0.1f)
    value bind volume                          // MutableReactive<Float>
}
```

`equalTo` and `contains` are lenses from `com.lightningkite.reactive.extensions`: they read as a `Boolean` and write back to the original value.  `radioToggleButton` and `toggleButton` are containers styled as selected while checked; a `radioToggleButton` can't be unchecked by tapping it again.

### Progress

```kotlin
progressBar(ratio)                              // Reactive<Float>, 0..1
circularProgress { ::ratio { ratio() } }
activityIndicator()
```

### Images

```kotlin
sizeConstraints(height = 10.rem).image {
    source = Resources.imagesSnowyBackground   // generated from your resources folder
    scaleType = ImageScaleType.Crop            // Fit (default), Crop, Stretch, NoScale
    description = "Snowy mountains"
}
image {
    source = ImageRemote("https://example.com/photo.jpg")
    description = "Profile photo"
}
zoomableImage { source = photo }
```

An image shows a loading indicator until it has loaded.  Other sources: `ImageRaw(blob)`, `ImageLocal(file)`, `ImageVector(…)`.

### Icons

```kotlin
icon(Icon.home, "Home")
icon(Icon.settings.resize(3.rem), "Settings")

val lockOpen = Icon(
    1.5.rem, 1.5.rem, 0, -960, 960, 960,            // size, then the SVG viewBox
    listOf("M240-640h360v-80q0-50-35-85t-85-35…"),  // SVG path data
)
icon(lockOpen, "Unlock")
```

Icons take their color from the theme.  Built in: `add`, `arrowBack`, `block`, `certification`, `chat`, `chevronDown`, `chevronLeft`, `chevronRight`, `close`, `collapse`, `copy`, `darkMode`, `delete`, `deleteForever`, `done`, `dot`, `download`, `email`, `expand`, `externalLink`, `filterList`, `group`, `help`, `home`, `info`, `lightMode`, `list`, `login`, `logout`, `menu`, `moreHoriz`, `moreVert`, `notification`, `notificationFilled`, `passkey`, `person`, `remove`, `search`, `send`, `settings`, `sort`, `star`, `starFilled`, `sync`, `upload`, `warning`.  For anything else, copy the path from [Material Symbols](https://fonts.google.com/icons) (viewBox `0 -960 960 960`).

### Video and web content

```kotlin
sizeConstraints(height = 15.rem).video {
    source = VideoRemote("https://example.com/clip.mp4")
    showControls = true                         // also: loop, playing, time, volume
}

sizeConstraints(height = 20.rem).webView {
    load(WebViewSource.Url("https://example.com"))   // or WebViewSource.Html("…")
}
```

A web view grants the page nothing beyond rendering unless you pass `permissions` to `load` (scripts, popups, …).

### Menus

```kotlin
menuButton {
    icon(Icon.moreVert, "More")
    requireClick = true                       // otherwise it also opens on hover (web)
    opensMenu {
        col {
            button { text("Rename"); onClick { rename() } }
            danger.button { text("Delete"); onClick { delete() } }
        }
    }
}
```

For tooltips, dialogs and other popovers see [Dialogs](dialogs.md).

### Spacing

```kotlin
separator()          // a line between siblings
space()              // a blank space sized from the theme; space(2.0) for twice that
expanding.space()    // pushes siblings apart in a row or col
```

### Your own components

A component is an extension function.  Use `ElementWriter.CanAddTheme` as the receiver so callers can still apply modifiers (`expanding.userCard(user)`) and so `card`, `important` and the like work inside it:

```kotlin
fun ElementWriter.CanAddTheme.userCard(user: Reactive<User>) {
    card.row {
        icon(Icon.person, "User")
        expanding.col {
            text { ::content { user().name } }
            subtext { ::content { user().email } }
        }
    }
}
```

## Gotchas

- **A button has no text property.**  Nest a view: `button { text("Save") }`, not `button { text = "Save" }`.
- **Setup runs once.**  `content = name.value` sets the text once.  To follow a value, use `::content { name() }`.
- **`select` takes a `Reactive` list.**  Wrap a fixed list in `Constant(...)`; `bind(selected, listOf(...))` doesn't compile.
- **`numberInput` holds a `Double?`.**  Bind an `Int?` through `.asDouble()`; for text holding a number, use `textInput` with `.asInt()`.
- **Set `keyboardHints`** on every text input: the default is a plain text keyboard with autocorrect, which is wrong for emails, passwords and codes.  There is no `KeyboardHints.url`.
- **Only the icons listed above exist.**  `Icon.save`, `Icon.edit`, `Icon.calendar` and `Icon.event` don't; define your own `Icon`.
- **Give images and icons a description** unless they are purely decorative; it is what screen readers read.
- **Helpers need the right receiver.**  On plain `ElementWriter`, theme modifiers like `card` don't resolve; use `ElementWriter.CanAddTheme`.

## See also

- [Forms](forms.md): labels, validation and submitting
- [Layout](layout.md): sizing and placing these views
- [Error handling](error-handling.md): what happens when an action throws
- The example app's [Controls page](https://kiteui.cs.lightningkite.com/controls) shows every control in every theme.
