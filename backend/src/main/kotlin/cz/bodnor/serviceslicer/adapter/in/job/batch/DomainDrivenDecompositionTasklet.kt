package cz.bodnor.serviceslicer.adapter.`in`.job.batch

import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand
import cz.bodnor.serviceslicer.application.module.decomposition.command.DomainExpertDecompositionCommand.DomainDecompositionType.DOMAIN_DRIVEN
import cz.bodnor.serviceslicer.domain.job.JobParameterLabel.DECOMPOSITION_JOB_ID
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
class DomainDrivenDecompositionTasklet(
    private val commandBus: CommandBus,
    @Value("#{jobParameters['${DECOMPOSITION_JOB_ID}']}") private val decompositionJobId: UUID,
) : Tasklet {

    override fun execute(
        contribution: StepContribution,
        chunkContext: ChunkContext,
    ): RepeatStatus {
        commandBus(
            DomainExpertDecompositionCommand(
                decompositionJobId = decompositionJobId,
                decompositionType = DOMAIN_DRIVEN,
            ),
        )

        return RepeatStatus.FINISHED
    }
}
