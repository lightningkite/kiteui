package scan

import org.jetbrains.kotlin.config.CompilerConfigurationKey

/** Directory the plugin writes its per-compilation report files into. */
val KEY_OUTPUT: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("usage-scan output dir")

/** "decls" = dump every declaration in a watched package; "refs" = record every reference INTO a watched package. */
val KEY_MODE: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("usage-scan mode")

/** Comma-separated package prefixes to watch, e.g. "com.lightningkite.kiteui". */
val KEY_PREFIXES: CompilerConfigurationKey<String> = CompilerConfigurationKey.create("usage-scan prefixes")

const val PLUGIN_ID: String = "com.lightningkite.usage-scan"
