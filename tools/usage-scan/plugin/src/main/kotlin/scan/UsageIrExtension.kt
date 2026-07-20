package scan

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.ir.visitors.acceptVoid
import java.io.File
import java.util.UUID

/**
 * Walks the IR of a compilation and records, as stable string keys, either:
 *  - every declaration that lives in a watched package ("decls" mode, run over the library), or
 *  - every reference INTO a watched package ("refs" mode, run over downstream code).
 *
 * Keys are intentionally overload-insensitive (fully-qualified name + kind, no parameter types).
 * Collapsing overloads biases the union toward "keep public", which is the safe direction for a
 * visibility decision: at worst a rarely-used overload stays public.
 */
class UsageIrExtension(
    private val outputDir: String,
    private val mode: String,
    private val prefixes: List<String>,
) : IrGenerationExtension {

    private val found = LinkedHashSet<String>()

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val collectRefs = mode != "decls"
        moduleFragment.acceptVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                if (collectRefs && element is IrExpression) recordType(element.type)
                if (collectRefs && element is IrDeclarationReference) recordSymbol(element.symbol)
                if (!collectRefs && element is IrDeclaration) recordOwnDeclaration(element)
                if (collectRefs && element is IrDeclaration) recordDeclarationTypes(element)
                element.acceptChildrenVoid(this)
            }
        })
        write(moduleFragment.name.asString())
    }

    /** In decls mode: emit the key of any declaration that lives in a watched package. */
    private fun recordOwnDeclaration(declaration: IrDeclaration) {
        when (declaration) {
            is IrClass -> emitClass(declaration.fqNameWhenAvailable?.asString())
            is IrFunction -> if (!declaration.isFakeOverride) emitCallable(declaration.fqNameWhenAvailable?.asString())
            is IrProperty -> if (!declaration.isFakeOverride) emitCallable(declaration.fqNameWhenAvailable?.asString())
            is IrField -> emitCallable(declaration.fqNameWhenAvailable?.asString())
            else -> {}
        }
    }

    /** In refs mode: a call/field-access/reference resolves to some owner declaration. */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun recordSymbol(symbol: IrSymbol) {
        val owner = symbol.owner as? IrDeclaration ?: return
        when (owner) {
            is IrConstructorCall -> {}
            is IrClass -> emitClass(owner.fqNameWhenAvailable?.asString())
            is IrFunction -> {
                emitCallable(owner.fqNameWhenAvailable?.asString())
                (owner.parent as? IrClass)?.let { emitClass(it.fqNameWhenAvailable?.asString()) }
            }
            is IrProperty -> emitCallable(owner.fqNameWhenAvailable?.asString())
            is IrField -> emitCallable(owner.fqNameWhenAvailable?.asString())
            else -> {}
        }
    }

    /** In refs mode: types used in downstream declaration signatures (params, returns, supertypes). */
    private fun recordDeclarationTypes(declaration: IrDeclaration) {
        when (declaration) {
            is IrFunction -> {
                recordType(declaration.returnType)
                declaration.parameters.forEach { recordType(it.type) }
            }
            is IrProperty -> declaration.backingField?.let { recordType(it.type) }
            is IrField -> recordType(declaration.type)
            is IrClass -> declaration.superTypes.forEach { recordType(it) }
            else -> {}
        }
    }

    private fun recordType(type: IrType?) {
        if (type == null) return
        emitClass(type.classFqName?.asString())
        (type as? IrSimpleType)?.arguments?.forEach { arg ->
            (arg as? IrType)?.let { recordType(it) }
        }
    }

    private fun watched(fqName: String?): Boolean =
        fqName != null && prefixes.any { fqName == it || fqName.startsWith("$it.") }

    private fun emitClass(fqName: String?) {
        if (watched(fqName)) found.add("CLASS:$fqName")
    }

    private fun emitCallable(fqName: String?) {
        if (watched(fqName)) found.add("CALL:$fqName")
    }

    private fun write(moduleName: String) {
        if (found.isEmpty()) return
        val dir = File(outputDir).apply { mkdirs() }
        val safe = moduleName.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        val out = File(dir, "$safe.$mode.${UUID.randomUUID()}.txt")
        out.writeText(found.sorted().joinToString("\n", postfix = "\n"))
    }
}
