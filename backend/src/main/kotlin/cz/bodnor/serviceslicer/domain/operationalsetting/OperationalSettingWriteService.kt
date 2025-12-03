package cz.bodnor.serviceslicer.domain.operationalsetting

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class OperationalSettingWriteService(
    private val repository: OperationalSettingRepository,
) {

    @Transactional
    fun save(operationalSetting: OperationalSetting) = repository.save(operationalSetting)

    @Transactional
    fun delete(id: UUID) = repository.deleteById(id)
}
