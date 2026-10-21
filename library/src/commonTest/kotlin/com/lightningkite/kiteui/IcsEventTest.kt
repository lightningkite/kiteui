package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.time.Instant

class IcsEventTest {
    @Test
    fun calendarEventIsValidIcs() {
        val ics = icsEvent(
            title = "Lunch, with Sam; maybe",
            description = "Line one\nLine two",
            location = "Cafe \\ Bar",
            start = Instant.parse("2026-10-14T18:30:00Z"),
            end = Instant.parse("2026-10-14T19:30:00Z"),
            now = Instant.parse("2026-10-01T00:00:00Z"),
        )
        val lines = ics.split("\r\n")
        assertEquals("BEGIN:VCALENDAR", lines.first())
        assertEquals("", lines.last(), "ends with a line break")
        assertContains(lines, "DTSTART:20261014T183000Z")
        assertContains(lines, "DTEND:20261014T193000Z")
        assertContains(lines, "DTSTAMP:20261001T000000Z")
        assertContains(lines, "SUMMARY:Lunch\\, with Sam\\; maybe")
        assertContains(lines, "DESCRIPTION:Line one\\nLine two")
        assertContains(lines, "LOCATION:Cafe \\\\ Bar")
    }
}
