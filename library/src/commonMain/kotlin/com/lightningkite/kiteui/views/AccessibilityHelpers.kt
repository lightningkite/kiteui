// by Claude - shared helpers for accessibility value/action overrides across all platforms.
// Eliminates duplication of identical logic in Android, iOS, and commonHtml view implementations.
package com.lightningkite.kiteui.views

import com.lightningkite.reactive.core.MutableReactiveValue

// --- Action set constants ---

val CLICK_AND_SET_VALUE_ACTIONS: Set<String> = setOf("click", "setValue")
val SET_VALUE_ACTIONS: Set<String> = setOf("setValue")

// --- Boolean controls (Checkbox, Switch, RadioButton, RadioToggleButton, ToggleButton) ---

fun readBooleanValue(prop: MutableReactiveValue<Boolean>): String =
    prop.value.toString()

fun writeBooleanValue(prop: MutableReactiveValue<Boolean>, value: String?) {
    prop.value = when (value?.lowercase()) {
        "true" -> true
        "false" -> false
        else -> throw IllegalArgumentException("Expected 'true' or 'false', got '$value'")
    }
}

fun performBooleanAction(
    prop: MutableReactiveValue<Boolean>,
    action: String,
    value: String?,
    fallback: (String, String?) -> String?
): String? = when (action) {
    "click" -> { prop.value = !prop.value; null }
    "setValue" -> { writeBooleanValue(prop, value); null }
    else -> fallback(action, value)
}

// --- String controls (TextField, TextArea, AutoCompleteTextField, FormattedTextInput) ---

fun readStringValue(prop: MutableReactiveValue<String>): String =
    prop.value

fun writeStringValue(prop: MutableReactiveValue<String>, value: String?) {
    prop.value = value ?: ""
}

fun performStringSetValueAction(
    prop: MutableReactiveValue<String>,
    action: String,
    value: String?,
    fallback: (String, String?) -> String?
): String? = when (action) {
    "setValue" -> { writeStringValue(prop, value); null }
    else -> fallback(action, value)
}

// --- Nullable-parseable controls (NumberField, Slider, LocalDate/Time/DateTime fields) ---

fun <T> readNullableValue(prop: MutableReactiveValue<T?>): String? =
    prop.value?.toString()

fun <T> writeNullableValue(prop: MutableReactiveValue<T?>, value: String?, parser: (String) -> T?) {
    prop.value = value?.let(parser)
}

fun <T> performNullableSetValueAction(
    prop: MutableReactiveValue<T?>,
    parser: (String) -> T?,
    action: String,
    value: String?,
    fallback: (String, String?) -> String?
): String? = when (action) {
    "setValue" -> { writeNullableValue(prop, value, parser); null }
    else -> fallback(action, value)
}

// --- Non-nullable parseable controls (Slider) ---

fun <T> readNonNullValue(prop: MutableReactiveValue<T>): String =
    prop.value.toString()

fun <T> writeNonNullValue(prop: MutableReactiveValue<T>, value: String?, parser: (String) -> T) {
    prop.value = parser(value ?: throw IllegalArgumentException("Cannot set null value"))
}

fun <T> performNonNullSetValueAction(
    prop: MutableReactiveValue<T>,
    parser: (String) -> T,
    action: String,
    value: String?,
    fallback: (String, String?) -> String?
): String? = when (action) {
    "setValue" -> { writeNonNullValue(prop, value, parser); null }
    else -> fallback(action, value)
}
