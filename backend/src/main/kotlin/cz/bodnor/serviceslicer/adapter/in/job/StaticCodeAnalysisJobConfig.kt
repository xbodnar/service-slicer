package cz.bodnor.serviceslicer.adapter.`in`.job

import cz.bodnor.serviceslicer.adapter.`in`.job.batch.ActorDrivenDecompositionTasklet
import cz.bodnor.serviceslicer.adapter.`in`.job.batch.BuildDependencyGraphTasklet
import cz.bodnor.serviceslicer.adapter.`in`.job.batch.DetectGraphCommunitiesTasklet
import cz.bodnor.serviceslicer.adapter.`in`.job.batch.DomainDrivenDecompositionTasklet
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
    private val buildDependencyGraphTasklet: BuildDependencyGraphTasklet,
    private val detectGraphCommunitiesTasklet: DetectGraphCommunitiesTasklet,
    private val domainDrivenDecompositionTasklet: DomainDrivenDecompositionTasklet,
    private val actorDrivenDecompositionTasklet: ActorDrivenDecompositionTasklet,
) {

    @Bean
    fun buildDependencyGraphStep() = StepBuilder("BUILD_DEPENDENCY_GRAPH_STEP", jobRepository)
        .tasklet(buildDependencyGraphTasklet, txManager)
        .build()

    @Bean
    fun detectGraphCommunitiesStep() = StepBuilder("DETECT_GRAPH_COMMUNITIES_STEP", jobRepository)
        .tasklet(detectGraphCommunitiesTasklet, txManager)
        .build()

    @Bean
    fun domainDrivenDecompositionStep() = StepBuilder("DOMAIN_DRIVEN_DECOMPOSITION_STEP", jobRepository)
        .tasklet(domainDrivenDecompositionTasklet, txManager)
        .build()

    @Bean
    fun actorDrivenDecompositionStep() = StepBuilder("ACTOR_DRIVEN_DECOMPOSITION_STEP", jobRepository)
        .tasklet(actorDrivenDecompositionTasklet, txManager)
        .build()

    @Bean
    fun staticCodeAnalysisJob(): Job = JobBuilder(JobType.STATIC_CODE_ANALYSIS.name, jobRepository)
        .start(buildDependencyGraphStep())
        .next(detectGraphCommunitiesStep())
        .next(domainDrivenDecompositionStep())
        .next(actorDrivenDecompositionStep())
        .build()
}
