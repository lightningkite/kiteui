package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.SizeConstraints
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.setClipboardText
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.sizedBox
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.toast
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import kotlin.math.absoluteValue

@Routable("docs/shorthand-builder")
object ShorthandBuilderPage : Page {

    override val title: Reactive<String> = Constant("Shorthand UI Builder")

    // ---- Grammar ----

    private enum class ElementKind(val char: Char, val isContainer: Boolean, val label: String, val argumentMeaning: String? = null) {
        Col('v', true, "col", "Gap in rem") {
            override fun ElementWriter.render(node: Node) { col {
                node.arg?.toDoubleOrNull()?.let { gap = it.rem }
                node.children.forEach { renderNode(it) }
            } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = containerSuffix(sb, node, indent)
        },
        Row('h', true, "row", "Gap in rem") {
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
        H1('1', false, "h1", "text contents") {
            override fun ElementWriter.render(node: Node) { h1 { content = node.arg ?: "Heading 1" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        H2('2', false, "h2", "text contents") {
            override fun ElementWriter.render(node: Node) { h2 { content = node.arg ?: "Heading 2" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        H3('3', false, "h3", "text contents") {
            override fun ElementWriter.render(node: Node) { h3 { content = node.arg ?: "Heading 3" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        Subtext('s', false, "subtext", "text contents") {
            override fun ElementWriter.render(node: Node) { subtext { content = node.arg ?: "subtext" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        Text('t', false, "text", "text contents") {
            override fun ElementWriter.render(node: Node) { text { content = node.arg ?: "text" } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = textCallSuffix(sb, node)
        },
        Image('i', false, "image", "URL of image") {
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
            override fun ElementWriter.render(node: Node) { button {
                node.children.forEach { renderNode(it) }
            } }
            override fun writeAfterLabel(sb: StringBuilder, node: Node, indent: Int) = containerSuffix(sb, node, indent)
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

    /**
     * Buckets follow KiteUI's enforced order:
     * alignment(0) → weight(1) → sizing(2) → theme(3) → scrolling(4) → element.
     * All sizing modifiers collapse into a single `sizedBox(...)` call at codegen and runtime.
     */
    private enum class ModKind(val char: Char, val bucket: Int) {
        Centered('c', 0) {
            override fun label(arg: String?) = "centered"
        },
        Expanding('e', 1) {
            override fun label(arg: String?) = arg?.toFloatOrNull()?.let { "weight(${it}f)" } ?: "expanding"
        },
        Width('W', 2) {
            override fun label(arg: String?) = "setWidth"
        },
        Height('T', 2) {
            override fun label(arg: String?) = "setHeight"
        },
        MaxWidth('X', 2) {
            override fun label(arg: String?) = "maxWidth"
        },
        MaxHeight('Y', 2) {
            override fun label(arg: String?) = "maxHeight"
        },
        Card('k', 3) {
            override fun label(arg: String?) = "card"
        },
        FieldTheme('F', 3) {
            override fun label(arg: String?) = "fieldTheme"
        },
        Padded('p', 3) {
            override fun label(arg: String?) = "padded"
        },
        Important('I', 3) {
            override fun label(arg: String?) = "important"
        },
        Scrolling('S', 4) {
            override fun label(arg: String?) = "scrolling"
        },
        ScrollingHorizontally('H', 4) {
            override fun label(arg: String?) = "scrollingHorizontally"
        },
        ;

        abstract fun label(arg: String?): String
    }

    private data class Mod(val kind: ModKind, val arg: String?)

    private val elementByChar = ElementKind.entries.associateBy { it.char }
    private val modByChar = ModKind.entries.associateBy { it.char }

    private data class Node(
        val kind: ElementKind,
        val arg: String?,
        val mods: List<Mod>,
        val children: MutableList<Node> = mutableListOf(),
    )

    private data class ParseResult(val roots: List<Node>, val errors: List<String>)

    // ---- Parser ----

    private fun parse(input: String): ParseResult {
        val errors = mutableListOf<String>()
        val roots = mutableListOf<Node>()
        val stack: ArrayDeque<MutableList<Node>> = ArrayDeque<MutableList<Node>>().apply { addLast(roots) }
        val pendingMods = mutableListOf<Mod>()

        // Consumes an optional "(...)" immediately after index `i`. Returns (arg, newIndex).
        fun consumeArg(i: Int): Pair<String?, Int> {
            if (i >= input.length || input[i] != '(') return null to i
            val end = input.indexOf(')', i)
            return if (end < 0) {
                errors += "Unterminated '(' starting at $i"
                input.substring(i + 1) to input.length
            } else {
                input.substring(i + 1, end) to end + 1
            }
        }

        var i = 0
        while (i < input.length) {
            val c = input[i]
            if (c.isWhitespace()) {
                if (stack.size > 1) stack.removeLast()
                i++
                continue
            }
            if (c == '(') {
                errors += "Unexpected '(' at $i — args must follow an element or modifier"
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
                val (arg, next) = consumeArg(i + 1)
                pendingMods += Mod(mod, arg)
                i = next
                continue
            }
            val elem = elementByChar[c]
            if (elem != null) {
                val (arg, next) = consumeArg(i + 1)
                val node = Node(elem, arg, pendingMods.toList())
                pendingMods.clear()
                stack.last().add(node)
                if (elem.isContainer) stack.addLast(node.children)
                i = next
                continue
            }
            errors += "Unknown char '$c' at $i"
            i++
        }
        if (pendingMods.isNotEmpty()) {
            errors += "Trailing modifier(s) with no element: ${pendingMods.joinToString("") { it.kind.char.toString() }}"
        }
        return ParseResult(roots, errors)
    }

    /** Latest mod per kind, in canonical bucket order. */
    private fun orderedMods(mods: List<Mod>): List<Mod> =
        mods.groupBy { it.kind }
            .map { (_, v) -> v.last() }
            .sortedBy { it.kind.bucket }

    // ---- Code generation ----

    private fun generateCode(roots: List<Node>): String {
        if (roots.isEmpty()) return ""
        val sb = StringBuilder()
        roots.forEach { writeKotlin(sb, it, 0) }
        return sb.toString().trimEnd()
    }

    private fun writeKotlin(sb: StringBuilder, node: Node, indent: Int) {
        val pad = "    ".repeat(indent)
        sb.append(pad)
        val ordered = orderedMods(node.mods)
        var sizingEmitted = false
        for (m in ordered) {
            if (m.kind.bucket == 2) {
                if (!sizingEmitted) {
                    sb.append(sizedBoxLabel(ordered.filter { it.kind.bucket == 2 })).append('.')
                    sizingEmitted = true
                }
            } else {
                sb.append(m.kind.label(m.arg)).append('.')
            }
        }
        sb.append(node.kind.label)
        node.kind.writeAfterLabel(sb, node, indent)
    }

    private fun sizedBoxLabel(sizing: List<Mod>): String {
        val parts = mutableListOf<String>()
        sizing.firstOrNull { it.kind == ModKind.Width }?.arg?.let { parts += "width = ${it}.rem" }
        sizing.firstOrNull { it.kind == ModKind.Height }?.arg?.let { parts += "height = ${it}.rem" }
        sizing.firstOrNull { it.kind == ModKind.MaxWidth }?.arg?.let { parts += "maxWidth = ${it}.rem" }
        sizing.firstOrNull { it.kind == ModKind.MaxHeight }?.arg?.let { parts += "maxHeight = ${it}.rem" }
        return "sizedBox(SizeConstraints(${parts.joinToString(", ")}))"
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
        val byKind: Map<ModKind, Mod> = node.mods.groupBy { it.kind }.mapValues { it.value.last() }
        val unsafe: ViewWriter = this.withUnsafeModifiers()

        val a: ElementWriter.CanAddWeight =
            if (ModKind.Centered in byKind) unsafe.centered else unsafe
        val w: ElementWriter.CanAddShownWhen = byKind[ModKind.Expanding]?.let { m ->
            a.weight(m.arg?.toFloatOrNull() ?: 1f)
        } ?: a
        val sized: ElementWriter.CanAddTheme = run {
            val width = byKind[ModKind.Width]?.arg?.toDoubleOrNull()?.rem
            val height = byKind[ModKind.Height]?.arg?.toDoubleOrNull()?.rem
            val maxW = byKind[ModKind.MaxWidth]?.arg?.toDoubleOrNull()?.rem
            val maxH = byKind[ModKind.MaxHeight]?.arg?.toDoubleOrNull()?.rem
            if (width != null || height != null || maxW != null || maxH != null) {
                w.sizedBox(SizeConstraints(width = width, height = height, maxWidth = maxW, maxHeight = maxH))
            } else w
        }
        var t: ElementWriter.CanAddTheme = sized
        if (ModKind.Card in byKind) t = t.card
        if (ModKind.FieldTheme in byKind) t = t.fieldTheme
        if (ModKind.Padded in byKind) t = t.padded
        if (ModKind.Important in byKind) t = t.important
        val s: ElementWriter = when {
            ModKind.Scrolling in byKind -> t.scrolling
            ModKind.ScrollingHorizontally in byKind -> t.scrollingHorizontally
            else -> t
        }

        with(node.kind) { s.render(node) }
    }

    // ---- Page render ----

    private val shorthand = Signal(
        "c1(KiteUI Shorthand)cs(every feature in one screen) z chioo " +
            "z2(Sign up) hFf(Email)Fn(Age)e(2)Fa(Tell us about yourself...) " +
            "z2(Preferences) khxt(Subscribe)wt(Dark mode)rt(Plan A)e(2)Ib(Save) " +
            "z2(Gallery) chW(4)iW(6)iW(8)i " +
            "z ckmv1(Built with)3(KiteUI)"
    )

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
                    col {
                        gap = 0.25.rem
                        ElementKind.entries.forEach {
                            row {
                                sizedBox(SizeConstraints(width = 2.rem)).text {
                                    content = it.char.toString()
                                    align = Align.End
                                }
                                expanding.text(it.label)
                                it.argumentMeaning?.let {
                                    text(it)
                                }
                            }
                        }
                    }
                    space()
                    h3("Modifiers")
                    col {
                        gap = 0.25.rem
                        ModKind.entries.forEach {
                            row {
                                sizedBox(SizeConstraints(width = 2.rem)).text {
                                    content = it.char.toString()
                                    align = Align.End
                                }
                                expanding.text(it.label(null))
                            }
                        }
                    }
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

}
