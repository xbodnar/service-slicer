package cz.bodnor.serviceslicer.application.module.file

import cz.bodnor.serviceslicer.application.module.file.command.FetchGitRepositoryCommand
import cz.bodnor.serviceslicer.application.module.file.port.out.UploadDirectoryToStorage
import cz.bodnor.serviceslicer.application.module.file.service.FetchGitRepository
import cz.bodnor.serviceslicer.domain.file.DirectoryWriteService
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandHandler
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@Component
class FetchGitRepositoryCommandHandler(
    private val fetchGitRepository: FetchGitRepository,
    private val directoryWriteService: DirectoryWriteService,
    private val uploadDirectoryToStorage: UploadDirectoryToStorage,
) :
    CommandHandler<FetchGitRepositoryCommand.Result, FetchGitRepositoryCommand> {

    override val command = FetchGitRepositoryCommand::class

    @OptIn(ExperimentalPathApi::class)
    @Transactional
    override fun handle(command: FetchGitRepositoryCommand): FetchGitRepositoryCommand.Result {
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

            return FetchGitRepositoryCommand.Result(
                dirId = directory.id,
            )
        } finally {
            // 5. Delete git folder from tmp dir
            gitRepoDir?.deleteRecursively()
        }
    }
}
