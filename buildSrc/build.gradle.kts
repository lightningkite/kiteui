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
val src = file("../gradle-plugin/src/main/kotlin")
val dest = file("src/main/kotlin")

src.walkTopDown().filter { it.isFile }.forEach {
    val out = dest.resolve(it.relativeTo(src))
    out.parentFile.mkdirs()
    it.copyTo(out, overwrite = true)
}