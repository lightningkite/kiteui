package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kotlinx.serialization.uri.encodeURIComponent
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLScriptElement
import org.w3c.dom.url.URL
import org.w3c.files.File
import org.w3c.files.FilePropertyBag
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.json

// by Claude

public class JsExternalServices(private val ctx: ElementContext) : ExternalServicesAccess {

    override fun openLink(url: String, newTab: Boolean) {
        window.open(url, if (newTab) "_blank" else "_self")
    }

    override fun openMap(latitude: Double, longitude: Double, label: String?, zoom: Float?) {
        openTab("https://www.google.com/maps/@${latitude},${longitude},${zoom ?: 16}")
    }

    override suspend fun requestFile(mimeTypes: List<String>): FileReference? = ctx.requestFileInput(mimeTypes, {}).firstOrNull()
    override suspend fun requestFiles(mimeTypes: List<String>): List<FileReference> = ctx.requestFileInput(mimeTypes, { multiple = true })
    override suspend fun requestCaptureSelf(mimeTypes: List<String>): FileReference? =
        ctx.requestFileInput(mimeTypes, { setAttribute("capture", "user") }).firstOrNull()
    override suspend fun requestCaptureEnvironment(mimeTypes: List<String>): FileReference? =
        ctx.requestFileInput(mimeTypes, { setAttribute("capture", "environment") }).firstOrNull()

    override fun setClipboardText(value: String) {
        window.navigator.clipboard.writeText(value)
    }

    override suspend fun download(name: String, url: String, preferredDestination: DownloadLocation, onDownloadProgress: ((progress: Float) -> Unit)?) {
        if (!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = com.lightningkite.kiteui.fetch(
            url, method = HttpMethod.GET,
            httpHeaders().apply {
                append("mode", "cors")
                append("cache", "no-cache")
                append("Cache-Control", "no-cache")
            },
        ).let { URL.createObjectURL(it.blob()) }
        a.style.display = "none"
        a.download = name
        document.body?.appendChild(a)
        a.click()
        document.body?.removeChild(a)
    }

    override suspend fun download(name: String, blob: Blob, preferredDestination: DownloadLocation) {
        if (!name.matches(validDownloadName)) throw IllegalArgumentException("Name $name has invalid characters!")
        val a = document.createElement("a") as HTMLAnchorElement
        val url = URL.Companion.createObjectURL(blob)
        a.href = url
        a.download = name
        a.target = "_blank"
        a.click()
        afterTimeout(60_000) { URL.Companion.revokeObjectURL(url) }
    }

    override suspend fun share(namesToBlobs: List<Pair<String, Blob>>) {
        val files = namesToBlobs.map {
            val name = it.first
            val blob = it.second
            val type = blob.type.split("/").lastOrNull() ?: "png"
            val fileName = if (name.contains(".")) name else "${name}.${type}"
            File(fileBits = arrayOf(blob), fileName = fileName, options = FilePropertyBag(type = blob.type))
        }
        val data = json("files" to files.toTypedArray())
        if (window.navigator.asDynamic().canShare()) {
            window.navigator.asDynamic().share(data)
        } else throw IllegalArgumentException("Your browser won't let you share these files.")
    }

    override fun share(title: String, message: String?, url: String?) {
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

    override fun openEvent(title: String, description: String, location: String, start: LocalDateTime, end: LocalDateTime, zone: TimeZone) {
        fun LocalDateTime.format() = buildString {
            if (zone.id != "SYSTEM") { append("TZID="); append(zone.id); append(':') }
            append(year.toString().padStart(4, '0'))
            append(month.number.toString().padStart(2, '0'))
            append(day.toString().padStart(2, '0'))
            append('T')
            append(hour.toString().padStart(2, '0'))
            append(minute.toString().padStart(2, '0'))
            append(second.toString().padStart(2, '0'))
        }
        var calText = "BEGIN:VCALENDAR\nVERSION:2.0\nCALSCALE:GREGORIAN\nPRODID:adamgibbons/ics\nMETHOD:PUBLISH\nBEGIN:VEVENT\n"
        calText += "UID:" + window.asDynamic().crypto.randomUUID() + "\n"
        calText += "SUMMARY:" + title + "\n"
        calText += "DTSTART:" + start.format() + "\n"
        calText += "DTSTAMP:" + start.format() + "\n"
        calText += "DTEND:" + end.format() + "\n"
        calText += "DESCRIPTION:" + description + "\n"
        calText += "LOCATION:" + location + "\n"
        calText += "END:VEVENT\nEND:VCALENDAR"
        val a = document.createElement("a") as HTMLAnchorElement
        a.href = "data:text/plain;charset=utf-8," + encodeURIComponent(calText.replace("\n", "\r\n"))
        a.download = "event.ics"
        a.click()
    }

    // by Claude
    override suspend fun getCurrentPosition(): GeolocationResult {
        return suspendCancellableCoroutine<GeolocationResult> { cont ->
            window.navigator.asDynamic().geolocation.getCurrentPosition(
                { result: dynamic ->
                    cont.resume(GeolocationResult(
                        latitude = result.coords.latitude,
                        longitude = result.coords.longitude,
                        accuracyInMeters = result.coords.accuracy,
                    ))
                },
                { error: dynamic ->
                    cont.resumeWithException(Exception("Geolocation error: $error"))
                },
                json("enableHighAccuracy" to true, "timeout" to 10000, "maximumAge" to 0)
            )
        }
    }
}

public actual fun externalServicesAccessDefault(context: ElementContext): ExternalServicesAccess = JsExternalServices(context)

private val validDownloadName = Regex("[a-zA-Z0-9.\\-_]+")

private var lastFileInput: HTMLInputElement? = null
private fun removeFileInput() {
    lastFileInput?.let { document.body!!.removeChild(it) }
    lastFileInput = null
}

public suspend fun ElementContext.requestFileInput(
    mimeTypes: List<String>,
    setup: HTMLInputElement.() -> Unit
): List<FileReference> = suspendCancellableCoroutine {
    removeFileInput()
    (document.createElement("input") as HTMLInputElement).apply {
        type = "file"
        hidden = true
        accept = mimeTypes.joinToString(",")
        setup()
        // iOS Safari requires 'addEventListener' opposed to setting onchange directly, and requires it to be attached to the dom.
        addEventListener("change", { _ ->
            it.resume(files?.let { (0 until it.length).map { index -> it.item(index)!! } } ?: listOf())
            removeFileInput()
        })
        addEventListener("cancel", { _ ->
            it.resume(listOf())
            removeFileInput()
        })
        document.body!!.appendChild(this)
        lastFileInput = this
    }.click()
}

private fun innerShare(title: String, message: String?, url: String?) {
    window.navigator.asDynamic().share(
        json("text" to (message ?: undefined), "title" to title, "url" to (url ?: undefined))
    )
}
