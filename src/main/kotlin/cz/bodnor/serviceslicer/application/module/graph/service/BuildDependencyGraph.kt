package cz.bodnor.serviceslicer.application.module.graph.service

import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import cz.bodnor.serviceslicer.application.module.graph.service.WeightedReference
import cz.bodnor.serviceslicer.application.module.graph.service.WeightedReferencedTypeCollector
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNode
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.util.UUID

@Component
class BuildDependencyGraph(
    private val collectCompilationUnits: CollectCompilationUnits,
) {

    private val logger = logger()

    operator fun invoke(
        projectId: UUID,
        javaProjectRootDir: Path,
    ): Map<String, ClassNode> {
        val javaParser = buildParser(javaProjectRootDir)

        // Phase 1: Collect all ClassOrInterfaceDeclarations
        val classOrInterfaceTypeDeclarations = collectCompilationUnits(javaParser, javaProjectRootDir)
            .flatMap { it.findAll(ClassOrInterfaceDeclaration::class.java) }
            .associateBy { it.resolve().qualifiedName }

        // Phase 2: Create all ClassNodes with empty references
        val classNodes = classOrInterfaceTypeDeclarations.mapValues { it.value.toEmptyClassNode(projectId) }

        // Phase 3: Resolve references with weights and build the final graph
        classNodes.forEach { (nodeFqn, classNode) ->
            val declaration =
                classOrInterfaceTypeDeclarations[nodeFqn]
                    ?: error("No ClassOrInterfaceTypeDeclaration found for $nodeFqn")

            val weightedRefs = mutableMapOf<String, WeightedReference>()
            try {
                WeightedReferencedTypeCollector().visit(declaration, weightedRefs)

                weightedRefs.filter { (targetFqn, _) -> targetFqn != nodeFqn } // remove self references
                    .forEach { (targetFqn, weights) ->
                        classNodes[targetFqn]?.let { targetNode ->
                            classNode.addDependency(
                                target = targetNode,
                                weight = weights.totalWeight,
                                methodCalls = weights.methodCalls,
                                fieldAccesses = weights.fieldAccesses,
                                objectCreations = weights.objectCreations,
                                typeReferences = weights.typeReferences,
                            )
                        }
                    }
            } catch (e: Exception) {
                logger.warn("Error while visiting ${declaration.name}")
            }
        }

        return classNodes
    }

    private fun ClassOrInterfaceDeclaration.toEmptyClassNode(projectId: UUID): ClassNode {
        val referenceType = this.resolve()
        val fqn = referenceType.qualifiedName

        return ClassNode(
            simpleClassName = referenceType.name,
            fullyQualifiedClassName = fqn,
            projectId = projectId,
            type = when {
                referenceType.isClass -> ClassNodeType.CLASS
                referenceType.isInterface -> ClassNodeType.INTERFACE
                referenceType.isEnum -> ClassNodeType.ENUM
                else -> error("Unexpected class node type: $referenceType")
            },
        )
    }

    private fun buildParser(javaProjectRootDir: Path): JavaParser {
        val rootPackageDir = javaProjectRootDir.resolve("src/main/java")

        val combinedTypeSolver = CombinedTypeSolver()
        combinedTypeSolver.add(ReflectionTypeSolver())  // Resolves JDK types
        combinedTypeSolver.add(JavaParserTypeSolver(rootPackageDir))  // Resolves project types

        val javaSymbolSolver = JavaSymbolSolver(combinedTypeSolver)
        val parserConfig = ParserConfiguration()
        parserConfig.setSymbolResolver(javaSymbolSolver)

        return JavaParser(parserConfig)
    }
}
