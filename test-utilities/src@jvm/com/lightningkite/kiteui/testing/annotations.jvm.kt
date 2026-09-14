package com.lightningkite.kiteui.testing

import kotlin.reflect.KClass

/**
 * JS actual - no-op annotation (JUnit/Robolectric only on Android).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
actual annotation class JUnitRunWith(actual val value: KClass<out JUnitRunner>)

actual abstract class JUnitRunner

actual open class Application

/**
 * JS actual - no-op class (Robolectric only on Android).
 */
actual class RobolectricTestRunner: JUnitRunner()

/**
 * JS actual - no-op annotation (Robolectric config only on Android).
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
actual annotation class RobolectricConfig(
)

@MustBeDocumented
@Retention(value = AnnotationRetention.RUNTIME)
@Target(allowedTargets = [AnnotationTarget.FILE, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER])
actual annotation class GraphicsMode actual constructor(actual val value: GraphicsModeEnum)
actual enum class GraphicsModeEnum { LEGACY, NATIVE }