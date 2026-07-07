plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.3.20")
    implementation("org.apache.pdfbox:fontbox:2.0.27")
}
kotlin {
    jvmToolchain(17)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}
val dest = file("src/main/kotlin")

// Mirror the plugin sources (KiteUiPlugin.kt, resource codegen, etc.)
val pluginSrc = file("../gradle-plugin/src/main/kotlin")
pluginSrc.walkTopDown().filter { it.isFile }.forEach {
    val out = dest.resolve(it.relativeTo(pluginSrc))
    out.parentFile.mkdirs()
    it.copyTo(out, overwrite = true)
}

// Mirror the companion sources (autoroute codegen helpers)
val companionSrc = file("../build-companion/src/main/kotlin")
companionSrc.walkTopDown().filter { it.isFile }.forEach {
    val out = dest.resolve(it.relativeTo(companionSrc))
    out.parentFile.mkdirs()
    it.copyTo(out, overwrite = true)
}