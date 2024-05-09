package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.RView
import ViewWriter
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract



@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.activityIndicator(setup: ActivityIndicator.() -> Unit = {}): ActivityIndicator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ActivityIndicator(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.autoCompleteTextField(setup: AutoCompleteTextField.() -> Unit = {}): AutoCompleteTextField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(AutoCompleteTextField(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.button(setup: Button.() -> Unit = {}): Button {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Button(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.canvas(setup: Canvas.() -> Unit = {}): Canvas {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Canvas(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.checkbox(setup: Checkbox.() -> Unit = {}): Checkbox {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Checkbox(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.dismissBackground(setup: DismissBackground.() -> Unit = {}): DismissBackground {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(DismissBackground(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.externalLink(setup: ExternalLink.() -> Unit = {}): ExternalLink {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ExternalLink(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.icon(setup: IconView.() -> Unit = {}): IconView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(IconView(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.image(setup: ImageView.() -> Unit = {}): ImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ImageView(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.zoomableImage(setup: ZoomableImageView.() -> Unit = {}): ZoomableImageView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ZoomableImageView(context), setup)
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
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.link(setup: Link.() -> Unit = {}): Link {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Link(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localDateField(setup: LocalDateField.() -> Unit = {}): LocalDateField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalDateField(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localDateTimeField(setup: LocalDateTimeField.() -> Unit = {}): LocalDateTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalDateTimeField(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.localTimeField(setup: LocalTimeField.() -> Unit = {}): LocalTimeField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(LocalTimeField(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.menuButton(setup: MenuButton.() -> Unit = {}): MenuButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(MenuButton(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.numberField(setup: NumberField.() -> Unit = {}): NumberField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(NumberField(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.progressBar(setup: ProgressBar.() -> Unit = {}): ProgressBar {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ProgressBar(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.radioButton(setup: RadioButton.() -> Unit = {}): RadioButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RadioButton(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.radioToggleButton(setup: RadioToggleButton.() -> Unit = {}): RadioToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RadioToggleButton(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.recyclerView(setup: RecyclerView.() -> Unit = {}): RecyclerView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RecyclerView(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.rowCollapsingToColumn(breakpoint: Dimension, setup: RowCollapsingToColumn.() -> Unit = {}): RowCollapsingToColumn {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowCollapsingToColumn(context, breakpoint), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.select(setup: Select.() -> Unit = {}): Select {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Select(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.separator(setup: Separator.() -> Unit = {}): Separator {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Separator(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.space(setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Space(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit = {}): Space {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Space(context, multiplier), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.stack(setup: Stack.() -> Unit = {}): Stack {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Stack(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.swapView(setup: SwapView.() -> Unit = {}): SwapView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(SwapView(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.switch(setup: Switch.() -> Unit = {}): Switch {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Switch(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textArea(setup: TextArea.() -> Unit = {}): TextArea {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextArea(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.textField(setup: TextField.() -> Unit = {}): TextField {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(TextField(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.header(level: Int, setup: HeaderView.() -> Unit = {}): HeaderView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(HeaderView(context, level), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.text(setup: BodyTextView.() -> Unit = {}): BodyTextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(BodyTextView(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.subtext(setup: SubTextView.() -> Unit = {}): SubTextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(SubTextView(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.toggleButton(setup: ToggleButton.() -> Unit = {}): ToggleButton {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ToggleButton(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.video(setup: Video.() -> Unit = {}): Video {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(Video(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.viewPager(setup: ViewPager.() -> Unit = {}): ViewPager {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(ViewPager(context), setup)
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.webView(setup: WebView.() -> Unit = {}): WebView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(WebView(context), setup)
}


@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.horizontalRecyclerView(setup: RecyclerView.() -> Unit = {}): RecyclerView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RecyclerView(context)) {
        vertical = false
        setup()
    }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.row(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context)) { vertical = false; setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.column(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context)) { vertical = true; setup() }
}
@OptIn(ExperimentalContracts::class)
@ViewDsl
inline fun ViewWriter.col(setup: RowOrCol.() -> Unit = {}): RowOrCol {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return write(RowOrCol(context), { vertical = true; setup() })
}