package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.RViewWrapper

/**
 * A custom layout that implements flexbox-like wrapping behavior for Android.
 * This is similar to the FlexLayout used in the iOS implementation.
 */


actual class RowWrapping actual constructor(context: RContext) : RView(context) {

    @Composable
    override fun compose() {
        // Use FlowRow from Compose Foundation for wrapping behavior
        androidx.compose.foundation.layout.FlowRow(
            horizontalArrangement = Arrangement.spacedBy(this.gap?.value?.dp ?: 0.dp),
            verticalArrangement = Arrangement.spacedBy(this.gap?.value?.dp ?: 0.dp)
        ) {
            children.forEach { child ->
                child.compose()
            }
        }
    }
}

actual class RowOrCol actual constructor(context: RContext) :
    RView(context) {
    var m_vertical = true
    actual var vertical: Boolean
        get() = m_vertical
        set(value) {
            m_vertical = value
        }

    actual fun spacingOverrideBeforeNext(amount: Dimension) {
        // TODO: Implement spacingOverrideBeforeNext
    }

    @Composable
    override fun compose() {
        if (vertical) {
            Column(
                verticalArrangement = Arrangement.spacedBy(this.gap?.value?.dp ?: 0.dp)
            ) {
                children.forEach { child ->
                    child.compose()
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(this.gap?.value?.dp ?: 0.dp)
            ) {
                children.forEach { child ->
                    child.compose()
                }
            }
        }
    }
}