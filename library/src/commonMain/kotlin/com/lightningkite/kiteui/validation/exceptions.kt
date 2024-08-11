package com.lightningkite.kiteui.validation

class Invalid(val reason: String): Exception("Invalid: $reason")