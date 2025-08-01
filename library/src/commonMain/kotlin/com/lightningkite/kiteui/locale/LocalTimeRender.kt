package com.lightningkite.kiteui.locale

import kotlinx.datetime.*

public enum class RenderSize { Numerical, Abbreviation, Full }
public expect fun LocalDate.renderToString(size: RenderSize = RenderSize.Full, includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
public expect fun LocalTime.renderToString(size: RenderSize = RenderSize.Full): String
public expect fun LocalDateTime.renderToString(size: RenderSize = RenderSize.Full, includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
public fun Instant.renderToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault(), includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
    = toLocalDateTime(zone).renderToString(size, includeWeekday = includeWeekday, includeYear = includeYear, includeEra = includeEra)
public fun Instant.renderDateToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault(), includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
        = toLocalDateTime(zone).date.renderToString(size, includeWeekday = includeWeekday, includeYear = includeYear, includeEra = includeEra)
public fun Instant.renderTimeToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault()): String
    = toLocalDateTime(zone).time.renderToString(size)
public expect fun TimeZone.renderToString(size: RenderSize = RenderSize.Full): String
public expect fun DayOfWeek.renderToString(size: RenderSize = RenderSize.Full): String