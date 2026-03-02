// by Claude - end-to-end tests for MockExternalServices
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.GeolocationResult
import com.lightningkite.kiteui.MockExternalServices
import com.lightningkite.kiteui.createFileReferenceFromBytes
import com.lightningkite.kiteui.testing.JUnitRunWith
import com.lightningkite.kiteui.testing.RobolectricTestRunner
import com.lightningkite.kiteui.testing.UiTestConfig
import com.lightningkite.kiteui.testing.UiTestScope
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.mppexampleapp.internal.MockExternalServicesTestPage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@JUnitRunWith(RobolectricTestRunner::class)
class MockExternalServicesUiTest {

    private fun mockTest(
        setupMock: MockExternalServices.() -> Unit = {},
        block: suspend UiTestScope.() -> Unit
    ) {
        val mock = MockExternalServices()
        mock.setupMock()
        uiTest(
            config = UiTestConfig(externalServices = mock),
            content = { with(MockExternalServicesTestPage) { render() } },
            block = block
        )
    }

    @Test
    fun filePickWithNoMockReturnsNull() = mockTest {
        // No mock file queued — requestFile returns null from delegate (null)
        click("pickFile")
        // Verify the call was recorded
        val mock = mockExternalServices!!
        val call = mock.calls.filterIsInstance<MockExternalServices.Call.RequestFile>().single()
        assertEquals(listOf("image/*"), call.mimeTypes)
    }

    @Test
    fun filePickReturnsMockFile() = mockTest(
        setupMock = {
            pendingFileResponses.add(
                createFileReferenceFromBytes("hello".encodeToByteArray(), "image/png", "test-photo.png")
            )
        }
    ) {
        click("pickFile")
        // Verify the call was recorded with correct mime types
        val mock = mockExternalServices!!
        val requestCall = mock.calls.filterIsInstance<MockExternalServices.Call.RequestFile>().single()
        assertEquals(listOf("image/*"), requestCall.mimeTypes)
        // Verify the pending response was consumed
        assertTrue(mock.pendingFileResponses.isEmpty(), "Mock file should have been consumed")
    }

    @Test
    fun geolocationReturnsMockPosition() = mockTest(
        setupMock = {
            pendingGeolocation.add(GeolocationResult(40.0, -111.0, 5.0))
        }
    ) {
        click("getLocation")
        // Verify the call was recorded with the correct result
        val mock = mockExternalServices!!
        val call = mock.calls.filterIsInstance<MockExternalServices.Call.GetCurrentPosition>().single()
        assertNotNull(call.result, "GetCurrentPosition should have returned the mock result")
        assertEquals(40.0, call.result!!.latitude)
        assertEquals(-111.0, call.result!!.longitude)
        // Verify the pending response was consumed
        assertTrue(mock.pendingGeolocation.isEmpty(), "Mock geolocation should have been consumed")
    }

    @Test
    fun geolocationWithNoMockThrows() = mockTest {
        // No mock queued and no delegate — getCurrentPosition throws
        click("getLocation")
        val mock = mockExternalServices!!
        val call = mock.calls.filterIsInstance<MockExternalServices.Call.GetCurrentPosition>().single()
        // Result is null because no mock was queued
        assertEquals(null, call.result)
    }

    @Test
    fun openLinkRecorded() = mockTest {
        click("openLink")
        val mock = mockExternalServices!!
        val call = mock.calls.filterIsInstance<MockExternalServices.Call.OpenLink>().single()
        assertEquals("https://example.com", call.url)
    }

    @Test
    fun clipboardRecorded() = mockTest {
        click("setClipboard")
        val mock = mockExternalServices!!
        val call = mock.calls.filterIsInstance<MockExternalServices.Call.SetClipboardText>().single()
        assertEquals("copied", call.text)
    }

    @Test
    fun mockExternalServicesAccessorWorks() = mockTest {
        assertNotNull(mockExternalServices, "mockExternalServices should be accessible from UiTestScope")
        assertIs<MockExternalServices>(mockExternalServices)
    }
}
