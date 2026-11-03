plugins {
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    kotlin("jvm")
    alias(libs.plugins.kotlin.plugin.serialization)
    application
}

application {
    mainClass.set("com.lightningkite.kiteui.aidriver.MainKt")
}

kotlin {
    compilerOptions {
        optIn.addAll("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    implementation(libs.lightning.server.core)
    implementation(libs.lightning.server.engine.ktor)
    implementation(libs.lightning.server.typed)
    implementation(libs.ktor.server.netty)
    testImplementation(kotlin("test"))
}

// Fat JAR task using Gradle's built-in Jar
tasks.register<Jar>("fatJar") {
    archiveBaseName.set("kiteui-ai-driver")
    archiveClassifier.set("")
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.lightningkite.kiteui.aidriver.MainKt"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    with(tasks.jar.get())
}

// Task to install the server JAR globally
tasks.register("installDriver") {
    dependsOn("fatJar")
    doLast {
        val homeDir = System.getProperty("user.home")
        val binDir = file("$homeDir/.kiteui/bin")
        val jarDir = file("$homeDir/.kiteui/ai-driver")
        binDir.mkdirs()
        jarDir.mkdirs()

        val fatJar = tasks.named<Jar>("fatJar").get().archiveFile.get().asFile
        fatJar.copyTo(file("$jarDir/kiteui-ai-driver.jar"), overwrite = true)

        // Unix bash script
        val script = file("$binDir/kiteui-drive")
        script.writeText(buildString {
            appendLine("#!/bin/bash")
            appendLine("""PORT="${'$'}{KITEUI_DRIVER_PORT:-7474}"""")
            appendLine("""SERVER="http://127.0.0.1:${'$'}PORT"""")
            appendLine("""JAR="${'$'}HOME/.kiteui/ai-driver/kiteui-ai-driver.jar"""")
            appendLine("""PIDFILE="${'$'}HOME/.kiteui/ai-driver/server.pid"""")
            appendLine()
            appendLine("ensure_running() {")
            appendLine("""    curl -s --max-time 1 -X POST "${'$'}SERVER" -d "ls" >/dev/null 2>&1 && return 0""")
            appendLine("""    [ -f "${'$'}JAR" ] || { echo "ERROR: ${'$'}JAR not found. Run: ./gradlew :ai-driver-server:installDriver"; exit 1; }""")
            appendLine("""    java -jar "${'$'}JAR" "${'$'}PORT" > "${'$'}HOME/.kiteui/ai-driver/server.log" 2>&1 &""")
            appendLine("""    echo ${'$'}! > "${'$'}PIDFILE"""")
            appendLine("""    for i in ${'$'}(seq 1 30); do""")
            appendLine("""        curl -s --max-time 1 -X POST "${'$'}SERVER" -d "ls" >/dev/null 2>&1 && return 0""")
            appendLine("        sleep 0.5")
            appendLine("    done")
            appendLine("""    echo "ERROR: Server failed to start"; exit 1""")
            appendLine("}")
            appendLine()
            appendLine("""case "${'$'}{1:-}" in""")
            appendLine("""    stop)  [ -f "${'$'}PIDFILE" ] && kill "${'$'}(cat "${'$'}PIDFILE")" 2>/dev/null && rm "${'$'}PIDFILE"; echo "Stopped" ;;""")
            appendLine("""    start) ensure_running && echo "Server running on port ${'$'}PORT" ;;""")
            appendLine("""    *)     ensure_running""")
            appendLine("""           # Join args with tabs so quoted strings with spaces survive""")
            appendLine("""           IFS=${'$'}'\t'; payload="${'$'}*"; unset IFS""")
            appendLine("""           result=${'$'}(curl -s -X POST "${'$'}SERVER" --data-raw "${'$'}payload")""")
            appendLine("""           printf '%s\n' "${'$'}result" ;;""")
            appendLine("esac")
        })
        script.setExecutable(true)

        // Windows batch script
        val batScript = file("$binDir/kiteui-drive.bat")
        batScript.writeText(buildString {
            appendLine("@echo off")
            appendLine("setlocal enabledelayedexpansion")
            appendLine()
            appendLine("if defined KITEUI_DRIVER_PORT (set PORT=%KITEUI_DRIVER_PORT%) else (set PORT=7474)")
            appendLine("set SERVER=http://127.0.0.1:%PORT%")
            appendLine("set JAR=%USERPROFILE%\\.kiteui\\ai-driver\\kiteui-ai-driver.jar")
            appendLine("set PIDFILE=%USERPROFILE%\\.kiteui\\ai-driver\\server.pid")
            appendLine("set LOGFILE=%USERPROFILE%\\.kiteui\\ai-driver\\server.log")
            appendLine()
            appendLine("if \"%~1\"==\"stop\" goto :stop")
            appendLine("if \"%~1\"==\"start\" goto :start")
            appendLine("goto :command")
            appendLine()
            appendLine(":ensure_running")
            appendLine("curl -s --max-time 1 -X POST \"%SERVER%\" -d \"ls\" >nul 2>&1 && exit /b 0")
            appendLine("if not exist \"%JAR%\" (")
            appendLine("    echo ERROR: %JAR% not found. Run: gradlew :ai-driver-server:installDriver")
            appendLine("    exit /b 1")
            appendLine(")")
            appendLine("start /b \"\" javaw -jar \"%JAR%\" %PORT% > \"%LOGFILE%\" 2>&1")
            appendLine("set /a TRIES=0")
            appendLine(":wait_loop")
            appendLine("if !TRIES! geq 30 (")
            appendLine("    echo ERROR: Server failed to start")
            appendLine("    exit /b 1")
            appendLine(")")
            appendLine("timeout /t 1 /nobreak >nul")
            appendLine("curl -s --max-time 1 -X POST \"%SERVER%\" -d \"ls\" >nul 2>&1 && exit /b 0")
            appendLine("set /a TRIES+=1")
            appendLine("goto :wait_loop")
            appendLine()
            appendLine(":stop")
            appendLine("if exist \"%PIDFILE%\" (")
            appendLine("    set /p PID=<\"%PIDFILE%\"")
            appendLine("    taskkill /pid !PID! /f >nul 2>&1")
            appendLine("    del \"%PIDFILE%\"")
            appendLine(")")
            appendLine("echo Stopped")
            appendLine("goto :eof")
            appendLine()
            appendLine(":start")
            appendLine("call :ensure_running")
            appendLine("if errorlevel 1 goto :eof")
            appendLine("echo Server running on port %PORT%")
            appendLine("goto :eof")
            appendLine()
            appendLine(":command")
            appendLine("call :ensure_running")
            appendLine("if errorlevel 1 goto :eof")
            appendLine("rem Join args with tabs")
            appendLine("set PAYLOAD=%~1")
            appendLine("shift")
            appendLine(":arg_loop")
            appendLine("if \"%~1\"==\"\" goto :send")
            appendLine("set \"PAYLOAD=!PAYLOAD!\t%~1\"")
            appendLine("shift")
            appendLine("goto :arg_loop")
            appendLine(":send")
            appendLine("for /f \"delims=\" %%r in ('curl -s -X POST \"%SERVER%\" --data-raw \"%PAYLOAD%\"') do echo %%r")
        })

        println("Installed kiteui-drive to $binDir/kiteui-drive")
        if (System.getProperty("os.name").lowercase().contains("win")) {
            println("Installed kiteui-drive.bat to $binDir/kiteui-drive.bat")
        }
        println("Add $binDir to your PATH if not already there.")
    }
}
