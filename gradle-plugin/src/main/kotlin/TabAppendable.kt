package com.lightningkite.kiteui

internal class TabAppendable(val wraps: Appendable, val tabString: String = "    ") {
//    var imports = HashSet<String>()
    public var tabs = 0
    public var needIndent = false
    public inline fun tab(action: ()->Unit) {
        tabs++
        action()
        tabs--
    }
    public fun appendLine() {
        indentIfNeeded()
        wraps.appendLine()
        needIndent = true
    }
    public fun appendLine(text: String) {
        indentIfNeeded()
        wraps.appendLine(text)
        needIndent = true
    }
    public fun appendLine(character: Char) {
        indentIfNeeded()
        wraps.appendLine(character)
        needIndent = true
    }
    public fun append() {
        indentIfNeeded()
        wraps.append()
    }
    public fun append(text: String) {
        indentIfNeeded()
        wraps.append(text)
    }
    public fun append(character: Char) {
        indentIfNeeded()
        wraps.append(character)
    }

    public fun indentIfNeeded() {
        if(needIndent) {
            needIndent = false
            repeat(tabs) {
                wraps.append(tabString)
            }
        }
    }
}