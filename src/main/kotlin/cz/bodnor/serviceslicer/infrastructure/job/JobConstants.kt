package cz.bodnor.serviceslicer.infrastructure.job

enum class JobType {
    STATIC_CODE_ANALYSIS,
    DYNAMIC_CODE_ANALYSIS,
}

object JobParameterLabel {
    const val PROJECT_ID = "PROJECT_ID"
}
