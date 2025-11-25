package cz.bodnor.serviceslicer.infrastructure.config

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging

inline fun <reified T> T.logger(): KLogger = KotlinLogging.logger {}
