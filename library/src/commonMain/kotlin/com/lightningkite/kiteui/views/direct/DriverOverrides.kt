package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.DriverActionException
import com.lightningkite.kiteui.views.driverChildren
import com.lightningkite.kiteui.views.driverSnapshot
import com.lightningkite.kiteui.views.l2.overlayFrame
import kotlinx.datetime.*

// Password/new-password fields must never surface their plaintext through the driver
// snapshot (used by test automation and MCP-connected agents), since that would defeat
// the UI's visual masking.
private fun KeyboardHints.isSecret(): Boolean =
    autocomplete == AutoComplete.Password || autocomplete == AutoComplete.NewPassword

private fun redactedDriverValue(hints: KeyboardHints, actual: String): String =
    if (hints.isSecret()) "•".repeat(actual.length) else actual

// --- Button ---

public fun Button.buttonDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    action?.let { a -> put("click") { a.startAction(this@buttonDriverActions); "OK" } }
    secondaryAction?.let { a -> put("longClick") { a.startAction(this@buttonDriverActions); "OK" } }
}

// --- TextInput ---

public fun TextInput.textInputDriverValue(): String = redactedDriverValue(keyboardHints, content.value)
public fun TextInput.textInputDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@textInputDriverActions); "OK" } }
}

// --- TextArea ---

public fun TextArea.textAreaDriverValue(): String = redactedDriverValue(keyboardHints, content.value)
public fun TextArea.textAreaDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@textAreaDriverActions); "OK" } }
}

// --- Checkbox ---

public fun Checkbox.checkboxDriverValue(): String = checked.value.toString()
public fun Checkbox.checkboxDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "toggle" to { checked.set(!checked.value); "OK" },
    "setValue" to { args ->
        val v = args.firstOrNull()?.toBooleanStrictOrNull() ?: throw DriverActionException("expected true/false argument")
        checked.set(v); "OK"
    },
)

// --- RadioButton ---

public fun RadioButton.radioDriverValue(): String = checked.value.toString()
public fun RadioButton.radioDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "select" to { checked.set(true); "OK" },
)

// --- Switch ---

public fun Switch.switchDriverValue(): String = checked.value.toString()
public fun Switch.switchDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "toggle" to { checked.set(!checked.value); "OK" },
    "setValue" to { args ->
        val v = args.firstOrNull()?.toBooleanStrictOrNull() ?: throw DriverActionException("expected true/false argument")
        checked.set(v); "OK"
    },
)

// --- ToggleButton ---

public fun ToggleButton.toggleDriverValue(): String = checked.value.toString()
public fun ToggleButton.toggleDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "toggle" to { checked.set(!checked.value); "OK" },
    "setValue" to { args ->
        val v = args.firstOrNull()?.toBooleanStrictOrNull() ?: throw DriverActionException("expected true/false argument")
        checked.set(v); "OK"
    },
)

// --- Slider ---

public fun Slider.sliderDriverValue(): String = value.value.toString()
public fun Slider.sliderDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "setValue" to { args ->
        val v = args.firstOrNull()?.toFloatOrNull() ?: throw DriverActionException("expected number argument")
        value.set(v.coerceIn(min, max)); "OK"
    },
)

// --- Link ---

public fun Link.linkDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    if (to != null) put("click") {
        val page = to?.invoke() ?: throw DriverActionException("no navigation target on this link")
        if (resetsStack) onNavigator.reset(page) else onNavigator.navigate(page)
        "OK"
    }
}

// --- MenuButton ---

public fun MenuButton.menuDriverActions(
    click: (() -> Unit)? = null,
): Map<String, suspend (List<String>) -> String> = buildMap {
    click?.let { fn ->
        put("click") {
            fn()
            context.overlayFrame?.driverChildren()?.lastOrNull()?.driverSnapshot()
                ?: "opened"
        }
    }
}

// --- AutoCompleteTextField ---

public fun AutoCompleteTextField.autoCompleteDriverValue(): String = redactedDriverValue(keyboardHints, content.value)
public fun AutoCompleteTextField.autoCompleteDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@autoCompleteDriverActions); "OK" } }
}

// --- NumberInput ---

public fun NumberInput.numberInputDriverValue(): String = content.value?.toString() ?: ""
public fun NumberInput.numberInputDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull()?.toDoubleOrNull() ?: throw DriverActionException("expected number argument")
        val clamped = range?.let { v.coerceIn(it) } ?: v
        content.set(clamped); "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@numberInputDriverActions); "OK" } }
}

// --- FormattedTextInput ---

public fun FormattedTextInput.formattedTextInputDriverValue(): String = redactedDriverValue(keyboardHints, content.value)
public fun FormattedTextInput.formattedTextInputDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@formattedTextInputDriverActions); "OK" } }
}

// --- ExternalLink ---

public fun ExternalLink.externalLinkDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    if (to != null) put("click") {
        // ExternalLink doesn't navigate in-app, just report the URL
        "OK: ${to}"
    }
}

// --- RadioToggleButton ---

public fun RadioToggleButton.radioToggleDriverValue(): String = checked.value.toString()
public fun RadioToggleButton.radioToggleDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "select" to { checked.set(true); "OK" },
)

// --- LocalDateField ---

public fun LocalDateField.localDateDriverValue(): String = content.value?.toString() ?: ""
public fun LocalDateField.localDateDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull() ?: throw DriverActionException("expected date argument (yyyy-MM-dd)")
        try { content.set(LocalDate.parse(v)) } catch (e: Exception) { throw DriverActionException("invalid date '$v': ${e.message}") }
        "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@localDateDriverActions); "OK" } }
}

// --- LocalTimeField ---

public fun LocalTimeField.localTimeDriverValue(): String = content.value?.toString() ?: ""
public fun LocalTimeField.localTimeDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull() ?: throw DriverActionException("expected time argument (HH:mm or HH:mm:ss)")
        try { content.set(LocalTime.parse(v)) } catch (e: Exception) { throw DriverActionException("invalid time '$v': ${e.message}") }
        "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@localTimeDriverActions); "OK" } }
}

// --- LocalDateTimeField ---

public fun LocalDateTimeField.localDateTimeDriverValue(): String = content.value?.toString() ?: ""
public fun LocalDateTimeField.localDateTimeDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull() ?: throw DriverActionException("expected datetime argument (yyyy-MM-ddTHH:mm:ss)")
        try { content.set(LocalDateTime.parse(v)) } catch (e: Exception) { throw DriverActionException("invalid datetime '$v': ${e.message}") }
        "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@localDateTimeDriverActions); "OK" } }
}
