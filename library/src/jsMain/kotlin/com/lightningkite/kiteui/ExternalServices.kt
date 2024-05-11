package com.lightningkite.kiteui

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.url.URL
import kotlin.coroutines.resume
import kotlin.js.json

actual object ExternalServices {
    actual fun openTab(url: String) {
        window.open(url, "_blank")
    }

    suspend fun requestFileInput(mimeTypes: List<String>, setup: HTMLInputElement.() -> Unit): List<FileReference> =
        suspendCoroutineCancellable {
            (document.createElement("input") as HTMLInputElement).apply {
                type = "file"
                accept = mimeTypes.joinToString(",")
                setup()
                onchange =
                    { e -> it.resume(files?.let { (0 until it.length).map { index -> it.item(index)!! } } ?: listOf()) }
                oncancel = { e -> it.resume(listOf()) }
            }.click()
            return@suspendCoroutineCancellable { }
        }

    actual suspend fun requestFile(mimeTypes: List<String>) = requestFileInput(mimeTypes, {}).firstOrNull()
    actual suspend fun requestFiles(mimeTypes: List<String>) = requestFileInput(mimeTypes, { multiple = true })
    actual suspend fun requestCaptureSelf(mimeTypes: List<String>) =
        requestFileInput(mimeTypes, { setAttribute("capture", "user") }).firstOrNull()

    actual suspend fun requestCaptureEnvironment(mimeTypes: List<String>) =
        requestFileInput(mimeTypes, { setAttribute("capture", "environment") }).firstOrNull()

    actual fun setClipboardText(value: String) {
        window.navigator.clipboard.writeText(value)
    }

    private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")
    actual fun download(name: String, url: String) {
        if(!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = url
        a.download = name
        a.target = "_blank"
        a.click()
    }
    @JsName("downloadBlob")
    actual fun download(name: String, blob: Blob) {
        if(!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        val a = document.createElement("a") as HTMLAnchorElement
        val url = URL.Companion.createObjectURL(blob)
        a.href = url
        a.download = name
        a.target = "_blank"
        a.click()
        afterTimeout(60_000) {
            URL.Companion.revokeObjectURL(url)
        }
    }

    actual fun share(title: String, message: String?, url: String?) {
        val navigator = window.navigator.asDynamic()
        if (navigator.canShare == undefined || navigator.share == undefined) {
            val s = document.createElement("script") as HTMLScriptElement
            s.onload = { innerShare(title, message, url) }
            s.type = "text/javascript"
            s.src = "https://unpkg.com/share-api-polyfill/dist/share-min.js"
            document.head?.appendChild(s)
        } else {
            innerShare(title, message, url)
        }
    }

    private fun innerShare(title: String, message: String?, url: String?) {
        window.navigator.asDynamic().share(
            json(
                "text" to (message ?: undefined),
                "title" to title,
                "url" to (url ?: undefined)
            )
        )
    }

    actual fun openEvent(
        title: String,
        description: String,
        location: String,
        start: LocalDateTime,
        end: LocalDateTime,
        zone: TimeZone
    ) {
        fun LocalDateTime.format() = buildString {
            if(zone.id != "SYSTEM") {
                append("TZID=")
                append(zone.id)
                append(':')
            }
            append(year.toString().padStart(4, '0'))
            append(monthNumber.toString().padStart(2, '0'))
            append(dayOfMonth.toString().padStart(2, '0'))
            append('T')
            append(hour.toString().padStart(2, '0'))
            append(minute.toString().padStart(2, '0'))
            append(second.toString().padStart(2, '0'))
        }
        var calText = "BEGIN:VCALENDAR\nVERSION:2.0\nCALSCALE:GREGORIAN\nPRODID:adamgibbons/ics\nMETHOD:PUBLISH\nBEGIN:VEVENT\n";
        calText += "UID:" + window.asDynamic().crypto.randomUUID() + "\n";
        calText += "SUMMARY:" + title + "\n";
        calText += "DTSTART:" + start.format() + "\n";
        calText += "DTSTAMP:" + start.format() + "\n";
        calText += "DTEND:" + end.format() + "\n";
        calText += "DESCRIPTION:" + description + "\n";
        calText += "LOCATION:" + location + "\n";
        calText += "END:VEVENT\nEND:VCALENDAR";
        val a = document.createElement("a") as HTMLAnchorElement;
        a.href = "data:text/plain;charset=utf-8," + encodeURIComponent(calText.replace("\n", "\r\n"));
        a.download = "event.ics";
        a.click();
    }

    actual fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
        TODO()
    }
}