package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.decompositioncandidate.BoundaryMetrics
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import org.springframework.stereotype.Component

@Component
class CalculateBoundaryMetrics {

    operator fun invoke(
        serviceClasses: List<ClassNode>,
        allClasses: List<ClassNode>,
    ): BoundaryMetrics {
        val serviceClassNames = serviceClasses.map { it.fullyQualifiedClassName }.toSet()

        // Count internal dependencies (within the service)
        var internalDependencies = 0
        // Count external dependencies (to other services)
        var externalDependencies = 0

        serviceClasses.forEach { classNode ->
            classNode.dependencies.forEach { dependency ->
                val targetClassName = dependency.target.fullyQualifiedClassName
                if (targetClassName in serviceClassNames) {
                    internalDependencies++
                } else {
                    externalDependencies++
                }
            }
        }

        val totalDependencies = internalDependencies + externalDependencies

        // Calculate cohesion: ratio of internal dependencies to total dependencies
        // If there are no dependencies, cohesion is 0.0
        val cohesion = if (totalDependencies > 0) {
            internalDependencies.toDouble() / totalDependencies
        } else {
            0.0
        }

        // Count unique external classes this service depends on (coupling)
        val externalDependencyTargets = serviceClasses
            .flatMap { it.dependencies }
            .map { it.target.fullyQualifiedClassName }
            .filter { it !in serviceClassNames }
            .toSet()
        val coupling = externalDependencyTargets.size

        return BoundaryMetrics(
            size = serviceClasses.size,
            cohesion = cohesion,
            coupling = coupling,
            internalDependencies = internalDependencies,
            externalDependencies = externalDependencies,
        )
    }
}
