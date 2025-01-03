package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.RecyclerView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.*
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSIndexPath
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

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