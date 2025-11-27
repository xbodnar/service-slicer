package cz.bodnor.serviceslicer.domain.sut

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class SystemUnderTestReadService(
    private val repository: SystemUnderTestRepository,
) : BaseFinderService<SystemUnderTest>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = SystemUnderTest::class
}
