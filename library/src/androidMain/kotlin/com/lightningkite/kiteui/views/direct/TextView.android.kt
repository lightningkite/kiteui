package com.lightningkite.kiteui.views.direct

import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView as AndroidTextView
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.ReadableState
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.ViewDsl
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.opacity

actual typealias NTextView = AndroidTextView

inline fun ViewWriter.textElement(textSize: Float, crossinline setup: TextView.() -> Unit) =
    viewElement(factory = ::AndroidTextView, wrapper = ::TextView) {
        val androidText = native
        androidText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        handleTheme(native, viewLoads = true, foreground = applyTextColorFromTheme)
        setup(TextView(androidText))
    }

inline fun ViewWriter.header(textSize: Float, crossinline setup: TextView.() -> Unit) =
    viewElement(factory = ::AndroidTextView, wrapper = ::TextView) {
        val androidText = native
        androidText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
        handleTheme(native, viewLoads = true, foreground = applyTextColorFromThemeHeader)
        setup(TextView(androidText))
    }

object TextSizes {
    val h1: Float get() = 2.rem.value
    val h2: Float get() = 1.6.rem.value
    val h3: Float get() = 1.4.rem.value
    val h4: Float get() = 1.3.rem.value
    val h5: Float get() = 1.2.rem.value
    val h6: Float get() = 1.1.rem.value
    val body: Float get() = 1.rem.value
    val subtext: Float get() = 0.8.rem.value
}

@ViewDsl
actual inline fun ViewWriter.h1Actual(crossinline setup: TextView.() -> Unit): Unit = header(TextSizes.h1, setup)

@ViewDsl
actual inline fun ViewWriter.h2Actual(crossinline setup: TextView.() -> Unit): Unit = header(TextSizes.h2, setup)

@ViewDsl
actual inline fun ViewWriter.h3Actual(crossinline setup: TextView.() -> Unit): Unit = header(TextSizes.h3, setup)

@ViewDsl
actual inline fun ViewWriter.h4Actual(crossinline setup: TextView.() -> Unit): Unit = header(TextSizes.h4, setup)

@ViewDsl
actual inline fun ViewWriter.h5Actual(crossinline setup: TextView.() -> Unit): Unit = header(TextSizes.h5, setup)

@ViewDsl
actual inline fun ViewWriter.h6Actual(crossinline setup: TextView.() -> Unit): Unit = header(TextSizes.h6, setup)

@ViewDsl
actual inline fun ViewWriter.textActual(crossinline setup: TextView.() -> Unit): Unit = textElement(TextSizes.body, setup)

@ViewDsl
actual inline fun ViewWriter.subtextActual(crossinline setup: TextView.() -> Unit): Unit = textElement(TextSizes.subtext,
    { opacity = 0.8; setup(this) })
actual var TextView.content: String
    get() {
        return native.text.toString()
    }
    set(value) {
        native.text = value
    }
actual var TextView.align: Align
    get() {
        return when (native.gravity) {
            Gravity.START -> Align.Start
            Gravity.END -> Align.End
            Gravity.CENTER -> Align.Center
            Gravity.CENTER_VERTICAL -> Align.Start
            Gravity.CENTER_HORIZONTAL -> Align.Center
            else -> Align.Start
        }
    }
    set(value) {
        when (value) {
            Align.Start -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_START
            Align.End -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_END
            Align.Center -> native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_CENTER
            Align.Stretch -> {
                native.textAlignment = android.widget.TextView.TEXT_ALIGNMENT_TEXT_START
                native.updateLayoutParams<ViewGroup.LayoutParams> {
                    this.width = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }
        }
    }
actual var TextView.textSize: Dimension
    get() {
        return Dimension(native.textSize)
    }
    set(value) {
        native.setTextSize(TypedValue.COMPLEX_UNIT_PX, value.value.toFloat())
    }
val CompoundButton.checked: Writable<Boolean>
    get() {
        return object : EquatableByRef("checked", this), Writable<Boolean> {
            override fun addListener(listener: () -> Unit): () -> Unit =
                addListener(CompoundButton::setOnCheckedChangeListener, { CompoundButton.OnCheckedChangeListener { _, _ -> listener() } }, listener)

            override val state get() = ReadableState(this@checked.isChecked)
            override suspend fun set(value: Boolean) { this@checked.isChecked = value }
        }
    }
val android.widget.TextView.content: Writable<String>
    get() {
        return object : EquatableByRef("content", this), Writable<String> {
            override fun addListener(listener: () -> Unit): () -> Unit {
                val watcher = object: TextWatcher {
                    override fun afterTextChanged(s: Editable?) { listener() }
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                }
                addTextChangedListener(watcher)
                return { removeTextChangedListener(watcher) }
            }
            override val state get() = ReadableState(this@content.text?.toString() ?: "")
            override suspend fun set(value: String) { this@content.text = value }
        }
    }

abstract class EquatableByRef(val key: String, val ref: Any) {
    override fun hashCode(): Int = key.hashCode() + ref.hashCode()
    override fun equals(other: Any?): Boolean = other is EquatableByRef && this.key == other.key && this.ref == other.ref
}