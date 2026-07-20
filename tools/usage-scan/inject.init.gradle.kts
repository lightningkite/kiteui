// Injects the usage-scan compiler plugin into every Kotlin compilation of the target build,
// without editing any of the target's build files.
//
// Configured via environment variables:
//   USAGE_SCAN_JAR       absolute path to plugin.jar
//   USAGE_SCAN_OUT       absolute path to output directory for report files
//   USAGE_SCAN_MODE      "refs" (default) or "decls"
//   USAGE_SCAN_PREFIXES  comma-separated package prefixes (default com.lightningkite.kiteui)
//
// Tasks are matched by name and configured by REFLECTION so this script needs no Kotlin Gradle
// plugin on its own classpath (which would otherwise fail to match the build's task classes due
// to Gradle's init-script classloader isolation).

val jar = System.getenv("USAGE_SCAN_JAR") ?: error("USAGE_SCAN_JAR not set")
val out = System.getenv("USAGE_SCAN_OUT") ?: error("USAGE_SCAN_OUT not set")
val mode = System.getenv("USAGE_SCAN_MODE") ?: "refs"
val prefixes = System.getenv("USAGE_SCAN_PREFIXES") ?: "com.lightningkite.kiteui"

val pluginArgs = listOf(
    "-Xplugin=$jar",
    "-P", "plugin:com.lightningkite.usage-scan:outputDir=$out",
    "-P", "plugin:com.lightningkite.usage-scan:mode=$mode",
    "-P", "plugin:com.lightningkite.usage-scan:prefixes=$prefixes",
)

// Matches compileKotlinJvm, compileKotlinJs, compileKotlinJvmSsr, compileKotlinIosArm64,
// compile{Debug,Release}KotlinAndroid, compileTestKotlin*, etc.
val kotlinCompileName = Regex("^compile.*[Kk]otlin.*")

allprojects {
    tasks.configureEach {
        if (!kotlinCompileName.matches(name)) return@configureEach
        val task = this
        try {
            val compilerOptions = task.javaClass.getMethod("getCompilerOptions").invoke(task) ?: return@configureEach
            val freeArgsProp = compilerOptions.javaClass.getMethod("getFreeCompilerArgs").invoke(compilerOptions)
            // ListProperty has several add/addAll overloads; pick addAll(Iterable) to avoid the
            // add(Provider) overload (which would throw "argument type mismatch" for a String).
            val addAll = freeArgsProp.javaClass.methods.first {
                it.name == "addAll" && it.parameterCount == 1 && it.parameterTypes[0] == java.lang.Iterable::class.java
            }
            addAll.invoke(freeArgsProp, pluginArgs)
            logger.lifecycle("usage-scan: injected into ${task.path}")
        } catch (e: NoSuchMethodException) {
            // Not a Kotlin compile task after all; ignore.
        }
    }
}
