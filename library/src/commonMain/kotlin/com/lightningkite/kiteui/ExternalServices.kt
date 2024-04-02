package com.lightningkite.kiteui

expect object ExternalServices {
    fun openTab(url: String)
    suspend fun requestFile(mimeTypes: List<String> = listOf("*/*")): FileReference?
    suspend fun requestFiles(mimeTypes: List<String> = listOf("*/*")): List<FileReference>
    suspend fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*")): FileReference?
    suspend fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*")): FileReference?
    fun setClipboardText(value: String)
//    fun download(blob: Blob)
//    fun download(url: String)
}
