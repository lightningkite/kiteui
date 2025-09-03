package com.lightningkite.kiteui.views.direct


import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.collectAsMutableState
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.reactive.core.Signal

actual class TextView actual constructor(context: RContext) :
    RView(context) {

    var m_content = Signal("")
    val m_align = Signal<Align?>(null)


    @Composable
    override fun compose() {
        val contentState = m_content.collectAsMutableState()
        val alignState = m_align.collectAsMutableState()
        Text(text = contentState.value, textAlign = alignState.value?.toComposeAlign())
    }


    actual var content: String
        get() {
            return m_content.value
        }
        set(value) {
            println("DEBUG value ${value}")
            m_content.value = value
        }

    actual var align: Align
        get() = m_align.value ?: Align.Start
        set(value) {
            m_align.value = value
        }
    actual var ellipsis: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var wraps: Boolean
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var wordBreak: WordBreak
        get() = TODO("Not yet implemented")
        set(value) {}
    actual var lineClamp: Int?
        get() = TODO("Not yet implemented")
        set(value) {}

    actual fun setBasicHtmlContent(html: String) {
    }
}
