package cz.bodnor.serviceslicer.domain.file

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class Directory(
    id: UUID = UUID.randomUUID(),
) : CreatableEntity(id) {

    val storageKey: String = id.toString()
}

@Repository
interface DirectoryRepository : JpaRepository<Directory, UUID>
