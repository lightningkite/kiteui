package com.lightningkite.kiteui.testing

import android.view.View
import android.widget.ScrollView
import android.widget.HorizontalScrollView
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.direct.TextInput

/**
 * Android implementation of Interactions.
 */
actual object Interactions {
    actual fun click(view: RView) {
        // Call performClick on the native Android View
        view.native.performClick()
    }

    actual fun typeText(view: RView, text: String, append: Boolean) {
        when (view) {
            is TextInput -> {
                val currentText = view.content.value
                view.content.value = if (append) currentText + text else text
            }
            else -> {
                // Try to find a content property via reflection
                try {
                    val contentProperty = view::class.members.find { it.name == "content" }
                    if (contentProperty != null) {
                        @Suppress("UNCHECKED_CAST")
                        val content = contentProperty.call(view) as? com.lightningkite.reactive.MutableReactiveValue<String>
                        if (content != null) {
                            val currentText = content.value
                            content.value = if (append) currentText + text else text
                        } else {
                            throw IllegalArgumentException("View does not have a MutableReactiveValue<String> content property")
                        }
                    } else {
                        throw IllegalArgumentException("View does not have a content property")
                    }
                } catch (e: Exception) {
                    throw IllegalArgumentException("Cannot type text into view of type ${view::class.simpleName}: ${e.message}", e)
                }
            }
        }
    }

    actual fun scrollBy(view: RView, dx: Int, dy: Int) {
        val native = view.native
        when (native) {
            is ScrollView -> native.scrollBy(dx, dy)
            is HorizontalScrollView -> native.scrollBy(dx, dy)
            is NestedScrollView -> native.scrollBy(dx, dy)
            else -> {
                // Try generic scrollBy
                try {
                    native.scrollBy(dx, dy)
                } catch (e: Exception) {
                    println("Warning: Could not scroll view of type ${native::class.simpleName}")
                }
            }
        }
    }

    actual fun scrollToView(scrollView: RView, targetView: RView) {
        val scrollNative = scrollView.native
        val targetNative = targetView.native

        when (scrollNative) {
            is ScrollView -> {
                scrollNative.post {
                    scrollNative.requestChildFocus(targetNative, targetNative)
                }
            }
            is NestedScrollView -> {
                scrollNative.post {
                    scrollNative.requestChildFocus(targetNative, targetNative)
                }
            }
            else -> {
                println("Warning: scrollToView not supported for ${scrollNative::class.simpleName}")
            }
        }
    }
}
