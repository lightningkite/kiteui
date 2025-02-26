# KiteUI

A Kotlin Multiplatform UI Framework inspired by Solid.js.

## Goals

- Small JS size
- Web Client and server-side rendering, Android, iOS, eventually desktop
- Pretty by default - ugliness should take effort
- Simple Routing
- Easy to extend into native components on the platform
- Make loading and issue handling pretty without manual work

## Interesting design decisions

- Base navigation around URLs to be very compatible with web
- Use themes for styling; avoid direct styling.
- Derive theme variants from existing themes.  Make theme variants semantically based.
- Don't use kotlinx.coroutines, it's too big - include a custom, simpler, and more limited implementation
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
object SampleLogInPage : KiteUiPage {
    override fun ViewContext.render() {
        val email = Property("")
        val password = Property("")
        frame {
            image {
                source = ImageRemote("https://picsum.photos/seed/login/1080/1920")
                scaleType = ImageMode.Crop
                alpha = 0.5
            } in bordering
            col {
                space {} in weight(1f)
                col {
                    h1 { content = "My App" }
                    label {
                        content = "Email"
                        textField {
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }
                    label {
                        content = "Password"
                        textField {
                            keyboardHints = KeyboardHints.password
                            content bind password
                        }
                    }
                    button {
                        h6 { content = "Log In" }
                        onClick {
                            launch {
                                fetch("fake-login/${email.await()}")
                                navigator.navigate(ControlsPage)
                            }
                        }
                    } in important
                } in card in sizedBox(SizeConstraints(maxWidth = 50.rem))
                space {} in weight(1f)
            } in scrolling in withPadding
        } in bordering
    }
}
```
