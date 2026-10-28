package com.lightningkite.kiteui

import com.lightningkite.kiteui.navigation.CanBlockBack
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.ElementWriter
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import java.awt.HeadlessException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFailsWith

// jvmTest runs with java.awt.headless, as kiteUiHeadless does.
class HeadlessTest {
    private val services = DesktopExternalServices()

    private fun assertSaysWhy(attempt: String, block: () -> Unit) {
        val message = assertFailsWith<HeadlessException> { block() }.message
        assertContains(message.orEmpty(), attempt)
    }

    @Test
    fun openingALinkSaysWhyItFailed() = assertSaysWhy("open https://example.com") {
        services.openLink("https://example.com", newTab = true)
    }

    @Test
    fun fileDialogPointsAtTheDriversMock() = assertSaysWhy("mock file") {
        runBlocking { services.requestFile(listOf("*/*")) }
    }

    @Test
    fun openingAnEventSaysWhyItFailed() = assertSaysWhy("calendar") {
        val start = LocalDateTime(2026, 1, 1, 9, 0)
        services.openEvent("Meeting", "", "", start, start, TimeZone.UTC)
    }

    @Test
    fun leavingAPageWithUnsavedChangesSaysWhyItFailed() = assertSaysWhy("unsaved changes") {
        val unsaved = object : Page, CanBlockBack {
            override fun ElementWriter.CanAddTheme.render() {}
            override fun onNavigateAwayAttempt() = false
        }
        val navigator = PageNavigator { Routes(listOf(), mapOf()) }
        navigator.stack.value = listOf(unsaved)
        navigator.navigate(Page.Empty)
    }
}
