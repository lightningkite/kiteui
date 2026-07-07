# KiteUI

[![Maven Central Version](https://img.shields.io/maven-central/v/com.lightningkite.kiteui/library)](https://central.sonatype.com/artifact/com.lightningkite.kiteui/library)
[![Nightly](https://img.shields.io/maven-metadata/v?strategy=latestProperty&label=lightningkite-maven-nightly&metadataUrl=https://lightningkite-maven.s3.us-west-2.amazonaws.com/com/lightningkite/kiteui/library/maven-metadata.xml
)](https://lightningkite-maven.s3.us-west-2.amazonaws.com/com/lightningkite/kiteui/library/maven-metadata.xml)
[![License](https://img.shields.io/github/license/lightningkite/kiteui?style=flat-square)](https://www.apache.org/licenses/LICENSE-2.0)
[![CI Status](https://img.shields.io/github/actions/workflow/status/lightningkite/kiteui/publishInternal.yml)](https://github.com/lightningkite/kiteui/publishInternal.yml)
[![KDoc](https://img.shields.io/badge/docs-kdoc-blue)](https://lightningkite-maven.s3.us-west-2.amazonaws.com/com/lightningkite/kiteui/library/docs/index.html)

![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?logo=kotlin&label=2.3.20)
![Android](https://img.shields.io/badge/platform-android-blue)
![JVM](https://img.shields.io/badge/platform-jvm-blue)
![JS](https://img.shields.io/badge/platform-js-blue)
![iOS](https://img.shields.io/badge/platform-ios-blue)

[![Donate](https://img.shields.io/badge/donate-stripe-white)](https://donate.stripe.com/6oUfZh2HndqKbny4mr4ow00)

A Kotlin Multiplatform UI Framework inspired by Solid.js that uses the native view components of each platform.

## Why make this library instead of using Compose Multiplatform?

1. **Web targets:** Compose is built on its own rendering stack, and thus it must render its entire UI on web in a canvas.  This makes accessibility and good performance extremely difficult and server-side rendering impossible.
2. **Smaller Binary Sizes:** Compose generates fairly large binaries, since it must recreate the entire UI set of the target platform - i.e. text fields, checkboxes, and more.
3. **URL-based navigation:** Deep linking is difficult in Compose, so creating links to pages requires extra effort.

For these reasons, we think Compose isn't sufficient nor can be made sufficient for any multiplatform development that includes a web target.  That's why we created this library.

Here is a comparison of the same UI built with KiteUI and Compose Web:

|                                           | Compose Web                                                                                                                                                          | KiteUI Web                                                                                                    |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Live site                                 | [https://zal.im/wasm/jetsnack](https://zal.im/wasm/jetsnack)                                                                                                         | [https://kiteui-jetsnack-demo.cs.lightningkite.com/](https://kiteui-jetsnack-demo.cs.lightningkite.com)       |
| Source Code                               | [https://github.com/JetBrains/compose-multiplatform/tree/master/examples/jetsnack](https://github.com/JetBrains/compose-multiplatform/tree/master/examples/jetsnack) | [https://github.com/lightningkite/kiteui-jetsnack-demo](https://github.com/lightningkite/kiteui-jetsnack-demo) |
| Web Binary Size (including fonts and css) | 12.17389 MB (July 30, 2025)                                                                                                                                          | 0.77092 MB (July 30, 2025)                                                                                    |
Performance in KiteUI is faster or equivalent to the performance of Compose multiplatform Web from internal testing.

## Features

- **Smaller Binary Size**: KiteUI makes fairly small bundle sizes.  
  - For a practical comparison: The [sample project for Kite UI](https://kiteui.cs.lightningkite.com) has a JS bundle of less than 2 megabytes, and the [image viewer example for Compose Web](https://zal.im/wasm/iv/), which is a far smaller scope, has a WASM bundle of 8 megabytes
- **Fine-Grained Reactivity**: Inspired by Solid.js, KiteUI uses a fine-grained reactivity system that only updates what needs to be updated while avoiding compiler magic.
- **Semantic Theming System**: KiteUI's theming system is built around semantic concepts rather than direct styling, making it easier to maintain consistent UI across your application.
- **Web-First Approach**: With URL-based navigation and (upcoming) server-side rendering capabilities, KiteUI is designed to work seamlessly in web environments while still supporting native platforms.
- **True Native Component Integration**: Because KiteUI uses the underlying platform's view system, integrating a native view not accounted for in the library is extremely easy.
- **Automatic Load State**: The reactivity mechanism tracks loading in progress, so your elements will automatically go into a visual loading state if they're waiting for data.
- **Annotation URL-based routing**: A simple annotation binds pages and their URLs together.

## Goals

- Small JS size
- Web Client and server-side rendering, Android, iOS, eventually desktop
- Pretty by default - ugliness should take effort
- Simple Routing
- Easy to extend into native components on the platform
- Make loading and issue handling pretty without manual work

## Interesting design decisions

- Base navigation around URLs to be very compatible with web
- Use fine-grained reactivity based on SolidJS
- Use themes for styling; avoid direct styling.
- Derive theme variants from existing themes programmatically.  Make theme variants semantically based.
- Don't use a KMP network client, they're all too big - include a custom, simpler, and more limited implementation

## Project Status

We are using this in production, but I will not declare the API as totally finalized.  However, with the sheer number of projects we have written in it, we at least have to keep some level of compatibility.  We'll document how to adjust to new changes as they come.

## Road Map

- [ ] Server-side rendering - We have a lot of stuff prepared for this, but there's work that must be completed to finish it.
- [ ] WASM target - should be fairly easy to implement, since at one time we had it implemented but it was slower and bigger.  From what I hear performance has improved enough to make restoring this target sensible.
- [ ] Desktop Target - We're still not sure what underlying UI framework we wish to target.  Compose Desktop, Swing, and JavaFX are all possibilities, though we'll likely go with Compose.
- [ ] Form validation - I feel like there could be some tools that would make form validation easier.  Still haven't found the right API though.
- [ ] Performance Improvements - while it's already running fairly fast, I always would like more time to improve it.

## Importing

Maven repository: `maven("https://lightningkite-maven.s3.us-west-2.amazonaws.com")`
Dependency: `api("com.lightningkite.kiteui:library:<current tag>")`

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
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val email = Signal("")
        val password = Signal("")
        unpadded.frame {
            image {
                source = Resources.imagesSnowyBackground
                scaleType = ImageScaleType.Crop
                opacity = 0.5
            }
            padded.scrolling.col {
                expanding.space()
                centered.sizeConstraints(maxWidth = 50.rem).card.col {
                    h1 { content = "My App" }
                    sizeConstraints(width = 20.rem).field("Email") {
                        fieldTheme.textInput {
                            hint = "Email"
                            keyboardHints = KeyboardHints.email
                            content bind email
                        }
                    }
                    sizeConstraints(width = 20.rem).field("Password") {
                        fieldTheme.textInput {
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
                    centered.sizeConstraints(width = 15.rem).important.button {
                        h6 { content = "Log In" }
                        onClick {
                            delay(1000)
                            fakeLogin(email)
                        }
                    }
                }
                expanding.space()
            }
        }
    }

    private suspend fun ElementWriter.fakeLogin(email: Signal<String>) {
        fetch("fake-login/${email()}")
        context.pageNavigator.navigate(ControlsPage)
    }
}
```
