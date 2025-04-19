package cz.bodnor.serviceslicer.infrastructure.job

import cz.bodnor.serviceslicer.application.module.analysis.PrepareProjectRootTasklet
import org.springframework.batch.core.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class StaticCodeAnalysisJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val prepareProjectRootTasklet: PrepareProjectRootTasklet,
) {

    fun prepareProjectRootStep() = StepBuilder("PREPARE_PROJECT_ROOT_STEP", jobRepository)
        .tasklet(prepareProjectRootTasklet, txManager)
        .build()

    @Bean
    fun staticCodeAnalysisJob(): Job = JobBuilder(JobType.STATIC_CODE_ANALYSIS.name, jobRepository)
        .start(prepareProjectRootStep())
        .build()
}
