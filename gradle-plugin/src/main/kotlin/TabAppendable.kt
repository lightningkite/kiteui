package com.lightningkite.kiteui

class TabAppendable(val wraps: Appendable, val tabString: String = "    ") {
//    var imports = HashSet<String>()
    var tabs = 0
    var needIndent = false
    inline fun tab(action: ()->Unit) {
        tabs++
        action()
        tabs--
    }
    fun appendLine() {
        indentIfNeeded()
        wraps.appendLine()
        needIndent = true
    }
    fun appendLine(text: String) {
        indentIfNeeded()
        wraps.appendLine(text)
        needIndent = true
    }
    fun appendLine(character: Char) {
        indentIfNeeded()
        wraps.appendLine(character)
        needIndent = true
    }
    fun append() {
        indentIfNeeded()
        wraps.append()
    }
    fun append(text: String) {
        indentIfNeeded()
        wraps.append(text)
    }
    fun append(character: Char) {
        indentIfNeeded()
        wraps.append(character)
    }

    fun indentIfNeeded() {
        if(needIndent) {
            needIndent = false
            repeat(tabs) {
                wraps.append(tabString)
            }
        }
    }
}