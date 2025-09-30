package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.Platform
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.FieldLabelSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ImageScaleType
import com.lightningkite.kiteui.models.ImageSource
import com.lightningkite.kiteui.models.VideoSource
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.usesTouchscreen
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.l2.Recycler2
import com.lightningkite.kiteui.views.l2.RecyclerViewPagingPlacer
import com.lightningkite.kiteui.views.l2.icon
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.activityIndicator(setup: ActivityIndicator.() -> Unit = {}): ActivityIndicator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ActivityIndicator(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.autoCompleteTextField(setup: AutoCompleteTextField.() -> Unit = {}): AutoCompleteTextField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(AutoCompleteTextField(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.button(setup: Button.() -> Unit = {}): Button {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Button(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.canvas(setup: Canvas.() -> Unit = {}): Canvas {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Canvas(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.checkbox(setup: Checkbox.() -> Unit = {}): Checkbox {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Checkbox(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.dismissBackground(setup: DismissBackground.() -> Unit = {}): DismissBackground {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(DismissBackground(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.externalLink(setup: ExternalLink.() -> Unit = {}): ExternalLink {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ExternalLink(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.icon(setup: IconView.() -> Unit = {}): IconView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(IconView(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.image(setup: ImageView.() -> Unit = {}): ImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return ImageView(this).apply {
        setup()
        postSetup()
    }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.zoomableImage(setup: ZoomableImageView.() -> Unit = {}): ZoomableImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return ZoomableImageView(this).apply {
        setup()
        postSetup()
    }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rawImage(source: ImageSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: RawImageView.() -> Unit = {}): RawImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RawImageView(context, source, description, scaleType) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rawImageUnsized(source: ImageSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: SizelessRawImageView.() -> Unit = {}): SizelessRawImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(SizelessRawImageView(context, source, description, scaleType) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rawImageZoomable(source: ImageSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: RawImageViewZoomable.() -> Unit = {}): RawImageViewZoomable {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RawImageViewZoomable(context, source, description, scaleType) , setup)
}
//@OptIn(ExperimentalContracts::class)
//@ViewDsl
//inline fun ViewWriter.zoomableImage(setup: ZoomableImageView.() -> Unit = {}): ZoomableImageView {
//    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
//    return write(ZoomableImageView(context) , setup)
//}
class Label(val label: TextView, val container: RowOrCol): ViewWriter(), ViewModifiable by container {
    override val context: RContext
        get() = container.context
    override fun addChild(view: RView) {
        container.addChild(view)
    }
    var content: String by label::content
}
@OptIn(ExperimentalContracts::class)
@Deprecated("use the new label: label(String, RowOrCol.() -> ViewModifiable)")
inline fun ViewWriter.label(setup: Label.() -> Unit = {}): Label {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val l: Label
    col {
        val label = subtext()
        gap = 0.px
        l = Label(label, this)
        setup(l)
    }
    return l
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.label(label: String, content: RowOrCol.() -> ViewModifiable): ViewModifiable {
    contract { callsInPlace(content, InvocationKind.EXACTLY_ONCE) }
    return col {
        FieldLabelSemantic.onNext - text(label)
        spacingOverrideBeforeNext(0.px)
        content()
    }
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.phoneNumberInput(setup: PhoneNumberInput.() -> Unit = {}): PhoneNumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return PhoneNumberInput(this).apply(setup)
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.link(setup: Link.() -> Unit = {}): Link {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Link(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localDateField(setup: LocalDateField.() -> Unit = {}): LocalDateField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalDateField(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localDateTimeField(setup: LocalDateTimeField.() -> Unit = {}): LocalDateTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalDateTimeField(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localTimeField(setup: LocalTimeField.() -> Unit = {}): LocalTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalTimeField(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.menuButton(setup: MenuButton.() -> Unit = {}): MenuButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(MenuButton(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.numberInput(setup: NumberInput.() -> Unit = {}): NumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberInput(context) , setup)
}
@Deprecated("Use numberInput instead", ReplaceWith("this.numberInput(setup)", "com.lightningkite.kiteui.views.direct.numberInput"))
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.numberField(setup: NumberInput.() -> Unit = {}): NumberInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberInput(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.progressBar(setup: ProgressBar.() -> Unit = {}): ProgressBar {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ProgressBar(context) , setup)
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.circularProgress(setup: CircularProgress.() -> Unit = {}): CircularProgress {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CircularProgress(context), setup)
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.radioButton(setup: RadioButton.() -> Unit = {}): RadioButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RadioButton(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.radioToggleButton(setup: RadioToggleButton.() -> Unit = {}): RadioToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RadioToggleButton(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rowCollapsingToColumn(breakpoint: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, listOf(breakpoint)), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rowCollapsingToColumn(verticalBefore: Dimension, verticalAfter: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, listOf(verticalBefore, verticalAfter)), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rowCollapsingToColumn(verticalBefore: Dimension, verticalAfter: Dimension, horizontalAgainAfter: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, listOf(verticalBefore, verticalAfter, horizontalAgainAfter)), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.select(setup: Select.() -> Unit = {}): Select {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Select(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.separator(setup: Separator.() -> Unit = {}): Separator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Separator(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.space(setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Space(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Space(context, multiplier), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.frame(setup: Frame.() -> Unit = {}): Frame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Frame(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.coordinatorFrame(setup: CoordinatorFrame.() -> Unit = {}): CoordinatorFrame {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CoordinatorFrame(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.coordinatorDragHandle(setup: CoordinatorDragHandle.() -> Unit = {}): CoordinatorDragHandle {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(CoordinatorDragHandle(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.swapView(setup: SwapView.() -> Unit = {}): SwapView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(SwapView(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.switch(setup: Switch.() -> Unit = {}): Switch {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Switch(context) , setup)
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.slider(setup: Slider.() -> Unit = {}): Slider {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Slider(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textArea(setup: TextArea.() -> Unit = {}): TextArea {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextArea(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textInput(setup: TextInput.() -> Unit = {}): TextInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextInput(context) , setup)
}
@Deprecated("Use textInput instead", ReplaceWith("this.textInput(setup)", "com.lightningkite.kiteui.views.direct.textInput"))
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textField(setup: TextInput.() -> Unit = {}): TextInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextInput(context) , setup)
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.formattedTextInput(setup: FormattedTextInput.() -> Unit = {}): FormattedTextInput {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(FormattedTextInput(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.text(setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextView(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.toggleButton(setup: ToggleButton.() -> Unit = {}): ToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ToggleButton(context) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.video(setup: VideoView.() -> Unit = {}): VideoView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return VideoView(this).apply {
        setup()
        postSetup()
    }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.media(setup: MediaView.() -> Unit = {}): MediaView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return MediaView(this).apply {
        setup()
        postSetup()
    }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rawVideo(source: VideoSource, description: String, scaleType: ImageScaleType = ImageScaleType.Fit, setup: RawVideoView.() -> Unit = {}): RawVideoView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RawVideoView(context, source, description, scaleType) , setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.webView(setup: WebView.() -> Unit = {}): WebView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(WebView(context) , setup)
}


@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rowWrapping(setup: RowWrapping.() -> Unit = {}): RowWrapping {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowWrapping(context) ) { setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.row(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context) ) { vertical = false; setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.column(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context) ) { vertical = true; setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.col(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context) , { vertical = true; setup() })
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.programmatic(setup: ProgrammaticLayout.() -> Unit = {}): ProgrammaticLayout {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ProgrammaticLayout(context), setup)
}

@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.recyclerView(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return Recycler2(this, true).apply(setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.viewPager(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return Recycler2(this, false).apply {

        placer = RecyclerViewPagingPlacer()
        snapToElements = Align.Center
        scrollSnapStop = true

        with(outerFrame) {
            if(!Platform.usesTouchscreen) {
                align(Align.Start, Align.Center) - frame {
                    button {
                        icon(Icon.chevronLeft, "Previous")
                        onClick { centerIndex set centerIndex() - 1 }
                    }
                }
                align(Align.End, Align.Center) - frame {
                    button {
                        icon(Icon.chevronRight, "Next")
                        onClick { centerIndex set centerIndex() + 1 }
                    }
                }
            }
        }
    }.apply(setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.horizontalRecyclerView(setup: Recycler2.() -> Unit = {}): Recycler2 {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return Recycler2(this, false).apply(setup)
}
