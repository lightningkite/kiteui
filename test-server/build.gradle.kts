plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.lightningkite.kiteui.testserver.MainKt")
}

dependencies {
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.websockets)
}
