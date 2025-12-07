package cz.bodnor.serviceslicer.adapter.`in`.web.decomposition

import cz.bodnor.serviceslicer.application.module.decomposition.command.CreateDecompositionJobCommand
import cz.bodnor.serviceslicer.application.module.decomposition.query.GetDecompositionJobSummaryQuery
import cz.bodnor.serviceslicer.domain.decomposition.DecompositionJob
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page

@Mapper(componentModel = "spring")
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
}
