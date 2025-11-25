package cz.bodnor.serviceslicer.domain.job

enum class JobType {
    STATIC_CODE_ANALYSIS,
    BENCHMARK,
}

object JobParameterLabel {
    const val PROJECT_ID = "PROJECT_ID"
    const val BENCHMARK_ID = "BENCHMARK_ID"
    const val BENCHMARK_RUN_ID = "BENCHMARK_RUN_ID"
}
