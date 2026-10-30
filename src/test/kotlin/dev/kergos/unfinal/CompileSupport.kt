@file:OptIn(org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi::class)

package dev.kergos.unfinal

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import java.lang.reflect.Modifier
import kotlin.test.assertEquals

internal val javaStubs = listOf(
    SourceFile.java(
        "Level.java",
        """
        package javadefault;

        public enum Level { EAGER, LAZY }
        """,
    ),
    SourceFile.java(
        "JavaLazy.java",
        """
        package javadefault;

        public @interface JavaLazy {
            Level value() default Level.LAZY;
        }
        """,
    ),
)

internal const val SUBJECT_CLASS = "User"

internal fun compile(
    classAnnotations: List<String> = emptyList(),
    propertyAnnotations: List<String> = emptyList(),
    methodAnnotations: List<String> = emptyList(),
    source: String,
    extraSources: List<SourceFile> = emptyList(),
): JvmCompilationResult {
    val options = classAnnotations.selectors("allPropertiesInClassAnnotation") +
        propertyAnnotations.selectors("propertyAnnotation") +
        methodAnnotations.selectors("methodAnnotation")

    return compileWithOptions(options, source, extraSources)
}

internal fun compileWithoutOptions(source: String): JvmCompilationResult = compileWithOptions(emptyList(), source)

private fun List<String>.selectors(option: String) = map { PluginOption(PLUGIN_ID, option, it) }

private fun compileWithOptions(
    options: List<PluginOption>,
    source: String,
    extraSources: List<SourceFile> = emptyList(),
): JvmCompilationResult {
    val result = compileRawOptions(options, source, extraSources)

    assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode, "compilation failed:\n${result.messages}")
    return result
}

/** For the cases that expect the compiler to reject the options instead of compiling. */
internal fun compileRawOptions(
    options: List<PluginOption>,
    source: String,
    extraSources: List<SourceFile> = emptyList(),
): JvmCompilationResult = KotlinCompilation().apply {
    sources = listOf(SourceFile.kotlin("$SUBJECT_CLASS.kt", source)) + javaStubs + extraSources
    compilerPluginRegistrars = listOf(UnfinalRegistrar())
    commandLineProcessors = listOf(UnfinalCommandLineProcessor())
    pluginOptions = options
    inheritClassPath = true
    messageOutputStream = System.out
}.compile()

internal fun JvmCompilationResult.fieldIsFinal(name: String, className: String = SUBJECT_CLASS): Boolean =
    Modifier.isFinal(classLoader.loadClass(className).getDeclaredField(name).modifiers)

internal fun JvmCompilationResult.classIsFinal(className: String = SUBJECT_CLASS): Boolean =
    Modifier.isFinal(classLoader.loadClass(className).modifiers)

internal fun JvmCompilationResult.methodIsFinal(name: String, className: String = SUBJECT_CLASS): Boolean =
    Modifier.isFinal(classLoader.loadClass(className).getDeclaredMethod(name).modifiers)
