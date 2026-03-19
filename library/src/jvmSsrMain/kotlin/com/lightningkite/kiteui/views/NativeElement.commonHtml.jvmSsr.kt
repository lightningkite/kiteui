package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DropTargetDelegate

actual fun NativeElement.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
    // No-op
}

actual fun NativeElement.nativeSetDragData(data: DragData?) { /* No-op */ }
actual fun NativeElement.nativeOnDrop(listener: DropTargetDelegate?) { /* No-op */ }