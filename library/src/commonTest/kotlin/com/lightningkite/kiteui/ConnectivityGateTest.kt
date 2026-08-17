package com.lightningkite.kiteui

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConnectivityGateTest {
    /**
     * The distinction the gate exists to draw: a blocked request will be refused identically on every
     * attempt, so retrying it can only spin forever.
     */
    @Test fun blockedRequestsAreNotRetried() = runTest {
        var attempts = 0
        assertFailsWith<RequestBlockedException> {
            ConnectivityGate(delay = {}).run("test") {
                attempts++
                throw RequestBlockedException("CORS")
            }
        }
        assertEquals(1, attempts)
    }

    /** Callers wanting either failure catch the base type; catching [ConnectionException] gets only retryable ones. */
    @Test fun bothFailuresShareABaseType() {
        val caught = mutableListOf<String>()
        for (e in listOf(ConnectionException("offline"), RequestBlockedException("CORS"))) {
            try {
                throw e
            } catch (f: FetchException) {
                caught.add(f::class.simpleName!!)
            }
        }
        assertEquals(listOf("ConnectionException", "RequestBlockedException"), caught)
    }
}
