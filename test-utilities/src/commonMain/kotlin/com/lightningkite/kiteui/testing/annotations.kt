package com.lightningkite.kiteui.testing

import kotlin.reflect.KClass

/**
 * Cross-platform test runner annotation.
 * On Android, maps to JUnit's @RunWith annotation.
 * On other platforms, this is a no-op.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
expect annotation class JUnitRunWith(val value: KClass<out JUnitRunner>)

expect abstract class JUnitRunner

expect open class Application

/**
 * Cross-platform Robolectric test runner reference.
 * On Android, maps to org.robolectric.RobolectricTestRunner.
 * On other platforms, this is a simple marker class.
 */
expect class RobolectricTestRunner: JUnitRunner

/**
 * Cross-platform Robolectric configuration annotation.
 * On Android, maps to @Config with SDK configuration.
 * On other platforms, this is a no-op.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class RobolectricConfig(
//    val sdk: IntArray,
//    val minSdk: Int,
//    val maxSdk: Int,
//    val fontScale: Float,
//    val manifest: String,
//    val application: KClass<out Application>,
//    val qualifiers: String,
//    val shadows: Array<KClass<*>>,
//    val instrumentedPackages: Array<String>
)

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.FILE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
expect annotation class GraphicsMode(val value: GraphicsModeEnum) {
}

expect enum class GraphicsModeEnum {
    LEGACY,
    NATIVE,
}