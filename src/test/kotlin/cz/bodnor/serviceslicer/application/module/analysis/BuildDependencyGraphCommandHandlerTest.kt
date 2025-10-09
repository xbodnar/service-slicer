package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.application.module.analysis.command.BuildDependencyGraphCommand
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import cz.bodnor.serviceslicer.toUUID
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.io.path.Path

class BuildDependencyGraphCommandHandlerTest(
    @Autowired private val commandBus: CommandBus,
    @Autowired private val classNodeRepository: ClassNodeRepository,
) : IntegrationTest() {

    @Test
    fun `should save each node exactly once`() {
        // given
        helperService.getProject(id = 1.toUUID()) { it.setJavaProjectRoot(Path("src/test/resources/petclinic")) }

        // when
        commandBus(BuildDependencyGraphCommand(projectId = 1.toUUID()))

        // then
        val classNodes = classNodeRepository.findAll()
        classNodes.size shouldBe 23
    }

    @Test
    fun `should save relationships between nodes`() {
        // given
        helperService.getProject(id = 1.toUUID()) { it.setJavaProjectRoot(Path("src/test/resources/petclinic")) }

        // when
        commandBus(BuildDependencyGraphCommand(projectId = 1.toUUID()))

        // then
        val classNodes = classNodeRepository.findAllByProjectId(projectId = 1.toUUID())

        // Verify that at least some nodes have dependencies
        val nodesWithDependencies = classNodes.filter { it.dependencies.isNotEmpty() }
        nodesWithDependencies.isNotEmpty() shouldBe true

        // Verify that dependencies are properly saved with all properties
        val petClinicApplication = classNodes.find { it.simpleClassName == "PetClinicApplication" }!!
        petClinicApplication.dependencies.isNotEmpty() shouldBe true

        // Verify dependency properties are saved
        val firstDependency = petClinicApplication.dependencies.first()
        (firstDependency.weight > 0) shouldBe true
    }
}
