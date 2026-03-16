package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.DriverActionException
import kotlinx.datetime.*

// --- Button ---

fun Button.buttonDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    action?.let { a -> put("click") { a.startAction(this@buttonDriverActions); "OK" } }
    secondaryAction?.let { a -> put("longClick") { a.startAction(this@buttonDriverActions); "OK" } }
}

// --- TextInput ---

fun TextInput.textInputDriverValue(): String = content.value
fun TextInput.textInputDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@textInputDriverActions); "OK" } }
}

// --- TextArea ---

fun TextArea.textAreaDriverValue(): String = content.value
fun TextArea.textAreaDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@textAreaDriverActions); "OK" } }
}

// --- Checkbox ---

fun Checkbox.checkboxDriverValue(): String = checked.value.toString()
fun Checkbox.checkboxDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "toggle" to { checked.set(!checked.value); "OK" },
    "setValue" to { args ->
        val v = args.firstOrNull()?.toBooleanStrictOrNull() ?: throw DriverActionException("expected true/false argument")
        checked.set(v); "OK"
    },
)

// --- RadioButton ---

fun RadioButton.radioDriverValue(): String = checked.value.toString()
fun RadioButton.radioDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "select" to { checked.set(true); "OK" },
)

// --- Switch ---

fun Switch.switchDriverValue(): String = checked.value.toString()
fun Switch.switchDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "toggle" to { checked.set(!checked.value); "OK" },
    "setValue" to { args ->
        val v = args.firstOrNull()?.toBooleanStrictOrNull() ?: throw DriverActionException("expected true/false argument")
        checked.set(v); "OK"
    },
)

// --- ToggleButton ---

fun ToggleButton.toggleDriverValue(): String = checked.value.toString()
fun ToggleButton.toggleDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "toggle" to { checked.set(!checked.value); "OK" },
    "setValue" to { args ->
        val v = args.firstOrNull()?.toBooleanStrictOrNull() ?: throw DriverActionException("expected true/false argument")
        checked.set(v); "OK"
    },
)

// --- Slider ---

fun Slider.sliderDriverValue(): String = value.value.toString()
fun Slider.sliderDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "setValue" to { args ->
        val v = args.firstOrNull()?.toFloatOrNull() ?: throw DriverActionException("expected number argument")
        value.set(v.coerceIn(min, max)); "OK"
    },
)

// --- Link ---

fun Link.linkDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    if (to != null) put("click") {
        val page = to?.invoke() ?: throw DriverActionException("no navigation target on this link")
        if (resetsStack) onNavigator.reset(page) else onNavigator.navigate(page)
        "OK"
    }
}

// --- MenuButton ---

fun MenuButton.menuDriverActions(
    click: (suspend (List<String>) -> String)? = null,
): Map<String, suspend (List<String>) -> String> = buildMap {
    click?.let { put("click", it) }
}

// --- AutoCompleteTextField ---

fun AutoCompleteTextField.autoCompleteDriverValue(): String = content.value
fun AutoCompleteTextField.autoCompleteDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@autoCompleteDriverActions); "OK" } }
}

// --- NumberInput ---

fun NumberInput.numberInputDriverValue(): String = content.value?.toString() ?: ""
fun NumberInput.numberInputDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull()?.toDoubleOrNull() ?: throw DriverActionException("expected number argument")
        val clamped = range?.let { v.coerceIn(it) } ?: v
        content.set(clamped); "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@numberInputDriverActions); "OK" } }
}

// --- FormattedTextInput ---

fun FormattedTextInput.formattedTextInputDriverValue(): String = content.value
fun FormattedTextInput.formattedTextInputDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args -> content.set(args.joinToString(" ")); "OK" }
    action?.let { a -> put("submit") { a.startAction(this@formattedTextInputDriverActions); "OK" } }
}

// --- ExternalLink ---

fun ExternalLink.externalLinkDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    if (to != null) put("click") {
        // ExternalLink doesn't navigate in-app, just report the URL
        "OK: ${to}"
    }
}

// --- RadioToggleButton ---

fun RadioToggleButton.radioToggleDriverValue(): String = checked.value.toString()
fun RadioToggleButton.radioToggleDriverActions(): Map<String, suspend (List<String>) -> String> = mapOf(
    "select" to { checked.set(true); "OK" },
)

// --- LocalDateField ---

fun LocalDateField.localDateDriverValue(): String = content.value?.toString() ?: ""
fun LocalDateField.localDateDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull() ?: throw DriverActionException("expected date argument (yyyy-MM-dd)")
        try { content.set(LocalDate.parse(v)) } catch (e: Exception) { throw DriverActionException("invalid date '$v': ${e.message}") }
        "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@localDateDriverActions); "OK" } }
}

// --- LocalTimeField ---

fun LocalTimeField.localTimeDriverValue(): String = content.value?.toString() ?: ""
fun LocalTimeField.localTimeDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull() ?: throw DriverActionException("expected time argument (HH:mm or HH:mm:ss)")
        try { content.set(LocalTime.parse(v)) } catch (e: Exception) { throw DriverActionException("invalid time '$v': ${e.message}") }
        "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@localTimeDriverActions); "OK" } }
}

// --- LocalDateTimeField ---

fun LocalDateTimeField.localDateTimeDriverValue(): String = content.value?.toString() ?: ""
fun LocalDateTimeField.localDateTimeDriverActions(): Map<String, suspend (List<String>) -> String> = buildMap {
    put("setValue") { args ->
        val v = args.firstOrNull() ?: throw DriverActionException("expected datetime argument (yyyy-MM-ddTHH:mm:ss)")
        try { content.set(LocalDateTime.parse(v)) } catch (e: Exception) { throw DriverActionException("invalid datetime '$v': ${e.message}") }
        "OK"
    }
    action?.let { a -> put("submit") { a.startAction(this@localDateTimeDriverActions); "OK" } }
}
