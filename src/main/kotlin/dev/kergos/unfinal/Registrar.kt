package dev.kergos.unfinal

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class UnfinalRegistrar : CompilerPluginRegistrar() {
    override val pluginId = PLUGIN_ID
    override val supportsK2 get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        IrGenerationExtension.registerExtension(UnfinalIrGenerationExtension(configuration.options()))
    }
}
