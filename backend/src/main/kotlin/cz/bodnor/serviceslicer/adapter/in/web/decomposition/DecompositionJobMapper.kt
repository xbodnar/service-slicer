package cz.bodnor.serviceslicer.adapter.`in`.web.decomposition

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileMapper
import cz.bodnor.serviceslicer.application.module.decomposition.command.CreateDecompositionJobCommand
import cz.bodnor.serviceslicer.application.module.decomposition.query.GetDecompositionJobSummaryQuery
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import cz.bodnor.serviceslicer.domain.decompositioncandidate.DecompositionCandidate
import cz.bodnor.serviceslicer.domain.decompositioncandidate.ServiceBoundary
import cz.bodnor.serviceslicer.domain.graph.ClassNode
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page

@Mapper(componentModel = "spring", uses = [FileMapper::class])
interface DecompositionJobMapper {

    // INBOUND MAPPINGS

    fun toCommand(request: CreateDecompositionJobRequest): CreateDecompositionJobCommand

    // OUTBOUND MAPPINGS
    fun toDto(result: DecompositionJob): DecompositionJobDto

    @Mapping(target = "items", source = "content", defaultExpression = "java(java.util.List.of())")
    @Mapping(target = "currentPage", source = "number")
    @Mapping(target = "pageSize", source = "size")
    fun toDto(result: Page<DecompositionJob>): ListDecompositionJobsResponse

    fun toDto(result: GetDecompositionJobSummaryQuery.Result): DecompositionJobSummaryDto

    fun toDto(result: DecompositionCandidate): DecompositionCandidateDto

    fun toDto(result: ServiceBoundary): ServiceBoundaryDto

    @Suppress("ktlint:standard:max-line-length")
    @Mapping(
        target = "dependencies",
        expression = "java(result.getDependencies().stream().map(dependency -> dependency.getTarget().getFullyQualifiedClassName()).collect(java.util.stream.Collectors.toList()))",
    )
    fun toDto(result: ClassNode): ClassNodeDto
}
