package com.lightningkite.kiteui.locale

import kotlinx.datetime.*
import kotlin.time.Instant

enum class RenderSize { Numerical, Abbreviation, Full }
expect fun LocalDate.renderToString(size: RenderSize = RenderSize.Full, includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
expect fun LocalTime.renderToString(size: RenderSize = RenderSize.Full): String
expect fun LocalDateTime.renderToString(size: RenderSize = RenderSize.Full, includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
fun Instant.renderToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault(), includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
    = toLocalDateTime(zone).renderToString(size, includeWeekday = includeWeekday, includeYear = includeYear, includeEra = includeEra)
fun Instant.renderDateToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault(), includeWeekday: Boolean = false, includeYear: Boolean = true, includeEra: Boolean = false): String
        = toLocalDateTime(zone).date.renderToString(size, includeWeekday = includeWeekday, includeYear = includeYear, includeEra = includeEra)
fun Instant.renderTimeToString(size: RenderSize = RenderSize.Full, zone: TimeZone = TimeZone.currentSystemDefault()): String
    = toLocalDateTime(zone).time.renderToString(size)
expect fun TimeZone.renderToString(size: RenderSize = RenderSize.Full): String
expect fun DayOfWeek.renderToString(size: RenderSize = RenderSize.Full): String