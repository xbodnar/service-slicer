package cz.bodnor.serviceslicer.adapter.`in`.web.benchmark

import cz.bodnor.serviceslicer.adapter.`in`.web.file.FileMapper
import cz.bodnor.serviceslicer.adapter.`in`.web.operationalsetting.OperationalSettingMapper
import cz.bodnor.serviceslicer.adapter.`in`.web.sut.SystemUnderTestMapper
import cz.bodnor.serviceslicer.application.module.benchmark.command.CreateBenchmarkCommand
import cz.bodnor.serviceslicer.application.module.benchmark.command.UpdateBenchmarkCommand
import cz.bodnor.serviceslicer.application.module.benchmark.query.GetBenchmarkQuery
import cz.bodnor.serviceslicer.domain.benchmark.Benchmark
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.springframework.data.domain.Page
import java.util.UUID

@Mapper(
    componentModel = "spring",
    uses = [FileMapper::class, SystemUnderTestMapper::class, OperationalSettingMapper::class],
)
interface BenchmarkMapper {

    // INBOUND MAPPINGS
    fun toCommand(request: CreateBenchmarkRequest): CreateBenchmarkCommand

    @Mapping(target = "benchmarkId", source = "benchmarkId")
    fun toCommand(
        request: UpdateBenchmarkRequest,
        benchmarkId: UUID,
    ): UpdateBenchmarkCommand

    // OUTBOUND MAPPINGS

    @Mapping(target = "id", source = "benchmark.id")
    @Mapping(target = "createdAt", source = "benchmark.createdAt")
    @Mapping(target = "updatedAt", source = "benchmark.updatedAt")
    @Mapping(target = "name", source = "benchmark.name")
    @Mapping(target = "description", source = "benchmark.description")
    @Mapping(target = "operationalSetting", source = "benchmark.operationalSetting")
    @Mapping(target = "systemsUnderTest", source = "systemsUnderTest")
    fun toDto(result: GetBenchmarkQuery.Result): BenchmarkDetailDto

    fun toDto(result: Benchmark): BenchmarkDto

    @Mapping(source = "content", target = "items", defaultExpression = "java(java.util.List.of())")
    @Mapping(source = "number", target = "currentPage")
    @Mapping(source = "size", target = "pageSize")
    fun toDto(result: Page<Benchmark>): ListBenchmarksResponse
}
