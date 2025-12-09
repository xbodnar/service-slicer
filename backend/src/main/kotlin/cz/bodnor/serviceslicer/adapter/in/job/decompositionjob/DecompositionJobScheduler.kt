package cz.bodnor.serviceslicer.adapter.`in`.job.decompositionjob

import cz.bodnor.serviceslicer.application.module.job.JobContainer
import cz.bodnor.serviceslicer.application.module.job.JobLauncherService
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.domain.job.JobType
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DecompositionJobScheduler(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val jobContainer: JobContainer,
    private val jobLauncher: JobLauncher,
) {

    private val logger = logger()

    private var lastRun: LastRun? = null

    @Scheduled(fixedDelay = 5000)
    fun runDecompositionJob() {
        val decompositionJob = decompositionJobReadService.findOldestPending() ?: return

        val lastRun = this.lastRun

        if (lastRun?.decompositionJobId == decompositionJob.id) {
            lastRun.retrying()

            if (lastRun.retried > MAX_RETRIES) {
                logger.warn {
                    "Max retries reached for decompositionJobId ${decompositionJob.id} " +
                        "- job is probably stuck in PENDING state, skipping..."
                }
                return
            }
        } else {
            this.lastRun = LastRun(decompositionJob.id)
        }

        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.DECOMPOSITION_JOB_ID, decompositionJob.id, UUID::class.java)
            .toJobParameters()

        logger.info { "Starting Job ${batchJob.name} for decompositionJobId ${decompositionJob.id}" }

        jobLauncher.run(batchJob, jobParameters)
    }

    data class LastRun(
        val decompositionJobId: UUID,
    ) {

        var retried = 0
            private set

        fun retrying() {
            retried++
        }
    }

    companion object {
        private const val MAX_RETRIES = 5
    }
}
