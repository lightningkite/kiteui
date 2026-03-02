// by Claude - AI driver server: JVM daemon + CLI tool
plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinPluginSerialization)
    application
}

kotlin {
    jvmToolchain(17)
}

val ktorVersion = "3.3.1"

dependencies {
    implementation(project(":library"))
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.kotlinxCoroutinesCore)
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
}

application {
    mainClass.set("com.lightningkite.kiteui.aidriver.server.MainKt")
}
