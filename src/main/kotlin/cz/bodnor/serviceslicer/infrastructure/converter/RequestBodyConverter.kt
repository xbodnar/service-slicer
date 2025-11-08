package cz.bodnor.serviceslicer.infrastructure.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import cz.bodnor.serviceslicer.domain.apiop.RequestBody
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
object RequestBodyConverter : AttributeConverter<RequestBody, String> {

    private val objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())

    override fun convertToDatabaseColumn(attribute: RequestBody?): String? = objectMapper.writeValueAsString(attribute)

    override fun convertToEntityAttribute(dbData: String?): RequestBody? = dbData?.let {
        objectMapper.readValue(dbData, RequestBody::class.java)
    }
}
