package cz.bodnor.serviceslicer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ServiceSlicerApplication

fun main(args: Array<String>) {
    runApplication<ServiceSlicerApplication>(*args)
}
