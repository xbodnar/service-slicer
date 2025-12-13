package cz.bodnor.serviceslicer.domain.testcase

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TestCaseRepository : JpaRepository<TestCase, UUID> {

    @Query(
        value = """
            SELECT t.*
            FROM test_case t
            JOIN test_suite ts ON t.test_suite_id = ts.id
            WHERE ts.benchmark_run_id = :benchmarkRunId AND t.status = 'PENDING'
            ORDER BY
                CASE WHEN ts.is_baseline THEN 0 ELSE 1 END,
                ts.id,
                t.load
            LIMIT 1
    """,
        nativeQuery = true,
    )
    fun findNextTestCaseToExecute(benchmarkRunId: UUID): TestCase?
}
