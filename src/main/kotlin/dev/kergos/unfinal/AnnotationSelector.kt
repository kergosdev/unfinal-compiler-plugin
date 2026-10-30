@file:OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)

package dev.kergos.unfinal

import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.FqName

data class AnnotationSelector(
    val fqName: FqName,
    val member: String? = null,
    val value: String? = null,
) {
    override fun toString(): String = buildString {
        append(fqName.asString())
        member?.let { append('#').append(it) }
        value?.let { append('=').append(it) }
    }

    internal fun matches(annotation: IrAnnotation): Boolean {
        if (annotation.annotationFqName() != fqName) return false
        val memberName = member ?: return true
        val expected = value ?: return annotation.argument(memberName) != null
        val actual = annotation.effectiveArgument(memberName) ?: return false
        return actual == expected || actual.endsWith(".$expected")
    }
}

internal fun IrAnnotation.effectiveArgument(name: String): String? {
    argument(name)?.constantText()?.let { return it }

    val property = classSymbol.owner.constructors
        .flatMap { it.parameters }
        .firstOrNull { it.name.asString() == name }
        ?: return null

    return property.defaultValue?.expression?.constantText()
}

internal fun parseSelector(option: String, value: String): AnnotationSelector {
    val text = value.trim()
    val hash = text.indexOf('#')
    val fqnPart = if (hash < 0) text else text.substring(0, hash)
    if (fqnPart.isEmpty()) {
        throw CliOptionProcessingException("Option '$option' needs an annotation class name, got '$value'.")
    }

    val fqName = try {
        FqName(fqnPart)
    } catch (_: IllegalArgumentException) {
        throw CliOptionProcessingException("Option '$option' has an invalid annotation class name '$fqnPart'.")
    }
    return if (hash < 0) {
        AnnotationSelector(fqName)
    } else {
        val rest = text.substring(hash + 1)
        val equals = rest.indexOf('=')
        val member = if (equals < 0) rest else rest.substring(0, equals)
        if (member.isEmpty()) throw CliOptionProcessingException("Option '$option' has an empty member in '$value'.")

        if (equals >= 0 && equals == rest.lastIndex) {
            throw CliOptionProcessingException("Option '$option' has an empty value in '$value'.")
        }
        AnnotationSelector(fqName, member, if (equals < 0) null else rest.substring(equals + 1))
    }
}

internal fun List<AnnotationSelector>.matchesAny(annotations: List<IrAnnotation>): Boolean =
    any { selector -> annotations.any(selector::matches) }
