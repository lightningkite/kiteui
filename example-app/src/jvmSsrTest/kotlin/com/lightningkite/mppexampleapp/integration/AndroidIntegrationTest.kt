// by Claude - Android platform integration test via ai-driver-server
package com.lightningkite.mppexampleapp.integration

import com.lightningkite.kiteui.testing.ensureDaemon
import com.lightningkite.kiteui.testing.remoteUiTest
import com.lightningkite.kiteui.testing.waitForApp
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/**
 * Integration test that exercises the example app on Android through the ai-driver-server.
 *
 * The test will:
 * 1. Check for a running Android emulator or connected device
 * 2. Verify the debug APK is installed (or install it if built)
 * 3. Launch the app via `adb shell am start`
 * 4. Wait for the app to connect to the ai-driver daemon
 * 5. Run the full UI walkthrough
 *
 * Prerequisites:
 * - Android emulator running or device connected via USB
 * - Debug APK built and installed: `./gradlew :example-app:installDebug`
 * - Daemon running: `./gradlew aiDriverStart`
 *
 * Full automated run: `./scripts/run-integration-tests.sh android`
 */
// by Claude
class AndroidIntegrationTest {

    companion object {
        private const val APP_PACKAGE = "com.lightningkite.kiteuiexample"
        private const val MAIN_ACTIVITY = "$APP_PACKAGE/.MainActivity"
    }

    @Test
    fun fullWalkthrough() {
        // Pre-checks
        skipUnless(isCommandAvailable("adb"), "adb not found on PATH. Install Android SDK platform-tools.")
        skipUnless(hasConnectedDevice(), "No Android device/emulator connected. Start an emulator first.")

        ensureDaemon()

        // Check if the example app is already connected from Android
        val existingAndroid = tryListApps()?.firstOrNull {
            it.platform == "android" && it.appName == EXAMPLE_APP_NAME
        }

        val appId = if (existingAndroid != null) {
            println("Using existing Android app: ${existingAndroid.appId}")
            existingAndroid.appId
        } else {
            // Verify app is installed
            skipUnless(
                isAppInstalled(APP_PACKAGE),
                "App not installed on device. Run: ./gradlew :example-app:installDebug"
            )

            // Force-stop any existing instance to get a fresh start
            exec("adb", "shell", "am", "force-stop", APP_PACKAGE)
            Thread.sleep(500)

            // Launch the main activity
            println("Launching Android app...")
            val launchResult = exec("adb", "shell", "am", "start", "-n", MAIN_ACTIVITY)
            skipUnless(
                !launchResult.contains("Error"),
                "Failed to launch Android app: $launchResult"
            )

            // Wait for app to connect to daemon
            println("Waiting for Android app to connect to daemon...")
            waitForApp("android", appName = EXAMPLE_APP_NAME, timeout = 30.seconds)
        }

        remoteUiTest(appId = appId) {
            navigate("/")
            exampleAppWalkthrough()
        }
    }

    /** Check if any Android device/emulator is connected and online. */
    private fun hasConnectedDevice(): Boolean {
        val output = exec("adb", "devices")
        return output.lines().drop(1).any { it.contains("\tdevice") }
    }

    /** Check if a package is installed on the connected device. */
    private fun isAppInstalled(packageName: String): Boolean {
        val output = exec("adb", "shell", "pm", "list", "packages", packageName)
        return output.contains("package:$packageName")
    }
}
