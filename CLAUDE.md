# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

KiteUI is a Kotlin Multiplatform UI framework inspired by Solid.js that uses native view components on each platform (Android, iOS, JVM, JS/Web). It emphasizes small binary sizes, fine-grained reactivity, semantic theming, and URL-based navigation for web compatibility.

**Key Design Principles:**
- Uses fine-grained reactivity based on Solid.js (not Compose-style recomposition)
- Navigation is URL-based for web compatibility
- Semantic theming system (not direct styling)
- Platform-native view components (not canvas rendering)
- Custom lightweight network client (not Ktor for production)

## Project Structure

This is a multi-module Gradle project:

- **library/** - Core KiteUI framework code
  - `src/commonMain/` - Platform-independent code
  - `src/androidMain/`, `src/iosMain/`, `src/jsMain/`, `src/jvmMain/` - Platform-specific implementations
  - Key packages:
    - `views/` - View components and modifiers
    - `navigation/` - Routing and page system
    - `reactive/` - Reactive state management
    - `models/` - Theme, styling, and data models
- **example-app/** - Demo application showcasing KiteUI features
- **gradle-plugin/** - Gradle plugin for KiteUI projects
- **buildSrc/** - Build configuration utilities

## Development Commands

### Building
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :library:build
./gradlew :example-app:build

# Publish to Maven Local
./gradlew publishToMavenLocal
```

### Testing
```bash
# Run all tests
./gradlew allTests

# Run tests for specific platform
./gradlew :library:jvmTest
./gradlew :library:jsTest
./gradlew :library:iosX64Test
```

### Running Example App
```bash
# Run JS/Web version with Vite (development)
./gradlew :example-app:jsRun

# Run JS/Web version (production build)
# Use run configuration: "ExampleJSRun prod"

# Run Android version
# Use Android run configuration: "example-app"

# Run JVM version
./gradlew :example-app:jvmRun
```

### Publishing
```bash
# Publish to LightningKite repository
./gradlew publishAllPublicationsToLightningKiteRepository
```

## Architecture Patterns

### Pages and Navigation

Pages implement the `Page` interface and are annotated with `@Routable`:

```kotlin
@Routable("your/path")
object YourPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        // UI code
    }
}
```

For pages with parameters:
```kotlin
@Routable("items/{id}")
class ItemDetailPage(val id: String) : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
        // Access id parameter
    }
}
```

Navigate using `pageNavigator.navigate(SomePage)` or use `link` components.

### ViewWriter and Component Creation

UI is built using `ViewWriter` extension functions. Components return `ViewModifiable` for modifier chaining:

```kotlin
fun ViewWriter.myComponent(): ViewModifiable = col {
    text("Hello")
    button {
        text("Click me")
    }
}
```

Common containers: `row`, `col`, `frame`, `rowCollapsingToColumn`

### Modifiers

Modifiers are applied with the `-` operator. Order matters: Position > Visibility > Scroll > Theme

```kotlin
centered - scrolling - card - col {
    // content
}
```

Common modifiers:
- Layout: `centered`, `expanding`, `weight(f)`, `sizeConstraints(...)`
- Spacing: `padded`
- Scrolling: `scrolling`
- Theme: `card`, `important`, `fieldTheme`, etc.

### Reactivity System

KiteUI uses fine-grained reactivity, not recomposition:

**Property** - Basic reactive container:
```kotlin
val email = Property("")
textInput { content bind email }
```

**Reactive functions** - Auto-update when dependencies change:
```kotlin
text {
    ::content { "Email: ${email()}" }
}
```

**shared** - Cached computed value:
```kotlin
val fullName = shared { "${firstName()} ${lastName()}" }
```

**LazyProperty** - Computed value that can be overridden:
```kotlin
val calculated = LazyProperty { base() * multiplier() }
calculated.value = 100.0  // Override
calculated.reset()        // Back to calculation
```

**LateInitProperty** - For values not available at declaration:
```kotlin
val userData = LateInitProperty<UserData>()
// Components show loading until value is set
userData.value = fetchedData
```

### Theming

Apply semantic themes via modifiers. Switching themes creates backgrounds/cards. Switching to the same theme does NOT create a card (use explicit `card` modifier).

```kotlin
important - button { text("Important") }
card - col { /* content */ }
```

Theme switches should typically be applied to containers (`col`, `row`, `frame`, `button`), not individual elements.

### Component Guidelines (from GoodKiteuiCode.md)

- Components should take minimal parameters unique to each usage
- Components should load their own data for easier debugging
- Only create reusable components; single-use components should be inlined
- Modifier order: Position > Visibility > Scroll > Theme

## Key Files to Reference

- **GoodKiteuiCode.md** - Best practices for creating pages and components
- **ThemeRules.md** - Rules for semantic theming behavior
- **example-app/src/commonMain/kotlin/com/lightningkite/mppexampleapp/docs/CheatSheet.kt** - Comprehensive examples of all components and modifiers
- **library/src/commonMain/kotlin/com/lightningkite/kiteui/navigation/Page.kt** - Page interface
- **library/src/commonMain/kotlin/com/lightningkite/kiteui/views/ViewWriter.kt** - Core ViewWriter class

## Platform Targets

- **Android** - Min SDK varies, uses native Android views
- **iOS** - Deployment target 14.0, uses CocoaPods
- **JVM** - Desktop/server support
- **JS** - Web target with Vite bundling

## Current Branch Strategy

- `version-6` - Main development branch (use for PRs)
- `version-6.1` - Current working branch
