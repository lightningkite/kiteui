package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.*
import com.lightningkite.mppexampleapp.widgets.code

@Routable("docs/multiplatform")
object MultiplatformPage : DocPage {
    override val covers: List<String> = listOf(
        "platform", "expect", "actual", "platform-specific", "multiplatform",
        "android", "ios", "js", "jvm", "web", "native", "cross-platform"
    )

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        article {
            titledSection("Multiplatform Development") {
                text("Learn how to handle platform differences and write platform-specific code in KiteUI.")

                space()

                titledSection("Platform Detection") {
                    text("KiteUI provides built-in platform detection through the Platform enum:")

                    space()
                    code {
                        content = """
                            import com.lightningkite.kiteui.Platform

                            when (Platform.current) {
                                Platform.Android -> text("Running on Android")
                                Platform.iOS -> text("Running on iOS")
                                Platform.Web -> text("Running in Browser")
                                Platform.Desktop -> text("Running on Desktop")
                            }
                        """.trimIndent()
                    }

                    space()
                    text("This allows you to conditionally render UI or adjust behavior based on the platform.")
                }

                space()

                titledSection("Expect/Actual Pattern") {
                    text("For platform-specific implementations, use Kotlin's expect/actual mechanism:")

                    space()
                    h5("1. Declare expect function in commonMain:")
                    code {
                        content = """
                            // In commonMain
                            expect fun getPlatformName(): String
                            expect fun openExternalUrl(url: String)
                        """.trimIndent()
                    }

                    space()
                    h5("2. Implement actual functions per platform:")
                    code {
                        content = """
                            // In androidMain
                            actual fun getPlatformName() = "Android"
                            actual fun openExternalUrl(url: String) {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }

                            // In iosMain
                            actual fun getPlatformName() = "iOS"
                            actual fun openExternalUrl(url: String) {
                                UIApplication.sharedApplication.openURL(NSURL(string = url))
                            }

                            // In jsMain
                            actual fun getPlatformName() = "Web"
                            actual fun openExternalUrl(url: String) {
                                window.open(url, "_blank")
                            }

                            // In jvmMain
                            actual fun getPlatformName() = "JVM"
                            actual fun openExternalUrl(url: String) {
                                Desktop.getDesktop().browse(URI(url))
                            }
                        """.trimIndent()
                    }

                    space()
                    h5("3. Use in common code:")
                    code {
                        content = """
                            button {
                                text("Visit Website")
                                onClick {
                                    openExternalUrl("https://example.com")
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Platform-Specific UI") {
                    text("Adapt your UI for different platform conventions:")

                    space()
                    h5("Responsive Design:")
                    code {
                        content = """
                            when (Platform.current) {
                                Platform.Web -> {
                                    // Desktop-optimized layout
                                    row {
                                        weight(1f).sidebar()
                                        weight(3f).mainContent()
                                    }
                                }
                                else -> {
                                    // Mobile-optimized layout
                                    col {
                                        mainContent()
                                    }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    h5("Platform-Appropriate Controls:")
                    code {
                        content = """
                            // Use platform-native date pickers
                            field("Birth Date") {
                                datePicker {
                                    date bind birthDate
                                    // Renders as native control on each platform:
                                    // - Android: Material DatePicker
                                    // - iOS: UIDatePicker
                                    // - Web: HTML5 date input
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("File Handling") {
                    text("File operations vary by platform. Use KiteUI's unified file APIs:")

                    space()
                    h5("File Upload:")
                    code {
                        content = """
                            button {
                                text("Choose File")
                                onClick {
                                    requestFile(mimeTypes = listOf("image/*")) { file ->
                                        // file is a platform-agnostic FileReference
                                        val bytes = file.read()
                                        uploadToServer(bytes)
                                    }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    h5("File Download:")
                    code {
                        content = """
                            button {
                                text("Download Report")
                                onClick {
                                    val data = generateReport()
                                    downloadFile(
                                        filename = "report.pdf",
                                        mimeType = "application/pdf",
                                        data = data
                                    )
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Storage and Persistence") {
                    text("Use KiteUI's storage APIs for cross-platform data persistence:")

                    space()
                    h5("Key-Value Storage:")
                    code {
                        content = """
                            import com.lightningkite.kiteui.AppScope

                            // Save data
                            AppScope.storage["user_id"] = "12345"
                            AppScope.storage["theme"] = "dark"

                            // Retrieve data
                            val userId = AppScope.storage["user_id"]
                            val theme = AppScope.storage["theme"] ?: "light"

                            // Storage maps to platform-specific mechanisms:
                            // - Android: SharedPreferences
                            // - iOS: UserDefaults
                            // - Web: localStorage
                            // - JVM: Properties file
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Navigation Differences") {
                    text("Platform-specific navigation patterns:")

                    space()
                    code {
                        content = """
                            // Web: Full URL-based navigation
                            when (Platform.current) {
                                Platform.Web -> {
                                    // Browser back button works automatically
                                    // Deep linking just works
                                    context.pageNavigator.navigate(DetailPage(id = "123"))
                                }
                                else -> {
                                    // Mobile: Stack-based navigation
                                    context.pageNavigator.navigate(DetailPage(id = "123"))
                                    // Back button pops navigation stack
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Performance Considerations") {
                    card.col {
                        h5("Web:")
                        text("• Minimize bundle size - code splitting recommended")
                        text("• Lazy load images and heavy resources")
                        text("• Use efficient CSS for themes")

                        space()
                        h5("Mobile (Android/iOS):")
                        text("• Use RecyclerView for long lists")
                        text("• Optimize image loading and caching")
                        text("• Be mindful of memory usage")

                        space()
                        h5("Desktop (JVM):")
                        text("• Can handle larger data sets in memory")
                        text("• Take advantage of multi-threading")
                        text("• No bundle size concerns")
                    }
                }

                space()

                titledSection("Camera and Media") {
                    text("Access device capabilities with platform abstractions:")

                    space()
                    code {
                        content = """
                            button {
                                text("Take Photo")
                                onClick {
                                    requestCamera { photo ->
                                        // photo is a FileReference
                                        image {
                                            source = photo.uri
                                        }
                                    }
                                }
                            }

                            button {
                                text("Choose from Gallery")
                                onClick {
                                    requestFile(mimeTypes = listOf("image/*")) { image ->
                                        processImage(image)
                                    }
                                }
                            }
                        """.trimIndent()
                    }

                    space()
                    text("Note: Camera access requires platform-specific permissions setup.")
                }

                space()

                titledSection("Permissions") {
                    text("Handle permissions in platform-specific code:")

                    space()
                    code {
                        content = """
                            // In commonMain
                            expect suspend fun requestLocationPermission(): Boolean

                            // In androidMain
                            actual suspend fun requestLocationPermission(): Boolean {
                                // Use Android's permission system
                                return ActivityCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED
                            }

                            // In iosMain
                            actual suspend fun requestLocationPermission(): Boolean {
                                // Use iOS's permission system
                                val status = CLLocationManager.authorizationStatus()
                                return status == kCLAuthorizationStatusAuthorizedWhenInUse
                            }

                            // Usage in common code
                            button {
                                text("Get Location")
                                onClick {
                                    if (requestLocationPermission()) {
                                        val location = getCurrentLocation()
                                        toast("Location: ${'$'}location")
                                    } else {
                                        toast("Permission denied")
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Network Connectivity") {
                    text("Monitor network status across platforms:")

                    space()
                    code {
                        content = """
                            // In commonMain
                            expect fun observeNetworkStatus(): Signal<Boolean>

                            // Usage
                            val isOnline = observeNetworkStatus()

                            text {
                                ::content {
                                    if (isOnline()) "Online" else "Offline"
                                }
                            }

                            shownWhen { !isOnline() }.warning.card.text {
                                content = "No internet connection"
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Platform-Specific Dependencies") {
                    text("Add platform-specific dependencies in your build.gradle.kts:")

                    space()
                    code {
                        content = """
                            kotlin {
                                sourceSets {
                                    commonMain.dependencies {
                                        api("com.lightningkite.kiteui:library:${'$'}version")
                                    }

                                    androidMain.dependencies {
                                        // Android-specific dependencies
                                        implementation("androidx.core:core-ktx:1.12.0")
                                    }

                                    iosMain.dependencies {
                                        // iOS-specific dependencies
                                    }

                                    jsMain.dependencies {
                                        // JS-specific dependencies
                                    }
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Best Practices") {
                    card.col {
                        text("✓ Keep platform-specific code minimal - maximize common code")
                        text("✓ Use expect/actual for unavoidable platform differences")
                        text("✓ Test on all target platforms regularly")
                        text("✓ Respect platform conventions (navigation patterns, UI controls)")
                        text("✓ Use KiteUI's built-in abstractions when available")
                        text("✓ Handle permission requests gracefully with fallbacks")
                        text("✓ Consider platform capabilities when designing features")
                        text("✓ Optimize for each platform's strengths and limitations")
                    }
                }

                space()

                titledSection("Testing Platform-Specific Code") {
                    text("Structure your code to make platform-specific parts testable:")

                    space()
                    code {
                        content = """
                            // Create an interface in commonMain
                            interface PlatformService {
                                fun openUrl(url: String)
                                fun shareText(text: String)
                            }

                            // Inject platform implementation
                            expect fun getPlatformService(): PlatformService

                            // In tests, provide mock implementation
                            class MockPlatformService : PlatformService {
                                override fun openUrl(url: String) {
                                    println("Mock: Opening ${'$'}url")
                                }
                                override fun shareText(text: String) {
                                    println("Mock: Sharing ${'$'}text")
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Common Pitfalls") {
                    card.col {
                        h5("❌ Assuming all platforms have the same capabilities")
                        text("Solution: Check platform before using specific features")

                        space()
                        h5("❌ Hard-coding file paths")
                        text("Solution: Use platform-agnostic file APIs")

                        space()
                        h5("❌ Not handling permission denials")
                        text("Solution: Always provide fallback when permissions are denied")

                        space()
                        h5("❌ Using platform-specific types in common code")
                        text("Solution: Create common interfaces and use expect/actual")
                    }
                }
            }
        }
    }
}
