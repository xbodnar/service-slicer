package cz.bodnor.serviceslicer.adapter.`in`.web.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.CreateBenchmarkRunCommand
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.testcase.BaselineTestCase
import cz.bodnor.serviceslicer.domain.testcase.TargetTestCase
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page

@Mapper(componentModel = "spring")
interface BenchmarkRunMapper {

    // INBOUND MAPPINGS
    fun toCommand(request: CreateBenchmarkRunRequest): CreateBenchmarkRunCommand

    // OUTBOUND MAPPINGS

    @Mapping(target = "benchmarkId", source = "benchmark.id")
    @Mapping(target = "testDuration", expression = "java(result.getTestDurationString())")
    fun toDto(result: BenchmarkRun): BenchmarkRunDto

    @Mapping(target = "items", source = "content", defaultExpression = "java(java.util.List.of())")
    @Mapping(target = "currentPage", source = "number")
    @Mapping(target = "pageSize", source = "size")
    fun toDto(result: Page<BenchmarkRun>): ListBenchmarkRunsResponse

    @Mapping(target = "baselineSutId", source = "baselineSut.id")
    fun toDto(result: BaselineTestCase): BaselineTestCaseDto

    @Mapping(target = "targetSutId", source = "targetSut.id")
    fun toDto(result: TargetTestCase): TargetTestCaseDto
}
