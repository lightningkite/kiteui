package com.lightningkite.kiteui.views.direct

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*

actual class Select actual constructor(context: ElementContext): RView(context) {
    private var _driverSelectedDisplay: String? = null
    private var _driverSelectSetValue: (suspend (String) -> Unit)? = null
    override val driverValue: String? get() = _driverSelectedDisplay
    override val driverActions get() = super.driverActions + buildMap {
        _driverSelectSetValue?.let { setter -> put("setValue") { args: List<String> -> setter(args.joinToString(" ")); "OK" } }
    }
    override val native = Spinner(context.activity).apply {
        minimumHeight = 0
        isClickable = true
    }

    actual var enabled: Boolean
        get() = native.isEnabled
        set(value) {
            native.isEnabled = value
            refreshTheming()
        }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        var t = theme[ClickableSemantic]
        if(!enabled) t = t[DisabledSemantic]
        return super.applyState(t)
    }

    override fun refreshPadding() {
        native.setPaddingAll(0)
    }

    override fun applyTheme(theme: ThemeAndBack) {
        native.setPaddingAll(0)
        native.setPopupBackgroundDrawable(theme.theme.backgroundDrawableWithoutCorners(null).apply {
            cornerRadius = 8.dp.value
            removeListener?.invoke()
            removeListener = applyGradientRadiusListener(native)
        })


        val layerDrawable = background as? LayerDrawable ?: LayerDrawable(arrayOf())

        fun setOrAddDrawable(index: Int, drawable: Drawable) {
            if(index < layerDrawable.numberOfLayers) layerDrawable.setDrawable(index, drawable)
            else layerDrawable.addLayer(drawable)
        }
        setOrAddDrawable(0, getBackgroundWithRipple(theme.theme, theme.drawBackground, layerDrawable.takeIf { it.numberOfLayers >= 1 }?.getDrawable(0) as? RippleDrawable))
        ResourcesCompat.getDrawable(native.resources, R.drawable.baseline_arrow_drop_down_24, null)?.apply {
            colorFilter = PorterDuffColorFilter(theme.theme.foreground.closestColor().toInt(), PorterDuff.Mode.SRC_IN)
        }?.let {
            setOrAddDrawable(1, it)
            layerDrawable.setLayerGravity(1, Gravity.END or Gravity.CENTER_VERTICAL)
            layerDrawable.setLayerInsetEnd(1, theme.theme.gap.value.toInt())
        }
        updateCorners()

        background = layerDrawable
    }

    actual fun <T> bind(
        edits: MutableReactive<T>,
        data: Reactive<List<T>>,
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
                        override val representsView: RView = this@Select
                        override val context: ElementContext
                            get() = this@Select.context

                        override fun willAddChild(view: RView) {
                            view.parent = this@Select
                        }

                        override fun addChild(view: RView) {
                            newView = view
                        }
                    }
                    with(w) {
                        padded.text {
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

            var index = 0
            val set = Action("Set Value", Icon.send, frequencyCap = null, ignoreRetryWhileRunning = false) {
                val item = list[index]
                edits set item
            }
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if(!suppressChange) {
                    index = position
                    set.startAction(this@Select)
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
        // Driver support: track selected display and allow setValue
        reactiveScope {
            _driverSelectedDisplay = render(edits())
        }
        _driverSelectSetValue = { displayText ->
            val item = list.firstOrNull { render(it) == displayText }
                ?: throw com.lightningkite.kiteui.views.DriverActionException("No option matching '$displayText'")
            edits.set(item)
        }
    }
}


//@Suppress("ACTUAL_WITHOUT_EXPECT")
//actual class NSelect(context: Context): AppCompatSpinner(context) {
//    lateinit var viewWriter: ViewWriter
//}
//
//actual fun <T> Select.bind(
//    edits: MutableReactive<T>,
//    data: Reactive<List<T>>,
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
//            setLayerInsetEnd(1, it.gap.value.toInt())
//        }, foreground = { theme, nselect -> nselect.setPaddingAll(0) }, background = {
//            native.setPopupBackgroundDrawable(it.backgroundDrawable(8.dp.value, true))
//        }) {
//            native.viewWriter = newViews()
//            setup(this)
//        }
//    })
//}
