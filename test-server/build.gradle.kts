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

// `./gradlew :test-server:run` serves on the port the library tests use, for trying the example app against it by hand.
// The server stops when its stdin closes, so it is given the terminal's.
tasks.named<JavaExec>("run") {
    args("18787")
    standardInput = System.`in`
}
