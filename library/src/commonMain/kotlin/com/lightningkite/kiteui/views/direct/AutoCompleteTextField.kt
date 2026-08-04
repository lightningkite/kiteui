package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeElementWithAction
import com.lightningkite.reactive.core.*


public expect class AutoCompleteTextField(context: ElementContext) : NativeElementWithAction {
    public val content: MutableReactiveValue<String>
    public var keyboardHints: KeyboardHints

    /**
     * Placeholder shown while the field is empty, as on [TextField].
     *
     * Declared here because it previously existed only on the iOS and web implementations, so shared
     * code that set it compiled on those targets and failed on Android.
     */
    public var hint: String

    /**
     * Suggested completions offered to the user as they type.
     *
     * Platform support varies with what each platform's text field natively offers:
     * - **Web**: backed by an HTML `<datalist>`, shown by the browser's native autocomplete UI.
     * - **Android**: backed by `AutoCompleteTextView`'s built-in dropdown adapter.
     * - **iOS**: not yet implemented - `UITextField` has no built-in completion affordance, so this
     *   list is currently recorded but not surfaced to the user.
     */
    public var suggestions: List<String>
}