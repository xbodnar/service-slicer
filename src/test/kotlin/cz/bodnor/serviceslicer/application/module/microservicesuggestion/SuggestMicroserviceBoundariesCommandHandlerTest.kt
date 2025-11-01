package cz.bodnor.serviceslicer.application.module.microservicesuggestion

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.application.module.analysis.command.SuggestMicroserviceBoundariesCommand
import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import cz.bodnor.serviceslicer.domain.projectsource.JarProjectSource
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.toUUID
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path

@Disabled("Tests needs a JAR file")
class SuggestMicroserviceBoundariesCommandHandlerTest(
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectSourceRepository: ProjectSourceRepository,
    @Autowired private val commandBus: CommandBus,
) : IntegrationTest() {

    @Test
    fun `Should suggest microservice boundaries`() {
        // given
        projectRepository.save(
            Project(
                id = 1.toUUID(),
                name = "realworld example app",
                basePackageName = "io.spring",
                excludePackages = listOf("io.spring.graphql.types"),
            ),
        )
        projectSourceRepository.save(
            JarProjectSource(
                projectId = 1.toUUID(),
                jarFilePath = Path.of("src/test/resources/realworld-example-app-0.0.1-SNAPSHOT.jar"),
            ),
        )
        commandBus(BuildDependencyGraphCommand(projectId = 1.toUUID()))

        // when
        commandBus(SuggestMicroserviceBoundariesCommand(projectId = 1.toUUID()))

        // then
        // TODO: Assert something
    }
}
