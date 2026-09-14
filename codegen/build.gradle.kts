import com.lightningkite.deployhelpers.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    signing
    alias(libs.plugins.vannitechPublishing)
    alias(libs.plugins.dokka)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.fontbox)
    testImplementation(libs.junit)
}

lkLibrary("lightningkite", "kiteui") {
    description.set("KiteUI code generation, independent of any build tool")
}
