package cz.bodnor.serviceslicer.domain.project

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.nio.file.Path
import java.nio.file.Paths

@Converter
object PathHibernateConverter : AttributeConverter<Path, String> {
    override fun convertToDatabaseColumn(attribute: Path?) = attribute.toString()

    override fun convertToEntityAttribute(dbData: String?) = dbData?.let { Paths.get(it) }
}
