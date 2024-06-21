package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.dom.HTMLElement
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlinx.datetime.*


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSeparator = HTMLElement

@ViewDsl
actual inline fun ViewWriter.separatorActual(crossinline setup: Separator.() -> Unit): Unit = todo("separator")

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NContainingView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.stackActual(crossinline setup: ContainingView.() -> Unit): Unit = todo("stack")

@ViewDsl
actual inline fun ViewWriter.colActual(crossinline setup: ContainingView.() -> Unit): Unit = todo("col")

@ViewDsl
actual inline fun ViewWriter.rowActual(crossinline setup: ContainingView.() -> Unit): Unit = todo("row")

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLink = HTMLElement

@ViewDsl
actual inline fun ViewWriter.linkActual(crossinline setup: Link.() -> Unit): Unit = todo("link")
actual inline var Link.to: ()->Screen
    get() = TODO()
    set(value) {}
actual inline var Link.navigator: ScreenStack
    get() = TODO()
    set(value) {}
actual inline var Link.newTab: Boolean
    get() = TODO()
    set(value) {}
actual inline var Link.resetsStack: Boolean
    get() = TODO()
    set(value) {}

actual fun Link.onNavigate(action: suspend () -> Unit): Unit {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NExternalLink = HTMLElement

@ViewDsl
actual inline fun ViewWriter.externalLinkActual(crossinline setup: ExternalLink.() -> Unit): Unit = todo("externalLink")
actual inline var ExternalLink.to: String
    get() = TODO()
    set(value) {}
actual inline var ExternalLink.newTab: Boolean
    get() = TODO()
    set(value) {}

actual fun ExternalLink.onNavigate(action: suspend () -> Unit): Unit {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NImageView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.imageActual(crossinline setup: ImageView.() -> Unit): Unit = todo("image")
actual inline var ImageView.source: ImageSource?
    get() = TODO()
    set(value) {}
actual inline var ImageView.scaleType: ImageScaleType
    get() = TODO()
    set(value) {}
actual inline var ImageView.description: String?
    get() = TODO()
    set(value) {}
actual inline var ImageView.refreshOnParamChange: Boolean
    get() = false
    set(value) {}
actual var ImageView.naturalSize: Boolean
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.h1Actual(crossinline setup: TextView.() -> Unit): Unit = todo("h1")

@ViewDsl
actual inline fun ViewWriter.h2Actual(crossinline setup: TextView.() -> Unit): Unit = todo("h2")

@ViewDsl
actual inline fun ViewWriter.h3Actual(crossinline setup: TextView.() -> Unit): Unit = todo("h3")

@ViewDsl
actual inline fun ViewWriter.h4Actual(crossinline setup: TextView.() -> Unit): Unit = todo("h4")

@ViewDsl
actual inline fun ViewWriter.h5Actual(crossinline setup: TextView.() -> Unit): Unit = todo("h5")

@ViewDsl
actual inline fun ViewWriter.h6Actual(crossinline setup: TextView.() -> Unit): Unit = todo("h6")

@ViewDsl
actual inline fun ViewWriter.textActual(crossinline setup: TextView.() -> Unit): Unit = todo("text")

@ViewDsl
actual inline fun ViewWriter.subtextActual(crossinline setup: TextView.() -> Unit): Unit = todo("subtext")
actual inline var TextView.content: String
    get() = TODO()
    set(value) {}
actual inline var TextView.align: Align
    get() = TODO()
    set(value) {}
actual inline var TextView.textSize: Dimension
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLabel = HTMLElement

@ViewDsl
actual inline fun ViewWriter.labelActual(crossinline setup: Label.() -> Unit): Unit = todo("label")
actual inline var Label.content: String
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NActivityIndicator = HTMLElement

@ViewDsl
actual inline fun ViewWriter.activityIndicatorActual(crossinline setup: ActivityIndicator.() -> Unit): Unit =
    todo("activityIndicator")

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSpace = HTMLElement

@ViewDsl
actual inline fun ViewWriter.spaceActual(crossinline setup: Space.() -> Unit): Unit = todo("space")
actual fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit): Unit = TODO()

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NDismissBackground = HTMLElement

@ViewDsl
actual inline fun ViewWriter.dismissBackgroundActual(crossinline setup: DismissBackground.() -> Unit): Unit =
    todo("dismissBackground")

actual fun DismissBackground.onClick(action: suspend () -> Unit): Unit = TODO()

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NButton = HTMLElement

@ViewDsl
actual inline fun ViewWriter.buttonActual(crossinline setup: Button.() -> Unit): Unit = todo("button")
actual fun Button.onClick(action: suspend () -> Unit): Unit = TODO()
actual inline var Button.enabled: Boolean
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NCheckbox = HTMLElement

@ViewDsl
actual inline fun ViewWriter.checkboxActual(crossinline setup: Checkbox.() -> Unit): Unit = todo("checkbox")
actual inline var Checkbox.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val Checkbox.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioButton = HTMLElement

@ViewDsl
actual inline fun ViewWriter.radioButtonActual(crossinline setup: RadioButton.() -> Unit): Unit = todo("radioButton")
actual inline var RadioButton.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val RadioButton.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwitch = HTMLElement

@ViewDsl
actual inline fun ViewWriter.switchActual(crossinline setup: Switch.() -> Unit): Unit = todo("switch")
actual inline var Switch.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val Switch.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NToggleButton = HTMLElement

@ViewDsl
actual inline fun ViewWriter.toggleButtonActual(crossinline setup: ToggleButton.() -> Unit): Unit = todo("toggleButton")
actual inline var ToggleButton.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val ToggleButton.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioToggleButton = HTMLElement

@ViewDsl
actual inline fun ViewWriter.radioToggleButtonActual(crossinline setup: RadioToggleButton.() -> Unit): Unit =
    todo("radioToggleButton")

actual inline var RadioToggleButton.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val RadioToggleButton.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateField = HTMLElement

@ViewDsl
actual inline fun ViewWriter.localDateFieldActual(crossinline setup: LocalDateField.() -> Unit): Unit =
    todo("localDateField")

actual var LocalDateField.action: Action?
    get() = TODO()
    set(value) {}
actual val LocalDateField.content: Writable<LocalDate?> get() = Property(null)
actual inline var LocalDateField.range: ClosedRange<LocalDate>?
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalTimeField = HTMLElement

@ViewDsl
actual inline fun ViewWriter.localTimeFieldActual(crossinline setup: LocalTimeField.() -> Unit): Unit =
    todo("localTimeField")

actual var LocalTimeField.action: Action?
    get() = TODO()
    set(value) {}
actual val LocalTimeField.content: Writable<LocalTime?> get() = Property(null)
actual inline var LocalTimeField.range: ClosedRange<LocalTime>?
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateTimeField = HTMLElement

@ViewDsl
actual inline fun ViewWriter.localDateTimeFieldActual(crossinline setup: LocalDateTimeField.() -> Unit): Unit =
    todo("localDateTimeField")

actual var LocalDateTimeField.action: Action?
    get() = TODO()
    set(value) {}
actual val LocalDateTimeField.content: Writable<LocalDateTime?> get() = Property(null)
actual inline var LocalDateTimeField.range: ClosedRange<LocalDateTime>?
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextField = HTMLElement

@ViewDsl
actual inline fun ViewWriter.textFieldActual(crossinline setup: TextField.() -> Unit): Unit = todo("textField")
actual val TextField.content: Writable<String> get() = Property("")
actual inline var TextField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {}
actual var TextField.action: Action?
    get() = TODO()
    set(value) {}
actual inline var TextField.hint: String
    get() = TODO()
    set(value) {}
actual var TextField.align: Align
    get() = TODO()
    set(value) {}
actual var TextField.textSize: Dimension
    get() = TODO()
    set(value) {}
actual var TextField.enabled: Boolean
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NNumberField = HTMLElement

@ViewDsl
actual inline fun ViewWriter.numberFieldActual(crossinline setup: NumberField.() -> Unit): Unit = todo("textField")
actual val NumberField.content: Writable<Double?> get() = Property(null)
actual inline var NumberField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {}
actual var NumberField.action: Action?
    get() = TODO()
    set(value) {}
actual inline var NumberField.hint: String
    get() = TODO()
    set(value) {}
actual inline var NumberField.range: ClosedRange<Double>?
    get() = TODO()
    set(value) {}
actual var NumberField.align: Align
    get() = TODO()
    set(value) {}
actual var NumberField.textSize: Dimension
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextArea = HTMLElement

@ViewDsl
actual inline fun ViewWriter.textAreaActual(crossinline setup: TextArea.() -> Unit): Unit = todo("textArea")
actual val TextArea.content: Writable<String> get() = Property("")
actual inline var TextArea.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {}
actual inline var TextArea.hint: String
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSelect = HTMLElement

@ViewDsl
actual inline fun ViewWriter.selectActual(crossinline setup: Select.() -> Unit): Unit = todo("select")
actual fun <T> Select.bind(
    edits: Writable<T>,
    data: Readable<List<T>>,
    render: (T) -> String
) {
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NAutoCompleteTextField = HTMLElement

@ViewDsl
actual inline fun ViewWriter.autoCompleteTextFieldActual(crossinline setup: AutoCompleteTextField.() -> Unit): Unit =
    todo("autoCompleteTextField")

actual val AutoCompleteTextField.content: Writable<String> get() = Property("")
actual inline var AutoCompleteTextField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {}
actual var AutoCompleteTextField.action: Action?
    get() = TODO()
    set(value) {}
actual inline var AutoCompleteTextField.suggestions: List<String>
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwapView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.swapViewActual(crossinline setup: SwapView.() -> Unit): Unit = todo("swapView")

@ViewDsl
actual inline fun ViewWriter.swapViewDialogActual(crossinline setup: SwapView.() -> Unit): Unit = todo("swapViewDialog")
actual fun SwapView.swap(transition: ScreenTransition, createNewView: ViewWriter.() -> Unit): Unit = TODO()

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NWebView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.webViewActual(crossinline setup: WebView.() -> Unit): Unit = todo("webView")
actual inline var WebView.url: String
    get() = TODO()
    set(value) {}
actual inline var WebView.permitJs: Boolean
    get() = TODO()
    set(value) {}
actual inline var WebView.content: String
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NCanvas = HTMLElement

@ViewDsl
actual inline fun ViewWriter.canvasActual(crossinline setup: Canvas.() -> Unit): Unit = todo("canvas")
@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRecyclerView = HTMLElement

@ViewDsl
actual inline fun ViewWriter.recyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit = todo("recyclerView")

@ViewDsl
actual inline fun ViewWriter.horizontalRecyclerViewActual(crossinline setup: RecyclerView.() -> Unit): Unit =
    todo("horizontalRecyclerView")

actual var RecyclerView.columns: Int
    get() = 1
    set(value) {
        TODO()
    }

actual fun <T> RecyclerView.children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>) -> Unit): Unit =
    TODO()

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requiresClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.(popoverContext: PopoverContext) -> Unit
): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.hintPopover(
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.textPopover(message: String): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper = TODO()

@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper get() = TODO()

@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper get() = TODO()

@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper = TODO()

@ViewModifierDsl3
actual fun ViewWriter.changingSizeConstraints(constraints: suspend () -> SizeConstraints): ViewWrapper = TODO()

@ViewModifierDsl3
actual val ViewWriter.padded: ViewWrapper get() = TODO()

@ViewModifierDsl3
actual val ViewWriter.unpadded: ViewWrapper get() = TODO()
// End