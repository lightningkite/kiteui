package com.lightningkite.kiteui

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import java.io.BufferedWriter
import java.io.File

abstract class CommonSymbolProcessor(
    private val myCodeGenerator: CodeGenerator
): SymbolProcessor {
    abstract fun process2(resolver: Resolver)

    lateinit var fileCreator: (dependencies: Dependencies, packageName: String, fileName: String, extensionName: String) -> BufferedWriter

    var invoked = false
    final override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return listOf()
        invoked = true

        val stub = myCodeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            fileName = "test",
            extensionName = "txt",
            packageName = "com.lightningkite.lightningserver"
        ).writer().use { println("Will generate in common folder") }
        val outSample = myCodeGenerator.generatedFile.first().absoluteFile
        val projectFolder = generateSequence(outSample) { it.parentFile!! }
            .first { it.name == "build" }
            .parentFile!!
        val flavor = outSample.path.split(File.separatorChar)
            .dropWhile { it != "ksp" }
            .drop(2)
            .first()
            .let {
                if(it.contains("test", true)) "Test"
                else "Main"
            }
        val outFolder = projectFolder.resolve("build/generated/ksp/common/common$flavor/kotlin")
        outFolder.mkdirs()
        val createdFiles = HashSet<File>()
        val common = resolver.getAllFiles().any { it.filePath?.contains("/src/common", true) == true }

        // Acquire lock
        val lockFile = projectFolder.resolve("build/generated/ksp/.kiteui-lock")
        if(lockFile.exists()) {
            // A different process is doing KSP for us; bail after waiting a bit
            while(lockFile.exists()) {
                Thread.sleep(1000L)
            }
            return listOf()
        }
        try {
            lockFile.createNewFile()

            fileCreator = label@{ dependencies, packageName, fileName, extensionName ->
                if (!common) return@label myCodeGenerator.createNewFile(
                    dependencies,
                    packageName,
                    fileName,
                    extensionName
                ).bufferedWriter()
                val packagePath = packageName.split('.').filter { it.isNotBlank() }.joinToString("") { "$it/" }
                outFolder.resolve("${packagePath}$fileName.$extensionName")
                    .also { it.parentFile.mkdirs() }
                    .also { createdFiles += outFolder.resolve(it) }
                    .bufferedWriter()
            }
            process2(resolver)
            val manifest = outFolder.parentFile!!.resolve("kiteui-manifest.txt")
            manifest.takeIf { it.exists() }?.readLines()
                ?.map { File(it) }
                ?.toSet()
                ?.minus(createdFiles)
//            ?.forEach { outFolder.resolve(it).takeIf { it.exists() }?.delete() }
            manifest.writeText(createdFiles.joinToString("\n") + "\n")
        } finally {
            lockFile.delete()
        }
        return listOf()
    }

    fun createNewFile(dependencies: Dependencies, packageName: String, fileName: String, extensionName: String = "kt"): BufferedWriter {
        return fileCreator(dependencies, packageName, fileName, extensionName)
    }
}