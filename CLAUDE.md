# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 🚧 Active Migration - View-Split Branch

**IMPORTANT**: The `view-split` branch contains an ongoing architectural refactoring. The codebase is currently **BROKEN** during migration.

**See [MIGRATION.md](MIGRATION.md) for complete details** including:
- What's changing: RView → Element/NativeElement split
- Why: Type-enforced modifier ordering and better separation of concerns
- How to migrate code
- Current implementation status

**Key changes:**
- `RView` → `Element` (interface) + `NativeElement` (platform implementation)
- `RContext` → `ElementContext`
- `ViewWriter` → `ElementWriter` with compile-time modifier ordering enforcement
- Modifier order now enforced by type system: `alignment → weight → shownWhen → sizing → theme → scrolling → element`

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

See **[docs/TESTING_GUIDE.md](docs/TESTING_GUIDE.md)** for the full guide including prerequisites, gotchas, and how to write tests.

```bash
# Fastest — no external tools needed
./gradlew :example-app:jvmSsrTest

# Android (Robolectric, no device needed)
./gradlew :example-app:testDebugUnitTest

# iOS (requires running Simulator)
./gradlew :example-app:iosSimulatorArm64Test

# JS/Web (requires Chrome)
./gradlew :example-app:jsBrowserTest

# All platforms at once
./gradlew :example-app:allTests

# Filter to a specific test class (JVM SSR and Android only)
./gradlew :example-app:jvmSsrTest --tests "*.MyTestClass"
./gradlew :example-app:testDebugUnitTest --tests "*.MyTestClass"
```

Substitute `:library:` for `:example-app:` to run library tests instead.

### Running Example App
```bash
# Run JS/Web version with Vite (development)
# IMPORTANT: Always use Gradle tasks to run the dev server, NOT manual HTTP servers
./gradlew :example-app:viteRun

# Run JS/Web version (production build)
# Use run configuration: "ExampleJSRun prod"

# Run Android version
# Use Android run configuration: "example-app"

# Run JVM version
./gradlew :example-app:jvmRun
```

**Note for Claude:** When testing JS/Web changes, always use `./gradlew :example-app:viteRun` to start the dev server. Do NOT use Python HTTP servers or other manual servers - they don't handle SPA routing correctly.

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
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        // UI code
    }
}
```

For pages with parameters:
```kotlin
@Routable("items/{id}")
class ItemDetailPage(val id: String) : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        // Access id parameter
    }
}
```

Navigate using `context.pageNavigator.navigate(SomePage)` or use `link` components.

### ViewWriter and Component Creation

UI is built using `ViewWriter` extension functions. Components return `Unit` for modifier chaining:

```kotlin
fun ViewWriter.myComponent(): Unit = col {
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

- **[MIGRATION.md](MIGRATION.md)** - 🚧 View-split branch migration guide (active refactoring)
- **GoodKiteuiCode.md** - Best practices for creating pages and components
- **ThemeRules.md** - Rules for semantic theming behavior
- **example-app/src/commonMain/kotlin/com/lightningkite/mppexampleapp/docs/CheatSheet.kt** - Comprehensive examples of all components and modifiers
- **library/src/commonMain/kotlin/com/lightningkite/kiteui/navigation/Page.kt** - Page interface
- **library/src/commonMain/kotlin/com/lightningkite/kiteui/views/ElementWriter.kt** - Core ElementWriter interface (view-split) / ViewWriter (version-7)
- **library/src/commonMain/kotlin/com/lightningkite/kiteui/views/Element.kt** - Core Element interface (view-split only)

## Platform Targets

- **Android** - Min SDK varies, uses native Android views
- **iOS** - Deployment target 14.0, uses CocoaPods
- **JVM** - Desktop/server support
- **JS** - Web target with Vite bundling

## Current Branch Strategy

- `version-7` - Main development branch (use for PRs)
- `version-6` - Previous major version
- `view-split` - **ACTIVE MIGRATION BRANCH** (currently BROKEN) - Major architectural refactoring. See [MIGRATION.md](MIGRATION.md) for details.

When working on this branch, expect:
- Compilation errors in migrated but incomplete areas
- Missing implementations for some platforms
- Commits marked "BROKEN" are normal during migration
