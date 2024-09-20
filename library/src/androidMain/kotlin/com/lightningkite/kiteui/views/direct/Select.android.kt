package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

actual class Select actual constructor(context: RContext): RView(context) {
    override val native = Spinner(context.activity).apply {
        minimumHeight = 0
    }

    override fun applyForeground(theme: Theme) {
        super.applyForeground(theme)
        native.setPopupBackgroundDrawable(theme.backgroundDrawableWithoutCorners(null).apply { cornerRadius = 8.dp.value })
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun hasAlternateBackedStates(): Boolean = true
    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme
        if(!enabled) t = t[DisabledSemantic]
        return t
    }

    override fun applyPadding(dimension: Dimension?) {
        // padding handled by inner
        native.setPaddingAll(0)
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        val layerDrawable = background as? LayerDrawable ?: LayerDrawable(arrayOf(null, null))

        layerDrawable.setDrawable(0, getBackgroundWithRipple(theme, fullyApply, layerDrawable?.getDrawable(0) as? RippleDrawable))
        layerDrawable.setDrawable(1, ResourcesCompat.getDrawable(native.resources, R.drawable.baseline_arrow_drop_down_24, null)?.apply {
            colorFilter = PorterDuffColorFilter(theme.foreground.closestColor().toInt(), PorterDuff.Mode.SRC_IN)
        })
        layerDrawable.setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
        layerDrawable.setLayerInsetEnd(1, theme.spacing.value.toInt())

        background = layerDrawable
    }

    actual fun <T> bind(
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
                    var newView: RView? = null
                    val w = object: ViewWriter(), CalculationContext by this@Select {
                        override val context: RContext
                            get() = this@Select.context

                        override fun addChild(view: RView) {
                            view.parent = this@Select
                            newView = view
                        }
                    }
                    with(w) {
                        padded - text {
                            content = render(list[position])
                        }
                    }
                    return newView!!.native.also {
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
            list = data()
            adapter.notifyDataSetChanged()
            val currentlySelected = edits.once()
            val index = list.indexOf(currentlySelected)
            if (index != -1 && !suppressChange) {
                suppressChange = true
                native.setSelection(index)
                suppressChange = false
            }
        }
        reactiveScope {
            val currentlySelected = edits()
            val index = list.indexOf(currentlySelected)
            if (index != -1 && !suppressChange) {
                suppressChange = true
                native.setSelection(index)
                suppressChange = false
            }
        }
    }
}


//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual class NSelect(context: Context): AppCompatSpinner(context) {
//    lateinit var viewWriter: ViewWriter
//}
//
//actual fun <T> Select.bind(
//    edits: Writable<T>,
//    data: Readable<List<T>>,
//    render: (T) -> String
//) {
//    var suppressChange = false
//    var list: List<T> = listOf()
//    val adapter = object: BaseAdapter() {
//        override fun getCount(): Int = list.size
//        override fun getItem(position: Int): Any? = list.get(position)
//        override fun getItemId(position: Int): Long = position.toLong()
//        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
//            if(convertView != null) {
//                (convertView as TextView).text = render(list[position])
//                return convertView
//            } else {
//                with(native.viewWriter) {
//                    padded - text {
//                        content = render(list[position])
//                    }
//                }
//                return native.viewWriter.rootCreated!!.also {
//                    it.layoutParams = ViewGroup.LayoutParams(
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                        ViewGroup.LayoutParams.MATCH_PARENT,
//                    )
//                }
//            }
//        }
//    }
//    native.adapter = adapter
//    native.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
//        override fun onNothingSelected(parent: AdapterView<*>?) {
//
//        }
//
//        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//            if(!suppressChange) {
//                launch {
//                    suppressChange = true
//                    edits set list[position]
//                    suppressChange = false
//                }
//            }
//        }
//    }
//    reactiveScope {
//        list = data.await()
//        adapter.notifyDataSetChanged()
//        val currentlySelected = edits.awaitOnce()
//        val index = list.indexOf(currentlySelected)
//        if(index != -1 && !suppressChange) {
//            suppressChange = true
//            native.setSelection(index)
//            suppressChange = false
//        }
//    }
//    reactiveScope {
//        val currentlySelected = edits.await()
//        val index = list.indexOf(currentlySelected)
//        if(index != -1 && !suppressChange) {
//            suppressChange = true
//            native.setSelection(index)
//            suppressChange = false
//        }
//    }
//}
//
//@ViewDsl
//actual fun ViewWriter.selectActual(setup: Select.() -> Unit) {
//    return viewElement(factory = ::NSelect, wrapper = ::Select, setup = {
//        native.viewWriter = newViews()
//        native.minimumHeight = 0
//        setup(this)
//        handleThemeControl(native, viewLoads = true, customDrawable = {
//            // LayerDrawable has poor interfaces for dynamically adding layers, so we have to do this to be able to
//            // safely call setDrawable(1, ...) later
//            if (numberOfLayers < 2) {
//                addLayer(null)
//            }
//
//            val dropdown = ResourcesCompat.getDrawable(native.resources, R.drawable.baseline_arrow_drop_down_24, null)
//            dropdown?.colorFilter = PorterDuffColorFilter(it.foreground.closestColor().toInt(), PorterDuff.Mode.SRC_IN)
//
//            setDrawable(1, dropdown)
//            setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
//            setLayerInsetEnd(1, it.spacing.value.toInt())
//        }, foreground = { theme, nselect -> nselect.setPaddingAll(0) }, background = {
//            native.setPopupBackgroundDrawable(it.backgroundDrawable(8.dp.value, true))
//        }) {
//            native.viewWriter = newViews()
//            setup(this)
//        }
//    })
//}
