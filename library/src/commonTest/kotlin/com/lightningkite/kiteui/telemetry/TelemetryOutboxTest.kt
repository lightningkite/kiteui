package com.lightningkite.kiteui.telemetry

import com.lightningkite.services.telemetry.LogLevel
import com.lightningkite.services.telemetry.RecordedLog
import com.lightningkite.services.telemetry.TelemetryAttributes
import com.lightningkite.services.telemetry.TelemetryBatch
import com.lightningkite.services.telemetry.TelemetryKeys
import com.lightningkite.services.telemetry.telemetryAttributesOf
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

class TelemetryOutboxTest {
    private val resource = telemetryAttributesOf(TelemetryKeys.Service.name to "test")
    private fun log(message: String) = RecordedLog(Clock.System.now(), LogLevel.Warn, message, TelemetryAttributes.empty)

    @BeforeTest
    fun clearStorage() = clearStoredTelemetry()

    @Test
    fun uploadsOldestFirstAndForgetsWhatWasSent() = runTest {
        val sent = ArrayList<TelemetryBatch>()
        val outbox = TelemetryOutbox(resource) { sent += it }
        outbox.add(log("first"))
        outbox.persist()
        outbox.add(log("second"))
        outbox.persist()

        outbox.upload()

        assertEquals(listOf("first", "second"), sent.map { it.logs.single().message })
        assertEquals("test", sent.first().resource[TelemetryKeys.Service.name])
        assertTrue(outbox.batches.isEmpty())
    }

    @Test
    fun failedUploadKeepsTheBatchAndBacksOff() = runTest {
        var attempts = 0
        val outbox = TelemetryOutbox(resource) { attempts++; throw Exception("offline") }
        outbox.add(log("kept"))
        outbox.persist()

        outbox.upload()
        outbox.upload() // within the backoff window: not attempted

        assertEquals(1, attempts)
        assertEquals("kept", outbox.batches.single().logs.single().message)
    }

    @Test
    fun oversizedOutboxDropsOldestAndCountsTheDrop() = runTest {
        val outbox = TelemetryOutbox(resource) {}
        val big = "x".repeat(TelemetryOutbox.MAX_STORED_CHARS / 3)
        repeat(4) {
            outbox.add(log(big))
            outbox.persist()
        }
        assertTrue(outbox.batches.size < 4, "oldest batches should have been dropped")

        outbox.add(log("after"))
        outbox.persist()
        val dropped = outbox.batches.flatMap { it.logs }.filter { it.message.startsWith("Dropped ") }
        assertTrue(dropped.isNotEmpty(), "the drop is reported as a log")
    }

    @Test
    fun persistingNothingAddsNoBatch() {
        val outbox = TelemetryOutbox(resource) {}
        outbox.persist()
        assertTrue(outbox.batches.isEmpty())
    }
}
