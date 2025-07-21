# KiteUI

A Kotlin Multiplatform UI Framework inspired by Solid.js.

## Goals

- Small JS size
- Web Client and server-side rendering, Android, iOS, eventually desktop
- Pretty by default - ugliness should take effort
- Simple Routing
- Easy to extend into native components on the platform
- Make loading and issue handling pretty without manual work

## Why Choose KiteUI over Compose Multiplatform?

Update with table for https://blog.jetbrains.com/kotlin/2025/05/present-and-future-kotlin-for-web/

KiteUI offers several advantages over Compose Multiplatform for certain use cases:

- **Smaller JS Bundle Size**: KiteUI is designed with web performance in mind, resulting in significantly smaller JavaScript bundle sizes compared to Compose Multiplatform.
- **Fine-Grained Reactivity**: Inspired by Solid.js, KiteUI uses a fine-grained reactivity system that only updates what needs to be updated, leading to better performance than Compose's recomposition model.
- **Semantic Theming System**: KiteUI's theming system is built around semantic concepts rather than direct styling, making it easier to maintain consistent UI across your application.
- **Web-First Approach**: With URL-based navigation and server-side rendering capabilities, KiteUI is designed to work seamlessly in web environments while still supporting native platforms.
- **Simpler Learning Curve**: KiteUI's API is designed to be intuitive and straightforward, making it easier to learn for developers coming from web development backgrounds.
- **Native Component Integration**: KiteUI makes it easy to extend and integrate with native components on each platform, giving you the flexibility to use platform-specific features when needed.
- **Lightweight Network Client**: Instead of relying on heavy KMP network clients, KiteUI includes a simpler, more limited implementation that's sufficient for most use cases while keeping the bundle size small.

## Interesting design decisions

- Base navigation around URLs to be very compatible with web
- Use fine-grained reactivity
- Use themes for styling; avoid direct styling.
- Derive theme variants from existing themes.  Make theme variants semantically based.
- Don't use a KMP network client, they're all too big - include a custom, simpler, and more limited implementation

## Project Status

Early in development.  Web is basically usable at this point, but everything is subject to change.

### TO DO:

- [ ] Some kind of simple validation system
- [ ] Server-side rendering

## Take a look!

You can look at the [example project we're hosting](https://kiteui.cs.lightningkite.com/) to get an idea of what you can do.

Click the magnifying glass in the app to see the source!

### One example directly on this page:

[See for yourself](https://kiteui.cs.lightningkite.com/sample/login)

If you want to try another theme, start [here](https://kiteui.cs.lightningkite.com/), change the theme, then go to the "Sample Log In" sreen.

![Screenshot 1](docs/SampleLoginScreen_A.png) ![Screenshot 2](docs/SampleLoginScreen_B.png)

```kotlin
@Routable("sample/login")
object SampleLogInPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        val email = Property("")
        val password = Property("")
        frame {
            gap = 0.rem
            image {
                source = Resources.imagesSolera
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }
            padded - scrolling - col {
                expanding - space()
                centered - sizeConstraints(maxWidth = 50.rem) - card - col {
                    h1 { content = "My App" }
                    sizeConstraints(width = 20.rem) - field("Email") {
                        fieldTheme - textInput {
                            hint = "Email"
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }
                    sizeConstraints(width = 20.rem) - field("Password") {
                        fieldTheme - textInput {
                            hint = "Password"
                            keyboardHints = KeyboardHints.password
                            content bind password
                            action = Action(
                                title = "Log In",
                                icon = Icon.login,
                            ) {
                                fakeLogin(email)
                            }
                        }
                    }
                    centered - sizeConstraints(width = 15.rem) - important - button {
                        h6 { content = "Log In" }
                        onClick {
                            delay(1000)
                            fakeLogin(email)
                        }
                    }
                }
                expanding - space()
            }
        }
    }

    private suspend fun ViewWriter.fakeLogin(email: Property<String>) {
        fetch("fake-login/${email()}")
        pageNavigator.navigate(ControlsPage)
    }
}
```
