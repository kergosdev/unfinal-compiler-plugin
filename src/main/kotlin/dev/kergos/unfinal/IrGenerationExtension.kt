@file:OptIn(org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI::class)

package dev.kergos.unfinal

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class UnfinalIrGenerationExtension(private val options: Options) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (options.isEmpty) return
        moduleFragment.transformChildrenVoid(UnfinalizeTransformer(options))
    }
}

private class UnfinalizeTransformer(private val options: Options) : IrElementTransformerVoid() {
    override fun visitClass(declaration: IrClass): IrStatement {
        val allProperties = options.allPropertiesInClassAnnotation.isNotEmpty() && declaration.isMarked(options)

        declaration.declarations.forEach { member ->
            when (member) {
                is IrProperty -> if (allProperties || member.isSelected()) member.unfinalize()
                is IrSimpleFunction -> if (member.isSelected()) member.open()
                else -> Unit
            }
        }
        return super.visitClass(declaration)
    }

    private fun IrProperty.isSelected() = options.propertyAnnotation.matchesAny(annotationCandidates())

    private fun IrSimpleFunction.isSelected() = options.methodAnnotation.matchesAny(annotations)
}

private fun IrClass.isMarked(options: Options): Boolean {
    val selectors = options.allPropertiesInClassAnnotation
    if (selectors.isEmpty()) return false

    val queued = mutableSetOf(this)
    val pending = ArrayDeque(listOf(this))
    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        if (selectors.matchesAny(current.annotations)) return true
        current.superTypes.forEach { type ->
            type.classOrNull?.owner?.takeIf(queued::add)?.let(pending::addLast)
        }
    }
    return false
}

private fun IrProperty.unfinalize() {
    if (isVar || isConst || isDelegated) return
    backingField?.takeIf { it.isFinal }?.let { it.isFinal = false }
}

private fun IrSimpleFunction.open() {
    if (modality == Modality.FINAL && visibility != DescriptorVisibilities.PRIVATE) modality = Modality.OPEN
}

private fun IrProperty.annotationCandidates(): List<IrAnnotation> {
    val parameter = (parent as? IrClass)?.primaryConstructor
        ?.parameters
        ?.firstOrNull { it.kind == IrParameterKind.Regular && it.name == name }

    return listOfNotNull(this, backingField, getter, parameter).flatMap { it.annotations }
}
