package com.lightningkite.kiteui.views.direct

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.lightningkite.kiteui.models.CornerRadii

fun getShape(cornerRadii: CornerRadii) = when (cornerRadii) {
    is CornerRadii.Constant -> RoundedCornerShape(cornerRadii.value.value.dp)
    is CornerRadii.ForceConstant -> RoundedCornerShape(cornerRadii.value.value.dp)
    is CornerRadii.RatioOfSpacing -> RoundedCornerShape(50) // TODO: Calculate based on spacing
    is CornerRadii.RatioOfSize -> RoundedCornerShape(50) // TODO: Calculate based on size
    is CornerRadii.PerCorner -> {
        val radius = cornerRadii.value.value.dp
        RoundedCornerShape(
            topStart = if (cornerRadii.topLeft) radius else 0.dp,
            topEnd = if (cornerRadii.topRight) radius else 0.dp,
            bottomStart = if (cornerRadii.bottomLeft) radius else 0.dp,
            bottomEnd = if (cornerRadii.bottomRight) radius else 0.dp
        )
    }
}
