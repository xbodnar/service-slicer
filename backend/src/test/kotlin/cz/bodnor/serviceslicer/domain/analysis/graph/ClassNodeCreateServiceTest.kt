package cz.bodnor.serviceslicer.domain.analysis.graph

import cz.bodnor.serviceslicer.IntegrationTest
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test

class ClassNodeCreateServiceTest(
    @Autowired val classNodeWriteService: ClassNodeWriteService,
    @Autowired val classNodeRepository: ClassNodeRepository,
) : IntegrationTest() {
    @Test
    fun `Should save nodes with relationships`() {
        // Given
        val projectId = UUID.randomUUID()
        val a = ClassNode(
            type = ClassNodeType.CLASS,
            simpleClassName = "ClassA",
            fullyQualifiedClassName = "foo.bar.ClassA",
            projectId = projectId,
        )

        val b = ClassNode(
            type = ClassNodeType.CLASS,
            simpleClassName = "ClassB",
            fullyQualifiedClassName = "foo.bar.ClassB",
            projectId = projectId,
        )

        val c = ClassNode(
            type = ClassNodeType.CLASS,
            simpleClassName = "ClassC",
            fullyQualifiedClassName = "foo.bar.ClassC",
            projectId = projectId,
        )

        a.addDependency(target = b, weight = 1)
        a.addDependency(target = c, weight = 2)

        b.addDependency(target = c, weight = 3)

        // When
        classNodeWriteService.create(listOf(a, b, c))

        // Then
        val savedNodes = classNodeRepository.findAllByProjectId(projectId)
        savedNodes.size shouldBe 3
        savedNodes.find { it.simpleClassName == "ClassA" }?.dependencies?.size shouldBe 2
        savedNodes.find { it.simpleClassName == "ClassB" }?.dependencies?.size shouldBe 1
    }
}
