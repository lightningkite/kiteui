package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.AutoComplete
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.models.KeyboardType
import com.lightningkite.kiteui.views.FutureElement
import com.lightningkite.kiteui.views.autocomplete
import com.lightningkite.kiteui.views.inputMode
import com.lightningkite.kiteui.views.type

fun FutureElement.applyKeyboardHints(hints: KeyboardHints) {
    attributes.type = when (hints.type) {
        KeyboardType.Text -> "text"
        KeyboardType.Decimal -> "text"
        KeyboardType.Integer -> "text"
        KeyboardType.Phone -> "tel"
        KeyboardType.Email -> "text"
        KeyboardType.IntegerWithNegative -> "text"
        KeyboardType.DecimalWithNegative -> "text"
    }
    attributes.inputMode = when (hints.type) {
        KeyboardType.Text -> "text"
        KeyboardType.Decimal -> "decimal"
        KeyboardType.Integer -> "numeric"
        KeyboardType.Phone -> "tel"
        KeyboardType.Email -> "email"
        KeyboardType.IntegerWithNegative -> if (usingWebOnMobile()) "text" else "numeric"
        KeyboardType.DecimalWithNegative -> if (usingWebOnMobile()) "text" else "decimal"
    }

    val primaryAutocompleteValue: String?
    when (hints.autocomplete) {
        AutoComplete.Email -> {
            attributes.type = "email"
            primaryAutocompleteValue = "email"
        }

        AutoComplete.Password -> {
            attributes.type = "password"
            primaryAutocompleteValue = "current-password"
        }

        AutoComplete.NewPassword -> {
            attributes.type = "password"
            primaryAutocompleteValue = "new-password"
        }

        AutoComplete.Phone -> {
            primaryAutocompleteValue = "tel"
        }

        AutoComplete.OneTimeCode, null -> {
            primaryAutocompleteValue = null
        }
    }

    attributes.autocomplete = listOfNotNull(
        primaryAutocompleteValue,
        "webauthn".takeIf { hints.includePasskeys }
    ).joinToString(" ").takeIf { it.isNotEmpty() } ?: "off"
}