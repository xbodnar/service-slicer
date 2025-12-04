package cz.bodnor.serviceslicer.application.module.analysis

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.infrastructure.cqrs.command.CommandBus
import org.springframework.beans.factory.annotation.Autowired

class BuildDependencyGraphCommandHandlerTest(
    @Autowired private val commandBus: CommandBus,
    @Autowired private val classNodeRepository: ClassNodeRepository,
) : IntegrationTest() {
//    @Test
//    fun `should save each node exactly once`() {
//        // given
//        helperService.getProject(id = 1.toUUID()) { it.setJavaProjectRoot(Path("src/test/resources/petclinic")) }
//
//        // when
//        commandBus(BuildDependencyGraphCommand(projectId = 1.toUUID()))
//
//        // then
//        val classNodes = classNodeRepository.findAllByProjectId(1.toUUID())
//        classNodes.size shouldBe 23
//    }
}
