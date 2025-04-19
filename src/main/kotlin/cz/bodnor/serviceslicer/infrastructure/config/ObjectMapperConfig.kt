package cz.bodnor.serviceslicer.infrastructure.config

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperConfig {

    @Bean
    fun prepareObjectMapper(): ObjectMapper = ObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .registerModule(Jdk8Module())
        .registerModule(BlackbirdModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
        .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false)
}
