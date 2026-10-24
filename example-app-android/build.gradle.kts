// The Android app shell for :example-app.  AGP 9 doesn't allow com.android.application in a KMP module, so the
// code (including MainActivity and MainApplication) stays in :example-app and this module only packages it.
plugins {
    alias(libs.plugins.androidApplication)
}

group = "com.lightningkite"
version = "1.0-SNAPSHOT"

android {
    // Must differ from :example-app's namespace; applicationId is what identifies the installed app.
    namespace = "com.lightningkite.mppexampleapp.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lightningkite.kiteuiexample"
        minSdk = 24  // library-skia (Skiko) requires API 24+
        targetSdk = 36
        versionCode = 1
        versionName = project.version.toString()

        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(project(":example-app"))
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}
