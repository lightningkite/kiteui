@file:OptIn(ExperimentalNativeApi::class)

package com.lightningkite.kiteui.views


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.reactive.Property
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSValue
import platform.Foundation.UTF8String
import platform.Foundation.valueWithPointer
import platform.UIKit.UIView
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty

//class ExtensionProperty<A: NSObject, B>: ReadWriteProperty<A, B?> {
//    companion object {
//        val storage = HashMap<Any, HashMap<ExtensionProperty<*, *>, Any?>>()
//        fun remove(key: Any) = storage.remove(key)
//        fun debug() {
//            for((key, value) in storage) {
//                if(key is UIView) {
////                    if(key.window == null) println("Warning! $key is detatched but still holds external storage")
//                }
//            }
//        }
//    }
//    override fun getValue(thisRef: A, property: KProperty<*>): B? = getValue(thisRef)
//    override fun setValue(thisRef: A, property: KProperty<*>, value: B?) = setValue(thisRef, value)
//    @Suppress("UNCHECKED_CAST")
//    fun getValue(thisRef: A): B? = storage.get(thisRef)?.get(this) as B
//    fun setValue(thisRef: A, value: B?) {
//        storage.getOrPut(thisRef) { HashMap() }.put(this, value)
//    }
//}
class ExtensionProperty<A: NSObject, B>(): ReadWriteProperty<A, B?> {
    
    val key = NSValue.valueWithPointer((Random.nextLong().toString() as NSString).UTF8String)
    override fun getValue(thisRef: A, property: KProperty<*>): B? = getValue(thisRef)
    override fun setValue(thisRef: A, property: KProperty<*>, value: B?) = setValue(thisRef, value)
    
    @Suppress("UNCHECKED_CAST")
    fun getValue(thisRef: A): B? = com.lightningkite.kiteui.objc.getAssociatedObjectWithKey(thisRef, key) as? B
    
    fun setValue(thisRef: A, value: B?) = com.lightningkite.kiteui.objc.setAssociatedObjectWithKey(thisRef, key, value)
    companion object {
        fun debug() {}
    }
}

private val UIViewWeight = ExtensionProperty<UIView, Float>()
var UIView.extensionWeight: Float? by UIViewWeight

private val UIViewPadding = ExtensionProperty<UIView, Double>()
var UIView.extensionPadding: Double? by UIViewPadding

private val UIViewSizeRules = ExtensionProperty<UIView, SizeConstraints>()
var UIView.extensionSizeConstraints: SizeConstraints? by UIViewSizeRules

private val UIViewHorizontalAlign = ExtensionProperty<UIView, Align>()
var UIView.extensionHorizontalAlign: Align? by UIViewHorizontalAlign

private val UIViewVerticalAlign = ExtensionProperty<UIView, Align>()
var UIView.extensionVerticalAlign: Align? by UIViewVerticalAlign

private val UIViewFontAndStyle = ExtensionProperty<UIView, FontAndStyle>()
var UIView.extensionFontAndStyle: FontAndStyle? by UIViewFontAndStyle

private val UIViewTextSize = ExtensionProperty<UIView, Double>()
var UIView.extensionTextSize: Double? by UIViewTextSize

private val UIViewForcePadding = ExtensionProperty<UIView, Boolean>()
var UIView.extensionForcePadding: Boolean? by UIViewForcePadding

private val UIViewCollapsed = ExtensionProperty<UIView, Boolean>()
var UIView.extensionCollapsed: Boolean? by UIViewCollapsed

private val NSObjectStrongRefHolder = ExtensionProperty<NSObject, NSObject>()
var NSObject.extensionStrongRef: NSObject? by NSObjectStrongRefHolder
