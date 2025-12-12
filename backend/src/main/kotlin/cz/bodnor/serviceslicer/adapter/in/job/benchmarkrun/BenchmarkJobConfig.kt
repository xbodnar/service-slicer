package cz.bodnor.serviceslicer.adapter.`in`.job.benchmarkrun

import cz.bodnor.serviceslicer.adapter.`in`.job.benchmarkrun.ExecuteTestCaseTasklet
import cz.bodnor.serviceslicer.domain.job.JobType
import org.springframework.batch.core.Job
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager

@Configuration
class BenchmarkJobConfig(
    private val jobRepository: JobRepository,
    private val txManager: PlatformTransactionManager,
    private val executeTestCaseTasklet: ExecuteTestCaseTasklet,
    private val benchmarkRunExecutionListener: BenchmarkRunExecutionListener,
) {

    @Bean
    fun executeSutLoadTestsStep() = StepBuilder("EXECUTE_SUT_LOAD_TESTS_STEP", jobRepository)
        .tasklet(executeTestCaseTasklet, txManager)
        .build()

    @Bean
    fun benchmarkJob(): Job = JobBuilder(JobType.BENCHMARK.name, jobRepository)
        .listener(benchmarkRunExecutionListener)
        .start(executeSutLoadTestsStep())
        .build()
}
