package cz.bodnor.serviceslicer.application.module.analysis.graph

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.toUUID
import io.kotest.inspectors.shouldForAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.io.path.Path

class BuildDependencyGraphTest(
    @Autowired private val underTest: BuildDependencyGraph,
) : IntegrationTest() {

    @Test
    fun `should collect all nodes`() {
        // given
        val projectDir = Path("src/test/resources/petclinic")

        // when
        val result = underTest(projectId = 1.toUUID(), projectDir)

        // then
        result.size shouldBe 23
    }

    @Test
    fun `should correctly set relationships`() {
        // given
        val projectDir = Path("src/test/resources/petclinic")

        // when
        val result = underTest(projectId = 1.toUUID(), projectDir)

        // then
        val owner = result["org.springframework.samples.petclinic.owner.Owner"]!!
        with(owner) {
            with(dependencies.find { it.target.simpleClassName == "Pet" }) {
                this?.fieldAccesses shouldBe 0
                // Type references: field (1), return types (3), parameter (1), loop vars (2) = 7
                this?.typeReferences shouldBe 7
                // Method calls: Counts method calls with explicit scope
                // Note: Some methods may resolve to parent types or fail to resolve
                this?.methodCalls shouldBe 2
                this?.objectCreations shouldBe 0
            }
            with(dependencies.find { it.target.simpleClassName == "Person" }) {
                this?.fieldAccesses shouldBe 0
                this?.typeReferences shouldBe 1
                this?.methodCalls shouldBe 0
                this?.objectCreations shouldBe 0
            }
            with(dependencies.find { it.target.simpleClassName == "Visit" }) {
                this?.fieldAccesses shouldBe 0
                this?.typeReferences shouldBe 1
                this?.methodCalls shouldBe 0
                this?.objectCreations shouldBe 0
            }
        }
    }

    @Test
    fun `should correctly set projectId`() {
        // given
        val projectDir = Path("src/test/resources/petclinic")

        // when
        val result = underTest(projectId = 1.toUUID(), projectDir)

        // then
        result.values.shouldForAll { it.projectId shouldBe 1.toUUID() }
    }
}
