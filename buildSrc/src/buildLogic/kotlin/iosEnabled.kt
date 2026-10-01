import org.gradle.api.Project

/**
 * Whether this build declares iOS targets. Defaults to true only on macOS, overridable with `-Pkiteui.ios=true|false`.
 *
 * :library uses cinterop, which needs Xcode, so its iOS klibs can't be built on other hosts. Declaring iOS targets
 * there anyway makes modules without cinterop (e.g. :test-utilities) cross-compile their iOS code against :library
 * output that doesn't exist, and makes a locally published build advertise iOS variants it doesn't contain.
 */
val Project.iosEnabled: Boolean
    get() = providers.gradleProperty("kiteui.ios").orNull?.toBoolean()
        ?: System.getProperty("os.name").contains("Mac", ignoreCase = true)
