package cz.bodnor.serviceslicer.application.module.loadtest.port.out

import cz.bodnor.serviceslicer.domain.loadtestrun.OperationRunMetrics
import java.time.Instant
import java.util.UUID

interface QueryLoadTestMetrics {

    /**
     * Query load test metrics from Prometheus for a specific experiment and SUT.
     *
     * @param experimentId The experiment ID to filter metrics
     * @param sutId The system under test ID to filter metrics
     * @param targetVus The target number of virtual users (load level)
     * @param start The start timestamp of the load test run
     * @param end The end timestamp of the load test run
     * @return List of operation-level metrics aggregated by operation ID and component
     */
    operator fun invoke(
        experimentId: UUID,
        sutId: UUID,
        targetVus: Int,
        start: Instant,
        end: Instant,
    ): List<OperationRunMetrics>
}
