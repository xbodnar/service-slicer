package cz.bodnor.serviceslicer.service.analyzer

/**
 * Data class to hold the results of code analysis
 */
data class AnalysisData(
    /**
     * Number of classes analyzed
     */
    val classCount: Int,

    /**
     * Number of packages analyzed
     */
    val packageCount: Int,

    /**
     * Number of dependencies found
     */
    val dependencyCount: Int,

    /**
     * Dependency graph as JSON
     */
    val dependencyGraphJson: String,

    /**
     * Suggested microservices as JSON
     */
    val suggestedMicroservicesJson: String,
)
