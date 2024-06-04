package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextView = UILabelWithGradient

@ViewDsl
actual inline fun ViewWriter.h1Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 2.0.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 2.0.rem * 0.6,
            minHeight = 2.0.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h2Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 1.6.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 1.6.rem * 0.6,
            minHeight = 1.6.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h3Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 1.4.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 1.4.rem * 0.6,
            minHeight = 1.4.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h4Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 1.3.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 1.3.rem * 0.6,
            minHeight = 1.3.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h5Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 1.2.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 1.2.rem * 0.6,
            minHeight = 1.2.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h6Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 1.1.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 1.1.rem * 0.6,
            minHeight = 1.1.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.title
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.textActual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 1.0.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 1.0.rem * 0.6,
            minHeight = 1.0.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.body
            updateFont()
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.subtextActual(crossinline setup: TextView.() -> Unit): Unit = element(UILabelWithGradient()) {
    label.apply {
        extensionTextSize = 0.8.rem
        updateFont()
        setContentSizeCategoryChangeListener()
        extensionSizeConstraints = SizeConstraints(
            minWidth = 0.8.rem * 0.6,
            minHeight = 0.8.rem * 1.5,
        )
        numberOfLines = 0
    }
    handleTheme(
        this, viewLoads = true,
        foreground = {
            foreground = it.foreground
            label.extensionFontAndStyle = it.body
            updateFont()
        },
    ) { opacity = 0.8; setup(TextView(this)) }
}

actual inline var TextView.content: String
    get() = native.label.text ?: ""
    set(value) {
        native.label.text = value
        native.informParentOfSizeChange()
    }
actual inline var TextView.align: Align
    get() = when (native.label.textAlignment) {
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
        native.label.textAlignment = when (value) {
            Align.Start -> NSTextAlignmentLeft
            Align.Center -> NSTextAlignmentCenter
            Align.End -> NSTextAlignmentRight
            Align.Stretch -> NSTextAlignmentJustified
        }
    }

private val UILabelTextSize = ExtensionProperty<UILabel, Dimension>()
var UILabel.extensionTextSize: Dimension? by UILabelTextSize

actual inline var TextView.textSize: Dimension
    get() = native.label.extensionTextSize ?: Dimension(native.label.font.pointSize)
    set(value) {
        native.label.extensionTextSize = value
        native.updateFont()
        native.informParentOfSizeChange()
    }
actual var TextView.ellipsis: Boolean
    get() = TODO("Not yet implemented")
    set(value) {
        native.label.lineBreakMode = if(value) NSLineBreakByTruncatingTail else NSLineBreakByClipping
    }
actual var TextView.wraps: Boolean
    get() = TODO("Not yet implemented")
    set(value) {
        native.label.numberOfLines = if(value) 0 else 1
    }

// Calculated from font sizes shown at https://developer.apple.com/design/human-interface-guidelines/typography#Specifications
private val dynamicTypeScaleFactors = mapOf(
    UIContentSizeCategoryUnspecified to 1.0,
    UIContentSizeCategoryExtraSmall to 0.87,
    UIContentSizeCategorySmall to 0.91,
    UIContentSizeCategoryMedium to 0.95,
    UIContentSizeCategoryLarge to 1.0,
    UIContentSizeCategoryExtraLarge to 1.21,
    UIContentSizeCategoryExtraExtraLarge to 1.31,
    UIContentSizeCategoryExtraExtraExtraLarge to 1.42,
)
const val ENABLE_DYNAMIC_TYPE = false
fun preferredScaleFactor() = if (ENABLE_DYNAMIC_TYPE) {
    dynamicTypeScaleFactors[UIApplication.sharedApplication.preferredContentSizeCategory] ?: 1.0
} else {
    1.0
}
fun UILabelWithGradient.setContentSizeCategoryChangeListener() {
    if (ENABLE_DYNAMIC_TYPE) {
        NSNotificationCenter.defaultCenter.addObserverForName(UIContentSizeCategoryDidChangeNotification, null, NSOperationQueue.mainQueue) {
            updateFont()
            informParentOfSizeChange()
        }
    }
}
fun UILabelWithGradient.updateFont() = label.run {
    val textSize = extensionTextSize ?: return
    val alignment = textAlignment
    font = extensionFontAndStyle?.let {
        it.font.get(textSize.value * preferredScaleFactor(), it.weight.toUIFontWeight(), it.italic)
    } ?: UIFont.systemFontOfSize(textSize.value)
    textAlignment = alignment
}