package dev.kergos.unfinal

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

const val PLUGIN_ID = "dev.kergos.unfinal"

private const val SELECTOR_SYNTAX = "<annotation fqn>[#<member>[=<value>]]"

@OptIn(ExperimentalCompilerApi::class)
class UnfinalCommandLineProcessor : CommandLineProcessor {
    override val pluginId = PLUGIN_ID

    override val pluginOptions = listOf(
        cliOption(
            PluginOption.ALL_PROPERTIES_IN_CLASS,
            "Unfinalize every val property of classes carrying this annotation. Repeatable.",
        ),
        cliOption(
            PluginOption.PROPERTY,
            "Unfinalize only the val properties carrying this annotation. Repeatable.",
        ),
        cliOption(
            PluginOption.METHOD,
            "Open methods carrying this annotation. Repeatable.",
        ),
    )

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        val pluginOption = PluginOption.entries.firstOrNull { it.optionName == option.optionName }
            ?: throw CliOptionProcessingException("Unknown option: ${option.optionName}")

        configuration.addSelector(pluginOption, parseSelector(option.optionName, value))
    }
}

private fun cliOption(option: PluginOption, description: String) = CliOption(
    optionName = option.optionName,
    valueDescription = SELECTOR_SYNTAX,
    description = description,
    required = false,
    allowMultipleOccurrences = true,
)
