package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

actual class RecyclerView actual constructor(context: RContext) : RView(context) {
    val newViews = NewViewWriter(this, context)
    override val native = NRecyclerView()

    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ): Unit {
        native.renderer = ItemRenderer<T>(
            create = { parent, value ->
                val prop = Property(value)
                render(newViews, prop)
                val new = newViews.newView!!
                addChild(new)
                new.tag = prop
                new.native
            },
            update = { parent, element, value ->
                @Suppress("UNCHECKED_CAST")
                (children.find { it.native === element }?.tag as? Property<T>)?.value = value
            },
            shutdown = { parent, element ->
                // Shutdown may be called for fake space elements created by a columned ItemRenderer (see
                // NRecyclerView.rendererDirect) that this RView is unaware of so we require a greater than zero check
                children.indexOfFirst { it.native === element }.takeUnless { it < 0 }?.let(::removeChild)
            }
        )
        reactiveScope {
            native.data = items().asIndexed()
        }
    }

    actual var columns: Int
        get() = native.columns
        set(value) {
            native.columns = value
        }

    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        native.spacing = (value ?: if(useNavSpacing) theme.navSpacing else theme.spacing)
    }
    override fun applyForeground(theme: Theme) {
        native.spacing = (spacing ?: if(useNavSpacing) theme.navSpacing else theme.spacing)
    }

    actual fun scrollToIndex(
        index: Int,
        align: Align?,
        animate: Boolean
    ) {
        native.jump(index, align ?: Align.Center, animate)
    }

    actual val firstVisibleIndex: Readable<Int>
        get() = native.firstVisible

    actual val lastVisibleIndex: Readable<Int>
        get() = native.lastVisible

    actual var vertical: Boolean
        get() = native.vertical
        set(value) { native.vertical = value }
    
    init {
        onRemove {
            native.shutdown()
        }
    }
}
