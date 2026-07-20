package scan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration

class UsageScanRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val outputDir = configuration.get(KEY_OUTPUT) ?: return
        val mode = configuration.get(KEY_MODE) ?: "refs"
        val prefixes = (configuration.get(KEY_PREFIXES) ?: "com.lightningkite.kiteui")
            .split(",").map { it.trim() }.filter { it.isNotEmpty() }
        IrGenerationExtension.registerExtension(UsageIrExtension(outputDir, mode, prefixes))
    }
}
