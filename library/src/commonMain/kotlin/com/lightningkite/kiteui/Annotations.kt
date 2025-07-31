package com.lightningkite.kiteui

@Repeatable
@Target(AnnotationTarget.CLASS)
public annotation class Routable(val path: String)

@Target(AnnotationTarget.CLASS)
public annotation class FallbackRoute

@Target(AnnotationTarget.PROPERTY)
public annotation class QueryParameter(val name: String = "")

@Target(AnnotationTarget.PROPERTY)
public annotation class Hash

@Target(AnnotationTarget.CLASS)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This may change, use it at your own risk"
)

public annotation class InternalKiteUi
