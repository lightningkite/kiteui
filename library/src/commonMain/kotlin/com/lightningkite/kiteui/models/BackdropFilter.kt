package com.lightningkite.kiteui.models

sealed interface BackdropFilter {
    class Blur(val amount: Dimension): BackdropFilter
}