package cz.bodnor.serviceslicer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan(basePackages = ["cz.bodnor.serviceslicer"])
class ServiceSlicerApplication

fun main(args: Array<String>) {
    runApplication<ServiceSlicerApplication>(*args)
}
