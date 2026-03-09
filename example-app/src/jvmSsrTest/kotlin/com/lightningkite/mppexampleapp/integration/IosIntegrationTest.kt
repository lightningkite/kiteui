// by Claude - iOS platform integration test via ai-driver-server
package com.lightningkite.mppexampleapp.integration

import com.lightningkite.kiteui.testing.ensureDaemon
import com.lightningkite.kiteui.testing.remoteUiTest
import com.lightningkite.kiteui.testing.waitForApp
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test that exercises the example app on iOS through the ai-driver-server.
 *
 * The test will:
 * 1. Check for a booted iOS Simulator
 * 2. Launch the app via `xcrun simctl launch`
 * 3. Wait for the app to connect to the ai-driver daemon
 * 4. Run the full UI walkthrough
 *
 * Prerequisites:
 * - iOS Simulator booted (via Xcode or `xcrun simctl boot`)
 * - App built and installed on simulator:
 *     ./gradlew :example-app:iosSimulatorArm64MainBinaries
 *     cd ../example-app-ios/KiteUI\ Example\ App
 *     xcodebuild -workspace *.xcworkspace -scheme "KiteUI Example App" \
 *       -sdk iphonesimulator -destination "platform=iOS Simulator,name=iPhone 16" build
 *     xcrun simctl install booted <path-to-built-app.app>
 * - Daemon running: `./gradlew aiDriverStart`
 *
 * Full automated run: `./scripts/run-integration-tests.sh ios`
 */
// by Claude
class IosIntegrationTest {

    companion object {
        private const val BUNDLE_ID = "com.lightningkite.kiteuiexample"
    }

    @Test
    fun fullWalkthrough() {
        // Pre-checks
        skipUnless(isCommandAvailable("xcrun"), "xcrun not found. Install Xcode command-line tools.")
        skipUnless(hasBootedSimulator(), "No iOS Simulator booted. Start one via Xcode or: xcrun simctl boot <device-id>")

        ensureDaemon()

        // Check if the example app is already connected from iOS
        val existingIos = tryListApps()?.firstOrNull {
            it.platform == "ios" && it.appName == EXAMPLE_APP_NAME
        }

        val appId = if (existingIos != null) {
            println("Using existing iOS app: ${existingIos.appId}")
            existingIos.appId
        } else {
            // Try to launch on simulator (assumes app is installed)
            println("Launching iOS app on simulator...")
            val launchResult = exec("xcrun", "simctl", "launch", "booted", BUNDLE_ID)
            skipUnless(
                !launchResult.lowercase().contains("error"),
                buildString {
                    appendLine("Failed to launch iOS app: $launchResult")
                    appendLine("Build and install first:")
                    appendLine("  ./gradlew :example-app:iosSimulatorArm64MainBinaries")
                    appendLine("  # Then build and install via Xcode or xcodebuild")
                }
            )

            // Wait for app to connect to daemon
            println("Waiting for iOS app to connect to daemon...")
            waitForApp("ios", appName = EXAMPLE_APP_NAME, timeout = 30.seconds)
        }

        remoteUiTest(appId = appId) {
            navigate("/")
            exampleAppWalkthrough()
        }
    }

    /** Check if any iOS Simulator is booted. */
    private fun hasBootedSimulator(): Boolean {
        val output = exec("xcrun", "simctl", "list", "devices", "booted")
        return output.contains("(Booted)")
    }
}
