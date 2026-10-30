package dev.kergos.unfinal

import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

data class Options(
    /** Classes carrying one of these annotations have every `val` unfinalized. */
    val allPropertiesInClassAnnotation: List<AnnotationSelector> = emptyList(),
    /** `val` properties carrying one of these annotations are unfinalized, in any class. */
    val propertyAnnotation: List<AnnotationSelector> = emptyList(),
    /** Methods carrying one of these annotations are opened, in any class. */
    val methodAnnotation: List<AnnotationSelector> = emptyList(),
) {
    val isEmpty: Boolean
        get() = allPropertiesInClassAnnotation.isEmpty() && propertyAnnotation.isEmpty() && methodAnnotation.isEmpty()
}

/** Command-line name and configuration key of each option, spelled once. */
internal enum class PluginOption(val optionName: String) {
    ALL_PROPERTIES_IN_CLASS("allPropertiesInClassAnnotation"),
    PROPERTY("propertyAnnotation"),
    METHOD("methodAnnotation"),
    ;

    val key = CompilerConfigurationKey.create<List<AnnotationSelector>>(optionName)
}

internal fun CompilerConfiguration.options() = Options(
    allPropertiesInClassAnnotation = get(PluginOption.ALL_PROPERTIES_IN_CLASS.key) ?: emptyList(),
    propertyAnnotation = get(PluginOption.PROPERTY.key) ?: emptyList(),
    methodAnnotation = get(PluginOption.METHOD.key) ?: emptyList(),
)

internal fun CompilerConfiguration.addSelector(option: PluginOption, selector: AnnotationSelector) {
    put(option.key, (get(option.key) ?: emptyList()) + selector)
}
