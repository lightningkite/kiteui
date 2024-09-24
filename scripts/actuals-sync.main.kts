import java.io.File
import kotlin.math.min

// Expect generator

object Stuff {
    val outFile =
        File("/Users/jivie/Projects/kiteui/library/src/commonMain/kotlin/com/lightningkite/kiteui/views/direct/generated.kt")
    val out = outFile.printWriter()
    val implementations: List<Implementation> = listOf(Implementation("js", "HTMLElement"))
}


class Implementation(val variant: String, val todoElementType: String) {
    val outFile =
        File("/Users/jivie/Projects/kiteui/library/src/${variant}Main/kotlin/com/lightningkite/kiteui/views/direct/generated.kt")
    val existing = outFile.readText()
    val out = outFile.printWriter()
    var lastMark = 0

    init {
        out.appendLine(existing.lineSequence().takeWhile { it.isBlank() || it.startsWith("package ") || it.startsWith("import ") }.toList().dropLastWhile { it.isBlank() }.joinToString("\n").takeIf { it.isNotBlank() } ?: """
            package com.lightningkite.kiteui.views.direct

            import com.lightningkite.kiteui.*
            import com.lightningkite.kiteui.models.*
            import com.lightningkite.kiteui.navigation.*
            import com.lightningkite.kiteui.reactive.*
            import com.lightningkite.kiteui.views.*
            import kotlinx.datetime.*
        """.trimIndent())
        out.appendLine()
    }
    fun existing(string: String = "", startMark: Int = 0): String? {
        val index = existing.indexOf("\n" + string, startMark)
        if(index == -1) return null

        var nextLine = index
        while(true) {
            nextLine = existing.indexOf('\n', nextLine + 1)
            if (nextLine == -1) return null
            if(nextLine == existing.lastIndex) return null
            when(existing[nextLine + 1]) {
                ' ' -> {
                    val nextGet = existing.indexOf("get()", nextLine + 1)
                    val nextSet = existing.indexOf("set(value)", nextLine + 1)
                    val min = when {
                        nextGet == -1 -> nextSet
                        nextSet == -1 -> nextGet
                        else -> min(nextGet, nextSet)
                    }
                    if(min != -1 && existing.substring(nextLine + 1, min).isBlank()) {
                        lastMark = nextLine
                        return existing.substring(index + 1 + string.length, nextLine)
                    }
                    // Continue
                }
                ')',
                '}',
                '\n' -> { }
                else -> {
                    lastMark = nextLine
                    return existing.substring(index + 1 + string.length, nextLine)
                }
            }
        }
    }
    fun emitChanged(start: String, default: String) {
        val e = existing(start)?.trimEnd()
        out.println(start + (e ?: default.let { if(it == "ToDoElement") todoElementType else it }))
    }
    fun emitChangedMark(start: String, default: String) {
        val e = existing(start, lastMark)?.trimEnd()
        out.println(start + (e ?: default.let { if(it == "ToDoElement") todoElementType else it }))
    }
}

interface CodeEmitter {
    companion object: CodeEmitter
    fun common(string: String = "") = Stuff.out.println(string)
    fun impl() = Stuff.implementations.forEach {it.out.appendLine() }
    fun impl(string: String) = Stuff.implementations.forEach {it.out.appendLine(string) }
    fun impl(string: String, changing: String) = Stuff.implementations.forEach {it.emitChanged(string, changing) }
    fun implFollow(string: String, changing: String) = Stuff.implementations.forEach {it.emitChangedMark(string, changing) }
}

class Type(val typeName: String, val constructors: List<String> = listOf(typeName.decapitalize())): CodeEmitter {

    init {
        common()
        impl()
        common("expect class N$typeName : NView")
        common("value class $typeName(override val native: N$typeName) : RView<N$typeName>")
        impl("@Suppress(\"ACTUAL_WITHOUT_EXPECT\") actual typealias N$typeName = ", "ToDoElement")
        constructors.forEach {
            common("@ViewDsl expect fun ViewWriter.$it(setup: $typeName.() -> Unit = {}): Unit")
            impl("@ViewDsl actual fun ViewWriter.$it(setup: $typeName.() -> Unit): Unit", " = todo(\"$it\")")
        }
    }

    fun prop(name: String, type: String) {
        common("expect var $typeName.$name: $type")
        impl("actual inline var $typeName.$name: $type")
        implFollow("    get()", " = TODO()")
        implFollow("    set(value)", " { }")
    }

    fun writable(name: String, type: String, default: String) {
        common("expect val $typeName.$name: Writable<$type>")
        impl("actual val $typeName.$name: Writable<$type> get()", " = Property($default)")
    }

    fun readable(name: String, type: String, default: String) {
        common("expect val $typeName.$name: Readable<$type>")
        impl("actual val $typeName.$name: Readable<$type> get()", " = Property($default)")
    }

    fun action(name: String) {
        common("expect fun $typeName.$name(action: suspend () -> Unit)")
        impl("actual fun $typeName.$name(action: suspend () -> Unit): Unit", " = TODO()")
    }

    fun specialConstructor(name: String, vararg arguments: Argument) {
        common("expect fun ViewWriter.$name(${arguments.joinToString() { "${it.name}: ${it.type}" + (it.default?.let { " = $it" } ?: "") }}, setup: $typeName.() -> Unit = {})")
        impl("actual fun ViewWriter.$name(${arguments.joinToString() { "${it.name}: ${it.type}" }}, setup: $typeName.() -> Unit): Unit", " = TODO()")
    }
}

operator fun String.invoke(vararg constructors: String, setup: Type.() -> Unit = {}) {
    Type(this, constructors.toList().takeUnless { it.isEmpty() } ?: listOf(this.decapitalize())).apply(setup)
}

data class Argument(val name: String, val type: String, val default: String? = null)
infix fun String.ofType(other: String) = Argument(this, other)
infix fun Argument.default(other: String) = copy(default = other)

fun modifier(name: String, vararg arguments: Argument) {
    CodeEmitter.common("@ViewModifierDsl3 expect fun ViewWriter.$name(${arguments.joinToString() { "${it.name}: ${it.type}" + (it.default?.let { " = $it" } ?: "") }}): ViewWrapper")
    CodeEmitter.impl("@ViewModifierDsl3 actual fun ViewWriter.$name(${arguments.joinToString() { "${it.name}: ${it.type}" }}): ViewWrapper", " = TODO()")
}
fun modifierVal(name: String) {
    CodeEmitter.common("@ViewModifierDsl3 expect val ViewWriter.$name: ViewWrapper")
    CodeEmitter.impl("@ViewModifierDsl3 actual val ViewWriter.$name: ViewWrapper", " = TODO()")
}

CodeEmitter.common(
    """
    package com.lightningkite.kiteui.views.direct

    import com.lightningkite.kiteui.*
    import com.lightningkite.kiteui.models.*
    import com.lightningkite.kiteui.navigation.*
    import com.lightningkite.kiteui.reactive.*
    import com.lightningkite.kiteui.views.*
    import com.lightningkite.kiteui.views.canvas.*
    import kotlinx.datetime.*
""".trimIndent()
)

"Separator" {}
"ContainingView"("stack", "col", "row") {
    specialConstructor("grid", "columns" ofType "Int")
}
"Link" {
    prop("to", "KiteUiScreen")
    prop("newTab", "Boolean")
}
"ExternalLink" {
    prop("to", "String")
    prop("newTab", "Boolean")
}
"Image" {
    prop("source", "ImageSource")
    prop("scaleType", "ImageScaleType")
    prop("description", "String?")
}
"TextView"("h1", "h2", "h3", "h4", "h5", "h6", "header", "text", "subtext") {
    prop("content", "String")
    prop("align", "Align")
    prop("textSize", "Dimension")
}
"Label" {
    prop("content", "String")
}
"ActivityIndicator" {

}
"Space" {
    specialConstructor("space", "multiplier" ofType "Double")
}
"DismissBackground" {
    action("onClick")
}
"Button" {
    action("onClick")
    prop("enabled", "Boolean")
}
listOf(
    "Checkbox",
    "RadioButton",
    "Switch",
    "ToggleButton",
    "RadioToggleButton"
).forEach {
    it {
        prop("enabled", "Boolean")
        writable("checked", "Boolean", "false")
    }
}

"LocalDateField" {
    writable("content", "LocalDate?", "\"\"")
    prop("range", "ClosedRange<LocalDate>?")
}
"LocalTimeField" {
    writable("content", "LocalTime?", "\"\"")
    prop("range", "ClosedRange<LocalTime>?")
}
"LocalDateTimeField" {
    writable("content", "LocalDateTime?", "\"\"")
    prop("range", "ClosedRange<LocalDateTime>?")
}
"TextInput" {
    writable("content", "String", "\"\"")
    prop("keyboardHints", "KeyboardHints")
    prop("hint", "String")
    prop("range", "ClosedRange<Double>?")
}
"TextArea" {
    writable("content", "String", "\"\"")
    prop("keyboardHints", "KeyboardHints")
    prop("hint", "String")
}

"Select" {
    writable("selected", "String?", "null")
    prop("options", "List<WidgetOption>")
}
"AutoCompleteTextField" {
    writable("content", "String", "\"\"")
    prop("suggestions", "List<String>")
}

"SwapView"("swapView", "swapViewDialog") {
    common("expect fun SwapView.swap(transition: ScreenTransition = ScreenTransition.Fade, createNewView: ()->Unit): Unit")
    impl("actual fun SwapView.swap(transition: ScreenTransition, createNewView: ()->Unit): Unit", " = TODO()")
}

"WebView" {
    prop("url", "String")
    prop("permitJs", "Boolean")
    prop("content", "String")
}

"Canvas" {
    common("expect fun Canvas.redraw(action: DrawingContext2D.() -> Unit): Unit")
    impl("actual fun Canvas.redraw(action: DrawingContext2D.() -> Unit): Unit", " = TODO()")
    readable("width", "Double", "0.0")
    readable("height", "Double", "0.0")

    fun pointer(name: String) {
        common("expect fun Canvas.onPointer${name.capitalize()}(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): Unit")
        impl("actual fun Canvas.onPointer${name.capitalize()}(action: (id: Int, x: Double, y: Double, width: Double, height: Double) -> Unit): Unit", " = TODO()")
    }
    pointer("down")
    pointer("move")
    pointer("cancel")
    pointer("up")
}

"RecyclerView"(
    "recyclerView",
    "horizontalRecyclerView",
    "gridRecyclerView",
) {
    common("expect fun <T> RecyclerView.children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>)->Unit): Unit")
    impl("actual fun <T> RecyclerView.children(items: Readable<List<T>>, render: ViewWriter.(value: Readable<T>)->Unit): Unit", " = TODO()")
}

modifier("hasPopover", "requireClick" ofType "Boolean" default "false", "preferredDirection" ofType "PopoverPreferredDirection" default "PopoverPreferredDirection.belowRight", "setup" ofType "ViewWriter.()->Unit")
modifier("weight", "amount" ofType "Float")
modifier("gravity", "horizontal" ofType "Align", "vertical" ofType "Align")
modifierVal("scrolls", )
modifierVal("scrollsHorizontally", )
modifier("sizedBox", ("constraints" ofType "SizeConstraints"))
modifierVal("marginless", )
modifierVal("withPadding", )


CodeEmitter.impl("// End")

Stuff.out.flush()
Stuff.implementations.forEach { it.out.flush() }
println("DONE")