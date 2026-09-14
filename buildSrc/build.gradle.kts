plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.10")
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
// The plugin is self-hosted here so this repo's own example-app can use it without publishing.
// Its logic now lives in :codegen, which buildSrc cannot depend on, so both source trees are copied.
listOf(file("../gradle-plugin/src/main/kotlin"), file("../codegen/src/main/kotlin")).forEach { src ->
    val dest = file("src/main/kotlin")
    src.walkTopDown().filter { it.isFile }.forEach {
        val out = dest.resolve(it.relativeTo(src))
        out.parentFile.mkdirs()
        it.copyTo(out, overwrite = true)
    }
}