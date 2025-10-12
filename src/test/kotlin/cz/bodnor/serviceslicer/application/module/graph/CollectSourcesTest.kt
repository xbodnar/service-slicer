package cz.bodnor.serviceslicer.application.module.graph

import com.github.javaparser.JavaParser
import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.application.module.graph.service.CollectCompilationUnits
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.io.path.Path

class CollectSourcesTest(
    @Autowired private val underTest: CollectCompilationUnits,
) : IntegrationTest() {

    @Test
    fun `should collect all java classes`() {
        // given
        val projectDir = Path("src/test/resources/petclinic")

        // when
        val result = underTest(JavaParser(), projectDir)

        // then
        result.size shouldBe 24
    }
}
