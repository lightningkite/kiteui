package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.TextView
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.Spinner
import androidx.appcompat.widget.AppCompatSpinner
import androidx.core.content.res.ResourcesCompat
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.dp
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import java.util.*
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.awaitOnce
import com.lightningkite.kiteui.views.*

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual class NSelect(context: Context): AppCompatSpinner(context) {
    lateinit var viewWriter: ViewWriter
}

actual fun <T> Select.bind(
    edits: Writable<T>,
    data: Readable<List<T>>,
    render: (T) -> String
) {
    var suppressChange = false
    var list: List<T> = listOf()
    val adapter = object: BaseAdapter() {
        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any? = list.get(position)
        override fun getItemId(position: Int): Long = position.toLong()
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            if(convertView != null) {
                (convertView as TextView).text = render(list[position])
                return convertView
            } else {
                with(native.viewWriter) {
                    padded - text {
                        content = render(list[position])
                    }
                }
                return native.viewWriter.rootCreated!!.also {
                    it.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            }
        }
    }
    native.adapter = adapter
    native.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) {

        }

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if(!suppressChange) {
                launch {
                    suppressChange = true
                    edits set list[position]
                    suppressChange = false
                }
            }
        }
    }
    reactiveScope {
        list = data.await()
        adapter.notifyDataSetChanged()
        val currentlySelected = edits.awaitOnce()
        val index = list.indexOf(currentlySelected)
        if(index != -1 && !suppressChange) {
            suppressChange = true
            native.setSelection(index)
            suppressChange = false
        }
    }
    reactiveScope {
        val currentlySelected = edits.await()
        val index = list.indexOf(currentlySelected)
        if(index != -1 && !suppressChange) {
            suppressChange = true
            native.setSelection(index)
            suppressChange = false
        }
    }
}

@ViewDsl
actual fun ViewWriter.selectActual(setup: Select.() -> Unit) {
    return viewElement(factory = ::NSelect, wrapper = ::Select, setup = {
        native.viewWriter = newViews()
        native.minimumHeight = 0
        setup(this)
        handleThemeControl(native, viewLoads = true, customDrawable = {
            // LayerDrawable has poor interfaces for dynamically adding layers, so we have to do this to be able to
            // safely call setDrawable(1, ...) later
            if (numberOfLayers < 2) {
                addLayer(null)
            }

            val dropdown = ResourcesCompat.getDrawable(native.resources, R.drawable.baseline_arrow_drop_down_24, null)
            dropdown?.colorFilter = PorterDuffColorFilter(it.foreground.closestColor().toInt(), PorterDuff.Mode.SRC_IN)

            setDrawable(1, dropdown)
            setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
            setLayerInsetEnd(1, it.spacing.value.toInt())
        }, foreground = { theme, nselect -> nselect.setPaddingAll(0) }, background = {
            native.setPopupBackgroundDrawable(it.backgroundDrawable(8.dp.value, true))
        }) {
            native.viewWriter = newViews()
            setup(this)
        }
    })
}
