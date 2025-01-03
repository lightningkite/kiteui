@file:OptIn(ExperimentalNativeApi::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.reactive.LateInitProperty
import com.lightningkite.kiteui.objc.UIViewWithSizeOverridesProtocol
import com.lightningkite.kiteui.objc.UICollectionViewFlowLayout3Protocol
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.printStackTrace2
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.onRemove
import com.lightningkite.kiteui.reactive.reactiveScope
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIfAllowed
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

    companion object {
        fun makeLayout(vertical: Boolean, columns: Int): UICollectionViewFlowLayout {
            val layout = UICollectionViewFlowLayout()
            layout.setEstimatedItemSize(
                CGSizeMake(
                    UICollectionViewFlowLayoutAutomaticSize.width,
                    UICollectionViewFlowLayoutAutomaticSize.height
                )
            )
            layout.scrollDirection = if (vertical) UICollectionViewScrollDirection.UICollectionViewScrollDirectionVertical
            else UICollectionViewScrollDirection.UICollectionViewScrollDirectionHorizontal
            return layout
        }
    }

    override val native = UICollectionView2(
        CGRectMake(
            0.0,
            0.0,
            0.0,
            0.0
        ),
        makeLayout(true, 1)
    ).apply {
        backgroundColor = UIColor.clearColor
        var lastWidth = -1.0
        var lastHeight = -1.0
        onRelayout = WeakReference(label@{
            if (bounds.useContents { size.width != lastWidth || size.height != lastHeight }) {
                refreshEstimatedItemSize()
                bounds.useContents { lastWidth = size.width; lastHeight = size.height }
            }
        })
    }

    private fun refreshEstimatedItemSize() {
//        val rv = this
//        val rvn = native
//        val rvl = native.collectionViewLayout as? UICollectionViewFlowLayout ?: return
//        val size = native.bounds.useContents { CGSizeMake(size.width, size.height) }
//        rvl.setEstimatedItemSize(
//            CGSizeMake(
//                if (rv.vertical) {
//                    val s = size.useContents { width } -
//                            rvn.safeAreaInsets.useContents { left + right } -
//                            rvl.sectionInset.useContents { left + right } -
//                            (rv.columns - 1) * rvl.minimumInteritemSpacing
//                    s / rv.columns
//                } else 100.0,
//                if (!rv.vertical) {
//                    val s = size.useContents { height } -
//                            rvn.safeAreaInsets.useContents { top + bottom } -
//                            rvl.sectionInset.useContents { top + bottom } -
//                            (rv.columns - 1) * rvl.minimumInteritemSpacing
//                    s / rv.columns
//                } else 100.0,
//            ).also {
//                println("Estimated item size: ${it.useContents { "$width x $height" }}")
//            }
//        )
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

    private var lastSource: GeneralCollectionDelegate<*>? = null
    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit,
    ): Unit {
        val placeholders = 5
        val source = GeneralCollectionDelegate(this, render, placeholders)
        native.setDataSource(source)
        native.setDelegate(source)
        reactiveScope(onLoad = {
            source.loading = true
            native.reloadData()
        }) {
            source.list = items()
            source.loading = false
            native.reloadData()
        }
        lastSource = source
    }

    actual var vertical: Boolean = true
        set(value) {
            field = value
            native.collectionViewLayout = makeLayout(vertical, columns)
            refreshEstimatedItemSize()
        }


    actual var columns: Int = 1
        set(value) {
            field = value
            native.collectionViewLayout = makeLayout(vertical, columns)
            refreshEstimatedItemSize()
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

    actual val firstVisibleIndex: Readable<Int>
        get() = (native.delegate as? GeneralCollectionDelegate<*>)?.firstVisibleIndex ?: Constant(0)

    actual val lastVisibleIndex: Readable<Int>
        get() = (native.delegate as? GeneralCollectionDelegate<*>)?.lastVisibleIndex ?: Constant(0)

    val centerIndex: Readable<Int>
        get() = (native.delegate as? GeneralCollectionDelegate<*>)?.centerIndex ?: Constant(0)
}

private var nextId: Int = 1

@OptIn(ExperimentalObjCName::class, BetaInteropApi::class, ExperimentalForeignApi::class)
@ExportObjCClass
class ObsUICollectionViewCell<T> : UICollectionViewCell, UIViewWithSizeOverridesProtocol, UIViewWithSpacingRulesProtocol {
    constructor() : this(CGRectMake(0.0, 0.0, 0.0, 0.0))

    @OverrideInit
    constructor(frame: CValue<CGRect>) : super(frame = frame)

    @OverrideInit
    constructor(coder: NSCoder) : super(coder = coder)

    @OptIn(ExperimentalNativeApi::class)
    var recyclerView: WeakReference<RecyclerView>? = null

    var debugDescriptionInfo: String = ""
    var debugDescriptionInfo2: String = ""
    override fun debugDescription(): String? = "${super.debugDescription()} $debugDescriptionInfo $debugDescriptionInfo2"

//    var lockWidth = false
//    var lockHeight = false

    val id = nextId++
    val data = LateInitProperty<T>()
    var ready = false
    var myNeedsMeasure = true
    var lock = false

    override fun subviewDidChangeSizing(view: UIView?) {
        val it = view ?: return
        val index = subviews.indexOf(view)
        if (index != -1) childSizeCache[index].clear()
        setNeedsLayout()
        if(lock) return
        println("Cell $id: Unlocked subviewDidChangeSizing")
        // This is occurring because the style changes, which does invalidate the size at the moment.
        myNeedsMeasure = true
        // This is already scheduled; no need to schedule it under the hood.
        recyclerView?.get()?.native?.collectionViewLayout?.invalidateLayout()

//        frameLayoutSubviewDidChangeSizing(view, childSizeCache)
    }

    var padding: Double
        get() = extensionPadding ?: 0.0
        set(value) {
            extensionPadding = value
        }
    val spacingOverride: Property<Dimension?> = Property<Dimension?>(null)
    override fun getSpacingOverrideProperty() = spacingOverride

    private var lastSize: CGFloat = 0.0
    override fun preferredLayoutAttributesFittingAttributes(layoutAttributes: UICollectionViewLayoutAttributes): UICollectionViewLayoutAttributes {
        val rv = recyclerView?.get() ?: run {
            println("val rv = recyclerView?.get() is null")
            return layoutAttributes
        }
        val rvn = rv.native as UICollectionView
        val rvl = rvn.collectionViewLayout as? UICollectionViewFlowLayout ?: run {
            println("val rvl = rvn.collectionViewLayout as? UICollectionViewFlowLayout is null")
            return layoutAttributes
        }
        val modifiedSize = CGSizeMake(
            if (rv.vertical) {
                val s = rvn.bounds.useContents { size.width } -
                        rvn.safeAreaInsets.useContents { left + right } -
                        rvl.sectionInset.useContents { left + right } -
                        (rv.columns - 1) * rvl.minimumInteritemSpacing
                floor(s / rv.columns)
            } else 10000.0,
            if (!rv.vertical) {
                val s = rvn.bounds.useContents { size.height } -
                        rvn.safeAreaInsets.useContents { top + bottom } -
                        rvl.sectionInset.useContents { top + bottom } -
                        (rv.columns - 1) * rvl.minimumInteritemSpacing
                floor(s / rv.columns)
            } else 10000.0,
        )
        println("Cell $id: preferredLayoutAttributesFittingAttributes ${modifiedSize.useContents { "$width x $height" }}")
        if (myNeedsMeasure) {
            myNeedsMeasure = false
            frameLayoutSizeThatFits(modifiedSize, childSizeCache).useContents {
                println("Cell $id: Measured ${modifiedSize.useContents { "$width x $height" }} -> ${"$width x $height"}")
                val measured = this
                lastSize = if(rv.vertical) measured.height else measured.width
            }
        }
        println("Cell $id: preferredLayoutAttributesFittingAttributes reporting $lastSize")
        layoutAttributes.setBounds(CGRectMake(
            0.0,
            0.0,
            if(rv.vertical) modifiedSize.useContents { width }
            else lastSize,
            if(rv.vertical) lastSize
            else modifiedSize.useContents { width },
        ))
        return layoutAttributes
    }

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

    init {
        addSubview(UILabel(CGRectMake(0.0, 0.0, 100.0, 50.0)).apply {
            text = "Cell $id"
        })
    }
}

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES", "RETURN_TYPE_MISMATCH_ON_INHERITANCE", "MANY_INTERFACES_MEMBER_NOT_IMPLEMENTED")
class GeneralCollectionDelegate<T>(
    private val parentView: RView,
    private val render: ViewWriter.(value: Readable<T>) -> Unit,
    private val placeholders: Int,
) : NSObject(), UICollectionViewDelegateProtocol, UICollectionViewDataSourceProtocol {
    var list: List<T> = listOf()
    var loading: Boolean = false
    val registered = HashSet<String>()

    @OptIn(BetaInteropApi::class)
    @Suppress("CONFLICTING_OVERLOADS", "RETURN_TYPE_MISMATCH_ON_OVERRIDE", "PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun collectionView(collectionView: UICollectionView, cellForItemAtIndexPath: NSIndexPath): UICollectionViewCell {
        if (registered.add("main")) {
            collectionView.registerClass(object_getClass(ObsUICollectionViewCell<T>())!!, "main")
        }
        @Suppress("UNCHECKED_CAST")
        val cell = collectionView.dequeueReusableCellWithReuseIdentifier("main", cellForItemAtIndexPath) as ObsUICollectionViewCell<T>
        cell.lock = true
        collectionView.withoutAnimation {
            if (loading) {
                cell.data.unset()
                cell.myNeedsMeasure = true
            } else {
                list.getOrNull(cellForItemAtIndexPath.row.toInt())?.let {
                    cell.data.value = it
                    cell.myNeedsMeasure = true
                }
            }
            if (!cell.ready) {
                cell.recyclerView = (parentView as? RecyclerView)?.let(::WeakReference)
                object : ViewWriter() {
                    override val coroutineContext: CoroutineContext get() = parentView.coroutineContext
                    override val context: RContext get() = parentView.context
                    override fun willAddChild(view: RView) {
                        parentView.willAddChild(view)
                    }

                    override fun addChild(view: RView) {
                        parentView.addChild(view)
                        cell.addSubview(view.native)
                    }
                }.render(cell.data)
                cell.ready = true
            }
        }
        cell.lock = false
        return cell
    }

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
                it.start.let { if (firstVisibleIndex.value != it) firstVisibleIndex.value = it }
                it.endInclusive.let { if (lastVisibleIndex.value != it) lastVisibleIndex.value = it }
            }
        }
        native.indexPathForItemAtPoint(native.frame.useContents { CGPointMake(size.width / 2, size.height / 2) })?.row?.toInt()?.let {
            if (centerIndex.value != it) centerIndex.value = it
        }
    }

    override fun collectionView(collectionView: UICollectionView, numberOfItemsInSection: NSInteger): NSInteger = if (loading) placeholders.toLong() else list.size.toLong()

    val firstVisibleIndex = Property(0)
    val centerIndex = Property(0)
    val lastVisibleIndex = Property(0)
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

class UICollectionView2(frame: CValue<CGRect>, collectionViewLayout: UICollectionViewLayout) : UICollectionView(frame, collectionViewLayout) {
    var onRelayout: WeakReference<() -> Unit>? = null
    override fun layoutSubviews() {
        onRelayout?.get()?.invoke()
        super.layoutSubviews()
    }
}