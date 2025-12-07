package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.query.ListSystemsUnderTestQuery
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTest
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.data.domain.Page
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ListSystemsUnderTestQueryHandler(
    private val sutReadService: SystemUnderTestReadService,
) : QueryHandler<Page<SystemUnderTest>, ListSystemsUnderTestQuery> {

    override val query = ListSystemsUnderTestQuery::class

    @Transactional(readOnly = true)
    override fun handle(query: ListSystemsUnderTestQuery): Page<SystemUnderTest> =
        sutReadService.findAll(query.toPageable())
}
