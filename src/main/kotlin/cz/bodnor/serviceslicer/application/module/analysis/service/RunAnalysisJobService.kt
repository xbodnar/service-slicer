package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.job.JobContainer
import cz.bodnor.serviceslicer.infrastructure.job.JobParameterLabel
import cz.bodnor.serviceslicer.infrastructure.job.JobType
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class RunAnalysisJobService(
    private val analysisJobFinderService: AnalysisJobFinderService,
    private val jobContainer: JobContainer,
    private val jobLauncher: JobLauncher,
) {

    private val logger = logger()

    fun run(analysisJobId: UUID) {
        val analysisJob = analysisJobFinderService.getById(analysisJobId)

        val batchJob = jobContainer[JobType.STATIC_CODE_ANALYSIS]

        val jobParameters = JobParametersBuilder()
            .addJobParameter(JobParameterLabel.PROJECT_ID, analysisJob.projectId, UUID::class.java)
            .toJobParameters()

        logger.info("Starting job [$analysisJobId] for job [$analysisJob]")
        jobLauncher.run(batchJob, jobParameters)
    }
}
