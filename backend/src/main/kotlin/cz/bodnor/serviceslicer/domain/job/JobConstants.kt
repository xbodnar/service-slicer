package cz.bodnor.serviceslicer.domain.job

enum class JobType {
    STATIC_CODE_ANALYSIS,
}

object JobParameterLabel {
    const val PROJECT_ID = "PROJECT_ID"
}
