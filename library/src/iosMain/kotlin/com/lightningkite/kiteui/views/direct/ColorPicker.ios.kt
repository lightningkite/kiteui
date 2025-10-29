package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import platform.UIKit.*
import platform.CoreGraphics.*
import platform.darwin.*
import kotlinx.cinterop.*


actual class ColorPicker actual constructor(context: RContext) : RView(context) {
    override val native: WrapperView = WrapperView()
    private val button = FrameLayoutButton()

    private val _color = Signal(Color.white)
    actual val color: MutableReactiveValue<Color> = _color

    actual var enabled: Boolean
        get() = button.enabled
        set(value) {
            button.enabled = value
        }

    init {
        button.addSubview(UILabel().apply {
            text = "Pick Color"
            textAlignment = NSTextAlignmentCenter
        })
        native.addSubview(button)

        // Update button background when color changes
        onRemove(_color.addListener {
            val currentColor = _color.value
            button.backgroundColor = UIColor(
                red = currentColor.red.toDouble(),
                green = currentColor.green.toDouble(),
                blue = currentColor.blue.toDouble(),
                alpha = currentColor.alpha.toDouble()
            )
        })

        // Initial background color
        val initialColor = _color.value
        button.backgroundColor = UIColor(
            red = initialColor.red.toDouble(),
            green = initialColor.green.toDouble(),
            blue = initialColor.blue.toDouble(),
            alpha = initialColor.alpha.toDouble()
        )

        onRemove(button.setOnClick {
            if (!enabled) return@setOnClick

            val colorPicker = UIColorPickerViewController()
            colorPicker.selectedColor = UIColor(
                red = _color.value.red.toDouble(),
                green = _color.value.green.toDouble(),
                blue = _color.value.blue.toDouble(),
                alpha = _color.value.alpha.toDouble()
            )

            colorPicker.delegate = object : NSObject(), UIColorPickerViewControllerDelegateProtocol {
                override fun colorPickerViewControllerDidSelectColor(viewController: UIColorPickerViewController) {
                    val selectedColor = viewController.selectedColor
                    memScoped {
                        val r = alloc<CGFloatVar>()
                        val g = alloc<CGFloatVar>()
                        val b = alloc<CGFloatVar>()
                        val a = alloc<CGFloatVar>()

                        // Use getRed method to extract RGBA components
                        selectedColor.getRed(r.ptr, g.ptr, b.ptr, a.ptr)

                        _color.value = Color(
                            red = r.value.toFloat(),
                            green = g.value.toFloat(),
                            blue = b.value.toFloat(),
                            alpha = a.value.toFloat()
                        )
                    }
                }

                override fun colorPickerViewControllerDidFinish(viewController: UIColorPickerViewController) {
                    viewController.dismissViewControllerAnimated(true, null)
                }
            }

            context.controller.presentViewController(colorPicker, true, null)
        })
    }
}
