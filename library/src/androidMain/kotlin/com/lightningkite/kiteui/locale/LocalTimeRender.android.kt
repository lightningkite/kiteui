package com.lightningkite.kiteui.locale

import kotlinx.datetime.*
import java.text.DateFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

public actual fun LocalDate.renderToString(
    size: RenderSize,
    includeWeekday: Boolean,
    includeYear: Boolean,
    includeEra: Boolean
): String {
    return DateTimeFormatter.ofLocalizedDate(when(size) {
        RenderSize.Numerical -> FormatStyle.SHORT
        RenderSize.Abbreviation -> FormatStyle.MEDIUM
        RenderSize.Full -> FormatStyle.LONG
    }).format(this.atTime(LocalTime(12, 0)).toJavaLocalDateTime().atZone(ZoneId.systemDefault()))
}

public actual fun LocalTime.renderToString(size: RenderSize): String  {
    return DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(this.atDate(LocalDate(1970, 1, 1)).toJavaLocalDateTime().atZone(ZoneId.systemDefault()))
}
public actual fun LocalDateTime.renderToString(
    size: RenderSize,
    includeWeekday: Boolean,
    includeYear: Boolean,
    includeEra: Boolean
): String {
    return DateTimeFormatter.ofLocalizedDateTime(when(size) {
        RenderSize.Numerical -> FormatStyle.SHORT
        RenderSize.Abbreviation -> FormatStyle.MEDIUM
        RenderSize.Full -> FormatStyle.LONG
    }, FormatStyle.SHORT).format(this.toJavaLocalDateTime().atZone(ZoneId.systemDefault()))
}

public actual fun TimeZone.renderToString(size: RenderSize): String = this.toString()
public actual fun DayOfWeek.renderToString(size: RenderSize): String = this.toString()