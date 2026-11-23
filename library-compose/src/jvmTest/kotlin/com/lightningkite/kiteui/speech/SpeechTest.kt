package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.locale.LanguageCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeechTest {
    private val voiceList = """
        Albert              en_US    # Hello! My name is Albert.
        Daniel              en_GB    # Hello! My name is Daniel.
        Eddy (English (US)) en_US    # Hello! My name is Eddy.
        Amélie              fr_CA    # Bonjour, je m’appelle Amélie.
        Thomas              fr_FR    # Bonjour, je m’appelle Thomas.
    """.trimIndent()

    @Test
    fun picksTheVoiceForTheLanguageAndRegion() {
        assertEquals("Daniel", macVoiceFor(LanguageCode("en-GB"), voiceList))
        assertEquals("Thomas", macVoiceFor(LanguageCode("fr-fr"), voiceList))
    }

    @Test
    fun fallsBackToAnyVoiceForTheLanguage() {
        assertEquals("Daniel", macVoiceFor(LanguageCode("en"), voiceList))
        assertEquals("Amélie", macVoiceFor(LanguageCode("fr-BE"), voiceList))
    }

    @Test
    fun keepsVoiceNamesWithSpaces() {
        val list = "Eddy (English (US)) en_US    # Hello! My name is Eddy."
        assertEquals("Eddy (English (US))", macVoiceFor(LanguageCode("en-US"), list))
    }

    @Test
    fun skipsNoveltyVoices() {
        val list = """
            Albert              en_US    # Hello! My name is Albert.
            Bad News            en_US    # Hello! My name is Bad News.
            Samantha            en_US    # Hello! My name is Samantha.
        """.trimIndent()
        assertEquals("Samantha", macVoiceFor(LanguageCode("en-US"), list))
    }

    @Test
    fun noVoiceForAnUninstalledLanguage() {
        assertNull(macVoiceFor(LanguageCode("de-DE"), voiceList))
    }
}
