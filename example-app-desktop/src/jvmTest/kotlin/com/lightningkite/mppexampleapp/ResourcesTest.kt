package com.lightningkite.mppexampleapp

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ResourcesTest {
    @Test
    fun binaryResourceLoads() {
        val blob = runBlocking { Resources.imagesGifTest() }
        assertEquals("GIF", blob.data.copyOfRange(0, 3).decodeToString())
        assertEquals("image/gif", blob.type)
    }
}
