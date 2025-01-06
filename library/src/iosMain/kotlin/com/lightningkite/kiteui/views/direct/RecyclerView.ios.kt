@file:OptIn(ExperimentalNativeApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UICollectionViewFlowLayout3Protocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.ReactiveLoading
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIfAllowed
import com.lightningkite.kiteui.views.direct.RecyclerView
import com.lightningkite.kiteui.views.direct.RecyclerView.ChildType
import com.lightningkite.kiteui.views.extensionPadding
import com.lightningkite.kiteui.views.withoutAnimation
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import platform.CoreGraphics.*
import platform.CoreGraphics.CGSizeMake
import platform.CoreText.kCTFontManagerScopeUser
import platform.Foundation.NSCoder
import platform.Foundation.NSIndexPath
import platform.QuartzCore.CATextLayer
import platform.UIKit.*
import platform.darwin.EXCEPTION_STATE
import platform.darwin.NSInteger
import platform.darwin.NSObject
import platform.objc.object_getClass
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalNativeApi
import kotlin.experimental.ExperimentalObjCName
import kotlin.math.floor
import kotlin.math.max
import kotlin.native.ref.WeakReference

actual class RecyclerView actual constructor(context: RContext) : RView(context) {

    private var delegate: UICollectionViewDelegateProtocol? = object: NSObject(), UICollectionViewDelegateProtocol {
        @OptIn(ExperimentalForeignApi::class)
        override fun scrollViewDidScroll(scrollView: UIScrollView) {
            val native = scrollView as UICollectionView
            native.bounds.useContents {
                val outerBounds = this
                (native.indexPathsForVisibleItems as List<NSIndexPath>).filter {
                    native.cellForItemAtIndexPath(it)?.frame?.useContents {
                        this.origin.x >= outerBounds.origin.x &&
                                this.origin.y >= outerBounds.origin.y &&
                                this.origin.x + this.size.width <= outerBounds.origin.x + outerBounds.size.width &&
                                this.origin.y + this.size.height <= outerBounds.origin.y + outerBounds.size.height
                    } == true
                }.rangeOfOrNull { it.row.toInt() }?.let {
                    it.start.let { if (_firstVisibleIndex.value != it) _firstVisibleIndex.value = it }
                    it.endInclusive.let { if (_lastVisibleIndex.value != it) _lastVisibleIndex.value = it }
                }
            }
            native.indexPathForItemAtPoint(native.frame.useContents { CGPointMake(size.width / 2, size.height / 2) })?.row?.toInt()?.let {
                if (_centerIndex.value != it) _centerIndex.value = it
            }
        }
    }

    companion object {
        fun makeLayout(vertical: Boolean, columns: Int, padding: Dimension? = null, spacing: Dimension): UICollectionViewCompositionalLayout {
            println("makeLayout vertical: $vertical, columns: $columns, padding: $padding, spacing: $spacing")
//            NSCollectionLayoutSection.sectionWithListConfiguration()
            val layout = if(vertical) UICollectionViewCompositionalLayout(NSCollectionLayoutSection.sectionWithGroup(NSCollectionLayoutGroup.horizontalGroupWithLayoutSize(
                layoutSize = NSCollectionLayoutSize.sizeWithWidthDimension(
                    width = NSCollectionLayoutDimension.fractionalWidthDimension(1.0),
                    heightDimension = NSCollectionLayoutDimension.uniformAcrossSiblingsWithEstimate(100.0),
                ),
                subitem = NSCollectionLayoutItem.itemWithLayoutSize(
                    layoutSize = NSCollectionLayoutSize.sizeWithWidthDimension(
                        width = NSCollectionLayoutDimension.fractionalWidthDimension(1.0 / columns),
                        heightDimension = NSCollectionLayoutDimension.uniformAcrossSiblingsWithEstimate(100.0),
                    ),
                ),
                count = columns.toLong()
            )).apply {
                contentInsets = NSDirectionalEdgeInsetsMake(top = 0.0, leading = 0.0, bottom = spacing.value, trailing = 0.0)
            }) else UICollectionViewCompositionalLayout(NSCollectionLayoutSection.sectionWithGroup(NSCollectionLayoutGroup.verticalGroupWithLayoutSize(
                layoutSize = NSCollectionLayoutSize.sizeWithWidthDimension(
                    width = NSCollectionLayoutDimension.uniformAcrossSiblingsWithEstimate(100.0),
                    heightDimension = NSCollectionLayoutDimension.fractionalHeightDimension(1.0),
                ),
                subitem = NSCollectionLayoutItem.itemWithLayoutSize(
                    layoutSize = NSCollectionLayoutSize.sizeWithWidthDimension(
                        width = NSCollectionLayoutDimension.uniformAcrossSiblingsWithEstimate(100.0),
                        heightDimension = NSCollectionLayoutDimension.fractionalHeightDimension(1.0 / columns),
                    ),
                ),
                count = columns.toLong()
            )).apply {
                contentInsets = NSDirectionalEdgeInsetsMake(top = 0.0, leading = 0.0, bottom = 0.0, trailing = spacing.value)
            }).apply {
                configuration = configuration.apply {
                    scrollDirection =
                        UICollectionViewScrollDirection.UICollectionViewScrollDirectionHorizontal
                }
            }
            return layout
        }
    }

    override val native = UICollectionView(
        CGRectMake(
            0.0,
            0.0,
            0.0,
            0.0
        ),
        makeLayout(true, 1, null, 0.px)
    ).apply {
        backgroundColor = UIColor.clearColor
        delegate = this@RecyclerView.delegate
    }
    init {
        onRemove {
            native.delegate = null
            native.dataSource = null
            delegate = null
            dataSource = null
        }
    }

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
        render: ViewWriter.(value: Readable<T>) -> Unit,
    ): Unit = children(items, { it }, listOf(object : ChildType<T> {
        override fun matches(value: T): Boolean = true
        override fun render(viewWriter: ViewWriter, value: Readable<T>) = render(viewWriter, value)
    }))

    private var dataSource: UICollectionViewDiffableDataSource? = null

    interface ChildType<T> {
        fun matches(value: T): Boolean
        fun render(viewWriter: ViewWriter, value: Readable<T>)
    }

    fun <T, ID> children(items: Readable<List<T>>, identity: (T) -> ID, types: List<ChildType<T>>): Unit {
        dataSource = native.configure(this, items, identity, types)
    }

    actual var vertical: Boolean = true
        set(value) {
            field = value
            refreshLayout()
        }

    actual var columns: Int = 1
        set(value) {
            field = value
            refreshLayout()
        }

    private var paddingAmount: Dimension? = null
    private var defaultSpacing: Dimension = 0.rem
    override fun applyPadding(dimension: Dimension?) {
        super.applyPadding(dimension)
        paddingAmount = dimension
        val v = dimension?.value ?: 0.0
        native.contentInset = UIEdgeInsetsMake(v, v, v, v)
        defaultSpacing = theme.spacing
        refreshLayout()
    }

    override fun spacingSet(value: Dimension?) {
        super.spacingSet(value)
        refreshLayout()
    }

    private var last_vertical = vertical
    private var last_columns = columns
    private var last_paddingAmount = paddingAmount
    private var last_defaultSpacing = defaultSpacing
    fun refreshLayout() {
        if (
            last_vertical != vertical ||
            last_columns != columns ||
            last_paddingAmount != paddingAmount ||
            last_defaultSpacing != defaultSpacing
        ) {
            native.collectionViewLayout = makeLayout(vertical, columns, paddingAmount, defaultSpacing)
            last_vertical = vertical
            last_columns = columns
            last_paddingAmount = paddingAmount
            last_defaultSpacing = defaultSpacing
        }
    }

    actual fun scrollToIndex(
        index: Int,
        align: Align?,
        animate: Boolean,
    ) {
        if (index in 0..<native.numberOfItemsInSection(0L)) {
            native.scrollToItemAtIndexPath(
                NSIndexPath.indexPathForRow(index.toLong(), 0L),
                when (align) {
                    Align.Start -> UICollectionViewScrollPositionLeft or UICollectionViewScrollPositionTop
                    Align.Center -> UICollectionViewScrollPositionCenteredVertically or UICollectionViewScrollPositionCenteredHorizontally
                    Align.End -> UICollectionViewScrollPositionRight or UICollectionViewScrollPositionBottom
                    else -> UICollectionViewScrollPositionCenteredVertically or UICollectionViewScrollPositionCenteredHorizontally
                },
                animate
            )
        }
    }
    private val _firstVisibleIndex = Property<Int>(0)
    actual val firstVisibleIndex: Readable<Int> get() = _firstVisibleIndex
    private val _lastVisibleIndex = Property<Int>(0)
    actual val lastVisibleIndex: Readable<Int> get() = _lastVisibleIndex
    private val _centerIndex = Property<Int>(0)
    val centerIndex: Readable<Int> get() = _centerIndex
}