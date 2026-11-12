package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.locale.LanguageCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

// The JVM has no speech engine, so this drives the operating system's own command-line one.

private val osName = System.getProperty("os.name")

internal actual suspend fun platformSpeak(text: String, language: LanguageCode?) {
    when {
        osName.startsWith("Mac") -> {
            // The system voice is the user's own choice, so only pick another for a different language.
            val system = Locale.getDefault()
            val requested = language?.let { Locale.forLanguageTag(it.asString) }
            if (requested == null || (requested.language == system.language && requested.country in setOf("", system.country))) {
                run("say", input = text)
            } else {
                val voice = macVoiceFor(language, run("say", "-v", "?"))
                    ?: throw PlainTextException("No voice is installed for ${language.asString}.", "Can't speak")
                run("say", "-v", voice, input = text)
            }
        }
        osName.startsWith("Windows") -> run(
            "powershell", "-NoProfile", "-NonInteractive", "-Command", windowsSpeakScript,
            input = text,
            environment = mapOf("KITEUI_SPEECH_LANGUAGE" to (language?.asString ?: "")),
        )
        // speech-dispatcher, the usual Linux speech service; it falls back to its default voice for unknown languages.
        language == null -> run("spd-say", "--wait", "--", text)
        else -> run("spd-say", "--wait", "--language", language.language, "--", text)
    }
}

/** Picks a voice from `say -v ?` output, whose lines look like `Eddy (English (US)) en_US    # Hello! My name is Eddy.` */
internal fun macVoiceFor(language: LanguageCode, voiceList: String): String? {
    val line = Regex("""^(.+?)[ \t]+([a-z]{2,3})_(\w+)[ \t]+#""", RegexOption.MULTILINE)
    val voices = line.findAll(voiceList).map { it.destructured }.filter { (name, _, _) -> name !in macNoveltyVoices }.toList()
    val wantedLanguage = language.language.lowercase()
    val wantedRegion = language.region?.uppercase()
    val exact = voices.firstOrNull { (_, lang, region) -> lang == wantedLanguage && region == wantedRegion }
    val sameLanguage = voices.firstOrNull { (_, lang, _) -> lang == wantedLanguage }
    return (exact ?: sameLanguage)?.component1()
}

// Joke voices that macOS lists before its real English ones.
private val macNoveltyVoices = setOf(
    "Albert", "Bad News", "Bahh", "Bells", "Boing", "Bubbles", "Cellos", "Good News", "Jester",
    "Organ", "Superstar", "Trinoids", "Whisper", "Wobble", "Zarvox",
)

// The text arrives on stdin and the language in an environment variable, so neither is ever parsed as script.
private val windowsSpeakScript = """
    [Console]::InputEncoding = [Text.Encoding]::UTF8
    Add-Type -AssemblyName System.Speech
    ${'$'}synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
    ${'$'}lang = ${'$'}env:KITEUI_SPEECH_LANGUAGE
    if (${'$'}lang) {
        ${'$'}voice = ${'$'}synth.GetInstalledVoices() | Where-Object { ${'$'}_.VoiceInfo.Culture.Name -eq ${'$'}lang -or ${'$'}_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq ${'$'}lang } | Select-Object -First 1
        if (-not ${'$'}voice) { [Console]::Error.WriteLine("No voice is installed for ${'$'}lang."); exit 1 }
        ${'$'}synth.SelectVoice(${'$'}voice.VoiceInfo.Name)
    }
    ${'$'}synth.Speak([Console]::In.ReadToEnd())
""".trimIndent()

/** Runs a command to completion and returns its output; cancelling kills it. */
private suspend fun run(vararg command: String, input: String? = null, environment: Map<String, String> = emptyMap()): String = coroutineScope {
    val process = try {
        ProcessBuilder(*command).redirectErrorStream(true).apply { environment().putAll(environment) }.start()
    } catch (e: IOException) {
        throw PlainTextException("Text-to-speech needs the `${command.first()}` command, which isn't installed.", "Can't speak")
    }
    try {
        // Read alongside waiting, or a chatty command fills the pipe and never exits.
        val output = async(Dispatchers.IO) { process.inputStream.bufferedReader().readText() }
        withContext(Dispatchers.IO) { process.outputStream.bufferedWriter().use { it.write(input ?: "") } }
        val exitCode = process.onExit().await().exitValue()
        if (exitCode != 0) throw PlainTextException("Text-to-speech failed: `${command.first()}` exited with $exitCode. ${output.await().trim()}", "Can't speak")
        output.await()
    } finally {
        process.destroy()
    }
}

internal actual val platformCanListen: Boolean get() = false

internal actual suspend fun platformListen(language: LanguageCode?): String? =
    throw UnsupportedOperationException("Speech recognition isn't available on desktop.")
