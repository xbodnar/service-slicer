package cz.bodnor.serviceslicer.domain.file

import cz.bodnor.serviceslicer.domain.common.UpdatableEntity
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Represents a file uploaded to object storage.
 * Follows the direct-to-storage upload pattern.
 */
@Entity
class File(
    id: UUID = UUID.randomUUID(),
    val filename: String,
    val mimeType: String,
    val expectedSize: Long,
    val contentHash: String,
    val storageKey: String,
) : UpdatableEntity(id) {
    @Enumerated(EnumType.STRING)
    var status: FileStatus = FileStatus.PENDING
        private set

    fun markAsReady() {
        require(status == FileStatus.PENDING) { "File must be in PENDING status to mark as READY" }
        this.status = FileStatus.READY
    }

    fun markAsFailed() {
        require(status == FileStatus.PENDING) { "File must be in PENDING status to mark as FAILED" }
        this.status = FileStatus.FAILED
    }
}

enum class FileStatus {
    PENDING,
    READY,
    FAILED,
}

@Repository
interface FileRepository : JpaRepository<File, UUID>
