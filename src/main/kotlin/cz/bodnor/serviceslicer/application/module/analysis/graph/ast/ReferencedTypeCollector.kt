package cz.bodnor.serviceslicer.application.module.analysis.graph.ast

import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.type.ClassOrInterfaceType
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import com.github.javaparser.resolution.types.ResolvedReferenceType

class ReferencedTypeCollector : VoidVisitorAdapter<MutableSet<String>>() {

    override fun visit(
        n: ClassOrInterfaceType,
        collector: MutableSet<String>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n)?.qualifiedName?.let { collector += it }
    }

    override fun visit(
        n: ObjectCreationExpr,
        collector: MutableSet<String>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n)?.qualifiedName?.let { collector += it }
    }

    override fun visit(
        n: MethodCallExpr,
        collector: MutableSet<String>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n)?.qualifiedName?.let { collector += it }
    }

    override fun visit(
        n: VariableDeclarator,
        collector: MutableSet<String>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n.type)?.qualifiedName?.let { collector += it }
    }

    override fun visit(
        n: FieldDeclaration,
        collector: MutableSet<String>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n.elementType)?.qualifiedName?.let { collector += it }
    }

    override fun visit(
        n: MethodDeclaration,
        collector: MutableSet<String>,
    ) {
        super.visit(n, collector)
        resolveTypeSafely(n.type)?.qualifiedName?.let { collector += it }
        n.parameters.forEach { param ->
            resolveTypeSafely(param.type)?.qualifiedName?.let { collector += it }
        }
    }

    private fun resolveTypeSafely(expr: Expression): ResolvedReferenceType? = runCatching {
        expr.calculateResolvedType()
    }.getOrNull()?.let { if (it.isReferenceType) it.asReferenceType() else null }

    private fun resolveTypeSafely(type: com.github.javaparser.ast.type.Type): ResolvedReferenceType? =
        runCatching { type.resolve() }.getOrNull()?.let { if (it.isReferenceType) it.asReferenceType() else null }
}
