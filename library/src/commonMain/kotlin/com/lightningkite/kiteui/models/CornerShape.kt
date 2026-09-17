package com.lightningkite.kiteui.models

/**
 * Controls the curvature style of rounded corners.
 *
 * [Circular] uses standard circular arcs (CSS border-radius, default everywhere).
 * [Continuous] uses iOS-style superellipse/squircle curves that flow more smoothly
 * into the straight edges. On platforms without native support, the radius is
 * reduced to approximate the visual appearance.
 */
public enum class CornerShape {
    Circular,
    Continuous
}
