import com.lightningkite.deployhelpers.*
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    // alias(libs.plugins.dokka)
    signing
    id("com.vanniktech.maven.publish") version "0.30.0"
}

val kotlinVersion:String by project
val kspVersion:String by project
dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$kspVersion")
    implementation("org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    testImplementation("junit:junit:4.13.2")
}

mavenPublishing {
    // publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates(group.toString(), name, version.toString())
    pom {
        name.set("KiteUI-Processor")
        description.set("Automatically create your routers")
        github("lightningkite", "kiteui")

        licenses {
            mit()
        }

        developers {
            joseph()
            brady()
        }
    }

}