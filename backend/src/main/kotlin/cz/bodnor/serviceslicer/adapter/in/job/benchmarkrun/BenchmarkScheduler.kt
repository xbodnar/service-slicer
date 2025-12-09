package cz.bodnor.serviceslicer.adapter.`in`.job.benchmarkrun

import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class BenchmarkScheduler(
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val jobContainer: JobContainer,
    private val jobLauncher: JobLauncher,
) {

    private val logger = logger()

    @Scheduled(fixedDelay = 5000)
    fun runBenchmark() {
        val benchmarkRun = benchmarkRunReadService.findOldestPending() ?: return

        val batchJob = jobContainer[JobType.BENCHMARK]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.BENCHMARK_RUN_ID, benchmarkRun.id, UUID::class.java)
            .toJobParameters()

        logger.info { "Starting Job ${batchJob.name} for benchmarkRunId ${benchmarkRun.id}" }

        jobLauncher.run(batchJob, jobParameters)
    }
}
