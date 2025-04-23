package cz.bodnor.serviceslicer.application.module.analysis.graph

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.toUUID
import io.kotest.inspectors.forAny
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
        result.size shouldBe 24
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
            references.forAny { it.simpleClassName shouldBe "Person" }
            references.forAny { it.simpleClassName shouldBe "Pet" }
            references.forAny { it.simpleClassName shouldBe "Visit" }
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
