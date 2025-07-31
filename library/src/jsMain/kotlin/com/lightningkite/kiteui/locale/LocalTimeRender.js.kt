package com.lightningkite.kiteui.locale

import kotlinx.datetime.*
import kotlin.js.Date


public actual fun LocalDate.renderToString(
    size: RenderSize,
    includeWeekday: Boolean,
    includeYear: Boolean,
    includeEra: Boolean
): String {
    return this.atTime(12, 0).toInstant(TimeZone.currentSystemDefault()).toJSDate()
        .toLocaleDateString(options = dateLocaleOptions {
            this.day = "numeric"
            this.month = when (size) {
                RenderSize.Numerical -> "numeric"
                RenderSize.Abbreviation -> "short"
                RenderSize.Full -> "long"
            }
            if (includeWeekday)
                this.weekday = when (size) {
                    RenderSize.Numerical -> "short"
                    RenderSize.Abbreviation -> "short"
                    RenderSize.Full -> "long"
                }
            if (includeYear)
                this.year = when (size) {
                    RenderSize.Numerical -> "numeric"
                    RenderSize.Abbreviation -> "numeric"
                    RenderSize.Full -> "numeric"
                }
            if (includeEra)
                this.era = "short"
        })
}

public actual fun LocalTime.renderToString(size: RenderSize): String {
    return this.atDate(1970, 1, 1).toInstant(TimeZone.currentSystemDefault()).toJSDate()
        .toLocaleTimeString(options = dateLocaleOptions {
            this.hour = "numeric"
            this.minute = "numeric"
        })
}

public actual fun LocalDateTime.renderToString(
    size: RenderSize,
    includeWeekday: Boolean,
    includeYear: Boolean,
    includeEra: Boolean
): String {
    return this.toInstant(TimeZone.currentSystemDefault()).toJSDate().toLocaleString(options = dateLocaleOptions {
        this.day = "numeric"
        this.month = when (size) {
            RenderSize.Numerical -> "numeric"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        }
        if (includeWeekday)
            this.weekday = when (size) {
                RenderSize.Numerical -> "short"
                RenderSize.Abbreviation -> "short"
                RenderSize.Full -> "long"
            }
        if (includeYear)
            this.year = when (size) {
                RenderSize.Numerical -> "numeric"
                RenderSize.Abbreviation -> "numeric"
                RenderSize.Full -> "numeric"
            }
        if (includeEra)
            this.era = "short"
        this.hour = "numeric"
        this.minute = "numeric"
    })
}

public actual fun TimeZone.renderToString(size: RenderSize): String = this.id
public actual fun DayOfWeek.renderToString(size: RenderSize): String =
    LocalDate(2023, 12, 31).plus(DatePeriod(days = this.ordinal)).atTime(12, 0)
        .toInstant(TimeZone.currentSystemDefault()).toJSDate().toLocaleDateString(options = dateLocaleOptions {
        this.day = undefined
        this.weekday = when (size) {
            RenderSize.Numerical -> "short"
            RenderSize.Abbreviation -> "short"
            RenderSize.Full -> "long"
        }
        this.month = undefined
        this.year = undefined
        this.era = undefined
    })