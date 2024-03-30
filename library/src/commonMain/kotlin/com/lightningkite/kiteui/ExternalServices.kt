package com.lightningkite.kiteui

expect object ExternalServices {
    fun openTab(url: String)
    fun requestFile(mimeTypes: List<String> = listOf("*/*"), onResult: (FileReference?)->Unit)
    fun requestFiles(mimeTypes: List<String> = listOf("*/*"), onResult: (List<FileReference>)->Unit)
    fun requestCaptureSelf(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?)->Unit)
    fun requestCaptureEnvironment(mimeTypes: List<String> = listOf("image/*"), onResult: (FileReference?)->Unit)
    fun setClipboardText(value: String)
//    fun download(blob: Blob)
//    fun download(url: String)
}
