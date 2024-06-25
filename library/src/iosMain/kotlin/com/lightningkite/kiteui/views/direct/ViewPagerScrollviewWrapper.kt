package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.objc.UIViewWithSpacingRulesProtocol
import com.lightningkite.kiteui.views.ViewWriter
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
class ViewPagerScrollviewWrapper(newViews: ViewWriter): UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)),
    UIViewWithSpacingRulesProtocol {

    val pagingRecyclerView = NRecyclerView(false, newViews)

    init {
        addSubview(pagingRecyclerView)
        pagingRecyclerView.apply {
            elementsMatchSize = true
            pagingEnabled = true
        }
    }

    var spacing: Dimension = 0.px
        set(value) {
            field = value
            pagingRecyclerView.spacing = value
            applyOversizedScrollViewDimens()
        }

    override fun getSpacingOverrideProperty() = pagingRecyclerView.spacingOverride

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        return size
    }

    override fun layoutSubviews() {
        applyOversizedScrollViewDimens()
    }

    private fun applyOversizedScrollViewDimens() {
        val (pageWidth, pageHeight) = bounds.useContents { size.width to size.height }
        val scrollViewWidth = pageWidth + 1 * spacing.value
        println("pageWidth=$pageWidth, scrollViewWidth=$scrollViewWidth")
        pagingRecyclerView.setFrame(CGRectMake(-spacing.value, 0.0, scrollViewWidth, pageHeight))
    }
}