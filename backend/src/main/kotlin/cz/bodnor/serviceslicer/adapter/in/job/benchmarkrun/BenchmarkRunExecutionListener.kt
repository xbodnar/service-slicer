package cz.bodnor.serviceslicer.adapter.`in`.job.benchmarkrun

import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel
import cz.bodnor.serviceslicer.infrastructure.config.logger
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.jvm.java

@Component
class BenchmarkRunExecutionListener(
    private val benchmarkRunReadService: BenchmarkRunReadService,
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
) : JobExecutionListener {

    private val logger = logger()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun beforeJob(jobExecution: JobExecution) {
        val benchmarkRunId = extractBenchmarkRunId(jobExecution) ?: return

        logger.info { "Starting benchmark run with id: $benchmarkRunId" }

        val benchmarkRun = benchmarkRunReadService.getById(benchmarkRunId)
        benchmarkRun.started()
        benchmarkRunWriteService.save(benchmarkRun)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun afterJob(jobExecution: JobExecution) {
        val benchmarkRunId = extractBenchmarkRunId(jobExecution) ?: return

        logger.info { "Completed benchmark run with id: ${jobExecution.jobParameters}, status: ${jobExecution.status}" }

        val benchmarkRun = benchmarkRunReadService.getById(benchmarkRunId)

        when (jobExecution.status) {
            BatchStatus.COMPLETED -> benchmarkRun.completed()

            BatchStatus.FAILED, BatchStatus.STOPPED, BatchStatus.ABANDONED -> benchmarkRun.failed()

            else -> {
                logger.warn {
                    "Unexpected batch status: ${jobExecution.status} for benchmark run: $benchmarkRunId"
                }
                benchmarkRun.failed()
            }
        }
    }

    private fun extractBenchmarkRunId(jobExecution: JobExecution): UUID? = try {
        val parameter = jobExecution.jobParameters.getParameter(JobParameterLabel.BENCHMARK_RUN_ID)
        if (parameter?.type == UUID::class.java) {
            parameter.value as UUID
        } else {
            throw IllegalArgumentException("Benchmark run id is not a UUID")
        }
    } catch (e: Exception) {
        logger.error(e) { "Failed to extract benchmark run id from job parameters" }
        null
    }
}
