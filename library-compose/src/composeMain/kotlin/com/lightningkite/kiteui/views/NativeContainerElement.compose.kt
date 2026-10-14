package com.lightningkite.kiteui.views

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layoutId

public actual abstract class NativeContainerElement actual constructor(context: ElementContext) : NativeContainerElementCommonCode(context) {
    /** [children] as snapshot state, so composition sees additions and removals. */
    protected val nativeChildren: SnapshotStateList<NativeElement> = mutableStateListOf()

    actual override fun nativeAddChild(index: Int, element: Element) {
        nativeChildren.add(index, element.underlyingNativeElement)
    }

    actual override fun nativeRemoveChild(index: Int) {
        nativeChildren.removeAt(index)
    }

    actual override fun nativeClearChildren() {
        nativeChildren.clear()
    }

    /** Emits the shown children, each tagged with its element so layouts can read its alignment and weight. */
    @Composable
    protected fun ShownChildren() {
        for (child in nativeChildren) {
            if (child.takesSpace) key(child) { child.Render(Modifier.layoutId(child)) }
        }
    }
}
