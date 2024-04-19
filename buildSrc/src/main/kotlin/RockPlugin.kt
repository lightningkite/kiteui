package com.lightningkite.kiteui

import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import java.io.File

interface KiteUiPluginExtension {
    var packageName: String
    var iosProjectRoot: File
}

// Test Note

class KiteUiPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        val ext = extensions.create("kiteui", KiteUiPluginExtension::class.java)
        afterEvaluate {
            if (ext.packageName == null)
                throw IllegalArgumentException("KiteUiPluginExtension property packageName is null. Please configure KiteUiPluginExtension and provide a value")
            if (ext.iosProjectRoot == null)
                throw IllegalArgumentException("KiteUiPluginExtension property iosProjectRoot is null. Please configure KiteUiPluginExtension and provide a value")
        }
        tasks.create("kiteuiResourcesCommon", Task::class.java).apply {
            group = "build"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            afterEvaluate {

                val out = project.file("src/commonMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesExpect.kt")
                outputs.file(out)
                doLast {
                    val lines = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> "val ${r.name}: Font"
                                is Resource.Image -> "val ${r.name}: ImageResource"
                                is Resource.Video -> "val ${r.name}: VideoResource"
                                is Resource.Audio -> "val ${r.name}: AudioResource"
                                is Resource.Binary -> "suspend fun ${r.name}(): Blob"
                                else -> ""
                            }
                        }
                    out.writeText(
                        """
package ${ext.packageName}

import com.lightningkite.kiteui.models.*

expect object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesJs", Copy::class.java).apply {
            dependsOn("kiteuiResourcesCommon")
            group = "build"
            from("src/commonMain/resources")
            into("src/jsMain/resources/common")
            afterEvaluate {

                val out = project.file("src/jsMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                val gitIgnore = project.file("src/jsMain/resources/common/.gitignore")
                outputs.file(out)
                outputs.file(gitIgnore)
                val resourceFolder = project.file("src/commonMain/resources")
                inputs.files(resourceFolder)
                doLast {
                    gitIgnore.writeText("*\n")
                    val lines = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> "actual val ${r.name}: Font = Font(cssFontFamilyName = \"${r.name}\", direct = FontDirect(normal = \"common/${
                                    r.normal.relativeFile.toString().replace(File.separatorChar, '/')
                                }\", bold = ${
                                    r.bold?.relativeFile?.toString()?.replace(File.separatorChar, '/')
                                        ?.let { "\"common/$it\"" }
                                }, italic = ${
                                    r.italic?.relativeFile?.toString()?.replace(File.separatorChar, '/')
                                        ?.let { "\"common/$it\"" }
                                }, boldItalic = ${
                                    r.boldItalic?.relativeFile?.toString()?.replace(File.separatorChar, '/')
                                        ?.let { "\"common/$it\"" }
                                }))"

                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Binary -> "actual suspend fun ${r.name}(): Blob = fetch(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\").blob()"

                                else -> ""
                            }
                        }
                    out.writeText(
                        """
package ${ext.packageName}

import com.lightningkite.kiteui.models.*

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesIos").apply {
            dependsOn("kiteuiResourcesCommon")
            group = "build"

            afterEvaluate {
                val outKt = project.file("src/iosMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                outputs.file(outKt)

                val outProject = ext.iosProjectRoot
                val outAssets = outProject.resolve("Assets.xcassets")
                val outNonAssets = outProject.resolve("resourcesFromCommon")
                val outPlist = outProject.resolve("Info.plist")
//                outputs.files(outAssets)
//                outputs.files(outNonAssets)
//                outputs.file(outPlist)
                val resourceFolder = project.file("src/commonMain/resources")
                inputs.files(resourceFolder)
                doLast {
                    val resources = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }

                    run {
                        val original = outPlist.readText()
                        val uiAppFontsContent = resources.map { it.value }.filterIsInstance<Resource.Font>().flatMap {
                            it.files.map { f ->
                                val copyName = it.name + "-" + f.source.name
                                f.source.copyTo(outNonAssets.resolve(copyName), overwrite = true)
                                copyName
                            }
                        }.joinToString("") {
                            "<string>$it</string>"
                        }
                        if (original.contains("<key>UIAppFonts</key>")) {
                            outPlist.writeText(
                                original
                                    .substringBefore("<key>UIAppFonts</key>") +
                                        "<key>UIAppFonts</key><array>" +
                                        uiAppFontsContent +
                                        "</array>" +
                                        original.substringAfter("<key>UIAppFonts</key>")
                                            .substringAfter("</array>")
                            )
                        } else {
                            outPlist.writeText(
                                original
                                    .substringBefore("</dict>\n</plist>") +
                                        "<key>UIAppFonts</key><array>" +
                                        uiAppFontsContent +
                                        "</array>\n" +
                                        "</dict>\n</plist>"
                            )
                        }
                    }

                    resources.forEach {
                        when (val r = it.value) {
                            is Resource.Font -> {}
                            is Resource.Image -> {
                                val f = outAssets.resolve(it.key + ".imageset")
                                f.mkdirs()
                                val i = f.resolve(r.source.name)
                                r.source.copyTo(i, overwrite = true)
                                f.resolve("Contents.json").writeText(
                                    """
                                {
                                    "info": { "version": 1, "author": "xcode" },
                                    "images": [
                                        { 
                                            "filename": "${i.name}",
                                            "scale": "1x",
                                            "idiom": "universal"
                                        }
                                    ]
                                }
                            """.trimIndent()
                                )
                            }
                            is Resource.Audio -> {
                                val f = outAssets.resolve(it.key + ".dataset")
                                f.mkdirs()
                                val i = f.resolve(r.source.name)
                                r.source.copyTo(i, overwrite = true)
                                f.resolve("Contents.json").writeText(
                                    """
                                    {
                                      "data" : [
                                        {
                                          "filename" : "${i.name}",
                                          "idiom" : "universal"
                                        }
                                      ],
                                      "info" : {
                                        "author" : "xcode",
                                        "version" : 1
                                      }
                                    }

                            """.trimIndent()
                                )
                            }
                            is Resource.Video -> {
                                val f = outAssets.resolve(it.key + ".dataset")
                                f.mkdirs()
                                val i = f.resolve(r.source.name)
                                r.source.copyTo(i, overwrite = true)
                                f.resolve("Contents.json").writeText(
                                    """
                                    {
                                      "data" : [
                                        {
                                          "filename" : "${i.name}",
                                          "idiom" : "universal"
                                        }
                                      ],
                                      "info" : {
                                        "author" : "xcode",
                                        "version" : 1
                                      }
                                    }

                            """.trimIndent()
                                )
                            }

                            is Resource.Binary -> {
                                val f = outAssets.resolve(it.key + ".dataset")
                                f.mkdirs()
                                val i = f.resolve(r.source.name)
                                r.source.copyTo(i, overwrite = true)
                                f.resolve("Contents.json").writeText(
                                    """
                                    {
                                      "data" : [
                                        {
                                          "filename" : "${i.name}",
                                          "idiom" : "universal"
                                        }
                                      ],
                                      "info" : {
                                        "author" : "xcode",
                                        "version" : 1
                                      }
                                    }

                            """.trimIndent()
                                )
                            }

                            else -> {}
                        }
                    }

                    val lines = resources
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> "actual val ${r.name}: Font = fontFromFamilyInfo(normal = ${r.normal.postScriptName.str()}, " +
                                        "italic = ${r.italic?.postScriptName.str()}, " +
                                        "bold = ${r.bold?.postScriptName.str()}, " +
                                        "boldItalic = ${r.boldItalic?.postScriptName.str()})  // ${r}"

                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"${it.key}\")"
                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"${it.key}\", \"${r.source.extension}\")"
                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"${it.key}\", \"${r.source.extension}\")"
                                is Resource.Binary -> "actual suspend fun ${r.name}(): Blob = TODO()"
                                else -> ""
                            }
                        }
                    outKt.writeText(
                        """
package ${ext.packageName}

import com.lightningkite.kiteui.models.*

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesAndroid").apply {
            dependsOn("kiteuiResourcesCommon")
            group = "build"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            val androidResFolder = project.file("src/androidMain/res")

            afterEvaluate {
                val outKt =
                    project.file("src/androidMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                outputs.file(outKt)
                doLast {
                    val resources = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }
                    val androidDrawableFolder = androidResFolder.resolve("drawable-xhdpi").also { it.mkdirs() }
                    val androidRawFolder = androidResFolder.resolve("raw").also { it.mkdirs() }
                    resources.forEach { (key, value) ->
                        if (value !is Resource.Image) return@forEach
                        val destFile = androidDrawableFolder.resolve(key.snakeCase() + "." + value.source.extension)
                        value.source.copyTo(destFile, overwrite = true)
                    }
                    resources.forEach { (key, value) ->
                        if (value !is Resource.Video) return@forEach
                        val destFile = androidRawFolder.resolve(key.snakeCase() + "." + value.source.extension)
                        value.source.copyTo(destFile, overwrite = true)
                    }
                    resources.forEach { (key, value) ->
                        if (value !is Resource.Audio) return@forEach
                        val destFile = androidRawFolder.resolve(key.snakeCase() + "." + value.source.extension)
                        value.source.copyTo(destFile, overwrite = true)
                    }
                    resources.forEach { (key, value) ->
                        if (value !is Resource.Binary) return@forEach
                        val destFile = androidRawFolder.resolve(key.snakeCase() + "." + value.source.extension)
                        value.source.copyTo(destFile, overwrite = true)
                    }
                    val androidFontFolder = androidResFolder.resolve("font").also { it.mkdirs() }
                    resources.forEach { (key, value) ->
                        if (value !is Resource.Font) return@forEach
                        val xmlFile = androidFontFolder.resolve(key.snakeCase() + ".xml")
                        val variants = listOfNotNull(
                            value.normal.let {
                                val destFile =
                                    androidFontFolder.resolve(key.snakeCase() + "_normal." + it.source.extension)
                                it.source.copyTo(destFile, overwrite = true)
                                """
                            <font
                                android:fontStyle="normal"
                                android:fontWeight="400"
                                android:font="@font/${destFile.nameWithoutExtension}" />
                            """.trimIndent()
                            },
                            value.bold?.let {
                                val destFile =
                                    androidFontFolder.resolve(key.snakeCase() + "_bold." + it.source.extension)
                                it.source.copyTo(destFile, overwrite = true)
                                """
                            <font
                                android:fontStyle="normal"
                                android:fontWeight="700"
                                android:font="@font/${destFile.nameWithoutExtension}" />
                            """.trimIndent()
                            },
                            value.italic?.let {
                                val destFile =
                                    androidFontFolder.resolve(key.snakeCase() + "_italic." + it.source.extension)
                                it.source.copyTo(destFile, overwrite = true)
                                """
                            <font
                                android:fontStyle="italic"
                                android:fontWeight="400"
                                android:font="@font/${destFile.nameWithoutExtension}" />
                            """.trimIndent()
                            },
                            value.boldItalic?.let {
                                val destFile =
                                    androidFontFolder.resolve(key.snakeCase() + "_bold_italic." + it.source.extension)
                                it.source.copyTo(destFile, overwrite = true)
                                """
                            <font
                                android:fontStyle="italic"
                                android:fontWeight="700"
                                android:font="@font/${destFile.nameWithoutExtension}" />
                            """.trimIndent()
                            }
                        )
                        xmlFile.writeText(
                            """
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
${variants.joinToString("\n")}
</font-family>
                    """.trim()
                        )
                    }
                    val lines = resources
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> "actual val ${r.name}: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.${it.key.snakeCase()})"
                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(R.drawable.${it.key.snakeCase()})"
                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(R.raw.${it.key.snakeCase()})"
                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(R.raw.${it.key.snakeCase()})"
                                is Resource.Binary -> "actual suspend fun ${r.name}(): Blob = TODO()"
                                else -> ""
                            }
                        }
                    outKt.writeText(
                        """
package ${ext.packageName}

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.AndroidAppContext

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesAll").apply {
            group = "build"
            dependsOn("kiteuiResourcesCommon")
            dependsOn("kiteuiResourcesJs")
            dependsOn("kiteuiResourcesIos")
            dependsOn("kiteuiResourcesAndroid")
        }

        tasks.create("kiteuiLocalize").apply {
            group = "kiteui"
            afterEvaluate {
                val commonMain = project.file("src/commonMain/${ext.packageName.replace(".", "/")}")
                if(!commonMain.exists()) return@afterEvaluate
                val toRead = commonMain.listFiles().filter { it.name != "Strings.kt" }
                toRead.forEach {
                    if(it.isDirectory) inputs.dir(it)
                    else inputs.file(it)
                }
                val outKt = commonMain.resolve("Strings.kt")
                outputs.file(outKt)
                doLast {
                    val localizations = HashSet<NeededStringTemplate>()
                    toRead.filterNotNull().asSequence()
                        .flatMap { it.walkTopDown() }
                        .filter { it.extension == "kt" && it.isFile }
                        .forEach {
                            it.readText().localizer(localizations)
                        }
                    localizations.removeAll { it.content.joinToString("").isBlank() }
                    localizations.removeAll { it.content.joinToString("").first().isLowerCase() }
                    localizations.removeAll { !it.content.joinToString("").contains(' ') }
                    outKt.writeText("""
                        package ${ext.packageName}
                        
                        interface StringsBase {
                            ${localizations.joinToString("\n    ")}
                        }
                    """.trimIndent())
                    toRead.filterNotNull().asSequence()
                        .flatMap { it.walkTopDown() }
                        .filter { it.extension == "kt" && it.isFile }
                        .forEach {
                            it.writeText(it.readText().applyLocalizations(localizations))
                        }
                }
            }
        }

        Unit
    }
}

fun String?.str() = if (this == null) "null" else "\"$this\""

sealed class Resource {
    data class Font(
        val name: String,
        val normal: SubFont,
        val bold: SubFont? = null,
        val italic: SubFont? = null,
        val boldItalic: SubFont? = null,
    ) : Resource() {
        data class SubFont(
            val source: File,
            val relativeFile: File,
            val fontSuperFamily: String,
            val fontFamily: String,
            val fontSubFamily: String,
            val postScriptName: String,
        )

        val files get() = listOfNotNull(normal, bold, italic, boldItalic)
    }

    data class Image(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Video(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Audio(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Binary(val name: String, val source: File, val relativeFile: File) : Resource()
}

fun File.resources(): Map<String, Resource> {
    val out = HashMap<String, Resource>()
    walkTopDown().forEach { file ->
        if (file.name.isEmpty()) return@forEach
        if (file.nameWithoutExtension.isEmpty()) return@forEach
        if (file.isDirectory) return@forEach
        val relativeFile = file.relativeTo(this)
        val name = relativeFile.path.replace('/', ' ').replace('\\', ' ')
            .substringBeforeLast('.')
            .camelCase()
        when (relativeFile.extension) {
            "png", "jpg" -> out[name] = Resource.Image(name, file, relativeFile)
            "mp4" -> out[name] = Resource.Video(name, file, relativeFile)
            "mp3", "ogg", "wav" -> out[name] = Resource.Audio(name, file, relativeFile)
            "otf", "ttf" -> {
                val font = when (relativeFile.extension) {
                    "otf" -> OTFParser().parse(file)
                    "ttf" -> TTFParser().parse(file)
                    else -> throw IllegalArgumentException()
                }
                val sf = Resource.Font.SubFont(
                    source = file,
                    relativeFile = relativeFile,
                    fontSuperFamily = font.naming.getName(16, 1, 0, 0)
                        ?: font.naming.nameRecords.find { it.nameId == 16 }?.string ?: "",
                    fontFamily = font.naming.fontFamily,
                    fontSubFamily = font.naming.fontSubFamily,
                    postScriptName = font.naming.postScriptName,
                )
                if (relativeFile.nameWithoutExtension in setOf(
                        "bold",
                        "bold-italic",
                        "italic",
                        "normal",
                    )
                ) {
                    val folderName = relativeFile.parentFile.path.replace('/', ' ').replace('\\', ' ')
                        .substringBeforeLast('.')
                        .camelCase()
                    out[folderName] = (out[folderName] as? Resource.Font)?.let {
                        it.copy(
                            normal = if (relativeFile.nameWithoutExtension == "normal") sf else it.normal,
                            boldItalic = if (relativeFile.nameWithoutExtension == "bold-italic") sf else it.boldItalic,
                            italic = if (relativeFile.nameWithoutExtension == "italic") sf else it.italic,
                            bold = if (relativeFile.nameWithoutExtension == "bold") sf else it.bold,
                        )
                    } ?: Resource.Font(
                        folderName,
                        normal = sf,
                        boldItalic = if (relativeFile.nameWithoutExtension == "bold-italic") sf else null,
                        italic = if (relativeFile.nameWithoutExtension == "italic") sf else null,
                        bold = if (relativeFile.nameWithoutExtension == "bold") sf else null,
                    )
                } else out[name] = Resource.Font(name, sf)
            }

            "" -> {}
            else -> out[name] = Resource.Binary(name, file, relativeFile)
        }
    }
    return out
}

fun String.applyLocalizations(local: Set<NeededStringTemplate>): String {
    var value = this
    for(l in local) {
        value = l.pattern.replace(value) {
            l.use(it.groupValues.drop(1).map { it.trim('{', '}') })
        }
    }
    return value
}

fun String.localizer(out: MutableSet<NeededStringTemplate>) {
    fun argNameByIndex(index: Int) = ('a' + ((('x' - 'a') + index) % 26)).toString()
    val raw = this
    val stringStack = ArrayList<StringLitData>()
    val codeStack = ArrayList<CodeData>()
    codeStack.add(CodeData())
    var templateStarted = false
    val buildingArgName = StringBuilder()
    var escaped = false
    var inString = false
    for(index in 0 until raw.length) {
        val c = raw[index]
        if(templateStarted) {
            if(c == '{') {
                codeStack.add(CodeData(1))
                stringStack.last().args.add("")
                templateStarted = false
                inString = false
                continue
            } else if (c.isLetterOrDigit()) {
                buildingArgName.append(c)
                continue
            } else {
                inString = true
                stringStack.last().args.add(buildingArgName.toString())
                templateStarted = false
            }
        }
        if(inString) {
            val wasEscaped = escaped
            escaped = false
            if(c == '"' && !wasEscaped) {
                inString = false
                val finish = stringStack.removeLast()
                finish.finishSection()
                val t = NeededStringTemplate(
                    content = finish.content,
                    args = finish.args.indices.map { argNameByIndex(it) },
                )
                out.add(t)
                continue
            } else if(c == '\\') {
                escaped = true
            } else if(c == '$' && !wasEscaped) {
                stringStack.last().finishSection()
                templateStarted = true
                buildingArgName.clear()
                continue
            }
            stringStack.lastOrNull()?.append(c)
        } else {
            if(c == '"') {
                inString = true
                stringStack.add(StringLitData(index))
                continue
            }
            if(c == '{') {
                codeStack.last().braceLevel++
            } else if(c == '}') {
                if((--codeStack.last().braceLevel) == 0) {
                    inString = true
                }
            }
        }
    }
}
data class NeededStringTemplate(val content: List<String>, val args: List<String>, val name: String = "${content.zip(args) { a, b -> "$a ${b}" }.joinToString("") }${content.last()}".camelCase()) {
    fun useStart() = if(args.isEmpty()) "Strings.$name" else "Strings.$name("
    fun useEnd() = if(args.isEmpty()) "" else ")"
    fun use(inputs: List<String>) = if(args.isEmpty()) "Strings.$name" else "Strings.$name(${inputs.joinToString()})"
    override fun toString(): String {
        if(args.isEmpty())
            return "val $name: String get() = \"${content.first()}\""
        return "fun $name(${args.joinToString(){ "$it: Any?" }}): String = \"${content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("") }${content.last()}\""
    }

    val pattern = Regex("\"${content.zip(args) { a, b -> "${Regex.escape(a)}\\\$(\\{[^{}]*(?:\\{[^}]*\\}[^}]*)*\\}|[\\w\\d]+)" }.joinToString("") }${Regex.escape(content.last())}\"")
}
class CodeData(var braceLevel: Int = 0) {
}
class StringLitData(
    val start: Int,
) {
    val builder = StringBuilder()
    val content = ArrayList<String>()
    fun append(c: Char) = builder.append(c)
    fun finishSection() {
        content += builder.toString()
        builder.clear()
    }
    val args = ArrayList<String>()
}


val `casing separator regex` = Regex("([-_\\s]+([A-Z]*[a-z0-9]+))|([-_\\s]*[A-Z]+)")
inline fun String.caseAlter(crossinline update: (after: String) -> String): String =
    `casing separator regex`.replace(this) {
        if (it.range.start == 0) it.value
        else update(it.value.filter { !(it == '-' || it == '_' || it.isWhitespace()) })
    }

fun String.titleCase() = caseAlter { " " + it.capitalize() }.capitalize()
fun String.spaceCase() = caseAlter { " " + it }.decapitalize()
fun String.kabobCase() = caseAlter { "-$it" }.toLowerCase()
fun String.snakeCase() = caseAlter { "_$it" }.toLowerCase()
fun String.screamingSnakeCase() = caseAlter { "_$it" }.toUpperCase()
fun String.camelCase() = caseAlter { it.capitalize() }.decapitalize()
fun String.pascalCase() = caseAlter { it.capitalize() }.capitalize()
