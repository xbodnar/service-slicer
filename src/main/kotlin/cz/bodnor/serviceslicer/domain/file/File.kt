package cz.bodnor.serviceslicer.domain.file

import cz.bodnor.serviceslicer.domain.common.CreatableEntity
import jakarta.persistence.Entity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Entity
class File(
    id: UUID = UUID.randomUUID(),
    extension: String,
) : CreatableEntity(id) {

    val extension = extension.lowercase()

    init {
        require(this.extension.all { char -> char.isLetter() || char.isDigit() }) {
            "Extension should contain only alphabetic characters."
        }
    }

    fun fileName(): String = "$id.$extension"
}

@Repository
interface FileRepository : JpaRepository<File, UUID>
