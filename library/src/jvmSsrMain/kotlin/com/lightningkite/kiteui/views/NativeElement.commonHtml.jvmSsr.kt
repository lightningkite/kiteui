package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.DragData
import com.lightningkite.kiteui.models.DropTargetDelegate

public actual fun NativeElement.nativeScrollIntoView(
    horizontal: Align?,
    vertical: Align?,
    animate: Boolean
) {
    // No-op
}

public actual fun NativeElement.nativeSetDragData(data: DragData?) { /* No-op */ }
public actual fun NativeElement.nativeOnDrop(listener: DropTargetDelegate?) { /* No-op */ }