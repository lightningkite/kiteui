plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.4.20")
    implementation("org.apache.pdfbox:fontbox:3.0.8")
}
kotlin {
    jvmToolchain(17)
    // Build logic for this repo only. src/main/kotlin is a gitignored copy of the gradle-plugin sources (see below).
    sourceSets.main { kotlin.srcDir("src/buildLogic/kotlin") }
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}
val src = file("../gradle-plugin/src/main/kotlin")
val dest = file("src/main/kotlin")

src.walkTopDown().filter { it.isFile }.forEach {
    val out = dest.resolve(it.relativeTo(src))
    out.parentFile.mkdirs()
    it.copyTo(out, overwrite = true)
}