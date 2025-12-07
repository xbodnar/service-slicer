package cz.bodnor.serviceslicer.application.module.sut.query

import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestWithFiles
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.Query
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.UUID

data class GetSystemUnderTestQuery(val sutId: UUID) : Query<SystemUnderTestWithFiles>

data class ListSystemsUnderTestQuery(
    val page: Int = 0,
    val size: Int = 10,
) : Query<Page<SystemUnderTest>> {
    fun toPageable() = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdTimestamp"))
}
