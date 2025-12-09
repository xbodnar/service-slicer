package cz.bodnor.serviceslicer.adapter.`in`.job.decompositionjob

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
    private val louvainCommunityDetectionTasklet: LouvainCommunityDetectionTasklet,
    private val leidenCommunityDetectionTasklet: LeidenCommunityDetectionTasklet,
    private val labelPropagationCommunityDetectionTasklet: LabelPropagationCommunityDetectionTasklet,
    private val domainDrivenDecompositionTasklet: DomainDrivenDecompositionTasklet,
    private val actorDrivenDecompositionTasklet: ActorDrivenDecompositionTasklet,
    private val decompositionJobExecutionListener: DecompositionJobExecutionListener,
) {

    @Bean
    fun buildDependencyGraphStep() = StepBuilder("BUILD_DEPENDENCY_GRAPH_STEP", jobRepository)
        .tasklet(buildDependencyGraphTasklet, txManager)
        .build()

    @Bean
    fun louvainCommunityDetectionStep() = StepBuilder("LOUVAIN_COMMUNITY_DETECTION_STEP", jobRepository)
        .tasklet(louvainCommunityDetectionTasklet, txManager)
        .build()

    @Bean
    fun leidenCommunityDetectionStep() = StepBuilder("LEIDEN_COMMUNITY_DETECTION_STEP", jobRepository)
        .tasklet(leidenCommunityDetectionTasklet, txManager)
        .build()

    @Bean
    fun labelPropagationCommunityDetectionStep() =
        StepBuilder("LABEL_PROPAGATION_COMMUNITY_DETECTION_STEP", jobRepository)
            .tasklet(labelPropagationCommunityDetectionTasklet, txManager)
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
        .listener(decompositionJobExecutionListener)
        .start(buildDependencyGraphStep())
        .next(louvainCommunityDetectionStep())
        .next(leidenCommunityDetectionStep())
        .next(labelPropagationCommunityDetectionStep())
        .next(domainDrivenDecompositionStep())
        .next(actorDrivenDecompositionStep())
        .build()
}
