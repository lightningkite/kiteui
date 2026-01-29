package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.coordinatorFrame

actual fun ViewWriter.openBottomSheet(
    halfScreenRatio: Float,
    dim: Boolean,
    view: ViewWriter.() -> Unit
) {
    // Get the coordinator frame from context
    val coordinator = coordinatorFrame ?: run {
        // If no coordinator frame is available, we can't show a bottom sheet
        // This matches the Android implementation pattern which returns early if overlayFrame is null
        return
    }

    // Use the coordinator's bottomSheet method to display the sheet
    coordinator.bottomSheet(
        peekSize = null,  // No peek size - sheet starts from bottom
        partialRatio = halfScreenRatio,  // Use the provided ratio for partial expansion
        draggable = true,  // Allow user to drag the sheet
        startState = BottomSheetState.EXPANDED,  // Start expanded by default
        shouldRemoveExpandedCorners = false,  // Keep rounded corners
        blockBehind = dim,  // Dim the background if requested
        content = { control ->
            // Provide the user's view content
            view()
        }
    )
}
