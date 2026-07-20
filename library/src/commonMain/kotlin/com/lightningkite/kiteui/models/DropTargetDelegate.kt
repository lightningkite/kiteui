package com.lightningkite.kiteui.models

public interface DropTargetDelegate {
    public fun over(event: DragEvent): Boolean = true

    public fun enter(event: DragEvent): Boolean = true
    public fun exit(event: DragEvent): Boolean = true

    public fun end(event: DragEvent): Boolean = true

    public fun drop(event: DragEvent): Boolean
}