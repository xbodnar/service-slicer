package cz.bodnor.serviceslicer.application.module.project

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.application.module.project.command.CreateProjectCommand
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import cz.bodnor.serviceslicer.domain.project.ProjectStatus
import cz.bodnor.serviceslicer.domain.projectsource.GitProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.JarProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceRepository
import cz.bodnor.serviceslicer.domain.projectsource.ZipFileProjectSource
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path

class CreateProjectCommandHandlerTest(
    @Autowired private val commandBus: CommandBus,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectSourceRepository: ProjectSourceRepository,
) : IntegrationTest() {

    @Test
    fun `Should create GitHub project`() {
        // given
        val command = CreateProjectCommand.fromGit(
            projectName = "Test Project",
            gitUri = "https://github.com/spring-projects/spring-petclinic.git",
            projectRootRelativePath = Path.of("/api"),
            branchName = "main",
        )

        // when
        commandBus(command)

        // then
        projectRepository.findAll().first().apply {
            name shouldBe "Test Project"
            status shouldBe ProjectStatus.CREATED
        }
        projectSourceRepository.findAll().first().also {
            when (it) {
                is GitProjectSource -> {
                    it.projectRootRelativePath shouldBe "/api"
                    it.repositoryGitUri shouldBe "https://github.com/spring-projects/spring-petclinic.git"
                    it.branchName shouldBe "main"
                }

                else -> error("Unknown project source type: $it")
            }
        }
    }

    @Test
    fun `Should create ZIP project`() {
        // given
        val command = CreateProjectCommand.fromZip(
            projectName = "Test Project",
            file = Path.of("src/test/resources/petclinic.zip"),
            projectRootRelativePath = Path.of("/api"),
        )

        // when
        commandBus(command)

        // then
        projectRepository.findAll().first().apply {
            name shouldBe "Test Project"
            status shouldBe ProjectStatus.CREATED
        }
        projectSourceRepository.findAll().first().also {
            when (it) {
                is ZipFileProjectSource -> {
                    it.projectRootRelativePath shouldBe "/api"
                    it.zipFilePath shouldBe "src/test/resources/petclinic.zip"
                }

                else -> error("Unknown project source type: $it")
            }
        }
    }

    @Test
    fun `Should create JAR project`() {
        // given
        val command = CreateProjectCommand.fromJar(
            projectName = "Test Project",
            file = Path.of("src/test/resources/petclinic.jar"),
        )

        // when
        commandBus(command)

        // then
        projectRepository.findAll().first().apply {
            name shouldBe "Test Project"
            status shouldBe ProjectStatus.CREATED
        }
        projectSourceRepository.findAll().first().also {
            when (it) {
                is JarProjectSource -> {
                    it.jarFilePath shouldBe "src/test/resources/petclinic.jar"
                }

                else -> error("Unknown project source type: $it")
            }
        }
    }
}
