package cz.bodnor.serviceslicer.application.common

import cz.bodnor.serviceslicer.domain.common.DomainEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.reflect.KClass

@NoRepositoryBean
abstract class BaseFinderService<T : DomainEntity>(
    private val repository: JpaRepository<T, UUID>,
) {
    abstract fun errorBlock(message: String): Nothing

    abstract fun getEntityType(): KClass<T>

    private fun getEntityName(): String = getEntityType().simpleName!!

    @Transactional(readOnly = true)
    open fun findById(id: UUID) = repository.findByIdOrNull(id)

    @Transactional(readOnly = true)
    open fun existsById(id: UUID) = repository.existsById(id)

    @Transactional(readOnly = true)
    open fun getById(id: UUID) = repository.findByIdOrNull(id)
        ?: errorBlock("Entity ${getEntityName()} with id: $id not found!")

    @Transactional(readOnly = true)
    open fun findAllByIds(ids: Set<UUID>) = if (ids.isEmpty()) emptyList() else repository.findAllById(ids)

    @Transactional(readOnly = true)
    open fun getAllByIds(ids: Set<UUID>): List<T> = repository.findAllById(ids).also { result ->
        if (ids.size != result.size) {
            val resultIds = result.map { it.id }.toSet()
            val missing = ids - resultIds
            errorBlock("Entities ${getEntityName()} with ids: [${missing.joinToString()}] not found!")
        }
    }

    @Transactional(readOnly = true)
    open fun findAll(): List<T> = repository.findAll()
}
