package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView

/**
 * A custom layout that implements flexbox-like wrapping behavior for Android.
 * This is similar to the FlexLayout used in the iOS implementation.
 */


actual class RowWrapping actual constructor(context: RContext) : RView(context) {


    @Composable
    override fun compose() {
        println("In compose")
//        Row {
        Text("Compose:")
//            children.forEach { child ->
//                child.compose()
//            }
//        }
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
    }

    @Composable
    override fun compose() {
        if (vertical) {
            Column {
                children.forEach { child ->
                    child.compose()
                }
            }
        } else {
            Row {
                children.forEach { child ->
                    child.compose()
                }
            }
        }
    }
}