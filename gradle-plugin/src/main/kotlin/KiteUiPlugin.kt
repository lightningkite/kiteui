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
            val task = this
            group = "build"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            afterEvaluate {
//                tasks.findByName("compileCommonMainKotlinMetadata")?.dependsOn(task)

                val out = project.file("src/commonMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesExpect.kt")
                outputs.file(out)
                doLast {
                    var usesBlob = false
                    val lines = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> "val ${r.name}: Font"
                                is Resource.Image -> "val ${r.name}: ImageResource"
                                is Resource.Video -> "val ${r.name}: VideoResource"
                                is Resource.Audio -> "val ${r.name}: AudioResource"
                                is Resource.Binary -> { usesBlob = true; "suspend fun ${r.name}(): Blob" }
                                else -> ""
                            }
                        }

                    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
                        .also { if (usesBlob) it.add("import com.lightningkite.kiteui.Blob") }
                        .joinToString("\n")

                    out.writeText(
                        """
package ${ext.packageName}

$imports

expect object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesJs", Copy::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "build"
            from("src/commonMain/resources")
            into("src/jsMain/resources/common")
            afterEvaluate {
//                tasks.findByName("compileKotlinJs")?.dependsOn(task)

                val out = project.file("src/jsMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                val gitIgnore = project.file("src/jsMain/resources/common/.gitignore")
                outputs.file(out)
                outputs.file(gitIgnore)
                val resourceFolder = project.file("src/commonMain/resources")
                inputs.files(resourceFolder)
                doLast {
                    var usesBlob = false
                    gitIgnore.writeText("*\n")
                    val lines = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> {
                                    val normal = r.normal.entries.joinToString { "${it.key} to \"common/${it.value.relativeFile.toString().replace(File.separatorChar, '/')}\"" }
                                    val italics = r.italics.entries.joinToString { "${it.key} to \"common/${it.value.relativeFile.toString().replace(File.separatorChar, '/')}\"" }
                                    "actual val ${r.name}: Font = Font(cssFontFamilyName = \"${r.name}\", direct = FontDirect(normal = mapOf($normal), italics = mapOf($italics)))"
                                }

                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Binary -> { usesBlob = true; "actual suspend fun ${r.name}(): Blob = fetch(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\").blob()" }

                                else -> ""
                            }
                        }

                    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
                        .also {
                            if (usesBlob) {
                                it.add("import com.lightningkite.kiteui.Blob")
                                it.add("import com.lightningkite.kiteui.fetch")
                            }
                        }.joinToString("\n")

                    out.writeText(
                        """
package ${ext.packageName}

$imports

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesJvm", Task::class.java).apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "build"
            afterEvaluate {
//                tasks.findByName("compileKotlinJvm")?.dependsOn(task)
                val out = project.file("src/jvmMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                val gitIgnore = project.file("src/jvmMain/resources/common/.gitignore")
                outputs.file(out)
                outputs.file(gitIgnore)
                val resourceFolder = project.file("src/commonMain/resources")
                inputs.files(resourceFolder)
                doLast {
                    var usesBlob = false
                    gitIgnore.writeText("*\n")
                    val lines = resourceFolder.resources()
                        .entries
                        .sortedBy { it.key }
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> {
                                    val normal = r.normal.entries.joinToString { "${it.key} to \"common/${it.value.relativeFile.toString().replace(File.separatorChar, '/')}\"" }
                                    val italics = r.italics.entries.joinToString { "${it.key} to \"common/${it.value.relativeFile.toString().replace(File.separatorChar, '/')}\"" }
                                    "actual val ${r.name}: Font = Font(cssFontFamilyName = \"${r.name}\", direct = FontDirect(normal = mapOf($normal), italics = mapOf($italics)))"
                                }

                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\")"

                                is Resource.Binary -> { usesBlob = true; "actual suspend fun ${r.name}(): Blob = fetch(\"common/${
                                    r.relativeFile.toString().replace(File.separatorChar, '/')
                                }\").blob()" }

                                else -> ""
                            }
                        }

                    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
                        .also {
                            if (usesBlob) {
                                it.add("import com.lightningkite.kiteui.Blob")
                                it.add("import com.lightningkite.kiteui.fetch")
                            }
                        }
                        .joinToString("\n")

                    out.writeText(
                        """
package ${ext.packageName}

$imports

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesIos").apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "build"

            afterEvaluate {
//                tasks.findByName("compileKotlinIosSimulatorArm64")?.dependsOn(task)
//                tasks.findByName("compileKotlinIosArm64")?.dependsOn(task)
//                tasks.findByName("compileKotlinIosX64")?.dependsOn(task)
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
                                val i = outNonAssets.resolve(r.name + "." + r.relativeFile.extension)
                                r.source.copyTo(i, overwrite = true)
                            }

                            is Resource.Video -> {
                                val i = outNonAssets.resolve(r.name + "." + r.relativeFile.extension)
                                r.source.copyTo(i, overwrite = true)
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

                    var usesBlob = false
                    val lines = resources
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> {
                                    val normal = r.normal.entries.joinToString { "${it.key} to ${it.value.postScriptName.str()}" }
                                    val italics = r.italics.entries.joinToString { "${it.key} to ${it.value.postScriptName.str()}" }
                                    "actual val ${r.name}: Font = fontFromFamilyInfo(normal = mapOf($normal), italics = mapOf($italics))  // ${r}"
                                }

                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(\"${it.key}\")"
                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(\"${it.key}\", \"${r.source.extension}\")"
                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(\"${it.key}\", \"${r.source.extension}\")"
                                is Resource.Binary -> { usesBlob = true; "actual suspend fun ${r.name}(): Blob = TODO()" }
                                else -> ""
                            }
                        }

                    val imports = mutableListOf("import com.lightningkite.kiteui.models.*")
                        .also { if (usesBlob) it.add("import com.lightningkite.kiteui.Blob") }
                        .joinToString("\n")

                    outKt.writeText(
                        """
package ${ext.packageName}

$imports

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesAndroid").apply {
            val task = this
            dependsOn("kiteuiResourcesCommon")
            group = "build"
            val resourceFolder = project.file("src/commonMain/resources")
            inputs.files(resourceFolder)
            val androidResFolder = project.file("src/androidMain/res")

            afterEvaluate {
//                tasks.findByName("compileReleaseKotlinAndroid")?.dependsOn(task)
//                tasks.findByName("compileDebugKotlinAndroid")?.dependsOn(task)
                val outKt =
                    project.file("src/androidMain/kotlin/${ext.packageName.replace(".", "/")}/ResourcesActual.kt")
                outputs.file(outKt)
                // TODO: Manifest for tracking which files are under our control; git-ignore
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
                        val variants = value.normal.map {
                            val destFile =
                                androidFontFolder.resolve(key.snakeCase() + "_${it.key}_normal." + it.value.source.extension)
                            it.value.source.copyTo(destFile, overwrite = true)
                            """
                            <font
                                android:fontStyle="normal"
                                android:fontWeight="${it.key}"
                                android:font="@font/${destFile.nameWithoutExtension}" />
                            """.trimIndent()
                        } + value.italics.map {
                            val destFile =
                                androidFontFolder.resolve(key.snakeCase() + "_${it.key}_italic." + it.value.source.extension)
                            it.value.source.copyTo(destFile, overwrite = true)
                            """
                            <font
                                android:fontStyle="italic"
                                android:fontWeight="${it.key}"
                                android:font="@font/${destFile.nameWithoutExtension}" />
                            """.trimIndent()
                        }
                        xmlFile.writeText(
                            """
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:android="http://schemas.android.com/apk/res/android">
${variants.joinToString("\n")}
</font-family>
                    """.trim()
                        )
                    }
                    var usesBlob = false
                    val lines = resources
                        .joinToString("\n    ") {
                            when (val r = it.value) {
                                is Resource.Font -> "actual val ${r.name}: Font = AndroidAppContext.applicationCtx.resources.getFont(R.font.${it.key.snakeCase()})"
                                is Resource.Image -> "actual val ${r.name}: ImageResource = ImageResource(R.drawable.${it.key.snakeCase()})"
                                is Resource.Video -> "actual val ${r.name}: VideoResource = VideoResource(R.raw.${it.key.snakeCase()})"
                                is Resource.Audio -> "actual val ${r.name}: AudioResource = AudioResource(R.raw.${it.key.snakeCase()})"
                                is Resource.Binary -> { usesBlob = true; "actual suspend fun ${r.name}(): Blob = TODO()" }
                                else -> ""
                            }
                        }

                    val imports = mutableListOf(
                        "import com.lightningkite.kiteui.models.*",
                        "import com.lightningkite.kiteui.views.AndroidAppContext"
                    ).also { if (usesBlob) it.add("import com.lightningkite.kiteui.Blob") }
                        .joinToString("\n")

                    outKt.writeText(
                        """
package ${ext.packageName}

$imports

actual object Resources {
    $lines
}
        """.trimIndent()
                    )
                }
            }
        }

        tasks.create("kiteuiResourcesAll").apply {
            val task = this
            group = "build"
            dependsOn("kiteuiResourcesCommon")
            dependsOn("kiteuiResourcesJs")
            dependsOn("kiteuiResourcesIos")
            dependsOn("kiteuiResourcesAndroid")
            dependsOn("kiteuiResourcesJvm")
        }

        tasks.create("kiteuiLocalize").apply {
            val task = this
            group = "kiteui"
            afterEvaluate {
                val commonMain = project.file("src/commonMain/kotlin/${ext.packageName.replace(".", "/")}")
                if (!commonMain.exists()) {
                    println("File $commonMain does not exist.  No localization possible.")
                    return@afterEvaluate
                }
                val toRead = commonMain.listFiles().filter { it.name != "Strings.kt" }
                toRead.forEach {
                    if (it.isDirectory) inputs.dir(it)
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
                            try {
                                it.readText().localizer(localizations)
                            } catch (e: Exception) {
                                println("WARNING: Could not parse $it")
                                e.printStackTrace()
                            }
                        }
                    localizations.groupBy { it.name }
                        .filter { it.value.size > 1 }
                        .forEach { t, u ->
                            localizations.removeAll(u)
                            localizations.addAll(u.mapIndexed { index, it ->
                                it.copy(name = it.name + (index + 1))
                            })
                        }
                    val wordRegex = Regex("[A-Z][a-z]")
                    val commaWithoutSpace = Regex(",[^ ]")
                    outKt.writeText(
                        """
package ${ext.packageName}

interface StringsBase {
    ${localizations.joinToString("\n    ")}
}

object Strings: StringsBase
                    """.trimIndent()
                    )
                    toRead.filterNotNull().asSequence()
                        .flatMap { it.walkTopDown() }
                        .filter { it.extension == "kt" && it.isFile }
                        .forEach {
                            it.writeText(it.readText().applyLocalizations(ext.packageName, localizations))
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
        val normal: Map<Int, SubFont>,
        val italics: Map<Int, SubFont>,
    ) : Resource() {
        data class SubFont(
            val source: File,
            val relativeFile: File,
            val fontSuperFamily: String,
            val fontFamily: String,
            val fontSubFamily: String,
            val postScriptName: String,
        )

        val files get() = normal.values + italics.values
    }

    data class Image(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Video(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Audio(val name: String, val source: File, val relativeFile: File) : Resource()
    data class Binary(val name: String, val source: File, val relativeFile: File) : Resource()
}

val boldnessNames = mapOf(
    "Thin" to 100,
    "Hairline" to 100,
    "UltraLight" to 200,
    "ExtraLight" to 200,
    "Light" to 300,
    "Normal" to 400,
    "Regular" to 400,
    "Medium" to 500,
    "SemiBold" to 600,
    "DemiBold" to 600,
    "Bold" to 700,
    "ExtraBold" to 800,
    "UltraBold" to 800,
    "Black" to 900,
    "Heavy" to 900,
).mapKeys { it.key.lowercase() }
val boldnessNamesByLength = boldnessNames.entries.sortedByDescending { it.key.length }

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
            .filter { it.isLetterOrDigit() }
        when (relativeFile.extension) {
            "png", "jpg", "webp" -> out[name] = Resource.Image(name, file, relativeFile)
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
                val siblings = file.parentFile.listFiles() ?: arrayOf()
                val siblingsCommonPrefix = siblings.map { it.nameWithoutExtension }.reduceOrNull { a, b -> a.commonPrefixWith(b) } ?: ""
                val siblingsAreOfSameFont = siblings.isNotEmpty() && siblings.all {
                    it.nameWithoutExtension.removePrefix(siblingsCommonPrefix)
                        .filter { it.isLetter() }
                        .lowercase()
                        .replace("italic", "")
                        .let { it.isBlank() || it in boldnessNames.keys }
                }
                val italic = file.nameWithoutExtension.contains("italic", true)
                val weight = boldnessNamesByLength.find { (key, value) -> file.nameWithoutExtension.lowercase().contains(key) }?.value ?: 400
                if (siblingsAreOfSameFont) {
                    val folderName = relativeFile.parentFile.path.replace('/', ' ').replace('\\', ' ')
                        .substringBeforeLast('.')
                        .camelCase()
                    out[folderName] = (out[folderName] as? Resource.Font)?.let {
                        if(italic) {
                            it.copy(italics = it.italics + (weight to sf))
                        } else {
                            it.copy(normal = it.normal + (weight to sf))
                        }
                    } ?: Resource.Font(
                        folderName,
                        normal = if(!italic) mapOf(weight to sf) else mapOf(),
                        italics = if(italic) mapOf(weight to sf) else mapOf(),
                    )
                } else out[name] = Resource.Font(name, normal = mapOf(400 to sf), italics = mapOf())
            }

            "" -> {}
            else -> out[name] = Resource.Binary(name, file, relativeFile)
        }
    }
    return out
}

fun String.applyLocalizations(packageName: String, local: Set<NeededStringTemplate>): String {
    var value = this
    for (l in local) {
        value = l.pattern.replace(value) {
            l.use(it.groupValues.drop(1).map { it.trim('{', '}') })
        }
    }
    return value.lines().let {
        val i = it.indexOfLast { it.startsWith("import ") }
        if(i == -1) return@let it
        it.subList(0, i) + listOf("import ${packageName}.Strings") + it.subList(i, it.size)
    }.joinToString("\n")
}

fun String.localizer(out: MutableSet<NeededStringTemplate>) {
    val raw = this
    fun argNameByIndex(index: Int) = ('a' + ((('x' - 'a') + index) % 26)).toString()
    val stringStack = ArrayList<StringLitData>()
    val codeStack = ArrayList<CodeData>()
    codeStack.add(CodeData())
    var templateStarted = false
    val buildingArgName = StringBuilder()
    var escaped = false
    var inString = false
    var inComment = false
    var skipNext = 0
    var ignoreUntilLineEnd = false
    var annotationParenLevel = -1
    var printlnParenLevel = -1
    for (index in 0 until raw.length) {
        try {
            val c = raw[index]
            if (skipNext > 0) {
                skipNext--
                continue
            }
            if(inComment) {
                if(c == '/' && raw[index-1] == '*') inComment = false
                continue
            }
            if(ignoreUntilLineEnd) {
                if(c == '\n') ignoreUntilLineEnd = false
                continue
            }
            if (templateStarted) {
                if (c == '{') {
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
            if (inString) {
                val wasEscaped = escaped
                escaped = false
                if (c == '"' && !wasEscaped) {
                    if (stringStack.last().triple) {
                        if(index + 2 < raw.length && raw.substring(index, index + 3) != "\"\"\"") {
                            // It's a fakeout
                            stringStack.lastOrNull()?.append(c)
                            continue
                        } else {
                            skipNext = 2
                        }
                    }
                    inString = false
                    val finish = stringStack.removeLast()
                    finish.finishSection()
                    val t = NeededStringTemplate(
                        content = finish.content,
                        triple = finish.triple,
                        args = finish.args.indices.map { argNameByIndex(it) },
                    )
                    val prerender = finish.content.joinToString()
                    val word = Regex("[A-Z][a-z]")
                    val bannedChars = Regex("\\<\\>\\{\\}\\[\\]_")
                    val camel = Regex("[a-z][A-Z]")
                    if (
                        prerender.contains(word) &&
                        !prerender.contains(bannedChars) &&
                        !prerender.contains(camel) &&
                        annotationParenLevel == -1 &&
                        printlnParenLevel == -1 &&
                        (prerender.length >= 2 && (prerender[0].lowercaseChar() != 'm' || !prerender[1].isDigit()))
                    ) {
                        out.add(t)
                    }
                    continue
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '$' && !wasEscaped) {
                    stringStack.last().finishSection()
                    templateStarted = true
                    buildingArgName.clear()
                    continue
                }
                stringStack.lastOrNull()?.append(c)
            } else {
                if(c == '/' && raw.getOrNull(index + 1) == '*') {
                    inComment = true
                    continue
                }
                if(c == '/' && raw.getOrNull(index + 1) == '/') {
                    ignoreUntilLineEnd = true
                    continue
                }
                if (c == '"') {
                    val triple = index + 2 <= raw.length && raw.substring(index, index + 3) == "\"\"\""
                    if(triple) {
                        skipNext = 2
                    }
                    inString = true
                    stringStack.add(StringLitData(index).also {
                        it.triple = triple
                    })
                    continue
                }
                if (c == '{') {
                    codeStack.last().braceLevel++
                } else if (c == '}') {
                    if ((--codeStack.last().braceLevel) == 0) {
                        inString = true
                        codeStack.removeLast()
                    }
                }
                if (index + 7 < raw.length && raw.substring(index, index + 7) == "println") {
                    printlnParenLevel = codeStack.last().parenLevel
                }
                if (c == '@') {
                    annotationParenLevel = codeStack.last().parenLevel
                }
                if (c == '(') {
                    ++codeStack.last().parenLevel
                } else if (c == ')') {
                    val newLevel = --codeStack.last().parenLevel
                    if (newLevel == annotationParenLevel) {
                        annotationParenLevel = -1
                    }
                    if (newLevel == printlnParenLevel) {
                        printlnParenLevel = -1
                    }
                }
                if (c == ' ' && codeStack.last().parenLevel == codeStack.last().parenLevel) annotationParenLevel
            }
        } catch(e: Exception) {
            throw Exception("Failed parsing around ${raw.drop(index - 20).take(40)}. templateStarted: $templateStarted\n" +
                    "escaped: $escaped\n" +
                    "inString: $inString\n" +
                    "inComment: $inComment\n" +
                    "skipNext: $skipNext\n" +
                    "ignoreUntilLineEnd: $ignoreUntilLineEnd\n" +
                    "annotationParenLevel: $annotationParenLevel\n" +
                    "printlnParenLevel: $printlnParenLevel", e)
        }
    }
}

data class NeededStringTemplate(
    val content: List<String>,
    val triple: Boolean,
    val args: List<String>,
    val name: String = "${
        content.zip(args) { a, b -> "$a ${b}" }.joinToString("")
    }${content.last()}".let { if(it.firstOrNull()?.isLowerCase() == true) "lowercase $it" else it }.split(' ').filter { it.isNotBlank() }.take(8).joinToString(" ").filter { it.isLetterOrDigit() || it == ' ' }.camelCase()
) {
    fun useStart() = if (args.isEmpty()) "Strings.$name" else "Strings.$name("
    fun useEnd() = if (args.isEmpty()) "" else ")"
    fun use(inputs: List<String>) = if (args.isEmpty()) "Strings.$name" else "Strings.$name(${inputs.joinToString()})"
    override fun toString(): String {
        if(triple) {
            if (args.isEmpty())
                return "val $name: String get() = \"\"\"${content.first()}\"\"\""
            return "fun $name(${args.joinToString() { "$it: Any?" }}): String = \"\"\"${
                content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("")
            }${content.last()}\"\"\""
        } else {
            if (args.isEmpty())
                return "val $name: String get() = \"${content.first()}\""
            return "fun $name(${args.joinToString() { "$it: Any?" }}): String = \"${
                content.zip(args) { a, b -> "$a\${${b}}" }.joinToString("")
            }${content.last()}\""
        }
    }

    val pattern = Regex(
        "\"(?:\"\")?${
            content.zip(args) { a, b -> "${Regex.escape(a)}\\\$(\\{[^{}]*(?:\\{[^}]*\\}[^}]*)*\\}|[\\w\\d]+)" }
                .joinToString("")
        }${Regex.escape(content.last())}\"(?:\"\")?"
    )
}

class CodeData(var braceLevel: Int = 1, var parenLevel: Int = 1) {
}

class StringLitData(
    val start: Int,
) {
    var triple = false
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