package scan

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration

class UsageScanCommandLineProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption("outputDir", "<path>", "Directory to write report files", required = true),
        CliOption("mode", "decls|refs", "Dump declarations, or collect references", required = true),
        CliOption("prefixes", "<pkg,pkg>", "Comma-separated watched package prefixes", required = false),
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option.optionName) {
            "outputDir" -> configuration.put(KEY_OUTPUT, value)
            "mode" -> configuration.put(KEY_MODE, value)
            "prefixes" -> configuration.put(KEY_PREFIXES, value)
            else -> error("Unknown usage-scan option: ${option.optionName}")
        }
    }
}
