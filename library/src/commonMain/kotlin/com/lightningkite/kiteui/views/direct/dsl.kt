package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.PerformanceInfo
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

val perfAllViews = PerformanceInfo("AllViews")

val perfActivityIndicator = PerformanceInfo("ActivityIndicator", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.activityIndicator(setup: ActivityIndicator.() -> Unit = {}): ActivityIndicator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfActivityIndicator { ActivityIndicator(context) }, setup)
}
val perfAutoCompleteTextField = PerformanceInfo("AutoCompleteTextField", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.autoCompleteTextField(setup: AutoCompleteTextField.() -> Unit = {}): AutoCompleteTextField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfAutoCompleteTextField { AutoCompleteTextField(context) }, setup)
}
val perfButton = PerformanceInfo("Button", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.button(setup: Button.() -> Unit = {}): Button {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfButton { Button(context) }, setup)
}
val perfCanvas = PerformanceInfo("Canvas", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.canvas(setup: Canvas.() -> Unit = {}): Canvas {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfCanvas { Canvas(context) }, setup)
}
val perfCheckbox = PerformanceInfo("Checkbox", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.checkbox(setup: Checkbox.() -> Unit = {}): Checkbox {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfCheckbox { Checkbox(context) }, setup)
}
val perfDismissBackground = PerformanceInfo("DismissBackground", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.dismissBackground(setup: DismissBackground.() -> Unit = {}): DismissBackground {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfDismissBackground { DismissBackground(context) }, setup)
}
val perfExternalLink = PerformanceInfo("ExternalLink", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.externalLink(setup: ExternalLink.() -> Unit = {}): ExternalLink {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfExternalLink { ExternalLink(context) }, setup)
}
val perfIconView = PerformanceInfo("IconView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.icon(setup: IconView.() -> Unit = {}): IconView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfIconView { IconView(context) }, setup)
}
val perfImageView = PerformanceInfo("ImageView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.image(setup: ImageView.() -> Unit = {}): ImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfImageView { ImageView(context) }, setup)
}
val perfZoomableImageView = PerformanceInfo("ZoomableImageView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.zoomableImage(setup: ZoomableImageView.() -> Unit = {}): ZoomableImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfZoomableImageView { ZoomableImageView(context) }, setup)
}
class Label(val label: TextView, val container: RowOrCol): ViewWriter() {
    override val context: RContext
        get() = container.context
    override fun addChild(view: RView) {
        container.addChild(view)
    }
    var content: String by label::content
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.label(setup: Label.() -> Unit = {}): Label {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    val l: Label
    col {
        val label = subtext()
        spacing = 0.px
        l = Label(label, this)
        setup(l)
    }
    return l
}
val perfLink = PerformanceInfo("Link", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.link(setup: Link.() -> Unit = {}): Link {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfLink { Link(context) }, setup)
}
val perfLocalDateField = PerformanceInfo("LocalDateField", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localDateField(setup: LocalDateField.() -> Unit = {}): LocalDateField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfLocalDateField { LocalDateField(context) }, setup)
}
val perfLocalDateTimeField = PerformanceInfo("LocalDateTimeField", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localDateTimeField(setup: LocalDateTimeField.() -> Unit = {}): LocalDateTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfLocalDateTimeField { LocalDateTimeField(context) }, setup)
}
val perfLocalTimeField = PerformanceInfo("LocalTimeField", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localTimeField(setup: LocalTimeField.() -> Unit = {}): LocalTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfLocalTimeField { LocalTimeField(context) }, setup)
}
val perfMenuButton = PerformanceInfo("MenuButton", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.menuButton(setup: MenuButton.() -> Unit = {}): MenuButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfMenuButton { MenuButton(context) }, setup)
}
val perfNumberField = PerformanceInfo("NumberField", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.numberField(setup: NumberField.() -> Unit = {}): NumberField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfNumberField { NumberField(context) }, setup)
}
val perfProgressBar = PerformanceInfo("ProgressBar", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.progressBar(setup: ProgressBar.() -> Unit = {}): ProgressBar {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfProgressBar { ProgressBar(context) }, setup)
}
val perfRadioButton = PerformanceInfo("RadioButton", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.radioButton(setup: RadioButton.() -> Unit = {}): RadioButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRadioButton { RadioButton(context) }, setup)
}
val perfRadioToggleButton = PerformanceInfo("RadioToggleButton", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.radioToggleButton(setup: RadioToggleButton.() -> Unit = {}): RadioToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRadioToggleButton { RadioToggleButton(context) }, setup)
}
val perfRecyclerView = PerformanceInfo("RecyclerView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.recyclerView(setup: RecyclerView.() -> Unit = {}): RecyclerView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRecyclerView { RecyclerView(context) }, setup)
}
val perfRowCollapsingToColumn = PerformanceInfo("RowCollapsingToColumn", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rowCollapsingToColumn(breakpoint: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRowCollapsingToColumn { RowCollapsingToColumn(context, breakpoint) }, setup)
}
val perfSelect = PerformanceInfo("Select", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.select(setup: Select.() -> Unit = {}): Select {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSelect { Select(context) }, setup)
}
val perfSeparator = PerformanceInfo("Separator", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.separator(setup: Separator.() -> Unit = {}): Separator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSeparator { Separator(context) }, setup)
}
val perfSpace = PerformanceInfo("Space", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.space(setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSpace { Space(context) }, setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSpace { Space(context, multiplier) }, setup)
}
val perfStack = PerformanceInfo("Stack", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.stack(setup: Stack.() -> Unit = {}): Stack {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfStack { Stack(context) }, setup)
}
val perfSwapView = PerformanceInfo("SwapView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.swapView(setup: SwapView.() -> Unit = {}): SwapView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSwapView { SwapView(context) }, setup)
}
val perfSwitch = PerformanceInfo("Switch", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.switch(setup: Switch.() -> Unit = {}): Switch {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSwitch { Switch(context) }, setup)
}
val perfTextArea = PerformanceInfo("TextArea", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textArea(setup: TextArea.() -> Unit = {}): TextArea {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfTextArea { TextArea(context) }, setup)
}
val perfTextField = PerformanceInfo("TextField", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textField(setup: TextField.() -> Unit = {}): TextField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfTextField { TextField(context) }, setup)
}
val perfHeaderView = PerformanceInfo("HeaderView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.header(level: Int, setup: HeaderView.() -> Unit = {}): HeaderView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfHeaderView { HeaderView(context, level ) }, setup)
}
val perfBodyTextView = PerformanceInfo("BodyTextView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.text(setup: BodyTextView.() -> Unit = {}): BodyTextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfBodyTextView { BodyTextView(context) }, setup)
}
val perfSubTextView = PerformanceInfo("SubTextView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.subtext(setup: SubTextView.() -> Unit = {}): SubTextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfSubTextView { SubTextView(context) }, setup)
}
val perfToggleButton = PerformanceInfo("ToggleButton", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.toggleButton(setup: ToggleButton.() -> Unit = {}): ToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfToggleButton { ToggleButton(context) }, setup)
}
val perfVideo = PerformanceInfo("Video", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.video(setup: Video.() -> Unit = {}): Video {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfVideo { Video(context) }, setup)
}
val perfViewPager = PerformanceInfo("ViewPager", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.viewPager(setup: ViewPager.() -> Unit = {}): ViewPager {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfViewPager { ViewPager(context) }, setup)
}
val perfWebView = PerformanceInfo("WebView", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.webView(setup: WebView.() -> Unit = {}): WebView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfWebView { WebView(context) }, setup)
}


@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.horizontalRecyclerView(setup: RecyclerView.() -> Unit = {}): RecyclerView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRecyclerView { RecyclerView(context) }) {
        vertical = false
        setup()
    }
}
val perfRowOrCol = PerformanceInfo("RowOrCol", perfAllViews)
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.row(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRowOrCol { RowOrCol(context) }) { vertical = false; setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.column(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRowOrCol { RowOrCol(context) }) { vertical = true; setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.col(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(perfRowOrCol { RowOrCol(context) }, { vertical = true; setup() })
}