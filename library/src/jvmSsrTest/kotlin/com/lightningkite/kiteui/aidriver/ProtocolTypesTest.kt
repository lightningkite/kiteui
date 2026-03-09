// by Claude - unit tests for protocol data types: UiSnapshot, UiComponent, AppMessage, UiAction
package com.lightningkite.kiteui.aidriver

import kotlinx.serialization.json.Json
import kotlin.test.*

class ProtocolTypesTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    // ---- ScreenRect ----

    @Test fun screenRect_toString() {
        val rect = ScreenRect(top = 10.0, left = 20.0, width = 100.0, height = 50.0)
        assertEquals("x: 20.0-120.0, y: 10.0-60.0", rect.toString())
    }

    @Test fun screenRect_toString_origin() {
        val rect = ScreenRect(top = 0.0, left = 0.0, width = 375.0, height = 812.0)
        assertEquals("x: 0.0-375.0, y: 0.0-812.0", rect.toString())
    }

    // ---- UiComponent.render() ----

    @Test fun uiComponent_render_simple() {
        val comp = UiComponent("loginForm", "Col")
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertEquals("loginForm: Col\n", sb.toString())
    }

    @Test fun uiComponent_render_withValue() {
        val comp = UiComponent("emailInput", "TextInput", value = "test@example.com")
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("= \"test@example.com\""), "Should contain value")
    }

    @Test fun uiComponent_render_valueEscapesQuotes() {
        val comp = UiComponent("label", "Text", value = "say \"hello\"")
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("\\\"hello\\\""), "Quotes should be escaped")
    }

    @Test fun uiComponent_render_disabled() {
        val comp = UiComponent("submitBtn", "Button", enabled = false)
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("disabled"), "Should show disabled flag")
    }

    @Test fun uiComponent_render_hidden() {
        val comp = UiComponent("overlay", "Frame", shown = false)
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("hidden"), "Should show hidden flag")
    }

    @Test fun uiComponent_render_invisible() {
        val comp = UiComponent("spacer", "Frame", visible = false)
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("invisible"), "Should show invisible flag")
    }

    @Test fun uiComponent_render_multipleFlags() {
        val comp = UiComponent("item", "Row", enabled = false, shown = false)
        val sb = StringBuilder()
        comp.render(sb, 0)
        val text = sb.toString()
        assertTrue(text.contains("disabled"), "Should show disabled")
        assertTrue(text.contains("hidden"), "Should show hidden")
    }

    @Test fun uiComponent_render_nested() {
        val child = UiComponent("emailInput", "TextInput", value = "hi")
        val parent = UiComponent("form", "Col", children = listOf(child))
        val sb = StringBuilder()
        parent.render(sb, 0)
        val lines = sb.toString().lines()
        assertTrue(lines.any { it.startsWith("form:") }, "Parent should appear at indent 0")
        assertTrue(lines.any { it.startsWith("  ") && it.contains("emailInput") }, "Child should be indented 2 spaces")
    }

    @Test fun uiComponent_render_deeplyNested() {
        val grandchild = UiComponent("leaf", "Text")
        val child = UiComponent("mid", "Row", children = listOf(grandchild))
        val root = UiComponent("root", "Col", children = listOf(child))
        val sb = StringBuilder()
        root.render(sb, 0)
        val text = sb.toString()
        assertTrue(text.contains("    leaf:"), "Grandchild should be indented 4 spaces")
    }

    @Test fun uiComponent_render_withTheme() {
        val comp = UiComponent("card", "Col", theme = "important")
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("(theme: important)"), "Should show theme")
    }

    @Test fun uiComponent_render_withScreenCoordinates() {
        val comp = UiComponent("btn", "Button", screenCoordinates = ScreenRect(0.0, 0.0, 100.0, 44.0))
        val sb = StringBuilder()
        comp.render(sb, 0)
        assertTrue(sb.toString().contains("x: 0.0-100.0"), "Should contain screen coordinates")
    }

    // ---- UiSnapshot JSON round-trip ----

    @Test fun uiSnapshot_jsonRoundTrip_empty() {
        val snap = UiSnapshot(
            page = "LoginPage",
            url = "/login",
            settings = UiSnapshotSettings(),
            components = emptyList()
        )
        val decoded = json.decodeFromString(UiSnapshot.serializer(), json.encodeToString(UiSnapshot.serializer(), snap))
        assertEquals(snap, decoded)
    }

    @Test fun uiSnapshot_jsonRoundTrip_withComponents() {
        val snap = UiSnapshot(
            page = "HomePage",
            url = "/home",
            settings = UiSnapshotSettings(includeTheme = true, includeLayoutInfo = true),
            components = listOf(
                UiComponent(
                    "form", "Col",
                    children = listOf(
                        UiComponent("email", "TextInput", value = "user@example.com", actions = setOf("setValue")),
                        UiComponent("submit", "Button", actions = setOf("click"))
                    )
                )
            )
        )
        val decoded = json.decodeFromString(UiSnapshot.serializer(), json.encodeToString(UiSnapshot.serializer(), snap))
        assertEquals(snap, decoded)
    }

    // ---- AppMessage JSON round-trips ----

    @Test fun appMessage_register_jsonRoundTrip() {
        val msg: AppMessage = AppMessage.Register("MyApp", "android")
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    @Test fun appMessage_snapshotResponse_jsonRoundTrip() {
        val snap = UiSnapshot("Page", "/url", UiSnapshotSettings(), emptyList())
        val msg: AppMessage = AppMessage.SnapshotResponse("req-1", snap)
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    @Test fun appMessage_screenshotResponse_success_jsonRoundTrip() {
        val msg: AppMessage = AppMessage.ScreenshotResponse("req-2", base64 = "abc123==")
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    @Test fun appMessage_screenshotResponse_error_jsonRoundTrip() {
        val msg: AppMessage = AppMessage.ScreenshotResponse("req-3", error = "permission denied")
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    @Test fun appMessage_actionResult_success_jsonRoundTrip() {
        val msg: AppMessage = AppMessage.ActionResult("req-4", result = "ok")
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    @Test fun appMessage_actionResult_error_jsonRoundTrip() {
        val msg: AppMessage = AppMessage.ActionResult("req-5", error = "element not found")
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    @Test fun appMessage_changed_jsonRoundTrip() {
        val msg: AppMessage = AppMessage.Changed
        assertEquals(msg, json.decodeFromString(AppMessage.serializer(), json.encodeToString(AppMessage.serializer(), msg)))
    }

    // ---- UiAction JSON round-trips ----

    @Test fun uiAction_click_jsonRoundTrip() {
        val action: UiAction = UiAction.Click("loginButton")
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_longClick_jsonRoundTrip() {
        val action: UiAction = UiAction.LongClick("listItem")
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_setValue_jsonRoundTrip() {
        val action: UiAction = UiAction.SetValue("emailField", "user@example.com")
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_scroll_jsonRoundTrip() {
        val action: UiAction = UiAction.Scroll("list", dx = 0f, dy = 200f)
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_navigate_jsonRoundTrip() {
        val action: UiAction = UiAction.Navigate("/settings")
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_back_jsonRoundTrip() {
        val action: UiAction = UiAction.Back
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_forward_jsonRoundTrip() {
        val action: UiAction = UiAction.Forward
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    @Test fun uiAction_screenshot_jsonRoundTrip() {
        val action: UiAction = UiAction.Screenshot
        assertEquals(action, json.decodeFromString(UiAction.serializer(), json.encodeToString(UiAction.serializer(), action)))
    }

    // by Claude - verify CliCommand.Perform JSON round-trip (nested sealed class)
    @Test fun cliCommand_perform_click_jsonRoundTrip() {
        val cmd: CliCommand = CliCommand.Perform("android-1", UiAction.Click("loginBtn"))
        val encoded = json.encodeToString(CliCommand.serializer(), cmd)
        // Assert round-trip works and check the JSON format
        // by Claude - CliCommand uses "command" discriminator (not "type") to avoid collision with Find.type
        assertEquals("""{"command":"perform","appId":"android-1","action":{"type":"click","targetId":"loginBtn"}}""", encoded)
        val decoded = json.decodeFromString(CliCommand.serializer(), encoded)
        assertEquals(cmd, decoded)
    }

    @Test fun cliCommand_perform_back_jsonRoundTrip() {
        val cmd: CliCommand = CliCommand.Perform("android-1", UiAction.Back)
        val encoded = json.encodeToString(CliCommand.serializer(), cmd)
        println("Encoded Perform/Back: $encoded")
        val decoded = json.decodeFromString(CliCommand.serializer(), encoded)
        assertEquals(cmd, decoded)
    }
}
