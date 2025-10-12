package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.analysis.job.AnalysisJobRepository
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.job.JobContainer
import cz.bodnor.serviceslicer.infrastructure.job.JobParameterLabel
import cz.bodnor.serviceslicer.infrastructure.job.JobType
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class RestartFailedAnalysisJobService(
    private val analysisJobRepository: AnalysisJobRepository,
    private val jobContainer: JobContainer,
    private val jobLauncher: JobLauncher,
    private val jobExplorer: JobExplorer,
    private val jobRepository: JobRepository,
) {

    private val logger = logger()

    @Lazy
    @Autowired
    lateinit var self: RestartFailedAnalysisJobService

    @Transactional
    fun restart(analysisJobId: UUID) {
        val analysisJob = analysisJobRepository.findById(analysisJobId)
            .orElseThrow { IllegalArgumentException("Analysis job not found: $analysisJobId") }

        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        // Find the last job execution for this analysis job
        val jobInstances = jobExplorer.findJobInstancesByJobName(batchJob.name, 0, Int.MAX_VALUE)
        val jobInstance = jobInstances.firstOrNull { instance ->
            val executions = jobExplorer.getJobExecutions(instance)
            executions.any { execution ->
                val projectIdParam = execution.jobParameters.getParameter(JobParameterLabel.PROJECT_ID)
                projectIdParam?.value == analysisJob.projectId
            }
        } ?: throw IllegalStateException("No job instance found for analysis job $analysisJobId")

        val lastExecution = jobExplorer.getJobExecutions(jobInstance)
            .maxByOrNull { it.createTime }
            ?: throw IllegalStateException("No job execution found for job instance ${jobInstance.instanceId}")

        if (lastExecution.status != BatchStatus.FAILED) {
            throw IllegalStateException("Last job execution status is ${lastExecution.status}, expected FAILED")
        }

        logger.info("Restarting failed job [$analysisJobId] - job execution ID: ${lastExecution.id}")

        // Restart the job using the job repository
        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, analysisJob.projectId, UUID::class.java)
            .toJobParameters()

        self.runJob(batchJob, jobParameters)

        logger.info("Successfully restarted job [$analysisJobId]")
    }

    @Async
    fun runJob(
        job: Job,
        parameters: JobParameters,
    ) {
        jobLauncher.run(job, parameters)
    }
}
