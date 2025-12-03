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
    // An empty string for directories
    val filename: String,
    val fileSize: Long,
    val mimeType: String,
) : UpdatableEntity(id) {

    val storageKey: String = generateStorageKey(id, filename)

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

    companion object {
        private fun generateStorageKey(
            fileId: UUID,
            filename: String,
        ): String {
            val sanitizedFilename = filename.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            return "$fileId/$sanitizedFilename"
        }
    }
}

enum class FileStatus {
    PENDING,
    READY,
    FAILED,
}

@Repository
interface FileRepository : JpaRepository<File, UUID>
