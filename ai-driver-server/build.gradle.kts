plugins {
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.lightningkite.kiteui.aidriver.MainKt")
}

dependencies {
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerWebsockets)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktorClientCio)
    testImplementation(libs.ktorClientWebsockets)
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

        println("Installed kiteui-drive to $binDir/kiteui-drive")
        println("Add $binDir to your PATH if not already there.")
    }
}
