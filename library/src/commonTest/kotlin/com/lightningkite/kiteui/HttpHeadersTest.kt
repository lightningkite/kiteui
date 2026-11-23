package com.lightningkite.kiteui

import kotlin.test.Test
import kotlin.test.assertEquals

class HttpHeadersTest {
    @Test
    fun toMapLowercasesNames() {
        assertEquals(mapOf("x-kiteui-auth" to listOf("token")), HttpHeaders("X-Kiteui-Auth" to "token").toMap())
    }

    @Test
    fun plusTakesTheRightSideForSharedNamesAndKeepsTheRest() {
        val combined = HttpHeaders("Accept" to "text/plain", "X-Left" to "left") + HttpHeaders("accept" to "application/json")
        assertEquals("application/json", combined.get("Accept"))
        assertEquals("left", combined.get("X-Left"))
    }

    @Test
    fun plusLeavesBothSidesUnchanged() {
        val left = HttpHeaders("Accept" to "text/plain")
        val right = HttpHeaders("X-Right" to "right")
        left + right
        assertEquals(mapOf("accept" to listOf("text/plain")), left.toMap())
        assertEquals(mapOf("x-right" to listOf("right")), right.toMap())
    }
}
