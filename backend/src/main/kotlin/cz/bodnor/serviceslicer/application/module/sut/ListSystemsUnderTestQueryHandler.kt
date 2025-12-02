package cz.bodnor.serviceslicer.application.module.sut

import cz.bodnor.serviceslicer.application.module.sut.query.ListSystemsUnderTestQuery
import cz.bodnor.serviceslicer.domain.sut.SystemUnderTestReadService
import cz.bodnor.serviceslicer.infrastructure.cqrs.query.QueryHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ListSystemsUnderTestQueryHandler(
    private val sutReadService: SystemUnderTestReadService,
) : QueryHandler<ListSystemsUnderTestQuery.Result, ListSystemsUnderTestQuery> {

    override val query = ListSystemsUnderTestQuery::class

    @Transactional(readOnly = true)
    override fun handle(query: ListSystemsUnderTestQuery): ListSystemsUnderTestQuery.Result =
        ListSystemsUnderTestQuery.Result(sutReadService.findAll())
}
