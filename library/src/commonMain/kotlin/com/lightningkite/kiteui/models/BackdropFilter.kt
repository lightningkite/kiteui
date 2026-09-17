package com.lightningkite.kiteui.models

public sealed interface BackdropFilter {
    public class Blur(public val amount: Dimension): BackdropFilter
}