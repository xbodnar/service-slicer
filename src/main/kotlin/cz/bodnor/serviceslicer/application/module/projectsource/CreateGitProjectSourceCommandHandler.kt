package cz.bodnor.serviceslicer.application.module.projectsource

import cz.bodnor.serviceslicer.application.module.file.port.out.UploadDirectoryToStorage
import cz.bodnor.serviceslicer.application.module.projectsource.command.CreateGitProjectSourceCommand
import cz.bodnor.serviceslicer.application.module.projectsource.service.FetchGitRepository
import cz.bodnor.serviceslicer.domain.file.DirectoryWriteService
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@Component
class CreateGitProjectSourceCommandHandler(
    private val fetchGitRepository: FetchGitRepository,
    private val directoryWriteService: DirectoryWriteService,
    private val uploadDirectoryToStorage: UploadDirectoryToStorage,
    private val projectSourceWriteService: ProjectSourceWriteService,
) :
    CommandHandler<CreateGitProjectSourceCommand.Result, CreateGitProjectSourceCommand> {

    override val command = CreateGitProjectSourceCommand::class

    @OptIn(ExperimentalPathApi::class)
    @Transactional
    override fun handle(command: CreateGitProjectSourceCommand): CreateGitProjectSourceCommand.Result {
        var gitRepoDir: Path? = null

        try {
            // 1. Fetch git repository to tmp dir
            gitRepoDir = fetchGitRepository(
                uri = command.repositoryUrl,
                branch = command.branch,
            )

            // 2. Create directory to DB
            val directory = directoryWriteService.create()

            // 3. Upload git folder to MinIO
            uploadDirectoryToStorage(gitRepoDir, directory.storageKey)

            // 4. Create project source to DB
            val projectSource = projectSourceWriteService.create(
                jarFileId = command.jarFileId,
                projectDirId = directory.id,
            )

            return CreateGitProjectSourceCommand.Result(
                projectSourceId = projectSource.id,
            )
        } finally {
            // 5. Delete git folder from tmp dir
            gitRepoDir?.deleteRecursively()
        }
    }
}
