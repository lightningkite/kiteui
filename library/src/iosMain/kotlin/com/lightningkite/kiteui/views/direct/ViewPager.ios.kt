package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.RecyclerView
import com.lightningkite.kiteui.views.direct.RecyclerView.ChildType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSCoder
import platform.Foundation.NSIndexPath
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.object_getClass
import kotlin.coroutines.CoroutineContext
import kotlin.experimental.ExperimentalNativeApi
import kotlin.experimental.ExperimentalObjCName
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round
import kotlin.native.ref.WeakReference

actual class ViewPager actual constructor(context: RContext) : RView(context) {
    private val _centerIndex = Property(0)
    private var delegate: UICollectionViewDelegateProtocol? = object : NSObject(), UICollectionViewDelegateProtocol {
        @OptIn(ExperimentalForeignApi::class)
        override fun scrollViewDidScroll(scrollView: UIScrollView) {
            val native = scrollView as UICollectionView
//            native.bounds.useContents {
//                val outerBounds = this
//                (native.indexPathsForVisibleItems as List<NSIndexPath>).filter {
//                    native.cellForItemAtIndexPath(it)?.frame?.useContents {
//                        this.origin.x >= outerBounds.origin.x &&
//                                this.origin.y >= outerBounds.origin.y &&
//                                this.origin.x + this.size.width <= outerBounds.origin.x + outerBounds.size.width &&
//                                this.origin.y + this.size.height <= outerBounds.origin.y + outerBounds.size.height
//                    } == true
//                }.rangeOfOrNull { it.item.toInt() }?.let {
//                    it.start.let { if (_firstVisibleIndex.value != it) _firstVisibleIndex.value = it }
//                    it.endInclusive.let { if (_lastVisibleIndex.value != it) _lastVisibleIndex.value = it }
//                }
//            }
            native.indexPathForItemAtPoint(native.contentOffset.useContents {
                native.bounds.useContents {
                    CGPointMake(size.width / 2 + x, size.height / 2 + y)
                }
            })?.also{
                println("item: ${it.item}, sec: ${it.section}")
            }?.item?.toInt()?.let {
                if (_centerIndex.value != it) _centerIndex.value = it
            }
        }
    }
    override val native = UICollectionView(
        CGRectMake(
            0.0,
            0.0,
            0.0,
            0.0
        ),
        UICollectionViewCompositionalLayout(
            NSCollectionLayoutSection.sectionWithGroup(
                NSCollectionLayoutGroup.verticalGroupWithLayoutSize(
                    layoutSize = NSCollectionLayoutSize.sizeWithWidthDimension(
                        width = NSCollectionLayoutDimension.fractionalWidthDimension(1.0),
                        heightDimension = NSCollectionLayoutDimension.fractionalHeightDimension(1.0),
                    ),
                    subitems = listOf(
                        NSCollectionLayoutItem.itemWithLayoutSize(
                            layoutSize = NSCollectionLayoutSize.sizeWithWidthDimension(
                                width = NSCollectionLayoutDimension.fractionalWidthDimension(1.0),
                                heightDimension = NSCollectionLayoutDimension.fractionalHeightDimension(1.0),
                            ),
                        )
                    )
                )
            )
        ).apply {
            configuration = configuration.apply {
                scrollDirection =
                    UICollectionViewScrollDirection.UICollectionViewScrollDirectionHorizontal
            }
        }
    ).apply {
        pagingEnabled = true
        backgroundColor = UIColor.clearColor
        this.delegate = this@ViewPager.delegate
    }
    val newViews = NewViewWriter(this, context)


    override fun internalAddChild(index: Int, view: RView) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalClearChildren() {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    override fun internalRemoveChild(index: Int) {
        // Do nothing.  All children are virtual and managed by the native recycler view.
    }

    @OptIn(ExperimentalForeignApi::class)
    actual val index: Writable<Int>
        get() = _centerIndex
            .withWrite { value ->
                println("$value in 0..<${native.numberOfItemsInSection(0L)}")
                if (value in 0..<native.numberOfItemsInSection(0L)) {
                    native.scrollToItemAtIndexPath(
                        NSIndexPath.indexPathForItem(item = value.toLong(), inSection = 0L).also {
                            println("I'm gonna fuckin scroll to ${it.item} in ${it.section}")
                        },
                        UICollectionViewScrollPositionCenteredVertically or UICollectionViewScrollPositionCenteredHorizontally,
                        animationsEnabled
                    )
                }
            }

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit,
    ): Unit = children(items, { it }, listOf(object : RecyclerView.ChildType<T> {
        override fun matches(value: T): Boolean = true
        override fun render(viewWriter: ViewWriter, value: Readable<T>) = render(viewWriter, value)
    }))

    private var dataSource: UICollectionViewDiffableDataSource? = null

    fun <T, ID> children(items: Readable<List<T>>, identity: (T) -> ID, types: List<RecyclerView.ChildType<T>>): Unit {
        dataSource = native.configure(this, items, identity, types)
    }

    init {
        onRemove {
            dataSource = null
            delegate = null
            native.delegate = null
            native.dataSource = null
        }
    }
}


internal val ObsUICollectionViewCell_classRef = object_getClass(ObsUICollectionViewCell<Int>())!!

private var nextId: Int = 1

@OptIn(ExperimentalObjCName::class, BetaInteropApi::class, ExperimentalForeignApi::class)
@ExportObjCClass
class ObsUICollectionViewCell<T> : UICollectionViewCell, UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    constructor() : this(CGRectMake(0.0, 0.0, 0.0, 0.0))

    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame = frame)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder = coder)

//    var lockWidth = false
//    var lockHeight = false

    val id = nextId++
    var indexPath: NSIndexPath? = null
    val data = LateInitProperty<T>()
    var ready = false

    var myNeedsMeasure = true
    var lock = false
    @OptIn(ExperimentalNativeApi::class)
    var uiCollectionView: WeakReference<UICollectionView>? = null

    @OptIn(ExperimentalNativeApi::class)
    override fun subviewDidChangeSizing(view: UIView?) {
        val it = view ?: return
        val index = subviews.indexOf(view)
        if (index != -1) childSizeCache[index].clear()
        if (!lock) {
            uiCollectionView?.get()?.let {
//                indexPath?.let { path ->
//                    println("Invalidate item at path ${path.section} / ${path.item}")
//                    it.collectionViewLayout.invalidateLayoutWithContext(UICollectionViewLayoutInvalidationContext().apply {
//                        invalidateItemsAtIndexPaths(listOf(path))
//                    })
//                }
                it.collectionViewLayout.invalidateLayout()
            }
        }
    }

    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) {
            extensionPadding = value
        }
    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    private val childSizeCache: ArrayList<HashMap<Size, Size>> = ArrayList()
    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> = frameLayoutSizeThatFits(size, childSizeCache)
    override fun layoutSubviews() = frameLayoutLayoutSubviews(childSizeCache)
    override fun didAddSubview(subview: UIView) {
        super.didAddSubview(subview)
        frameLayoutDidAddSubview(subview, childSizeCache)
    }

    override fun willRemoveSubview(subview: UIView) {
        // Fixes a really cursed crash where "this" is null due to GC interactions
        @Suppress("SENSELESS_COMPARISON")
        if (this != null) frameLayoutWillRemoveSubview(subview, childSizeCache)
        super.willRemoveSubview(subview)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return frameLayoutHitTest(point, withEvent).takeUnless { it == this }
    }

    private var lastSize: CGFloat = 0.0
    override fun preferredLayoutAttributesFittingAttributes(layoutAttributes: UICollectionViewLayoutAttributes): UICollectionViewLayoutAttributes {
        println("Cell $id: preferredLayoutAttributesFittingAttributes ${layoutAttributes.size.useContents { "$width x $height" }}")
        val measured = frameLayoutSizeThatFits(layoutAttributes.size, childSizeCache)
        measured.useContents {
            println("Cell $id: Measured ${layoutAttributes.size.useContents { "$width x $height" }} -> ${"$width x $height"}")
        }
        layoutAttributes.setBounds(CGRectMake(
            0.0,
            0.0,
            measured.useContents { width },
            measured.useContents { height },
        ))
        return layoutAttributes
    }

//    init {
//        addSubview(UILabel(CGRectMake(0.0, 0.0, 100.0, 50.0)).apply {
//            text = "Cell $id"
//        })
//    }
}

inline fun <S, T : Comparable<T>> Iterable<S>.rangeOfOrNull(calculate: (S) -> T): ClosedRange<T>? {
    var min: T? = null
    var max: T? = null
    for (item in this) {
        val value = calculate(item)
        if (min == null || value < min) min = value
        if (max == null || value > max) max = value
    }
    if (min == null || max == null) return null
    else return min..max
}

@OptIn(ExperimentalNativeApi::class)
fun <T, ID> UICollectionView.configure(base: RView, items: Readable<List<T>>, identity: (T) -> ID, types: List<ChildType<T>>): UICollectionViewDiffableDataSource {
    val registrations = types.map {
        UICollectionViewCellRegistration.registrationWithCellClass(ObsUICollectionViewCell_classRef) { cell, path, value ->
            cell as ObsUICollectionViewCell<T>
            cell.uiCollectionView = WeakReference(this)
            cell.lock = true
            cell.withoutAnimation {
                cell.indexPath = path
                if (value == ReactiveLoading)
                    cell.data.unset()
                else
                    cell.data.value = value as T

                if (!cell.ready) {
                    cell.ready = true
                    it.render(object : ViewWriter() {
                        override val context: RContext get() = base.context
                        override fun willAddChild(view: RView) = base.willAddChild(view)
                        override val coroutineContext: CoroutineContext get() = base.coroutineContext
                        override fun addChild(view: RView) {
                            base.addChild(view)
                            cell.addSubview(view.native)
                        }
                    }, cell.data)
                }
            }
            cell.lock = false
        }
    }
    var data: List<T>? = null
    val new = UICollectionViewDiffableDataSource(base.native as UICollectionView) { collectionView, indexPath, identifier ->
        val v = data?.get(indexPath!!.item.toInt())
        val ri = if (v == null) 0 else types.indexOfFirst { it.matches(v) }.coerceAtLeast(0)
        val r = registrations[ri]
        collectionView!!.dequeueConfiguredReusableCellWithRegistration(r, indexPath!!, v ?: ReactiveLoading)
    }
    dataSource = new
    var first = false
    items.state.onSuccess { it ->
        data = it
        val s = NSDiffableDataSourceSnapshot().apply {
            appendSectionsWithIdentifiers(listOf(0))
            appendItemsWithIdentifiers(data.map(identity), intoSectionWithIdentifier = 0)
        }
        if (first) {
            new.applySnapshotUsingReloadData(s)
            first = false
        } else {
            new.applySnapshot(s, animatingDifferences = true)
        }
    }
//    base.reactiveScope(onLoad =  {
////            data = null
////            if(first) new.applySnapshotUsingReloadData(NSDiffableDataSourceSnapshot().apply {
////                appendSectionsWithIdentifiers()
////                appendItemsWithIdentifiers()
////            })
//    }) {
//        data = items()
//        val s = NSDiffableDataSourceSnapshot().apply {
//            appendSectionsWithIdentifiers(listOf(0))
//            appendItemsWithIdentifiers(data.map(identity), intoSectionWithIdentifier = 0)
//        }
//        if(first) {
//            new.applySnapshotUsingReloadData(s)
//            first = false
//        } else {
//            new.applySnapshot(s, animatingDifferences = true)
//        }
//    }
    return new
}
