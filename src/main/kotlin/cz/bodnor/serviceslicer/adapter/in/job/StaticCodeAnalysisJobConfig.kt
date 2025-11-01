package cz.bodnor.serviceslicer.adapter.`in`.job

import cz.bodnor.serviceslicer.adapter.`in`.job.batch.BuildDependencyGraphTasklet
import cz.bodnor.serviceslicer.adapter.`in`.job.batch.InitializeProjectTasklet
import cz.bodnor.serviceslicer.adapter.`in`.job.batch.SuggestMicroserviceBoundariesTasklet
import cz.bodnor.serviceslicer.domain.job.JobType
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
    private val initializeProjectTasklet: InitializeProjectTasklet,
    private val buildDependencyGraphTasklet: BuildDependencyGraphTasklet,
    private val suggestMicroserviceBoundariesTasklet: SuggestMicroserviceBoundariesTasklet,
) {

    @Bean
    fun initializeProjectStep() = StepBuilder("INITIALIZE_PROJECT_STEP", jobRepository)
        .tasklet(initializeProjectTasklet, txManager)
        .build()

    @Bean
    fun buildDependencyGraphStep() = StepBuilder("BUILD_DEPENDENCY_GRAPH_STEP", jobRepository)
        .tasklet(buildDependencyGraphTasklet, txManager)
        .build()

    @Bean
    fun suggestMicroserviceBoundariesStep() = StepBuilder("SUGGEST_MICROSERVICE_BOUNDARIES_STEP", jobRepository)
        .tasklet(suggestMicroserviceBoundariesTasklet, txManager)
        .build()

    @Bean
    fun staticCodeAnalysisJob(): Job = JobBuilder(JobType.STATIC_CODE_ANALYSIS.name, jobRepository)
        .start(initializeProjectStep())
        .next(buildDependencyGraphStep())
        .next(suggestMicroserviceBoundariesStep())
        .build()
}
