package cz.bodnor.serviceslicer.adapter.`in`.batch

import cz.bodnor.serviceslicer.application.module.analysis.command.SuggestMicroserviceBoundariesCommand
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
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
class SuggestMicroserviceBoundariesTasklet(
    private val commandBus: CommandBus,
    @Value("#{jobParameters['PROJECT_ID']}") private val projectId: UUID,
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus? {
        commandBus(SuggestMicroserviceBoundariesCommand(projectId))

        return RepeatStatus.FINISHED
    }
}
