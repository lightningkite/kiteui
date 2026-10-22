package com.lightningkite.kiteui

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

// Times are written in UTC: a TZID would need a matching VTIMEZONE block, and Kotlin/JS often only knows the "SYSTEM" zone.
@OptIn(ExperimentalUuidApi::class)
internal fun icsEvent(title: String, description: String, location: String, start: Instant, end: Instant, now: Instant): String {
    val utc = LocalDateTime.Format { year(); monthNumber(); day(); char('T'); hour(); minute(); second(); char('Z') }
    fun Instant.format() = toLocalDateTime(TimeZone.UTC).format(utc)
    fun String.escaped() = replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
    return listOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:-//Lightning Kite//KiteUI//EN",
        "METHOD:PUBLISH",
        "BEGIN:VEVENT",
        "UID:${Uuid.random()}",
        "DTSTAMP:${now.format()}",
        "DTSTART:${start.format()}",
        "DTEND:${end.format()}",
        "SUMMARY:${title.escaped()}",
        "DESCRIPTION:${description.escaped()}",
        "LOCATION:${location.escaped()}",
        "END:VEVENT",
        "END:VCALENDAR",
    ).joinToString("\r\n", postfix = "\r\n")
}
