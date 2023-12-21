@file:OptIn(ExperimentalForeignApi::class)

package com.lightningkite.rock.views.direct

import com.lightningkite.rock.*
import com.lightningkite.rock.models.*
import com.lightningkite.rock.navigation.*
import com.lightningkite.rock.reactive.*
import com.lightningkite.rock.views.*
import com.lightningkite.rock.views.canvas.DrawingContext2D
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.datetime.*
import platform.Foundation.NSDate
import platform.UIKit.*
import platform.darwin.NSObject
import platform.objc.sel_registerName


@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSeparator = UIView

@ViewDsl
actual fun ViewWriter.separator(setup: Separator.() -> Unit): Unit = todo("separator")

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NContainingView = UIView

@ViewDsl
actual fun ViewWriter.stack(setup: ContainingView.() -> Unit): Unit = element(FrameLayout()) {
    handleTheme(this, viewDraws = false)
    setup(ContainingView(this))
}

@ViewDsl
actual fun ViewWriter.col(setup: ContainingView.() -> Unit): Unit = element(LinearLayout()) {
    horizontal = false
    handleTheme(this, viewDraws = false)
    setup(ContainingView(this))
}

@ViewDsl
actual fun ViewWriter.row(setup: ContainingView.() -> Unit): Unit = element(LinearLayout()) {
    horizontal = true
    handleTheme(this, viewDraws = false)
    setup(ContainingView(this))
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLink = NativeLink

@ViewDsl
actual fun ViewWriter.link(setup: Link.() -> Unit): Unit = element(NativeLink()) {
    handleTheme(this, viewDraws = false)
    setup(Link(this))
    onNavigator = navigator
}
actual inline var Link.to: RockScreen
    get() = native.toScreen ?: RockScreen.Empty
    set(value) {
        native.toScreen = value
    }
actual inline var Link.newTab: Boolean
    get() = native.newTab
    set(value) { native.newTab = value }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NExternalLink = NativeLink

@ViewDsl
actual fun ViewWriter.externalLink(setup: ExternalLink.() -> Unit): Unit = element(NativeLink()) {
    handleTheme(this, viewDraws = false)
    setup(ExternalLink(this))
}
actual inline var ExternalLink.to: String
    get() = native.toUrl ?: ""
    set(value) { native.toUrl = value }
actual inline var ExternalLink.newTab: Boolean
    get() = native.newTab
    set(value) { native.newTab = value }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NImage = UIImageView

@ViewDsl
actual fun ViewWriter.image(setup: Image.() -> Unit): Unit = element(NImage()) {
    handleTheme(this, viewDraws = true)
    setup(Image(this))
}
actual inline var Image.source: ImageSource
    get() = TODO()
    set(value) {
        when(value) {
            is ImageRaw -> {}
            is ImageRemote -> {
                launch {
                    fetch(value.url).blob()
                }
            }
            is ImageResource -> UIImage.imageNamed(value.name)
            is ImageVector -> {}
            else -> {}
        }
    }
actual inline var Image.scaleType: ImageScaleType
    get() = TODO()
    set(value) {}
actual inline var Image.description: String?
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextView = UILabel

@ViewDsl
actual fun ViewWriter.h1(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 2)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.title
        it.title.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.h2(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 1.6)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.title
        it.title.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.h3(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 1.4)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.title
        it.title.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.h4(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 1.3)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.title
        it.title.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.h5(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 1.2)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.title
        it.title.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.h6(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 1.1)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.title
        it.title.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.text(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 1.0)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.body
        it.body.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
}
@ViewDsl
actual fun ViewWriter.subtext(setup: TextView.() -> Unit): Unit = element(StyledUILabel()) {
    font = UIFont.systemFontOfSize(UIFont.systemFontSize * 0.8)
    handleTheme(this) {
        this.textColor = it.foreground.closestColor().toUiColor()
        this.extensionFontAndStyle = it.body
        it.body.let { this.font = it.font.get(font.pointSize, if(it.bold) UIFontWeightBold else UIFontWeightRegular) }
    }
    setup(TextView(this))
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
            native.font = it.font.get(value.value, if(it.bold) UIFontWeightBold else UIFontWeightRegular)
        }
    }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLabel = UIView

@ViewDsl
actual fun ViewWriter.label(setup: Label.() -> Unit): Unit = todo("label")
actual inline var Label.content: String
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NActivityIndicator = UIActivityIndicatorView

@ViewDsl
actual fun ViewWriter.activityIndicator(setup: ActivityIndicator.() -> Unit): Unit = element(UIActivityIndicatorView()) {
    setup(ActivityIndicator(this))
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSpace = UIView

@ViewDsl
actual fun ViewWriter.space(setup: Space.() -> Unit): Unit = element(UIView()) {
    setup(Space(this))
}
actual fun ViewWriter.space(multiplier: Double, setup: Space.() -> Unit): Unit = element(UIView()) {
    handleTheme(this) {
        extensionSizeConstraints = SizeConstraints(minHeight = it.spacing * multiplier)
    }
    setup(Space(this))
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NDismissBackground = FrameLayout

@ViewDsl
actual fun ViewWriter.dismissBackground(setup: DismissBackground.() -> Unit): Unit = element(FrameLayout()) {
    handleTheme(this) {
        backgroundColor = it.background.closestColor().copy(alpha = 0.5f).toUiColor()
    }
    setup(DismissBackground(this))
}
@OptIn(ExperimentalForeignApi::class)
actual fun DismissBackground.onClick(action: suspend () -> Unit): Unit {
    val actionHolder = object: NSObject() {
        @ObjCAction
        fun eventHandler() = launch(action)
    }
    val rec = UITapGestureRecognizer(actionHolder, sel_registerName("eventHandler"))
    native.addGestureRecognizer(rec)
    calculationContext.onRemove {
        // Retain the sleeve until disposed
        rec.enabled
        actionHolder.description
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NButton = FrameLayoutButton

@ViewDsl
actual fun ViewWriter.button(setup: Button.() -> Unit): Unit = element(FrameLayoutButton()) {
    handleTheme(this, viewDraws = false)
    setup(Button(this))
}
actual fun Button.onClick(action: suspend () -> Unit): Unit {
    native.onEvent(UIControlEventTouchUpInside) { launch(action) }
}
actual inline var Button.enabled: Boolean
    get() = native.enabled
    set(value) { native.enabled = value }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NCheckbox = UIView

@ViewDsl
actual fun ViewWriter.checkbox(setup: Checkbox.() -> Unit): Unit = todo("checkbox")
actual inline var Checkbox.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val Checkbox.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioButton = UIView

@ViewDsl
actual fun ViewWriter.radioButton(setup: RadioButton.() -> Unit): Unit = todo("radioButton")
actual inline var RadioButton.enabled: Boolean
    get() = TODO()
    set(value) {}
actual val RadioButton.checked: Writable<Boolean> get() = Property(false)

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSwitch = UISwitch

@ViewDsl
actual fun ViewWriter.switch(setup: Switch.() -> Unit): Unit = element(UISwitch()) {
    handleTheme(this) {

    }
    setup(Switch(this))
}
actual inline var Switch.enabled: Boolean
    get() = native.enabled
    set(value) { native.enabled = value }
actual val Switch.checked: Writable<Boolean> get() {
    return object: Writable<Boolean> {
        override suspend fun awaitRaw(): Boolean = native.on
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(UIControlEventValueChanged) { listener() }
        }
        override suspend fun set(value: Boolean) { native.on = value }
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NToggleButton = FrameLayoutToggleButton

@ViewDsl
actual fun ViewWriter.toggleButton(setup: ToggleButton.() -> Unit): Unit = element(FrameLayoutToggleButton()) {
    handleTheme(this, viewDraws = false)
}
actual inline var ToggleButton.enabled: Boolean
    get() = native.enabled
    set(value) { native.enabled = value }
actual val ToggleButton.checked: Writable<Boolean> get() {
    return object: Writable<Boolean> {
        override suspend fun awaitRaw(): Boolean = native.on
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(UIControlEventValueChanged) { listener() }
        }
        override suspend fun set(value: Boolean) { native.on = value }
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioToggleButton = FrameLayoutToggleButton

@ViewDsl
actual fun ViewWriter.radioToggleButton(setup: RadioToggleButton.() -> Unit): Unit = element(FrameLayoutToggleButton()) {
    handleTheme(this, viewDraws = false)
    allowUnselect = false
}
actual inline var RadioToggleButton.enabled: Boolean
    get() = native.enabled
    set(value) { native.enabled = value }
actual val RadioToggleButton.checked: Writable<Boolean> get() {
    return object: Writable<Boolean> {
        override suspend fun awaitRaw(): Boolean = native.on
        override fun addListener(listener: () -> Unit): () -> Unit {
            return native.onEvent(UIControlEventValueChanged) { listener() }
        }
        override suspend fun set(value: Boolean) { native.on = value }
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateField = FrameLayoutInputButton

@ViewDsl
actual fun ViewWriter.localDateField(setup: LocalDateField.() -> Unit): Unit = element(FrameLayoutInputButton()) {
    handleTheme(this, viewDraws = false)
    val p = Property<LocalDate?>(null)
    currentValue = p
    _inputView = UIDatePicker().apply {
        datePickerMode = UIDatePickerMode.UIDatePickerModeDate
        date = p.value?.toNSDateComponents()?.date() ?: NSDate()
        onEvent(UIControlEventValueChanged) {
            p.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).date
        }
    }
    setup(LocalDateField(this))
}
actual var LocalDateField.action: Action?
    get() = native.action
    set(value) { native.action = value }
actual val LocalDateField.content: Writable<LocalDate?> get() {
    @Suppress("UNCHECKED_CAST")
    return native.currentValue as Property<LocalDate?>
}
actual inline var LocalDateField.range: ClosedRange<LocalDate>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.valueRange as ClosedRange<LocalDate>
    }
    set(value) { native.valueRange = value }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalTimeField = FrameLayoutInputButton

@ViewDsl
actual fun ViewWriter.localTimeField(setup: LocalTimeField.() -> Unit): Unit = element(FrameLayoutInputButton()) {
    handleTheme(this, viewDraws = false)
    val p = Property<LocalTime?>(null)
    currentValue = p
    _inputView = UIDatePicker().apply {
        datePickerMode = UIDatePickerMode.UIDatePickerModeTime
        date = p.value?.atDate(1970, 1, 1)?.toNSDateComponents()?.date() ?: NSDate()
        onEvent(UIControlEventValueChanged) {
            p.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault()).time
        }
    }
    setup(LocalTimeField(this))
}
actual var LocalTimeField.action: Action?
    get() = native.action
    set(value) { native.action = value }
actual val LocalTimeField.content: Writable<LocalTime?> get() {
    @Suppress("UNCHECKED_CAST")
    return native.currentValue as Property<LocalTime?>
}
actual inline var LocalTimeField.range: ClosedRange<LocalTime>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.valueRange as ClosedRange<LocalTime>
    }
    set(value) { native.valueRange = value }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NLocalDateTimeField = FrameLayoutInputButton

@ViewDsl
actual fun ViewWriter.localDateTimeField(setup: LocalDateTimeField.() -> Unit): Unit = element(FrameLayoutInputButton()) {
    handleTheme(this, viewDraws = false)
    val p = Property<LocalDateTime?>(null)
    currentValue = p
    _inputView = UIDatePicker().apply {
        datePickerMode = UIDatePickerMode.UIDatePickerModeDateAndTime
        date = p.value?.toNSDateComponents()?.date() ?: NSDate()
        onEvent(UIControlEventValueChanged) {
            p.value = this.date.toKotlinInstant().toLocalDateTime(TimeZone.currentSystemDefault())
        }
    }
    setup(LocalDateTimeField(this))
}
actual var LocalDateTimeField.action: Action?
    get() = native.action
    set(value) { native.action = value }
actual val LocalDateTimeField.content: Writable<LocalDateTime?> get() {
    @Suppress("UNCHECKED_CAST")
    return native.currentValue as Property<LocalDateTime?>
}
actual inline var LocalDateTimeField.range: ClosedRange<LocalDateTime>?
    get() {
        @Suppress("UNCHECKED_CAST")
        return native.valueRange as ClosedRange<LocalDateTime>
    }
    set(value) { native.valueRange = value }

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextField = UITextField

@ViewDsl
actual fun ViewWriter.textField(setup: TextField.() -> Unit): Unit = element(UITextField()) {
    smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
    smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
    handleTheme(this, viewDraws = true)
    calculationContext.onRemove {
        extensionDelegateStrongRef = null
    }
    setup(TextField(this))
}
actual val TextField.content: Writable<String> get() = object: Writable<String> {
    override suspend fun awaitRaw(): String = native.text ?: ""
    override fun addListener(listener: () -> Unit): () -> Unit {
        return native.onEvent(UIControlEventValueChanged) { listener() }
    }
    override suspend fun set(value: String) { native.text = value }
}
actual inline var TextField.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        native.autocapitalizationType = when(value.case) {
            KeyboardCase.None -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            KeyboardCase.Letters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
            KeyboardCase.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
            KeyboardCase.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
        }
        native.keyboardType = when(value.type) {
            KeyboardType.Text -> UIKeyboardTypeDefault
            KeyboardType.Integer -> UIKeyboardTypeNumberPad
            KeyboardType.Phone -> UIKeyboardTypePhonePad
            KeyboardType.Decimal -> UIKeyboardTypeNumbersAndPunctuation
            KeyboardType.Email -> UIKeyboardTypeEmailAddress
        }
    }
actual var TextField.action: Action?
    get() = TODO()
    set(value) {
        native.delegate = action?.let {
            val d = object: NSObject(), UITextFieldDelegateProtocol {
                override fun textFieldShouldReturn(textField: UITextField): Boolean {
                    launch { it.onSelect() }
                    return true
                }
            }
            native.extensionDelegateStrongRef = d
            d
        } ?: NextFocusDelegateShared
        native.returnKeyType = when(action?.title) {
            "Emergency Call" -> UIReturnKeyType.UIReturnKeyEmergencyCall
            "Go" -> UIReturnKeyType.UIReturnKeyGo
            "Next" -> UIReturnKeyType.UIReturnKeyNext
            "Continue" -> UIReturnKeyType.UIReturnKeyContinue
            "Default" -> UIReturnKeyType.UIReturnKeyDefault
            "Join" -> UIReturnKeyType.UIReturnKeyJoin
            "Done" -> UIReturnKeyType.UIReturnKeyDone
            "Yahoo" -> UIReturnKeyType.UIReturnKeyYahoo
            "Send" -> UIReturnKeyType.UIReturnKeySend
            "Google" -> UIReturnKeyType.UIReturnKeyGoogle
            "Route" -> UIReturnKeyType.UIReturnKeyRoute
            "Search" -> UIReturnKeyType.UIReturnKeySearch
            else -> UIReturnKeyType.UIReturnKeyDone
        }
    }
actual inline var TextField.hint: String
    get() = native.placeholder ?: ""
    set(value) { native.placeholder = value }
actual inline var TextField.range: ClosedRange<Double>?
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NTextArea = UITextView

@ViewDsl
actual fun ViewWriter.textArea(setup: TextArea.() -> Unit): Unit = element(UITextView()) {
    smartDashesType = UITextSmartDashesType.UITextSmartDashesTypeNo
    smartQuotesType = UITextSmartQuotesType.UITextSmartQuotesTypeNo
    handleTheme(this, viewDraws = true)
    setup(TextArea(this))
    calculationContext.onRemove {
        extensionDelegateStrongRef = null
    }
}
actual val TextArea.content: Writable<String> get() = object: Writable<String> {
    override suspend fun awaitRaw(): String = native.text ?: ""
    override fun addListener(listener: () -> Unit): () -> Unit {
        native.setDelegate(object: NSObject(), UITextViewDelegateProtocol {
            override fun textViewDidChange(textView: UITextView) {
                listener()
            }
        })
        return {
            native.setDelegate(null)
        }
    }
    override suspend fun set(value: String) { native.text = value }
}
actual inline var TextArea.keyboardHints: KeyboardHints
    get() = TODO()
    set(value) {
        native.autocapitalizationType = when(value.case) {
            KeyboardCase.None -> UITextAutocapitalizationType.UITextAutocapitalizationTypeNone
            KeyboardCase.Letters -> UITextAutocapitalizationType.UITextAutocapitalizationTypeAllCharacters
            KeyboardCase.Words -> UITextAutocapitalizationType.UITextAutocapitalizationTypeWords
            KeyboardCase.Sentences -> UITextAutocapitalizationType.UITextAutocapitalizationTypeSentences
        }
        native.keyboardType = when(value.type) {
            KeyboardType.Text -> UIKeyboardTypeDefault
            KeyboardType.Integer -> UIKeyboardTypeNumberPad
            KeyboardType.Phone -> UIKeyboardTypePhonePad
            KeyboardType.Decimal -> UIKeyboardTypeNumbersAndPunctuation
            KeyboardType.Email -> UIKeyboardTypeEmailAddress
        }
    }
actual inline var TextArea.hint: String
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSelect = UIView

@ViewDsl
actual fun ViewWriter.select(setup: Select.() -> Unit): Unit = todo("select")
actual val Select.selected: Writable<String?> get() = Property(null)
actual inline var Select.options: List<WidgetOption>
    get() = TODO()
    set(value) {}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NAutoCompleteTextField = UIView

@ViewDsl
actual fun ViewWriter.autoCompleteTextField(setup: AutoCompleteTextField.() -> Unit): Unit =
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
actual typealias NSwapView = FrameLayout

@ViewDsl
actual fun ViewWriter.swapView(setup: SwapView.() -> Unit) = element(FrameLayout()) {
    handleTheme(this, viewDraws = false)
    setup(SwapView(this))
}
@ViewDsl
actual fun ViewWriter.swapViewDialog(setup: SwapView.() -> Unit): Unit = element(FrameLayout()) {
    handleTheme(this, viewDraws = false)
    hidden = true
    setup(SwapView(this))
}
actual fun SwapView.swap(transition: ScreenTransition, createNewView: () -> Unit): Unit {
    println("Clearing views. Children count: ${native.subviews.size}")
    native.clearChildren()
    println("Creating a new view. Children count: ${native.subviews.size}")
    createNewView()
    println("Created a new view. Children count: ${native.subviews.size}")
    native.hidden = native.subviews.isEmpty()
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NWebView = UIView

@ViewDsl
actual fun ViewWriter.webView(setup: WebView.() -> Unit): Unit = todo("webView")
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
actual typealias NCanvas = UIView

@ViewDsl
actual fun ViewWriter.canvas(setup: Canvas.() -> Unit): Unit = todo("canvas")
actual fun Canvas.redraw(action: DrawingContext2D.() -> Unit): Unit = TODO()
actual val Canvas.width: Readable<Double> get() = Property(0.0)
actual val Canvas.height: Readable<Double> get() = Property(0.0)
actual fun Canvas.onPointerDown(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): Unit =
    TODO()

actual fun Canvas.onPointerMove(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): Unit =
    TODO()

actual fun Canvas.onPointerCancel(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): Unit =
    TODO()

actual fun Canvas.onPointerUp(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): Unit =
    TODO()

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRecyclerView = UIView

@ViewDsl
actual fun ViewWriter.recyclerView(setup: RecyclerView.() -> Unit): Unit = todo("recyclerView")
@ViewDsl
actual fun ViewWriter.horizontalRecyclerView(setup: RecyclerView.() -> Unit): Unit = todo("horizontalRecyclerView")
@ViewDsl
actual fun ViewWriter.gridRecyclerView(setup: RecyclerView.() -> Unit): Unit = todo("gridRecyclerView")
actual var RecyclerView.columns: Int
    get() = 1
    set(value) {
    }

actual fun <T> RecyclerView.children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>) -> Unit): Unit {
    // TODO()
}

@ViewModifierDsl3
actual fun ViewWriter.hasPopover(
    requireClick: Boolean,
    preferredDirection: PopoverPreferredDirection,
    setup: ViewWriter.() -> Unit
): ViewWrapper {
    // TODO
    return ViewWrapper
}

@ViewModifierDsl3
actual fun ViewWriter.weight(amount: Float): ViewWrapper {
    val parent = this.currentView
    this.beforeNextElementSetup {
        this.extensionWeight = amount
    }
    return ViewWrapper
}
@ViewModifierDsl3
actual fun ViewWriter.gravity(horizontal: Align, vertical: Align): ViewWrapper {
    beforeNextElementSetup {
        extensionHorizontalAlign = horizontal
        extensionVerticalAlign = vertical
    }
    return ViewWrapper
}
@ViewModifierDsl3
actual val ViewWriter.scrolls: ViewWrapper get() {
    wrapNext(ScrollLayout()) {
        handleTheme(this, viewDraws = false)
        horizontal = false
    }
    return ViewWrapper
}
@ViewModifierDsl3
actual val ViewWriter.scrollsHorizontally: ViewWrapper get() {
    wrapNext(ScrollLayout()) {
        handleTheme(this, viewDraws = false)
        horizontal = true
    }
    return ViewWrapper
}
@ViewModifierDsl3
actual fun ViewWriter.sizedBox(constraints: SizeConstraints): ViewWrapper {
    beforeNextElementSetup {
        println("Applying $constraints to $this")
        extensionSizeConstraints = constraints
    }
    return ViewWrapper
}
@ViewModifierDsl3
actual val ViewWriter.marginless: ViewWrapper get() = ViewWrapper
@ViewModifierDsl3
actual val ViewWriter.withDefaultPadding: ViewWrapper get() = ViewWrapper
// End
