@file:OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)

package dev.kergos.unfinal

import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

internal fun IrAnnotation.annotationFqName(): FqName? = classSymbol.fqNameWhenAvailable

internal fun IrAnnotation.argument(name: String): IrExpression? = argumentMapping[Name.identifier(name)]

/** Annotation argument as text, without a qualifier: enum entries keep only the entry name. */
internal fun IrExpression.constantText(): String? = when (this) {
    is IrConst -> value?.toString()
    is IrGetEnumValue -> symbol.owner.name.asString()
    else -> null
}
