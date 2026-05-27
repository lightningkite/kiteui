package com.lightningkite.kiteui.views.l2


@JsModule("turndown")
@JsNonModule
@JsName("default")
external class TurndownService(options: TurndownOptions = definedExternally) {
    val options: TurndownOptions
    val rules: dynamic

    fun turndown(html: String): String
    fun keep(filter: Array<String>)
    fun addRule(key: String, rule: dynamic)
}


external interface TurndownOptions {
    var rules: dynamic?
    var headingStyle: String?
    var hr: String?
    var bulletListMarker: String?
    var codeBlockStyle: String?
    var fence: String?
    var emDelimiter: String?
    var strongDelimiter: String?
    var linkStyle: String?
    var linkReferenceStyle: String?
    var br: String?
    var preformattedCode: Boolean?
    var blankReplacement: ((content: String, node: dynamic) -> String)?
    var keepReplacement: ((content: String, node: dynamic) -> String)?
    var defaultReplacement: ((content: String, node: dynamic) -> String)?
}
