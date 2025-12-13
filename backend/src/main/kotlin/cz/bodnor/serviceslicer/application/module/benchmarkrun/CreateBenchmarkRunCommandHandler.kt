package cz.bodnor.serviceslicer.application.module.benchmarkrun

import cz.bodnor.serviceslicer.application.module.benchmarkrun.command.CreateBenchmarkRunCommand
import cz.bodnor.serviceslicer.domain.benchmark.BenchmarkReadService
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRun
import cz.bodnor.serviceslicer.domain.benchmarkrun.BenchmarkRunWriteService
import cz.bodnor.serviceslicer.infrastructure.config.K6Properties
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Duration

@Component
class CreateBenchmarkRunCommandHandler(
    private val benchmarkRunWriteService: BenchmarkRunWriteService,
    private val benchmarkReadService: BenchmarkReadService,
    private val k6Properties: K6Properties,
) : CommandHandler<BenchmarkRun, CreateBenchmarkRunCommand> {

    override val command = CreateBenchmarkRunCommand::class

    @Transactional
    override fun handle(command: CreateBenchmarkRunCommand): BenchmarkRun {
        val benchmark = benchmarkReadService.getById(command.benchmarkId)

        val benchmarkRun = BenchmarkRun(
            benchmark = benchmark,
            testDuration = Duration.parse(command.testDuration ?: k6Properties.testDuration),
        )

        // Create a TestSuite for each SystemUnderTest in the Benchmark
        benchmark.systemsUnderTest.forEach { benchmarkSut ->
            val testSuite = benchmarkRun.addTestSuite(benchmarkSut.systemUnderTest, benchmarkSut.isBaseline)

            // Create a TestCase for each load in the operational profile
            benchmark.operationalSetting.operationalProfile.forEach { (load, frequency) ->
                testSuite.addTestCase(load, frequency)
            }
        }

        return benchmarkRunWriteService.save(benchmarkRun)
    }
}
