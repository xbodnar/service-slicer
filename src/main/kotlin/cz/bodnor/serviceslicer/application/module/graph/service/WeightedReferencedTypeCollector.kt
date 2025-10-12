package cz.bodnor.serviceslicer.application.module.graph.service

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedReferenceType

data class WeightedReference(
    var methodCalls: Int = 0,
    var fieldAccesses: Int = 0,
    var objectCreations: Int = 0,
    var typeReferences: Int = 0,
) {
    val totalWeight: Int
        get() = methodCalls + fieldAccesses + objectCreations + typeReferences
}

/**
 * Collects weighted references from a class to other types.
 *
 * Categories:
 * - typeReferences: Field types, method return types, parameters, local variables, extends/implements
 * - methodCalls: Method invocations on other types
 * - objectCreations: new SomeType() calls
 * - fieldAccesses: Accessing fields of other types (e.g., obj.field)
 */
class WeightedReferencedTypeCollector :
    com.github.javaparser.ast.visitor.VoidVisitorAdapter<MutableMap<String, WeightedReference>>() {

    // Track extends/implements relationships
    override fun visit(
        n: ClassOrInterfaceDeclaration,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)

        // Count extended types
        n.extendedTypes.forEach { extendedType ->
            resolveTypeSafely(extendedType)?.qualifiedName?.let { fqn ->
                collector.computeIfAbsent(fqn) { WeightedReference() }.typeReferences++
            }
        }

        // Count implemented interfaces
        n.implementedTypes.forEach { implementedType ->
            resolveTypeSafely(implementedType)?.qualifiedName?.let { fqn ->
                collector.computeIfAbsent(fqn) { WeightedReference() }.typeReferences++
            }
        }
    }

    // Track field type declarations
    override fun visit(
        n: FieldDeclaration,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n.elementType)?.qualifiedName?.let { fqn ->
            collector.computeIfAbsent(fqn) { WeightedReference() }.typeReferences++
        }
    }

    // Track method return types and parameters (but not local variables - handled separately)
    override fun visit(
        n: MethodDeclaration,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)

        // Return type
        resolveTypeSafely(n.type)?.qualifiedName?.let { fqn ->
            collector.computeIfAbsent(fqn) { WeightedReference() }.typeReferences++
        }

        // Parameters
        n.parameters.forEach { param ->
            resolveTypeSafely(param.type)?.qualifiedName?.let { fqn ->
                collector.computeIfAbsent(fqn) { WeightedReference() }.typeReferences++
            }
        }
    }

    // Track local variable declarations (inside methods)
    override fun visit(
        n: VariableDeclarator,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)

        // Only count if it's a local variable (not a field - those are handled by FieldDeclaration)
        // VariableDeclarator can appear in field declarations, so we skip those
        val parent = n.parentNode.orElse(null)
        val isField = parent?.parentNode?.orElse(null) is FieldDeclaration

        if (!isField) {
            resolveTypeSafely(n.type)?.qualifiedName?.let { fqn ->
                collector.computeIfAbsent(fqn) { WeightedReference() }.typeReferences++
            }
        }
    }

    // Track object instantiation
    override fun visit(
        n: ObjectCreationExpr,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n)?.qualifiedName?.let { fqn ->
            collector.computeIfAbsent(fqn) { WeightedReference() }.objectCreations++
        }
    }

    // Track method calls
    override fun visit(
        n: MethodCallExpr,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)

        // Try to get the type of the scope expression (the object the method is called on)
        // This is more accurate than declaringType() because we want to count against the actual type being used
        if (n.scope.isPresent) {
            val scopeType = resolveTypeSafely(n.scope.get())
            scopeType?.qualifiedName?.let { fqn ->
                collector.computeIfAbsent(fqn) { WeightedReference() }.methodCalls++
            }
        }
    }

    // Track field accesses (e.g., someObject.someField)
    override fun visit(
        n: FieldAccessExpr,
        collector: MutableMap<String, WeightedReference>,
    ) {
        super.visit(n, collector)

        // Resolve the type of the object whose field is being accessed
        resolveTypeSafely(n.scope)?.qualifiedName?.let { fqn ->
            collector.computeIfAbsent(fqn) { WeightedReference() }.fieldAccesses++
        }
    }

    private fun resolveTypeSafely(expr: Expression): ResolvedReferenceType? = runCatching {
        expr.calculateResolvedType()
    }.getOrNull()?.let { if (it.isReferenceType) it.asReferenceType() else null }

    private fun resolveTypeSafely(type: Type): ResolvedReferenceType? =
        runCatching { type.resolve() }.getOrNull()?.let { if (it.isReferenceType) it.asReferenceType() else null }
}
