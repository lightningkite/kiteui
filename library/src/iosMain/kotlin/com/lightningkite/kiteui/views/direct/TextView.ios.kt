package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextView = UILabel

@ViewDsl
actual inline fun ViewWriter.h1Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 2.0.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 2.0.rem * 0.6,
        minHeight = 2.0.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h2Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 1.6.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 1.6.rem * 0.6,
        minHeight = 1.6.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h3Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 1.4.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 1.4.rem * 0.6,
        minHeight = 1.4.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h4Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 1.3.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 1.3.rem * 0.6,
        minHeight = 1.3.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h5Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 1.2.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 1.2.rem * 0.6,
        minHeight = 1.2.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.title
            it.title.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h6Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 1.1.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 1.1.rem * 0.6,
        minHeight = 1.1.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.textActual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 1.0.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 1.0.rem * 0.6,
        minHeight = 1.0.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.body
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.subtextActual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    extensionTextSize = 0.8.rem
    updateFont()
    setContentSizeCategoryChangeListener()
    extensionSizeConstraints = SizeConstraints(
        minWidth = 0.8.rem * 0.6,
        minHeight = 0.8.rem * 1.5,
    )
    numberOfLines = 0
    handleTheme(
        this, viewLoads = true,
        foreground = {
            this.textColor = it.foreground.closestColor().toUiColor()
            this.extensionFontAndStyle = it.body
            updateFont()
        },
    ) { opacity = 0.8; setup(TextView(this)) }
}

actual inline var TextView.content: String
    get() = native.text ?: ""
    set(value) {
        native.text = value
        native.informParentOfSizeChange()
    }
actual inline var TextView.align: Align
    get() = when (native.textAlignment) {
        NSTextAlignmentLeft -> Align.Start
        NSTextAlignmentCenter -> Align.Center
        NSTextAlignmentRight -> Align.End
        NSTextAlignmentJustified -> Align.Stretch
        else -> Align.Start
    }
    set(value) {
        native.contentMode = when (value) {
            Align.Start -> UIViewContentMode.UIViewContentModeLeft
            Align.Center -> UIViewContentMode.UIViewContentModeCenter
            Align.End -> UIViewContentMode.UIViewContentModeRight
            Align.Stretch -> UIViewContentMode.UIViewContentModeScaleAspectFit
        }
        native.textAlignment = when (value) {
            Align.Start -> NSTextAlignmentLeft
            Align.Center -> NSTextAlignmentCenter
            Align.End -> NSTextAlignmentRight
            Align.Stretch -> NSTextAlignmentJustified
        }
    }

private val UILabelTextSize = ExtensionProperty<UILabel, Dimension>()
var UILabel.extensionTextSize: Dimension? by UILabelTextSize

actual inline var TextView.textSize: Dimension
    get() = native.extensionTextSize ?: Dimension(native.font.pointSize)
    set(value) {
        native.extensionTextSize = value
        native.updateFont()
        native.informParentOfSizeChange()
    }
actual var TextView.ellipsis: Boolean
    get() = TODO("Not yet implemented")
    set(value) {
        native.lineBreakMode = if(value) NSLineBreakByTruncatingTail else NSLineBreakByClipping
    }
actual var TextView.wraps: Boolean
    get() = TODO("Not yet implemented")
    set(value) {
        native.numberOfLines = if(value) 0 else 1
    }

// Calculated from font sizes shown at https://developer.apple.com/design/human-interface-guidelines/typography#Specifications
private val dynamicTypeScaleFactors = mapOf(
    UIContentSizeCategoryUnspecified to 1.0,
    UIContentSizeCategoryExtraSmall to 0.87,
    UIContentSizeCategorySmall to 0.91,
    UIContentSizeCategoryMedium to 0.95,
    UIContentSizeCategoryLarge to 1.0,
    UIContentSizeCategoryExtraLarge to 1.11,
    UIContentSizeCategoryExtraExtraLarge to 1.21,
    UIContentSizeCategoryExtraExtraExtraLarge to 1.32,
)
fun preferredScaleFactor() = dynamicTypeScaleFactors[UIApplication.sharedApplication.preferredContentSizeCategory] ?: 1.0
fun UILabel.setContentSizeCategoryChangeListener() {
    NSNotificationCenter.defaultCenter.addObserverForName(UIContentSizeCategoryDidChangeNotification, null, NSOperationQueue.mainQueue) {
        updateFont()
        informParentOfSizeChange()
    }
}
fun UILabel.updateFont() {
    val textSize = extensionTextSize ?: return
    val alignment = textAlignment
    font = extensionFontAndStyle?.let {
        it.font.get(textSize.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
    } ?: UIFont.systemFontOfSize(textSize.value)
    textAlignment = alignment
}