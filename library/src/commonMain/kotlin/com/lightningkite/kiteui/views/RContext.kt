package com.lightningkite.kiteui.views

public expect class RContext: RContextHelper {
    public fun split(): RContext
    public override val darkMode: Boolean?
}
public abstract class RContextHelper {
    public val addons: HashMap<String, Any?> = HashMap<String, Any?>()  // TODO: Use record
    public abstract val darkMode: Boolean?
}