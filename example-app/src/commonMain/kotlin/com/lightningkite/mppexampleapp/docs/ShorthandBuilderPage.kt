package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.setClipboardText
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.math.absoluteValue

@Routable("docs/shorthand-builder")
object ShorthandBuilderPage : Page {

    override val title: Reactive<String> = Constant("Shorthand UI Builder")

    // ---- Grammar ----

    private enum class ElementKind(val char: Char, val isContainer: Boolean, val label: String) {
        Col('v', true, "col") {
            override fun ElementWriter.render(node: Node) { col {
                node.arg?.toDoubleOrNull()?.let { gap = it.rem }
                node.children.forEach { renderNode(it) }
            } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = containerSuffix(sb, node, indent)
        },
        Row('h', true, "row") {
            override fun ElementWriter.render(node: Node) { row {
                node.arg?.toDoubleOrNull()?.let { gap = it.rem }
                node.children.forEach { renderNode(it) }
            } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = containerSuffix(sb, node, indent)
        },
        Frame('m', true, "frame") {
            override fun ElementWriter.render(node: Node) { frame { node.children.forEach { renderNode(it) } } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = containerSuffix(sb, node, indent)
        },
        H1('1', false, "h1") {
            override fun ElementWriter.render(node: Node) { h1 { content = node.arg ?: "Heading 1" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        H2('2', false, "h2") {
            override fun ElementWriter.render(node: Node) { h2 { content = node.arg ?: "Heading 2" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        H3('3', false, "h3") {
            override fun ElementWriter.render(node: Node) { h3 { content = node.arg ?: "Heading 3" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        Subtext('s', false, "subtext") {
            override fun ElementWriter.render(node: Node) { subtext { content = node.arg ?: "subtext" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        Text('t', false, "text") {
            override fun ElementWriter.render(node: Node) { text { content = node.arg ?: "text" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        Image('i', false, "image") {
            override fun ElementWriter.render(node: Node) {
                val seed = nextImageSeed()
                withUnsafeModifiers().sizedBox(SizeConstraints(width = 6.rem, height = 6.rem)).image {
                    source = ImageRemote(node.arg ?: "https://picsum.photos/seed/$seed/200/200")
                    description = node.arg ?: "image"
                }
            }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) {
                sb.append(" { /* source = ImageRemote(\"...\") */ }\n")
            }
        },
        IconV('o', false, "icon") {
            override fun ElementWriter.render(node: Node) {
                icon { source = Icon.home; description = node.arg ?: "icon" }
            }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) {
                sb.append("(Icon.home, ").append(quote(node.arg ?: "icon")).append(")\n")
            }
        },
        Button('b', false, "button") {
            override fun ElementWriter.render(node: Node) { button { text(node.arg ?: "Button") } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) {
                if (node.arg != null) wrapBraceBlock(sb, indent) { sb.append("text(").append(quote(node.arg)).append(")\n") }
                else sb.append(" { }\n")
            }
        },
        Field('f', false, "textInput") {
            override fun ElementWriter.render(node: Node) { textInput { hint = node.arg ?: "input" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = hintFieldSuffix(sb, node, indent)
        },
        TextArea('a', false, "textArea") {
            override fun ElementWriter.render(node: Node) { textArea { hint = node.arg ?: "" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = hintFieldSuffix(sb, node, indent)
        },
        Num('n', false, "numberInput") {
            override fun ElementWriter.render(node: Node) { numberInput { hint = node.arg ?: "" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = hintFieldSuffix(sb, node, indent)
        },
        Check('x', false, "checkbox") {
            override fun ElementWriter.render(node: Node) { checkbox(Signal(false)) }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) {
                sb.append(" { /* checked bind ... */ }\n")
            }
        },
        Toggle('w', false, "switch") {
            override fun ElementWriter.render(node: Node) { switch { checked bind Signal(false) } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) {
                sb.append(" { /* checked bind ... */ }\n")
            }
        },
        Radio('r', false, "radioButton") {
            override fun ElementWriter.render(node: Node) { radioButton(Signal(false)) }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) {
                sb.append(" { /* checked bind ... */ }\n")
            }
        },
        Sep('z', false, "separator") {
            override fun ElementWriter.render(node: Node) { separator() }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) { sb.append("()\n") }
        },
        Spc('_', false, "space") {
            override fun ElementWriter.render(node: Node) { space() }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) { sb.append("()\n") }
        },
        ;

        abstract fun ElementWriter.render(node: Node)
        abstract fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int)
    }

    /** Buckets follow KiteUI's enforced order: alignment → weight → theme → scrolling → element. */
    private enum class ModKind(val char: Char, val label: String, val bucket: Int) {
        Centered('c', "centered", 0),
        Expanding('e', "expanding", 1),
        Card('k', "card", 2),
        FieldTheme('F', "fieldTheme", 2),
        Padded('p', "padded", 2),
        Important('I', "important", 2),
        Scrolling('S', "scrolling", 3),
        ScrollingHorizontally('H', "scrollingHorizontally", 3),
    }

    private val elementByChar = ElementKind.entries.associateBy { it.char }
    private val modByChar = ModKind.entries.associateBy { it.char }

    private data class Node(
        val kind: ElementKind,
        val arg: String?,
        val mods: List<ModKind>,
        val children: MutableList<Node> = mutableListOf(),
    )

    private data class ParseResult(val roots: List<Node>, val errors: List<String>)

    // ---- Parser ----

    private fun parse(input: String): ParseResult {
        val errors = mutableListOf<String>()
        val roots = mutableListOf<Node>()
        val stack: ArrayDeque<MutableList<Node>> = ArrayDeque<MutableList<Node>>().apply { addLast(roots) }
        val pendingMods = mutableListOf<ModKind>()

        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c.isWhitespace()) {
                if (stack.size > 1) stack.removeLast()
                i++
                continue
            }
            if (c == '(') {
                errors += "Unexpected '(' at $i — args must follow an element"
                val end = input.indexOf(')', i)
                i = if (end < 0) input.length else end + 1
                continue
            }
            if (c == ')') {
                errors += "Unexpected ')' at $i"
                i++
                continue
            }
            val mod = modByChar[c]
            if (mod != null) {
                pendingMods += mod
                i++
                continue
            }
            val elem = elementByChar[c]
            if (elem != null) {
                i++
                var arg: String? = null
                if (i < input.length && input[i] == '(') {
                    val end = input.indexOf(')', i)
                    if (end < 0) {
                        errors += "Unterminated '(' starting at $i"
                        arg = input.substring(i + 1)
                        i = input.length
                    } else {
                        arg = input.substring(i + 1, end)
                        i = end + 1
                    }
                }
                val node = Node(elem, arg, pendingMods.toList())
                pendingMods.clear()
                stack.last().add(node)
                if (elem.isContainer && arg == null) stack.addLast(node.children)
                continue
            }
            errors += "Unknown char '$c' at $i"
            i++
        }
        if (pendingMods.isNotEmpty()) {
            errors += "Trailing modifier(s) with no element: ${pendingMods.joinToString("") { it.char.toString() }}"
        }
        return ParseResult(roots, errors)
    }

    private fun orderedMods(mods: List<ModKind>): List<ModKind> =
        mods.distinct().sortedBy { it.bucket }

    // ---- Code generation ----

    private fun generateCode(roots: List<Node>): String {
        if (roots.isEmpty()) return ""
        val sb = StringBuilder()
        roots.forEach { writeKotlin(sb, it, 0) }
        return sb.toString().trimEnd()
    }

    private fun writeKotlin(sb: StringBuilder, node: Node, indent: Int) {
        val pad = "    ".repeat(indent)
        val prefix = orderedMods(node.mods).joinToString("") { "${it.label}." }
        sb.append(pad).append(prefix).append(node.kind.label)
        node.kind.writeAfterLabel(sb, node, indent)
    }

    private fun containerSuffix(sb: StringBuilder, node: Node, indent: Int) {
        val pad = "    ".repeat(indent)
        if (node.children.isEmpty()) {
            sb.append(" { }\n")
        } else {
            sb.append(" {\n")
            node.children.forEach { writeKotlin(sb, it, indent + 1) }
            sb.append(pad).append("}\n")
        }
    }

    private fun textCallSuffix(sb: StringBuilder, node: Node) {
        if (node.arg != null) sb.append("(").append(quote(node.arg)).append(")\n")
        else sb.append(" { }\n")
    }

    private fun hintFieldSuffix(sb: StringBuilder, node: Node, indent: Int) {
        if (node.arg == null) {
            sb.append(" { }\n")
            return
        }
        wrapBraceBlock(sb, indent) { sb.append("hint = ").append(quote(node.arg)).append("\n") }
    }

    private inline fun wrapBraceBlock(sb: StringBuilder, indent: Int, write: () -> Unit) {
        val pad = "    ".repeat(indent)
        val innerPad = "    ".repeat(indent + 1)
        sb.append(" {\n").append(innerPad)
        write()
        sb.append(pad).append("}\n")
    }

    private fun quote(s: String): String {
        val escaped = s.replace("\\", "\\\\").replace("\"", "\\\"")
        return "\"$escaped\""
    }

    // ---- Live preview ----

    /** Counter for unique Lorem Picsum seeds per image instance. */
    private var imageSeedCounter = 0
    private fun nextImageSeed(): Int = (imageSeedCounter++).absoluteValue

    /** Applies parsed modifiers in KiteUI's enforced order, then dispatches to the element kind. */
    private fun ElementWriter.renderNode(node: Node) {
        val mods = node.mods.toSet()
        val unsafe: ViewWriter = this.withUnsafeModifiers()

        val a: ElementWriter.CanAddWeight = if (ModKind.Centered in mods) unsafe.centered else unsafe
        val w: ElementWriter.CanAddShownWhen = if (ModKind.Expanding in mods) a.expanding else a
        var t: ElementWriter.CanAddTheme = w
        if (ModKind.Card in mods) t = t.card
        if (ModKind.Padded in mods) t = t.padded
        if (ModKind.Important in mods) t = t.important
        val s: ElementWriter = if (ModKind.Scrolling in mods) t.scrolling else t

        with(node.kind) { s.render(node) }
    }

    // ---- Page render ----

    private val shorthand = Signal("v1(Welcome)s(subtitle here) hooo")

    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        val parsed: Reactive<ParseResult> = remember { parse(shorthand()) }
        val generated: Reactive<String> = remember { generateCode(parsed().roots) }

        col {
            // Preview — takes all extra vertical space.
            expanding.card.scrolling.col {
                reactive {
                    clearChildren()
                    val result = parsed()
                    if (result.roots.isEmpty()) subtext("(empty — type below)")
                    else result.roots.forEach { renderNode(it) }
                }
            }
            // Inline errors only. Sits above the input so growing it only steals from the preview.
            shownWhen { parsed().errors.isNotEmpty() }.subtext {
                ::content { "⚠️ " + parsed().errors.joinToString(" • ") }
            }
            // Input row at the bottom.
            fieldTheme.row {
                expanding.textInput {
                    hint = "Try: v1(Hi)s(world) hooo"
                    content bind shorthand
                }
                button {
                    text("?")
                    onClick { context.openCheatSheet() }
                }
                important.button {
                    text("Copy code")
                    onClick {
                        context.setClipboardText(generated.await())
                        context.toast("Copied Kotlin code")
                    }
                }
            }
        }
    }

    // ---- Help dialog (dynamically built from enum entries) ----

    private fun ElementContext.openCheatSheet() {
        dialog { close ->
            sizedBox(SizeConstraints(maxWidth = 32.rem)).card.col {
                h2("Shorthand cheat sheet")
                scrolling.col {
                    h3("Elements")
                    legendList(ElementKind.entries.map { it.char to it.label })
                    space()
                    h3("Modifiers")
                    legendList(ModKind.entries.map { it.char to it.label })
                    space()
                    subtext(
                        "Args go in parens: 1(Title), b(Save), f(Email). " +
                            "A space closes the current container."
                    )
                }
                row {
                    expanding.space()
                    important.button {
                        text("Close")
                        onClick { close() }
                    }
                }
            }
        }
    }

    private fun ElementWriter.legendList(entries: List<Pair<Char, String>>) {
        col {
            gap = 0.25.rem
            entries.forEach { (char, label) ->
                row {
                    sizedBox(SizeConstraints(width = 2.rem)).text {
                        content = char.toString()
                        align = Align.End
                    }
                    expanding.text(label)
                }
            }
        }
    }
}
