package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue

@OptIn(ExperimentalMaterialApi::class)
actual class AutoCompleteTextField actual constructor(context: RContext) :
    RViewWithAction(context) {
    actual val content: MutableReactiveValue<String> = Signal("")

    private val m_keyboardHints = Signal(KeyboardHints())
    actual var keyboardHints: KeyboardHints by m_keyboardHints

    private val m_suggestions = Signal(listOf<String>())
    actual var suggestions: List<String> by m_suggestions

    @Composable
    override fun compose() {
        val content = content.collectAsMutableState()
        val suggestions = m_suggestions.collectAsMutableState()

        var expanded by remember { mutableStateOf(false) }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = content.value,
                onValueChange = {
                    content.value = it
                    expanded = true
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth()
            )

            val filteringOptions = suggestions.value.filter { it.contains(content.value, ignoreCase = true) }
            if (filteringOptions.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {
                    filteringOptions.forEach { selectionOption ->
                        DropdownMenuItem(onClick = {
                            content.value = selectionOption
                            expanded = false
                        }) {
                            Text(selectionOption)
                        }
                    }
                }
            }
        }
    }
}