package com.lightningkite.kiteui.speech

import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.reactive.core.AppScope
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.Locale

// The JVM has no speech engine, so this runs the operating system's command-line one, one utterance at a time.
public actual object TTS {
    // In queue order; the first is being said.
    private val outstanding = OutstandingUtterances<Utterance>()
    public actual val isSpeaking: ReactiveValue<Boolean> get() = outstanding.isSpeaking

    // macOS and Windows ship their speech commands; Linux needs speech-dispatcher installed.
    public actual val isAvailable: Boolean
        get() = System.getProperty("os.name").let { it.startsWith("Mac") || it.startsWith("Windows") } ||
            System.getenv("PATH").orEmpty().split(File.pathSeparator).any { File(it, "spd-say").canExecute() }

    private var worker: Job? = null

    public actual fun warm() {}

    public actual fun queue(utterance: Utterance, onComplete: (Utterance.Result) -> Unit) {
        outstanding.add(utterance, onComplete)
        if (worker != null) return
        worker = AppScope.launch(Dispatchers.Main) {
            while (isActive) {
                val next = outstanding.first ?: break
                val reason = try {
                    say(next)
                    Utterance.Result.Completed
                } catch (e: CancellationException) {
                    throw e
                } catch (e: SayFailed) {
                    e.result
                } catch (e: Exception) {
                    Utterance.Error.Platform("Text-to-speech failed: ${e.message}")
                }
                outstanding.finish(next, reason)
            }
            if (isActive) worker = null
        }
    }

    public actual fun clear() {
        worker?.cancel()
        worker = null
        outstanding.cancelAll()
    }
}

/** Ends [say] early with [result]. */
private class SayFailed(val result: Utterance.Result) : Exception()

/** Says [utterance], returning once it's finished.  Cancelling stops it; failing throws [SayFailed]. */
private suspend fun say(utterance: Utterance) {
    val text = utterance.text
    val language = utterance.language
    val os = System.getProperty("os.name")
    when {
        os.startsWith("Mac") -> {
            // The system voice is the user's own choice, so only pick another for a different language.
            val system = Locale.getDefault()
            val requested = language?.let { Locale.forLanguageTag(it.asString) }
            if (requested == null || (requested.language == system.language && requested.country in setOf("", system.country))) {
                run("say", input = text)
            } else {
                val voice = macVoiceFor(language, run("say", "-v", "?"))
                    ?: throw SayFailed(Utterance.Error.LanguageUnavailable)
                run("say", "-v", voice, input = text)
            }
        }
        // The text arrives on stdin and the language in an environment variable, so neither is ever parsed as script.
        os.startsWith("Windows") -> run(
            "powershell", "-NoProfile", "-NonInteractive", "-Command", """
                ${'$'}ErrorActionPreference = 'Stop'
                [Console]::InputEncoding = [Text.Encoding]::UTF8
                Add-Type -AssemblyName System.Speech
                ${'$'}synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
                ${'$'}lang = ${'$'}env:KITEUI_SPEECH_LANGUAGE
                if (${'$'}lang) {
                    ${'$'}voice = ${'$'}synth.GetInstalledVoices() | Where-Object { ${'$'}_.VoiceInfo.Culture.Name -eq ${'$'}lang -or ${'$'}_.VoiceInfo.Culture.TwoLetterISOLanguageName -eq ${'$'}lang } | Select-Object -First 1
                    if (-not ${'$'}voice) { exit $WINDOWS_NO_VOICE }
                    ${'$'}synth.SelectVoice(${'$'}voice.VoiceInfo.Name)
                }
                ${'$'}synth.Speak([Console]::In.ReadToEnd())
            """.trimIndent(),
            input = text,
            environment = mapOf("KITEUI_SPEECH_LANGUAGE" to (language?.asString ?: "")),
            exitResults = mapOf(WINDOWS_NO_VOICE to Utterance.Error.LanguageUnavailable),
        )
        // speech-dispatcher, the usual Linux speech service; it falls back to its default voice for unknown languages.
        else -> run("spd-say", "--wait", *(if (language == null) emptyArray() else arrayOf("--language", language.language)), "--", text)
    }
}

// What the Windows script exits with when no voice matches the language.
private const val WINDOWS_NO_VOICE = 3

/** Picks a voice from `say -v ?` output, whose lines look like `Eddy (English (US)) en_US    # Hello! My name is Eddy.` */
internal fun macVoiceFor(language: LanguageCode, voiceList: String): String? {
    val voices = Regex("""^(.+?)[ \t]+([a-z]{2,3})_(\w+)[ \t]+#""", RegexOption.MULTILINE).findAll(voiceList)
        .map { it.destructured }
        .filter { (name, _, _) -> name !in macNoveltyVoices }
        .toList()
    val wanted = language.language.lowercase()
    return (voices.firstOrNull { (_, lang, region) -> lang == wanted && region == language.region?.uppercase() }
        ?: voices.firstOrNull { (_, lang, _) -> lang == wanted })?.component1()
}

// Joke voices that macOS lists before its real English ones.
private val macNoveltyVoices = setOf(
    "Albert", "Bad News", "Bahh", "Bells", "Boing", "Bubbles", "Cellos", "Good News", "Jester",
    "Organ", "Superstar", "Trinoids", "Whisper", "Wobble", "Zarvox",
)

/** Runs a command to completion and returns its output; cancelling kills it.  A failing exit code throws [SayFailed], with its [exitResults] entry if it has one. */
private suspend fun run(
    vararg command: String,
    input: String = "",
    environment: Map<String, String> = emptyMap(),
    exitResults: Map<Int, Utterance.Result> = emptyMap(),
): String = coroutineScope {
    val process = try {
        ProcessBuilder(*command).redirectErrorStream(true).apply { environment().putAll(environment) }.start()
    } catch (e: IOException) {
        throw SayFailed(Utterance.Error.TTSUnavailable)
    }
    try {
        // Read alongside waiting, or a chatty command fills the pipe and never exits.
        val output = async(Dispatchers.IO) { process.inputStream.bufferedReader().readText() }
        withContext(Dispatchers.IO) { process.outputStream.bufferedWriter().use { it.write(input) } }
        val exitCode = process.onExit().await().exitValue()
        if (exitCode != 0) throw SayFailed(
            exitResults[exitCode] ?: Utterance.Error.Platform("Text-to-speech failed: `${command.first()}` exited with $exitCode. ${output.await().trim()}")
        )
        output.await()
    } finally {
        process.destroy()
    }
}

public actual object ASR {
    public actual val isAvailable: Boolean get() = false
    public actual val isListening: ReactiveValue<Boolean> = Constant(false)
    public actual suspend fun listen(language: LanguageCode?): ASRResult = ASRError.ASRUnavailable
    public actual fun stop() {}
}
