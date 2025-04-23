package cz.bodnor.serviceslicer.application.module.analysis.graph

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import cz.bodnor.serviceslicer.application.module.analysis.graph.ast.ReferencedTypeCollector
import cz.bodnor.serviceslicer.domain.analysis.graph.TypeNode
import cz.bodnor.serviceslicer.domain.analysis.graph.TypeNodeType
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.UUID

@Component
class BuildDependencyGraph(
    private val collectCompilationUnits: CollectCompilationUnits,
) {

    operator fun invoke(
        projectId: UUID,
        projectRootDir: Path,
    ): Map<String, TypeNode> {
        val javaParser = buildParser(projectRootDir)

        // Phase 1: Collect all ClassOrInterfaceDeclarations
        val classOrInterfaceTypeDeclarations = collectCompilationUnits(javaParser, projectRootDir)
            .flatMap { it.findAll(ClassOrInterfaceDeclaration::class.java) }
            .associateBy { it.resolve().qualifiedName }

        // Phase 1: Create all TypeNodes with empty references
        val typeNodes = classOrInterfaceTypeDeclarations.mapValues { it.value.toEmptyTypeNode(projectId) }

        // Phase 2: Resolve references and build the final graph
        typeNodes.forEach { (fqn, typeNode) ->
            val declaration =
                classOrInterfaceTypeDeclarations[fqn] ?: error("No ClassOrInterfaceTypeDeclaration found for $fqn")

            val references = mutableSetOf<String>()
            ReferencedTypeCollector().visit(declaration, references)

            typeNode.references = references.mapNotNull { typeNodes[it] }
        }

        return typeNodes
    }

    private fun ClassOrInterfaceDeclaration.toEmptyTypeNode(projectId: UUID): TypeNode {
        val referenceType = this.resolve()
        val fqn = referenceType.qualifiedName

        return TypeNode(
            simpleClassName = referenceType.name,
            fullyQualifiedClassName = fqn,
            projectId = projectId,
            type = when {
                referenceType.isClass -> TypeNodeType.CLASS
                referenceType.isInterface -> TypeNodeType.INTERFACE
                referenceType.isEnum -> TypeNodeType.ENUM
                else -> error("Unexpected type node: $referenceType")
            },
        )
    }

    private fun buildParser(projectRootDir: Path): JavaParser {
        // TODO: Find java root source (check CollectCompilationUnits)
        val javaSymbolSolver = JavaSymbolSolver(JavaParserTypeSolver(projectRootDir.resolve("api/src/main/java")))
        val parserConfig = ParserConfiguration()
        parserConfig.setSymbolResolver(javaSymbolSolver)

        return JavaParser(parserConfig)
    }
}
