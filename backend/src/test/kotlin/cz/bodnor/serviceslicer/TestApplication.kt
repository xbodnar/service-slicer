package cz.bodnor.serviceslicer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

/**
 * Entry point for running tests. Excluding any beans from `adapter.out` to avoid external dependencies.
 */
@SpringBootApplication
@ComponentScan(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = ["cz\\.bodnor\\.serviceslicer\\.adapter\\.out\\..*"],
        ),
    ],
)
class TestApplication

fun main(args: Array<String>) {
    runApplication<TestApplication>(*args)
}
