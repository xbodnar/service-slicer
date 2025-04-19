package cz.bodnor.serviceslicer.domain.common

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.util.ProxyUtils
import java.time.Clock
import java.time.Instant
import java.util.UUID

@MappedSuperclass
abstract class DomainEntity(
    @Id val id: UUID,
) {
    @Version
    val version: Int = 0

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true
        if (javaClass != ProxyUtils.getUserClass(other)) return false
        other as DomainEntity
        return this.id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

@MappedSuperclass
abstract class CreatableEntity(
    id: UUID,
) : DomainEntity(id) {
    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private var createdTimestamp: Instant = Instant.now()

    val createdAt: Instant
        get() = createdTimestamp
}

@MappedSuperclass
abstract class UpdatableEntity(
    id: UUID,
) : CreatableEntity(id) {
    @LastModifiedDate
    @Column(name = "updated_at", updatable = true, nullable = false)
    private var updatedTimestamp: Instant = Instant.now()

    val updatedAt: Instant
        get() = updatedTimestamp
}

@MappedSuperclass
abstract class DeletableEntity(
    id: UUID,
) : UpdatableEntity(id) {
    private var deletedAt: Instant? = null

    val isDeleted: Boolean
        get() = deletedAt != null

    fun delete(clock: Clock = Clock.systemDefaultZone()) {
        if (!isDeleted) {
            deletedAt = Instant.now(clock)
        }
    }
}
