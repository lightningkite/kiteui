@file:OptIn(ExperimentalContracts::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.usesTouchscreen
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.RecyclerViewPagingPlacer
import com.lightningkite.kiteui.views.themed
import com.lightningkite.kiteui.views.write
import com.lightningkite.reactive.context.invoke
import com.lightningkite.reactive.extensions.modify
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract


public inline fun ElementWriter.activityIndicator(setup: ActivityIndicator.() -> Unit = {}): ActivityIndicator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ActivityIndicator(context), setup)
}

public inline fun ElementWriter.autoCompleteTextField(setup: AutoCompleteTextField.() -> Unit = {}): AutoCompleteTextField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(AutoCompleteTextField(context), setup)
}

public inline fun ElementWriter.button(setup: Button.() -> Unit = {}): Button {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Button(context), setup)
}

public inline fun ElementWriter.canvas(setup: Canvas.() -> Unit = {}): Canvas {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Canvas(context), setup)
}

public inline fun ElementWriter.checkbox(setup: Checkbox.() -> Unit = {}): Checkbox {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Checkbox(context), setup)
}

public inline fun ElementWriter.dismissBackground(setup: DismissBackground.() -> Unit = {}): DismissBackground {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(DismissBackground(context), setup)
}

public inline fun ElementWriter.externalLink(setup: ExternalLink.() -> Unit = {}): ExternalLink {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ExternalLink(context), setup)
}

public inline fun ElementWriter.icon(setup: IconView.() -> Unit = {}): IconView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(IconView(context), setup)
}

public inline fun ElementWriter.image(setup: ImageView.() -> Unit = {}): ImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ImageView(context), setup)
}

public inline fun ElementWriter.zoomableImage(setup: ZoomableImageView.() -> Unit = {}): ZoomableImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return ZoomableImageView(this).apply {
        setup()
        postSetup()
    }
}

public inline fun ElementWriter.rawImage(source: ImageSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: RawImageView.() -> Unit = {}): RawImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RawImageView(context, source, description, scaleType), setup)
}

public inline fun ElementWriter.rawImageUnsized(source: ImageSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: SizelessRawImageView.() -> Unit = {}): SizelessRawImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(SizelessRawImageView(context, source, description, scaleType), setup)
}

public inline fun ElementWriter.rawImageZoomable(source: ImageSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: RawImageViewZoomable.() -> Unit = {}): RawImageViewZoomable {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RawImageViewZoomable(context, source, description, scaleType), setup)
}

public inline fun ElementWriter.phoneNumberInput(setup: PhoneNumberInput.() -> Unit = {}): PhoneNumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(PhoneNumberInput(context), setup)
}

public inline fun ElementWriter.link(setup: Link.() -> Unit = {}): Link {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Link(context), setup)
}

public inline fun ElementWriter.localDateField(setup: LocalDateField.() -> Unit = {}): LocalDateField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalDateField(context), setup)
}

public inline fun ElementWriter.localDateTimeField(setup: LocalDateTimeField.() -> Unit = {}): LocalDateTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalDateTimeField(context), setup)
}

public inline fun ElementWriter.localTimeField(setup: LocalTimeField.() -> Unit = {}): LocalTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalTimeField(context), setup)
}

public inline fun ElementWriter.menuButton(setup: MenuButton.() -> Unit = {}): MenuButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(MenuButton(context), setup)
}

public inline fun ElementWriter.numberInput(setup: NumberInput.() -> Unit = {}): NumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberInput(context), setup)
}

public inline fun ElementWriter.progressBar(setup: ProgressBar.() -> Unit = {}): ProgressBar {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ProgressBar(context), setup)
}

public inline fun ElementWriter.circularProgress(setup: CircularProgress.() -> Unit = {}): CircularProgress {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CircularProgress(context), setup)
}

public inline fun ElementWriter.radioButton(setup: RadioButton.() -> Unit = {}): RadioButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RadioButton(context), setup)
}

public inline fun ElementWriter.radioToggleButton(setup: RadioToggleButton.() -> Unit = {}): RadioToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RadioToggleButton(context), setup)
}

public inline fun ElementWriter.rowCollapsingToColumn(breakpoint: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, listOf(breakpoint)), setup)
}

public inline fun ElementWriter.rowCollapsingToColumn(verticalBefore: Dimension, verticalAfter: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, listOf(verticalBefore, verticalAfter)), setup)
}

public inline fun ElementWriter.rowCollapsingToColumn(
    verticalBefore: Dimension,
    verticalAfter: Dimension,
    horizontalAgainAfter: Dimension,
    setup: RowCollapsingToColumn.() -> Unit = {}
): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, listOf(verticalBefore, verticalAfter, horizontalAgainAfter)), setup)
}

public inline fun ElementWriter.select(setup: Select.() -> Unit = {}): Select {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Select(context), setup)
}

public inline fun ElementWriter.separator(setup: Separator.() -> Unit = {}): Separator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Separator(context), setup)
}

public inline fun ElementWriter.space(setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Space(context), setup)
}

public inline fun ElementWriter.space(multiplier: Double, setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Space(context, multiplier), setup)
}

public inline fun ElementWriter.frame(setup: Frame.() -> Unit = {}): Frame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Frame(context), setup)
}

public inline fun ElementWriter.coordinatorFrame(setup: CoordinatorFrame.() -> Unit = {}): CoordinatorFrame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CoordinatorFrame(context), setup)
}

public inline fun ElementWriter.coordinatorDragHandle(setup: CoordinatorDragHandle.() -> Unit = {}): CoordinatorDragHandle {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CoordinatorDragHandle(context), setup)
}

public inline fun ElementWriter.swapView(setup: SwapView.() -> Unit = {}): SwapView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(SwapView(context), setup)
}

public inline fun ElementWriter.switch(setup: Switch.() -> Unit = {}): Switch {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Switch(context), setup)
}

public inline fun ElementWriter.slider(setup: Slider.() -> Unit = {}): Slider {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Slider(context), setup)
}

public inline fun ElementWriter.textArea(setup: TextArea.() -> Unit = {}): TextArea {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextArea(context), setup)
}

public inline fun ElementWriter.textInput(setup: TextInput.() -> Unit = {}): TextInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextInput(context), setup)
}

@Deprecated("Use textInput instead", ReplaceWith("textInput(setup)", "com.lightningkite.kiteui.views.direct.textInput"))
public inline fun ElementWriter.textField(setup: TextInput.() -> Unit = {}): TextInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextInput(context), setup)
}

public inline fun ElementWriter.formattedTextInput(setup: FormattedTextInput.() -> Unit = {}): FormattedTextInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(FormattedTextInput(context), setup)
}

public inline fun ElementWriter.text(setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextView(context), setup)
}

public inline fun ElementWriter.toggleButton(setup: ToggleButton.() -> Unit = {}): ToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ToggleButton(context), setup)
}

public inline fun ElementWriter.video(setup: VideoView.() -> Unit = {}): VideoView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(VideoView(context), setup)
}

public inline fun ElementWriter.media(setup: MediaView.() -> Unit = {}): MediaView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(MediaView(context), setup)
}

public inline fun ElementWriter.rawVideo(
    source: VideoSource,
    description: String,
    scaleType: ImageScaleType = ImageScaleType.Fit,
    preloadHint: PreloadHint = PreloadHint.METADATA,
    setup: RawVideoView.() -> Unit = {}
): RawVideoView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RawVideoView(context, source, description, scaleType, preloadHint), setup)
}

public inline fun ElementWriter.webView(setup: WebView.() -> Unit = {}): WebView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(WebView(context), setup)
}

public inline fun ElementWriter.rowWrapping(setup: RowWrapping.() -> Unit = {}): RowWrapping {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowWrapping(context), setup)
}

public inline fun ElementWriter.row(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context)) { vertical = false; setup() }
}

public inline fun ElementWriter.column(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context)) { vertical = true; setup() }
}

public inline fun ElementWriter.col(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context)) { vertical = true; setup() }
}

public inline fun ElementWriter.programmatic(setup: ProgrammaticLayout.() -> Unit = {}): ProgrammaticLayout {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ProgrammaticLayout(context), setup)
}

public inline fun ElementWriter.recyclerView(refreshAction: Action? = null, setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Recycler2(context, vertical = true, refreshAction = refreshAction), setup)
}

public inline fun ElementWriter.horizontalRecyclerView(
    refreshAction: Action? = null,
    setup: Recycler2.() -> Unit = {}
): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Recycler2(context, vertical = false, refreshAction = refreshAction), setup)
}

public inline fun ElementWriter.viewPager(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Recycler2(context, vertical = false)) {
        placer = RecyclerViewPagingPlacer()
        snapToElements = Align.Center
        scrollSnapStop = true

        with(outerFrame) {
            if (!Platform.usesTouchscreen) {
                align(Align.Start, Align.Center).frame {
                    button {
                        icon(Icon.chevronLeft, "Previous")
                        onClick("Previous") { this@write.centerIndex.modify { it - 1 } }
                    }
                }
                align(Align.End, Align.Center).frame {
                    button {
                        icon(Icon.chevronRight, "Next")
                        onClick("Next") { this@write.centerIndex.modify { it + 1 } }
                    }
                }
            }
        }

        setup()
    }
}