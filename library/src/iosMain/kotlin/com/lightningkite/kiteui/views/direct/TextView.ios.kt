package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import platform.UIKit.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextView = UILabel

@ViewDsl
actual inline fun ViewWriter.h1Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(2.0.rem.value)
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
            it.title.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h2Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(1.6.rem.value)
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
            it.title.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h3Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(1.4.rem.value)
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
            it.title.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h4Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(1.3.rem.value)
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
            it.title.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.h5Actual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(1.2.rem.value)
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
    font = UIFont.systemFontOfSize(1.1.rem.value)
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
            it.title.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.textActual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(1.0.rem.value)
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
            it.body.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
        },
    ) { setup(TextView(this)) }
}

@ViewDsl
actual inline fun ViewWriter.subtextActual(crossinline setup: TextView.() -> Unit): Unit = element(UILabel()) {
    font = UIFont.systemFontOfSize(0.8.rem.value)
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
            it.body.let { this.font = it.font.get(font.pointSize, it.weight.toUIFontWeight(), it.italic) }
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
actual inline var TextView.textSize: Dimension
    get() = Dimension(native.font.pointSize)
    set(value) {
        native.extensionFontAndStyle?.let {
            native.font = it.font.get(value.value, it.weight.toUIFontWeight(), it.italic)
        }
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