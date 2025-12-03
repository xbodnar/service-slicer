package cz.bodnor.serviceslicer.adapter.`in`.job.batch

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.ExecuteTestCaseCommand
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel.BENCHMARK_RUN_ID
import cz.bodnor.serviceslicer.infrastructure.config.logger
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.batch.core.ExitStatus
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.configuration.annotation.JobScope
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
@JobScope
class ExecuteTestCaseTasklet(
    private val commandBus: CommandBus,
    @param:Value("#{jobParameters['${BENCHMARK_RUN_ID}']}") private val benchmarkRunId: UUID,
) : Tasklet {

    private val logger = logger()

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus = runCatching {
        val result = commandBus(
            ExecuteTestCaseCommand(
                benchmarkRunId = benchmarkRunId,
            ),
        )

        if (result.hasMoreTests) {
            RepeatStatus.CONTINUABLE
        } else {
            RepeatStatus.FINISHED
        }
    }.onFailure { error ->
        logger.error(error) { "Failed to execute test case" }
        contribution.exitStatus = ExitStatus.FAILED
    }.getOrThrow()
}
