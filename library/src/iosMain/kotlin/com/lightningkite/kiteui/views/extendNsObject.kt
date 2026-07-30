@file:OptIn(ExperimentalNativeApi::class)

package com.lightningkite.kiteui.views


import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Edges
import com.lightningkite.kiteui.models.FontAndStyle
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.experimental.ExperimentalNativeApi
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSString
import platform.Foundation.NSValue
import platform.Foundation.UTF8String
import platform.Foundation.valueWithPointer
import platform.UIKit.UIView
import platform.darwin.NSObject

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
public class ExtensionProperty<A: NSObject, B>(): ReadWriteProperty<A, B?> {

    // UTF8String's pointer is only valid as long as the NSString it came from is kept alive,
    // so we retain keyString here rather than letting it be a throwaway temporary.
    private val keyString: NSString = Random.nextLong().toString() as NSString
    public val key: NSValue = NSValue.valueWithPointer(keyString.UTF8String)
    override fun getValue(thisRef: A, property: KProperty<*>): B? = getValue(thisRef)
    override fun setValue(thisRef: A, property: KProperty<*>, value: B?): Unit = setValue(thisRef, value)
    
    @Suppress("UNCHECKED_CAST")
    public fun getValue(thisRef: A): B? = com.lightningkite.kiteui.objc.getAssociatedObjectWithKey(thisRef, key) as? B
    
    public fun setValue(thisRef: A, value: B?): Unit = com.lightningkite.kiteui.objc.setAssociatedObjectWithKey(thisRef, key, value)
    public companion object {
        public fun debug() {}
    }
}

private val UIViewExplicitlyNeedsLayout = ExtensionProperty<UIView, Boolean>()
public var UIView.explicitlyNeedsLayout: Boolean? by UIViewExplicitlyNeedsLayout

private val UIViewIgnoreInteraction = ExtensionProperty<UIView, Boolean>()
public var UIView.extensionIgnoreInteraction: Boolean? by UIViewIgnoreInteraction

private val UIViewWeight = ExtensionProperty<UIView, Float>()
public var UIView.extensionWeight: Float? by UIViewWeight

private val UIViewSpacingBeforeOverride = ExtensionProperty<UIView, Dimension>()
public var UIView.extensionSpacingBeforeOverride: Dimension? by UIViewSpacingBeforeOverride

private val UIViewPadding = ExtensionProperty<UIView, Edges>()
public var UIView.extensionPadding: Edges? by UIViewPadding

private val UIViewSafeInsetPadding = ExtensionProperty<UIView, Edges>()
public var UIView.extensionSafeInsetPadding: Edges? by UIViewSafeInsetPadding

private val UIViewSizeRules = ExtensionProperty<UIView, SizeConstraints>()
public var UIView.extensionSizeConstraints: SizeConstraints? by UIViewSizeRules

private val UIViewHorizontalAlign = ExtensionProperty<UIView, Align>()
public var UIView.extensionHorizontalAlign: Align? by UIViewHorizontalAlign

private val UIViewVerticalAlign = ExtensionProperty<UIView, Align>()
public var UIView.extensionVerticalAlign: Align? by UIViewVerticalAlign

private val UIViewFontAndStyle = ExtensionProperty<UIView, FontAndStyle>()
public var UIView.extensionFontAndStyle: FontAndStyle? by UIViewFontAndStyle

private val UIViewTextSize = ExtensionProperty<UIView, Double>()
public var UIView.extensionTextSize: Double? by UIViewTextSize

private val UIViewForcePadding = ExtensionProperty<UIView, Boolean>()
public var UIView.extensionForcePadding: Boolean? by UIViewForcePadding

private val UIViewCollapsed = ExtensionProperty<UIView, Boolean>()
public var UIView.extensionCollapsed: Boolean? by UIViewCollapsed

private val NSObjectStrongRefHolder = ExtensionProperty<NSObject, NSObject>()
public var NSObject.extensionStrongRef: NSObject? by NSObjectStrongRefHolder
