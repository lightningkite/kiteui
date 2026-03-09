// by Claude - unit tests for CliFormat parser and encoder
package com.lightningkite.kiteui.aidriver

import kotlin.test.*

class CliFormatTest {

    // ---- Parse tests ----

    @Test fun parseList() {
        assertEquals(CliCommand.ListApps(), CliFormat.parse(listOf("list")))
    }

    @Test fun parseInfo() {
        assertEquals(CliCommand.Info("android-1"), CliFormat.parse(listOf("info", "android-1")))
    }

    @Test fun parseSnapshot_defaults() {
        assertEquals(
            CliCommand.Snapshot("android-1"),
            CliFormat.parse(listOf("snapshot", "android-1"))
        )
    }

    @Test fun parseSnapshot_withComponent() {
        assertEquals(
            CliCommand.Snapshot("android-1", component = "loginForm"),
            CliFormat.parse(listOf("snapshot", "android-1", "--component", "loginForm"))
        )
    }

    @Test fun parseSnapshot_withFormatJson() {
        assertEquals(
            CliCommand.Snapshot("android-1", format = CliCommand.Snapshot.Format.Json),
            CliFormat.parse(listOf("snapshot", "android-1", "--format", "Json"))
        )
    }

    @Test fun parseScreenshot_noPath() {
        assertEquals(
            CliCommand.Screenshot("android-1"),
            CliFormat.parse(listOf("screenshot", "android-1"))
        )
    }

    @Test fun parseScreenshot_withPath() {
        assertEquals(
            CliCommand.Screenshot("android-1", path = "out.png"),
            CliFormat.parse(listOf("screenshot", "android-1", "--path", "out.png"))
        )
    }

    @Test fun parseWait_defaults() {
        assertEquals(
            CliCommand.Wait("android-1"),
            CliFormat.parse(listOf("wait", "android-1"))
        )
    }

    @Test fun parseWait_withPage() {
        assertEquals(
            CliCommand.Wait("android-1", page = "HomePage"),
            CliFormat.parse(listOf("wait", "android-1", "--page", "HomePage"))
        )
    }

    @Test fun parseWait_withTimeout() {
        assertEquals(
            CliCommand.Wait("android-1", timeout = 5000L),
            CliFormat.parse(listOf("wait", "android-1", "--timeout", "5000"))
        )
    }

    @Test fun parseWait_withChange() {
        assertEquals(
            CliCommand.Wait("android-1", change = true),
            CliFormat.parse(listOf("wait", "android-1", "--change"))
        )
    }

    @Test fun parseRecord() {
        assertEquals(
            CliCommand.Record("android-1", "start"),
            CliFormat.parse(listOf("record", "android-1", "start"))
        )
    }

    @Test fun parseStart_defaults() {
        assertEquals(CliCommand.Start(), CliFormat.parse(listOf("start")))
    }

    @Test fun parseStart_withOptions() {
        assertEquals(
            CliCommand.Start(port = 8080, daemon = true),
            CliFormat.parse(listOf("start", "--port", "8080", "--daemon"))
        )
    }

    @Test fun parseStop() {
        assertEquals(CliCommand.Stop, CliFormat.parse(listOf("stop")))
    }

    @Test fun parseStatus() {
        assertEquals(CliCommand.Status, CliFormat.parse(listOf("status")))
    }

    // ---- Error tests ----

    @Test fun parseEmpty_throws() {
        assertFailsWith<CliParseException> { CliFormat.parse(emptyList()) }
    }

    @Test fun parseInfo_missingAppId_throws() {
        assertFailsWith<Exception> { CliFormat.parse(listOf("info")) }
    }

    // ---- Encode tests ----

    @Test fun encodeList() {
        assertEquals(listOf("list", "--format", "Text"), CliFormat.encode(CliCommand.ListApps()))
    }

    @Test fun encodeInfo() {
        assertEquals(listOf("info", "android-1"), CliFormat.encode(CliCommand.Info("android-1")))
    }

    @Test fun encodeSnapshot_defaults() {
        // The encoder always emits non-boolean optional fields (even with default values).
        // Only false-boolean optional flags are suppressed. So format=Text is always emitted.
        assertEquals(
            listOf("snapshot", "android-1", "--format", "Text"),
            CliFormat.encode(CliCommand.Snapshot("android-1"))
        )
    }

    @Test fun encodeSnapshot_withFormatJson() {
        assertEquals(
            listOf("snapshot", "android-1", "--format", "Json"),
            CliFormat.encode(CliCommand.Snapshot("android-1", format = CliCommand.Snapshot.Format.Json))
        )
    }

    @Test fun encodeStop() {
        assertEquals(listOf("stop"), CliFormat.encode(CliCommand.Stop))
    }

    @Test fun encodeStart_withOptions() {
        val tokens = CliFormat.encode(CliCommand.Start(port = 9090, daemon = true))
        assertTrue("--port" in tokens, "Expected --port in $tokens")
        assertTrue("9090" in tokens, "Expected 9090 in $tokens")
        assertTrue("--daemon" in tokens, "Expected --daemon in $tokens")
    }

    // ---- Perform: encode and parse (nested UiAction sealed class) ----

    @Test fun perform_encode_click() {
        assertEquals(
            listOf("perform", "android-1", "click", "loginBtn"),
            CliFormat.encode(CliCommand.Perform("android-1", UiAction.Click("loginBtn")))
        )
    }

    @Test fun perform_encode_back() {
        assertEquals(
            listOf("perform", "android-1", "back"),
            CliFormat.encode(CliCommand.Perform("android-1", UiAction.Back))
        )
    }

    @Test fun perform_encode_setValue() {
        assertEquals(
            listOf("perform", "android-1", "setValue", "emailField", "user@example.com"),
            CliFormat.encode(CliCommand.Perform("android-1", UiAction.SetValue("emailField", "user@example.com")))
        )
    }

    @Test fun perform_encode_navigate() {
        assertEquals(
            listOf("perform", "android-1", "navigate", "/home"),
            CliFormat.encode(CliCommand.Perform("android-1", UiAction.Navigate("/home")))
        )
    }

    @Test fun parsePerform_click() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.Click("loginBtn")),
            CliFormat.parse(listOf("perform", "android-1", "click", "loginBtn"))
        )
    }

    @Test fun parsePerform_longClick() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.LongClick("listItem")),
            CliFormat.parse(listOf("perform", "android-1", "longClick", "listItem"))
        )
    }

    @Test fun parsePerform_setValue() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.SetValue("emailField", "user@example.com")),
            CliFormat.parse(listOf("perform", "android-1", "setValue", "emailField", "user@example.com"))
        )
    }

    @Test fun parsePerform_scroll_withFlags() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.Scroll("myList", dx = 0f, dy = 200f)),
            CliFormat.parse(listOf("perform", "android-1", "scroll", "myList", "--dy", "200.0"))
        )
    }

    @Test fun parsePerform_navigate() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.Navigate("/home")),
            CliFormat.parse(listOf("perform", "android-1", "navigate", "/home"))
        )
    }

    @Test fun parsePerform_back() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.Back),
            CliFormat.parse(listOf("perform", "android-1", "back"))
        )
    }

    @Test fun parsePerform_forward() {
        assertEquals(
            CliCommand.Perform("android-1", UiAction.Forward),
            CliFormat.parse(listOf("perform", "android-1", "forward"))
        )
    }

    // ---- Round-trip tests (encode → parse → equals original) ----

    @Test fun roundTrip_list() {
        val cmd = CliCommand.ListApps()
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_info() {
        val cmd = CliCommand.Info("my-app")
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_snapshot() {
        val cmd = CliCommand.Snapshot("android-1", component = "loginForm", format = CliCommand.Snapshot.Format.Json)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_screenshot() {
        val cmd = CliCommand.Screenshot("android-1", path = "shot.png")
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_wait() {
        val cmd = CliCommand.Wait("android-1", page = "Home", timeout = 3000L, change = true)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_record() {
        val cmd = CliCommand.Record("android-1", "export")
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_start() {
        val cmd = CliCommand.Start(port = 8080, daemon = true)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_stop() {
        assertEquals(CliCommand.Stop, CliFormat.parse(CliFormat.encode(CliCommand.Stop)))
    }

    @Test fun roundTrip_status() {
        assertEquals(CliCommand.Status, CliFormat.parse(CliFormat.encode(CliCommand.Status)))
    }

    @Test fun roundTrip_perform_click() {
        val cmd = CliCommand.Perform("android-1", UiAction.Click("loginBtn"))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_longClick() {
        val cmd = CliCommand.Perform("android-1", UiAction.LongClick("listItem"))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_setValue() {
        val cmd = CliCommand.Perform("android-1", UiAction.SetValue("emailField", "test@example.com"))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_scroll() {
        val cmd = CliCommand.Perform("android-1", UiAction.Scroll("myList", dx = 0f, dy = 200f))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_navigate() {
        val cmd = CliCommand.Perform("android-1", UiAction.Navigate("/settings"))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_back() {
        val cmd = CliCommand.Perform("android-1", UiAction.Back)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_forward() {
        val cmd = CliCommand.Perform("android-1", UiAction.Forward)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    // by Claude - additional round-trip tests for Logs, Find, Mock, DragAndDrop, Snapshot interactiveOnly

    @Test fun roundTrip_logs_defaults() {
        val cmd = CliCommand.Logs("android-1")
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_logs_withOptions() {
        val cmd = CliCommand.Logs("android-1", lines = 50, format = CliCommand.Logs.Format.Json)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_find_defaults() {
        val cmd = CliCommand.Find("android-1")
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_find_withSearchFields() {
        val cmd = CliCommand.Find("android-1", value = "Submit", type = "Button", action = "click")
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_mock_file() {
        val cmd = CliCommand.Mock("android-1", MockType.File("photo.jpg", mimeType = "image/jpeg"))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_mock_geolocation() {
        val cmd = CliCommand.Mock("android-1", MockType.Geolocation(40.7128, -74.0060, accuracy = 5.0))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_perform_dragAndDrop() {
        val cmd = CliCommand.Perform("android-1", UiAction.DragAndDrop("item1", toTargetId = "dropZone"))
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }

    @Test fun roundTrip_snapshot_interactiveOnly() {
        val cmd = CliCommand.Snapshot("android-1", interactiveOnly = true)
        assertEquals(cmd, CliFormat.parse(CliFormat.encode(cmd)))
    }
}
