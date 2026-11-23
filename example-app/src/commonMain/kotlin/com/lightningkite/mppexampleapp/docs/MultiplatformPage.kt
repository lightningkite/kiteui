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
                        """.trimIndent()
                    }

                    space()
                    h5("2. Implement actual functions per platform:")
                    code {
                        content = """
                            // In androidMain
                            actual fun getPlatformName() = "Android"

                            // In iosMain
                            actual fun getPlatformName() = "iOS"

                            // In jsMain
                            actual fun getPlatformName() = "Web"
                        """.trimIndent()
                    }

                    space()
                    h5("3. Use in common code:")
                    code {
                        content = """
                            text("Running on ${'$'}{getPlatformName()}")
                        """.trimIndent()
                    }

                    space()
                    text("Check for a built-in first: links, files, sharing, maps, clipboard and location are already on context (context.openLink, context.requestFile, context.share, ...).")
                }

                space()

                titledSection("Platform-Specific UI") {
                    text("Adapt your UI to the screen, not the platform: a phone browser is the Web platform too.")

                    space()
                    h5("Responsive Design:")
                    code {
                        content = """
                            // A row on wide screens, a column on narrow ones (which ignores weights)
                            rowCollapsingToColumn(50.rem) {
                                weight(1f).sidebar()
                                weight(3f).mainContent()
                            }
                        """.trimIndent()
                    }

                    space()
                    h5("Platform-Appropriate Controls:")
                    code {
                        content = """
                            // Date fields use each platform's native picker
                            field("Birth Date") {
                                localDateField { content bind birthDate }
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
                                    // null if the user cancels; a FileReference works on every platform
                                    val file = context.requestFile(listOf("image/*")) ?: return@onClick
                                    uploadToServer(file)
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
                                    val report: Blob = generateReport()
                                    context.download("report.pdf", report)
                                }
                            }
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Storage and Persistence") {
                    text("A PersistentProperty is a Signal saved to platform storage. Declare it at the top level:")

                    space()
                    code {
                        content = """
                            val darkMode = PersistentProperty("darkMode", false)

                            // Bind it like any Signal; the value survives restarts
                            switch { checked bind darkMode }
                        """.trimIndent()
                    }

                    space()
                    h5("Raw Key-Value Storage:")
                    code {
                        content = """
                            PlatformStorage.set("user_id", "12345")
                            val userId: String? = PlatformStorage.get("user_id")
                            PlatformStorage.remove("user_id")
                        """.trimIndent()
                    }
                }

                space()

                titledSection("Navigation Differences") {
                    text("The same code navigates on every platform. On the web each page gets a URL and the browser's back button works; on Android the system back button pops the stack:")

                    space()
                    code {
                        content = """
                            context.pageNavigator.navigate(DetailPage(id = "123"))
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
                            val photo = Signal<ImageSource?>(null)

                            button {
                                text("Take Photo")
                                onClick {
                                    val file = context.requestCaptureEnvironment() ?: return@onClick
                                    photo.value = ImageLocal(file)
                                }
                            }

                            button {
                                text("Choose from Gallery")
                                onClick {
                                    val file = context.requestFile(listOf("image/*")) ?: return@onClick
                                    photo.value = ImageLocal(file)
                                }
                            }

                            image { ::source { photo() } }
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
                                        val position = context.getCurrentPosition()
                                        context.toast("Location: ${'$'}{position.latitude}, ${'$'}{position.longitude}")
                                    } else {
                                        context.toast("Permission denied")
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
