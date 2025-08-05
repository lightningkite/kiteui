package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.DragEvent

public interface DropTargetDelegate {
    public fun over(event: DragEvent): Boolean = true

    public fun enter(event: DragEvent): Boolean = true
    public fun exit(event: DragEvent): Boolean = true

    public fun end(event: DragEvent): Boolean = true

    public fun drop(event: DragEvent): Boolean
}