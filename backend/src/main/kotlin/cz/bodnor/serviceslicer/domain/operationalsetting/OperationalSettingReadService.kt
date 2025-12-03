package cz.bodnor.serviceslicer.domain.operationalsetting

import cz.bodnor.serviceslicer.domain.common.BaseFinderService
import org.springframework.stereotype.Service

@Service
class OperationalSettingReadService(
    private val repository: OperationalSettingRepository,
) : BaseFinderService<OperationalSetting>(repository) {
    override fun errorBlock(message: String) = error(message)

    override fun getEntityType() = OperationalSetting::class
}
