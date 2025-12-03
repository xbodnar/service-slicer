package cz.bodnor.serviceslicer.infrastructure.converter

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Converter(autoApply = true)
class DurationHibernateConverter : AttributeConverter<Duration, Long> {
    override fun convertToDatabaseColumn(attribute: Duration?): Long? = attribute?.inWholeMilliseconds

    override fun convertToEntityAttribute(dbData: Long?): Duration? = dbData?.milliseconds
}
