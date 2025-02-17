plugins {
    `kotlin-dsl`
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.0.20")
    implementation("org.apache.pdfbox:fontbox:2.0.27")
}
kotlin {
    jvmToolchain(17)
}
val src = file("../gradle-plugin/src/main/kotlin")
val dest = file("src/main/kotlin")

src.walkTopDown().filter { it.isFile }.forEach {
    val out = dest.resolve(it.relativeTo(src))
    out.parentFile.mkdirs()
    it.copyTo(out, overwrite = true)
}