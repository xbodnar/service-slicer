package cz.bodnor.serviceslicer.domain.job

enum class JobType {
    STATIC_CODE_ANALYSIS,
    BENCHMARK,
}

object JobParameterLabel {
    const val DECOMPOSITION_JOB_ID = "DECOMPOSITION_JOB_ID"
    const val BENCHMARK_RUN_ID = "BENCHMARK_RUN_ID"
}
