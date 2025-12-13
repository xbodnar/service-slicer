package cz.bodnor.serviceslicer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan(basePackages = ["cz.bodnor.serviceslicer"])
class ServiceSlicerApplication

fun main(args: Array<String>) {
    runApplication<ServiceSlicerApplication>(*args)
}
