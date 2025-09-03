package com.lightningkite.kiteui.views.direct

import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RViewWithAction
import com.lightningkite.reactive.core.MutableReactiveValue
import com.lightningkite.reactive.core.Signal

actual class TextInput actual constructor(context: RContext) :
    RViewWithAction(context) {


    var m_content = Signal<String>("")
    var test = ""
    actual var enabled: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val content: MutableReactiveValue<String>
        get() = m_content
    actual var keyboardHints: KeyboardHints
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var hint: String
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var align: Align
        get() = TODO("Not yet implemented")
        set(value) {}

    @Composable
    override fun compose() {
        var test by remember {
            mutableStateOf("test")
        }
        TextField(
            test,
            {
                println("it ${it}")
                test = it
                m_content.value = it
                println("DEBUG test ${test}")
            }
        )
    }
}