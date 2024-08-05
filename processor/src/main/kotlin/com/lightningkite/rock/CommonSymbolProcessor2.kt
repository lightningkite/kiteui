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
    abstract fun process2(resolver: Resolver, files: Set<KSFile>)
    abstract fun interestedIn(resolver: Resolver): Set<KSFile>

    private lateinit var fileCreator: (dependencies: Dependencies, packageName: String, fileName: String, extensionName: String) -> Writer

    private var invoked = false
    final override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return listOf()
        invoked = true

        val interestedIn = interestedIn(resolver)

        myCodeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            fileName = "$myId",
            extensionName = "txt",
            packageName = "com.lightningkite.lightningserver"
        ).writer().use {
            it.appendLine("Will generate in common folder; analyzed files below")
            interestedIn.forEach { f -> it.appendLine(f.filePath) }
        }
        val outSample = myCodeGenerator.generatedFile.first().absoluteFile
        val projectFolder = generateSequence(outSample) { it.parentFile!! }
            .first { it.name == "build" }
            .parentFile!!
        val common = interestedIn.any { it.filePath.contains("/src/common", true) }
        val flavor = outSample.path.split(File.separatorChar)
            .dropWhile { it != "ksp" }
            .drop(2)
            .first()
            .let {
                if (it.contains("test", true)) "Test"
                else "Main"
            }
        val outFolder = projectFolder.resolve("build/generated/ksp/common/common$flavor/kotlin")
        outFolder.mkdirs()

        if(common) {
            processFiles(
                version = version,
                dependencies = interestedIn.asSequence().map { it.filePath.let(::File) },
                lockFile = outFolder.resolve("$myId.lock"),
                destinationFolder = outFolder.resolve(myId).also { it.mkdirs() },
                action = {
                    fileCreator = label@{ _, packageName, fileName, extensionName ->
                        val packagePath = packageName.split('.').filter { it.isNotBlank() }.joinToString("") { "$it/" }
                        this.file("${packagePath}$fileName.$extensionName")
                    }
                    process2(resolver, interestedIn)
                }
            )
        } else {
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



fun Sequence<File>.checksum() = sumOf { it.readText().hashCode() }
interface FileGenerator {
    fun file(name: String): Writer
}

fun processFiles(
    version: Int,
    dependencies: Sequence<File>,
    lockFile: File,
    destinationFolder: File,
    action: FileGenerator.() -> Unit
) {
    val hash = dependencies.checksum() + version
    while (true) {
        try {
            RandomAccessFile(lockFile, "rw").channel.use { channel ->
                channel.lock().use {
                    channel.position(0)
                    val lastHash = channel.readInt()
                    if (lastHash == hash) return
                    val temp = Files.createTempDirectory("kspTemp").toFile()
                    try {
                        action(object : FileGenerator {
                            override fun file(name: String): Writer {
                                return temp.resolve(name).also {
                                    it.parentFile.mkdirs()
                                }.bufferedWriter()
                            }
                        })
                        destinationFolder.deleteRecursively()
                        temp.renameTo(destinationFolder)
                    } catch (e: Exception) {
                        // abandon
                        e.printStackTrace()
                    }
                    val out = ByteBuffer.allocate(4)
                    out.putInt(hash)
                    out.flip()
                    channel.position(0).write(out)
                }
            }
            break
        } catch (e: OverlappingFileLockException) {
            Thread.sleep(100)
        }
    }
}

fun FileChannel.readInt(): Int {
    position(0)
    val buff = ByteBuffer.allocate(1024)
    var total = 0
    while (total < 4) {
        val bytesRead = read(buff)
        if (bytesRead == -1) return 0
        total += bytesRead
    }
    buff.flip()
    return buff.getInt()
}