package cz.bodnor.serviceslicer.adapter.`in`.job.decompositionjob

import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobReadService
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJobWriteService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Component
class DecompositionJobExecutionListener(
    private val decompositionJobReadService: DecompositionJobReadService,
    private val decompositionJobWriteService: DecompositionJobWriteService,
) : JobExecutionListener {

    private val logger = logger()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun beforeJob(jobExecution: JobExecution) {
        val decompositionJobId = extractDecompositionJobId(jobExecution) ?: return

        logger.info { "Starting decomposition job with id: $decompositionJobId" }

        val decompositionJob = decompositionJobReadService.getById(decompositionJobId)
        decompositionJob.started()
        decompositionJobWriteService.save(decompositionJob)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun afterJob(jobExecution: JobExecution) {
        val decompositionJobId = extractDecompositionJobId(jobExecution) ?: return

        logger.info { "Completed decomposition job with id: $decompositionJobId, status: ${jobExecution.status}" }

        val decompositionJob = decompositionJobReadService.getById(decompositionJobId)

        when (jobExecution.status) {
            BatchStatus.COMPLETED -> decompositionJob.completed()

            BatchStatus.FAILED, BatchStatus.STOPPED, BatchStatus.ABANDONED -> decompositionJob.failed()

            else -> {
                logger.warn {
                    "Unexpected batch status: ${jobExecution.status} for decomposition job: $decompositionJobId"
                }
                decompositionJob.failed()
            }
        }

        decompositionJobWriteService.save(decompositionJob)
    }

    private fun extractDecompositionJobId(jobExecution: JobExecution): UUID? = try {
        val parameter = jobExecution.jobParameters.getParameter(JobParameterLabel.DECOMPOSITION_JOB_ID)
        if (parameter?.type == UUID::class.java) {
            parameter.value as UUID
        } else {
            throw IllegalArgumentException("Decomposition job id is not a UUID")
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to extract decomposition job id from job parameters" }
        null
    }
}
