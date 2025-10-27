package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.lightningkite.reactive.core.Signal
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.reactive.collectAsState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
actual class Select actual constructor(context: RContext) :
    RView(context) {
    private val m_enabled = Signal(true)
    actual var enabled: Boolean by m_enabled

    private var boundEdits: MutableReactive<*>? = null
    private var boundData: Reactive<List<*>>? = null
    private var boundRender: ((Any?) -> String)? = null

    actual fun <T> bind(
        edits: MutableReactive<T>,
        data: Reactive<List<T>>,
        render: (T) -> String
    ) {
        this.boundEdits = edits
        this.boundData = data
        this.boundRender = render as ((Any?) -> String)
    }

    @Composable
    override fun compose() {
        val enabled = m_enabled.collectAsMutableState()
        val edits = boundEdits?.collectAsState(null)
        val data = boundData?.collectAsState(null)
        val render = boundRender

        var expanded by remember { mutableStateOf(false) }

        if (edits == null || data == null || render == null) return

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = {
                expanded = !expanded
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = render(edits.value),
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                data.value?.forEach { selectionOption ->
                    DropdownMenuItem(onClick = {
                        launch { (boundEdits as? MutableReactive<Any?>)?.set(selectionOption) }
                        expanded = false
                    }) {
                        Text(render(selectionOption))
                    }
                }
            }
        }
    }
}