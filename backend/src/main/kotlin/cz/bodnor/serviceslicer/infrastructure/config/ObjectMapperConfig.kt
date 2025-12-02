package cz.bodnor.serviceslicer.infrastructure.config

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.blackbird.BlackbirdModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cz.bodnor.serviceslicer.domain.testcase.OperationId
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ObjectMapperConfig {

    @Bean
    fun prepareObjectMapper(): ObjectMapper {
        val operationIdModule = SimpleModule().apply {
            addKeyDeserializer(OperationId::class.java, OperationIdKeyDeserializer())
        }

        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .registerModule(BlackbirdModule())
            .registerModule(operationIdModule)
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN, true)
            .configure(DeserializationFeature.ACCEPT_FLOAT_AS_INT, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    @Bean
    fun hibernatePropertiesCustomizer(objectMapper: ObjectMapper): HibernatePropertiesCustomizer =
        HibernatePropertiesCustomizer { properties ->
            properties["hibernate.type.json_format_mapper"] = JacksonJsonFormatMapper(objectMapper)
        }

    class OperationIdKeyDeserializer : KeyDeserializer() {
        override fun deserializeKey(
            key: String,
            ctxt: DeserializationContext,
        ): OperationId = OperationId(key)
    }
}
