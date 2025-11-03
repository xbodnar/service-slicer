package cz.bodnor.serviceslicer.application.module.graph.build

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import cz.bodnor.serviceslicer.domain.projectsource.ProjectSourceRepository
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired

@Disabled("Tests require a project directory")
class BuildDependencyGraphJavaParserTest(
    @Autowired private val graphBuilder: BuildDependencyGraphJavaParser,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val projectSourceRepository: ProjectSourceRepository,
) : IntegrationTest() {
//
//    @Test
//    fun `should collect all nodes`() {
//        // given
//        projectRepository.save(
//            Project(
//                id = 1.toUUID(),
//                name = "PetClinic",
//                basePackageName = "java.org.springframework.samples.petclinic",
//                excludePackages = listOf(),
//            ),
//        )
//        projectSourceRepository.save(
//            ZipFileProjectSource(
//                projectId = 1.toUUID(),
//                projectRootRelativePath = Path.of(""),
//                zipFilePath = Path.of("src/test/resources/petclinic.zip"),
//            ).also { it.setProjectRoot(Path.of("src/test/resources/petclinic")) },
//        )
//
//        // when
//        val result = graphBuilder(projectId = 1.toUUID())
//
//        // then
//        result.nodes.size shouldBe 23
//    }
//
//    @Test
//    fun `should correctly set relationships`() {
//        // given
//        projectRepository.save(
//            Project(
//                id = 1.toUUID(),
//                name = "PetClinic",
//                basePackageName = "java.org.springframework.samples.petclinic",
//                excludePackages = listOf(),
//            ),
//        )
//        projectSourceRepository.save(
//            ZipFileProjectSource(
//                projectId = 1.toUUID(),
//                projectRootRelativePath = Path.of(""),
//                zipFilePath = Path.of("src/test/resources/petclinic.zip"),
//            ).also { it.setProjectRoot(Path.of("src/test/resources/petclinic")) },
//        )
//
//        // when
//        val result = graphBuilder(projectId = 1.toUUID())
//
//        // then
//        val owner = result.nodes.find {
//            it.fullyQualifiedClassName == "org.springframework.samples.petclinic.owner.Owner"
//        }!!
//        with(owner) {
//            with(dependencies.find { it.target.simpleClassName == "Pet" }) {
//                this?.fieldAccesses shouldBe 0
//                // Type references: field (1), return types (3), parameter (1), loop vars (2) = 7
//                this?.typeReferences shouldBe 7
//                // Method calls: Counts method calls with explicit scope
//                // Note: Some methods may resolve to parent types or fail to resolve
//                this?.methodCalls shouldBe 2
//                this?.objectCreations shouldBe 0
//            }
//            with(dependencies.find { it.target.simpleClassName == "Person" }) {
//                this?.fieldAccesses shouldBe 0
//                this?.typeReferences shouldBe 1
//                this?.methodCalls shouldBe 0
//                this?.objectCreations shouldBe 0
//            }
//            with(dependencies.find { it.target.simpleClassName == "Visit" }) {
//                this?.fieldAccesses shouldBe 0
//                this?.typeReferences shouldBe 1
//                this?.methodCalls shouldBe 0
//                this?.objectCreations shouldBe 0
//            }
//        }
//    }
//
//    @Test
//    fun `should correctly set projectId`() {
//        // given
//        projectRepository.save(
//            Project(
//                id = 1.toUUID(),
//                name = "PetClinic",
//                basePackageName = "java.org.springframework.samples.petclinic",
//                excludePackages = listOf(),
//            ),
//        )
//        projectSourceRepository.save(
//            ZipFileProjectSource(
//                projectId = 1.toUUID(),
//                projectRootRelativePath = Path.of(""),
//                zipFilePath = Path.of("src/test/resources/petclinic.zip"),
//            ).also { it.setProjectRoot(Path.of("src/test/resources/petclinic")) },
//        )
//
//        // when
//        val result = graphBuilder(projectId = 1.toUUID())
//
//        // then
//        result.nodes.shouldForAll { it.projectId shouldBe 1.toUUID() }
//    }
}
