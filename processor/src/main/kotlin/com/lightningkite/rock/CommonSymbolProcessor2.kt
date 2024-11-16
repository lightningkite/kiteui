package com.lightningkite.rock

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFile
import java.io.BufferedWriter
import java.io.File
import java.io.RandomAccessFile
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files

abstract class CommonSymbolProcessor2(
    private val myCodeGenerator: CodeGenerator,
    val myId: String,
    val version: Int = 0
) : SymbolProcessor {
    lateinit var log: Appendable
    abstract fun process2(resolver: Resolver, files: Set<KSFile>)
    abstract fun interestedIn(resolver: Resolver): Set<KSFile>

    private lateinit var fileCreator: (dependencies: Dependencies, packageName: String, fileName: String, extensionName: String) -> Writer

    private var invoked = false
    final override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return listOf()
        invoked = true

        val log = myCodeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            fileName = "$myId-log",
            extensionName = "txt",
            packageName = "com.lightningkite.lightningserver"
        ).writer()
        log.appendLine("Log started")
        this.log = log

        try {

            myCodeGenerator.createNewFile(
                Dependencies.ALL_FILES,
                fileName = "$myId",
                extensionName = "txt",
                packageName = "com.lightningkite.lightningserver"
            ).writer().use {
                it.appendLine("All reported files below")
                resolver.getAllFiles().forEach { f -> it.appendLine(f.filePath) }
            }
            val outSample = myCodeGenerator.generatedFile.first().absoluteFile
            val projectFolder = generateSequence(outSample) { it.parentFile!! }
                .first { it.name == "build" }
                .parentFile!!
            val common = resolver.getAllFiles().any { it.filePath.contains("/src/common", true) }
            val flavor = outSample.path.split(File.separatorChar)
                .dropWhile { it != "ksp" }
                .drop(2)
                .first()
                .let {
                    if (it.contains("test", true)) "Test"
                    else "Main"
                }

            val interestedIn = interestedIn(object : Resolver by resolver {
                override fun getAllFiles(): Sequence<KSFile> {
                    return resolver.getAllFiles().filter {
                        !common || it.filePath.contains("/src/common")
                    }
                }
            })
            val outFolder = projectFolder.resolve("build/generated/ksp/common/common$flavor/kotlin")
            outFolder.mkdirs()

            log.appendLine("Preparing to process ${interestedIn.size} files.")
            log.appendLine("Common mode: $common")

            if (common) {
                processFiles(
                    log = log,
                    version = version,
                    dependencies = interestedIn.asSequence().map { it.filePath.let(::File) },
                    lockFile = outFolder.resolve("$myId.lock"),
                    destinationFolder = outFolder.resolve(myId).also { it.mkdirs() },
                    action = {
                        fileCreator = label@{ _, packageName, fileName, extensionName ->
                            val packagePath =
                                packageName.split('.').filter { it.isNotBlank() }.joinToString("") { "$it/" }
                            this.file("${packagePath}$fileName.$extensionName")
                        }
                        process2(resolver, interestedIn)
                    }
                )
            } else {
                myCodeGenerator.createNewFile(
                    Dependencies.ALL_FILES,
                    fileName = "$myId.analyzed",
                    extensionName = "txt",
                    packageName = "com.lightningkite.lightningserver"
                ).writer().use {
                    it.appendLine("Analyzed files below")
                    interestedIn.forEach { f -> it.appendLine(f.filePath) }
                }
                fileCreator = label@{ dependencies, packageName, fileName, extensionName ->
                    myCodeGenerator.createNewFile(
                        dependencies,
                        packageName,
                        fileName,
                        extensionName
                    ).bufferedWriter()
                }
                process2(resolver, interestedIn)
            }
        } catch (t: Throwable) {
            log.appendLine("FAILED WITH:")
            log.appendLine(t.stackTraceToString())
            throw t
        } finally {
            log.close()
        }

        return listOf()
    }

    fun createNewFile(
        dependencies: Dependencies,
        packageName: String,
        fileName: String,
        extensionName: String = "kt"
    ): Writer {
        return fileCreator(dependencies, packageName, fileName, extensionName)
    }
}


fun Sequence<File>.checksum() = sumOf { it.readText().sumOf { it.code } }
interface FileGenerator {
    fun file(name: String): Writer
}

fun processFiles(
    log: Appendable,
    version: Int,
    dependencies: Sequence<File>,
    lockFile: File,
    destinationFolder: File,
    action: FileGenerator.() -> Unit
) {
    lockFile.parentFile.mkdirs()
    val dependenciesFile = File(lockFile.absolutePath + ".dependencies")
    if (!dependenciesFile.exists()) dependenciesFile.createNewFile()
    dependenciesFile.appendText(dependencies.joinToString("\n") + "\n\n")
    val hash = dependencies.checksum() + version
    val runningFile = File(lockFile.absolutePath + ".running")
    val pastHashesFile = File(lockFile.absolutePath + ".past")
    if (!pastHashesFile.exists()) pastHashesFile.createNewFile()
    var count = 0
    while (!runningFile.createNewFile() && count++ < 50) {
        Thread.sleep(100)
        log.appendLine("Waiting on lock...")
    }
    if (count >= 50) throw Exception("Waited, could not get lock")
    log.appendLine("Running...")
    val hashFromFile = lockFile.takeIf { it.exists() }?.readText()?.toIntOrNull()
    lockFile.writeText(hash.toString())
    pastHashesFile.appendText(hash.toString() + "\n")
    try {
        log.appendLine("Hash comparison: $hash vs $hashFromFile")
        if (hash != hashFromFile) {
            log.appendLine("Running the action!")
            destinationFolder.deleteRecursively()
            destinationFolder.mkdirs()
            action(object : FileGenerator {
                override fun file(name: String): Writer {
                    return destinationFolder.resolve(name).also {
                        it.parentFile.mkdirs()
                    }.bufferedWriter()
                }
            })
        }
    } finally {
        runningFile.delete()
        log.appendLine("Done.")
    }
}
