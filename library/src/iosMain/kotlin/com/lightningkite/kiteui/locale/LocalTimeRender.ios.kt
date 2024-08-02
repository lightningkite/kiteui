package com.lightningkite.kiteui.locale

import kotlinx.datetime.*
import platform.Foundation.*

actual fun LocalDate.renderToString(
    size: RenderSize,
    includeWeekday: Boolean,
    includeYear: Boolean,
    includeEra: Boolean
): String {
    return NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        NSDateFormatter.dateFormatFromTemplate(
            when(size){
                RenderSize.Numerical -> buildString {
                    if(includeYear) append("y")
                    if(includeWeekday) append("EEEEEE")
                    if(includeEra) append("G")
                    append("Md")
                }
                RenderSize.Abbreviation -> buildString {
                    if(includeYear) append("y")
                    if(includeWeekday) append("E")
                    if(includeEra) append("G")
                    append("MMMd")
                }
                RenderSize.Full -> buildString {
                    if(includeYear) append("y")
                    if(includeWeekday) append("EEEE")
                    if(includeEra) append("G")
                    append("MMMMd")
                }
            },
            0UL,
            NSLocale.currentLocale
        )?.let { dateFormat = it } ?: run {
            dateStyle = when (size) {
                RenderSize.Numerical -> NSDateFormatterShortStyle
                RenderSize.Abbreviation -> NSDateFormatterMediumStyle
                RenderSize.Full -> NSDateFormatterLongStyle
            }
            timeStyle = NSDateFormatterNoStyle
        }
    }.stringFromDate(this.atTime(12, 0, 0).toInstant(TimeZone.currentSystemDefault()).toNSDate())
}

actual fun LocalTime.renderToString(size: RenderSize): String {
    return NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        NSDateFormatter.dateFormatFromTemplate(
            "hma",
            0UL,
            NSLocale.currentLocale
        )?.let { dateFormat = it } ?: run {
            timeStyle = when (size) {
                RenderSize.Numerical -> NSDateFormatterShortStyle
                RenderSize.Abbreviation -> NSDateFormatterMediumStyle
                RenderSize.Full -> NSDateFormatterLongStyle
            }
            dateStyle = NSDateFormatterNoStyle
        }
    }.stringFromDate(this.atDate(Clock.System.todayIn(TimeZone.currentSystemDefault())).toInstant(TimeZone.currentSystemDefault()).toNSDate())
}
actual fun LocalDateTime.renderToString(
    size: RenderSize,
    includeWeekday: Boolean,
    includeYear: Boolean,
    includeEra: Boolean
): String {
    return NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        NSDateFormatter.dateFormatFromTemplate(
            when(size){
                RenderSize.Numerical -> buildString {
                    if(includeYear) append("y")
                    if(includeWeekday) append("EEEEEE")
                    if(includeEra) append("G")
                    append("Mdhma")
                }
                RenderSize.Abbreviation -> buildString {
                    if(includeYear) append("y")
                    if(includeWeekday) append("E")
                    if(includeEra) append("G")
                    append("MMMdhma")
                }
                RenderSize.Full -> buildString {
                    if(includeYear) append("y")
                    if(includeWeekday) append("EEEE")
                    if(includeEra) append("G")
                    append("MMMMdhma")
                }
            },
            0UL,
            NSLocale.currentLocale
        )?.let { dateFormat = it } ?: run {
            dateStyle = when (size) {
                RenderSize.Numerical -> NSDateFormatterShortStyle
                RenderSize.Abbreviation -> NSDateFormatterMediumStyle
                RenderSize.Full -> NSDateFormatterLongStyle
            }
            timeStyle = when (size) {
                RenderSize.Numerical -> NSDateFormatterShortStyle
                RenderSize.Abbreviation -> NSDateFormatterMediumStyle
                RenderSize.Full -> NSDateFormatterLongStyle
            }
        }
    }.stringFromDate(this.toInstant(TimeZone.currentSystemDefault()).toNSDate())
}

actual fun TimeZone.renderToString(size: RenderSize): String = this.id
actual fun DayOfWeek.renderToString(size: RenderSize): String = NSDateFormatter().apply {
    locale = NSLocale.currentLocale
}.let {
    when(size) {
        RenderSize.Numerical -> (it.veryShortWeekdaySymbols[this.isoDayNumber] as NSString) as String
        RenderSize.Abbreviation -> (it.shortWeekdaySymbols[this.isoDayNumber] as NSString) as String
        RenderSize.Full -> (it.weekdaySymbols[this.isoDayNumber] as NSString) as String
    }
}