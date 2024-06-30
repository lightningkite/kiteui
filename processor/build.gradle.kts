import com.lightningkite.deployhelpers.developer
import com.lightningkite.deployhelpers.github
import com.lightningkite.deployhelpers.mit
import com.lightningkite.deployhelpers.standardPublishing

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.dokka)
    id("signing")
    `maven-publish`
}

val kotlinVersion:String by project
val kspVersion:String by project
dependencies {
    implementation(libs.kspSymbolProcessing)
    implementation(libs.kotlinCompiler)
    implementation(libs.kotlinStdLib)
    testImplementation(libs.kotlinTestJunit)
}

standardPublishing {
    name.set("KiteUI-Processor")
    description.set("Automatically create your routers")
    github("lightningkite", "kiteui")

    licenses {
        mit()
    }

    developers {
        developer(
            id = "LightningKiteJoseph",
            name = "Joseph Ivie",
            email = "joseph@lightningkite.com",
        )
        developer(
            id = "bjsvedin",
            name = "Brady Svedin",
            email = "brady@lightningkite.com",
        )
    }
}

