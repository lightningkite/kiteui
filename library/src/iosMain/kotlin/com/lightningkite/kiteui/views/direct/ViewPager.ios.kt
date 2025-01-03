package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.objc.*
import com.lightningkite.kiteui.views.*
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

actual class ViewPager actual constructor(context: RContext): RView(context) {
    override val native = UICollectionView(
        CGRectMake(
            0.0,
            0.0,
            0.0,
            0.0
        ), ViewPagerLayout(this)
    ).apply {
            backgroundColor = UIColor.clearColor
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
        get() = ((native.delegate as? GeneralCollectionDelegate<*>)?.centerIndex ?: Constant(0))
            .withWrite { value ->
                if(value in 0..<native.numberOfItemsInSection(0L)) {
                    native.scrollToItemAtIndexPath(
                        NSIndexPath.indexPathForRow(value.toLong(), 0L),
                        UICollectionViewScrollPositionCenteredVertically or UICollectionViewScrollPositionCenteredHorizontally,
                        animationsEnabled
                    )
                }
            }

    actual fun <T> children(
        items: Readable<List<T>>,
        render: ViewWriter.(value: Readable<T>) -> Unit
    ): Unit {
        val placeholders = 5
        val source = GeneralCollectionDelegate(this, render, placeholders)
        reactiveScope(onLoad = {
            source.loading = true
            native.reloadData()
        }) {
            source.list = items.await()
            source.loading = false
            native.reloadData()
        }
        native.setDataSource(source)
        native.setDelegate(source)
    }

}


//package com.lightningkite.kiteui.views.direct
//
//import kotlinx.cinterop.BetaInteropApi
//import kotlinx.cinterop.CValue
//import kotlinx.cinterop.ExperimentalForeignApi
//import kotlinx.cinterop.useContents
//import platform.CoreGraphics.CGPoint
//import platform.CoreGraphics.CGPointMake
//import platform.CoreGraphics.CGRectMake
//import platform.CoreGraphics.CGSizeMake
//import platform.Foundation.NSIndexPath
//import platform.UIKit.*
//import kotlin.math.abs
//import kotlin.math.ceil
//import kotlin.math.floor
//import kotlin.math.round
//
//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual typealias NViewPager = UICollectionView
//
//actual fun <T> ViewPager.children(
//    items: Readable<List<T>>,
//    render: ViewWriter.(value: Readable<T>) -> Unit
//) = native.children(items, render)
//
//@OptIn(ExperimentalForeignApi::class)
//@ViewDsl
//actual inline fun ViewWriter.viewPagerActual(crossinline setup: ViewPager.() -> Unit) = element(
//    UICollectionView(
//        CGRectMake(
//            0.0,
//            0.0,
//            0.0,
//            0.0
//        ), ViewPagerLayout())
//) {
//    calculationContext.onRemove {
//        extensionStrongRef = null
//    }
//    backgroundColor = UIColor.clearColor
//    handleTheme(this, viewDraws = false)
//    extensionViewWriter = newViews()
//    setup(ViewPager(this))
//}
//
//@OptIn(ExperimentalForeignApi::class)
//actual val ViewPager.index: Writable<Int>
//    get() = ((native.delegate as? GeneralCollectionDelegate<*>)?.centerIndex ?: Constant(0))
//        .withWrite { value ->
//            if(value in 0..<native.numberOfItemsInSection(0L)) {
//                native.scrollToItemAtIndexPath(
//                    NSIndexPath.indexPathForRow(value.toLong(), 0L),
//                    UICollectionViewScrollPositionCenteredVertically or UICollectionViewScrollPositionCenteredHorizontally,
//                    animationsEnabled
//                )
//            }
//        }
//
@OptIn(ExperimentalForeignApi::class)
class ViewPagerLayout(val scope: CoroutineScope): UICollectionViewFlowLayout(), UICollectionViewFlowLayout2Protocol {

    override fun prepareLayout() {
        scrollDirection = UICollectionViewScrollDirection.UICollectionViewScrollDirectionHorizontal
        sectionInset = UIEdgeInsetsMake(0.0, 0.0, 0.0, 0.0)
        sectionInsetReference = UICollectionViewFlowLayoutSectionInsetReference.UICollectionViewFlowLayoutSectionInsetFromSafeArea
        val collectionView = collectionView!!
        scope.onRemove(collectionView.layer.observe("bounds") {
          itemSize = collectionView.bounds.useContents { CGSizeMake(size.width, size.height) }
        })
    }

    override fun targetContentOffsetForProposedContentOffset(proposedContentOffset: CValue<CGPoint>, withScrollingVelocity: CValue<CGPoint>): CValue<CGPoint> {
        val collectionView = collectionView!!

        // Page width used for estimating and calculating paging.
        val pageWidth = itemSize.useContents { width } + minimumInteritemSpacing

        // Make an estimation of the current page position.
        val approximatePage = collectionView.contentOffset.useContents { x } / pageWidth
        // Determine the current page based on velocity.
        val currentPage = if(withScrollingVelocity.useContents { x } == 0.0) round(approximatePage) else if (withScrollingVelocity.useContents { x } < 0.0) floor(approximatePage) else ceil(approximatePage)

        // Create custom flickVelocity.
        val flickVelocity = withScrollingVelocity.useContents { x } * 0.3

        // Check how many pages the user flicked, if <= 1 then flickedPages should return 0.
        val flickedPages = if(abs(round(flickVelocity)) <= 1) 0.0 else round(flickVelocity)

        // Calculate newHorizontalOffset.
        val newHorizontalOffset = ((currentPage + flickedPages) * pageWidth) - collectionView.contentInset.useContents { left }

        return CGPointMake(newHorizontalOffset, proposedContentOffset.useContents { y })
    }
}