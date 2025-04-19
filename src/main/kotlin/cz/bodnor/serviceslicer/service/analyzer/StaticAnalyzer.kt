package cz.bodnor.serviceslicer.service.analyzer

/**
 * Interface for static code analysis
 */
interface StaticAnalyzer {
    /**
     * Analyze the code in the specified directory
     *
     * @param projectDirectory Path to the project directory
     * @return Analysis data
     */
    fun analyze(projectDirectory: String): AnalysisData
}
