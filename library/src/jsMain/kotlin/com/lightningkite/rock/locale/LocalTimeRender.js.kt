package com.lightningkite.rock.locale

import kotlinx.datetime.*
import kotlin.js.Date


actual fun LocalDate.renderToString(size: RenderSize, includeWeekday: Boolean, includeYear: Boolean, includeEra: Boolean): String {
    return this.atTime(12, 0).toInstant(TimeZone.currentSystemDefault()).toJSDate().toLocaleDateString(options = dateLocaleOptions {
        this.day = "numeric"
        this.month = when(size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        }
        this.weekday = if(includeWeekday) when(size) {
            RenderSize.Numerical -> "short"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        } else null
        this.year = if(includeYear) when(size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "numeric"
            RenderSize.Full -> "numeric"
        } else null
        this.era = if(includeEra) "short" else null
    })
}
actual fun LocalTime.renderToString(size: RenderSize): String {
    return this.atDate(1970, 1, 1).toInstant(TimeZone.currentSystemDefault()).toJSDate().toLocaleTimeString(options = dateLocaleOptions {
        this.hour = "numeric"
        this.minute = "numeric"
    })
}
actual fun LocalDateTime.renderToString(size: RenderSize, includeWeekday: Boolean, includeYear: Boolean, includeEra: Boolean): String {
    return this.toInstant(TimeZone.currentSystemDefault()).toJSDate().toLocaleString(options = dateLocaleOptions {
        this.day = "numeric"
        this.month = when(size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        }
        this.year = if(includeYear) when(size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "numeric"
            RenderSize.Full -> "numeric"
        } else null
        this.weekday = if(includeWeekday) when(size) {
            RenderSize.Numerical -> "short"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        } else null
        this.era = if(includeEra) "short" else null
        this.hour = "numeric"
        this.minute = "numeric"
    })
}
actual fun Instant.renderToString(size: RenderSize, zone: TimeZone, includeWeekday: Boolean, includeYear: Boolean, includeEra: Boolean): String {
    return this.toJSDate().toLocaleString(options = dateLocaleOptions {
        this.day = "numeric"
        this.month = when(size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        }
        this.year = if(includeYear) when(size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "numeric"
            RenderSize.Full -> "numeric"
        } else null
        this.weekday = if(includeWeekday) when(size) {
            RenderSize.Numerical -> "short"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        } else null
        this.era = if(includeEra) "short" else null
        this.hour = "numeric"
        this.minute = "numeric"
    })
}
actual fun TimeZone.renderToString(size: RenderSize): String = this.id
actual fun DayOfWeek.renderToString(size: RenderSize): String = LocalDate(2023, 12, 31).plus(DatePeriod(days = this.ordinal)).atTime(12, 0).toInstant(TimeZone.currentSystemDefault()).toJSDate().toLocaleDateString(options = dateLocaleOptions {
    this.day = null
    this.weekday = when(size) {
        RenderSize.Numerical -> "short"
        RenderSize.Abbreviation -> "short"
        RenderSize.Full -> "long"
    }
    this.month = null
    this.year = null
    this.era = null
})