package com.lightningkite.kiteui

@Repeatable
@Target(AnnotationTarget.CLASS)
annotation class Routable(val path: String)

@Target(AnnotationTarget.CLASS)
annotation class FallbackRoute

@Target(AnnotationTarget.PROPERTY)
annotation class QueryParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
annotation class Hash

@Suppress
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.BINARY)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This may change, use it at your own risk"
)

annotation class InternalKiteUi
