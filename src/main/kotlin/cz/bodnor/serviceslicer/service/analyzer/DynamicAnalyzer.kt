package cz.bodnor.serviceslicer.service.analyzer

/**
 * Interface for dynamic code analysis
 *
 * Note: This is just a skeleton interface as per the requirements.
 * The actual implementation will be done in later stages.
 */
interface DynamicAnalyzer {
    /**
     * Analyze the code in the specified directory by running the application
     * and collecting runtime information.
     *
     * @param projectDirectory Path to the project directory
     * @param mainClass The main class to run
     * @param args Arguments to pass to the main class
     * @return Analysis data
     */
    fun analyze(
        projectDirectory: String,
        mainClass: String,
        args: List<String>,
    ): AnalysisData

    /**
     * Analyze the code in the specified directory by running the tests
     * and collecting runtime information.
     *
     * @param projectDirectory Path to the project directory
     * @param testClasses List of test classes to run
     * @return Analysis data
     */
    fun analyzeWithTests(
        projectDirectory: String,
        testClasses: List<String>,
    ): AnalysisData

    /**
     * Analyze the code in the specified directory by instrumenting the code
     * and collecting runtime information.
     *
     * @param projectDirectory Path to the project directory
     * @param classesToInstrument List of classes to instrument
     * @return Analysis data
     */
    fun analyzeWithInstrumentation(
        projectDirectory: String,
        classesToInstrument: List<String>,
    ): AnalysisData
}
