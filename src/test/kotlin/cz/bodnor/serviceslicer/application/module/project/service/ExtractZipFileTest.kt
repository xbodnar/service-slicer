package cz.bodnor.serviceslicer.application.module.project.service

import cz.bodnor.serviceslicer.IntegrationTest
import io.kotest.matchers.shouldBe
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Path
import kotlin.test.Test

class ExtractZipFileTest(
    @Autowired private val underTest: ExtractZipFile,
) : IntegrationTest() {

    @Test
    fun `should extract zip file`() {
        // given
        val sourceDir = Path.of("src/test/resources/zip")
        val sourceFile = sourceDir.resolve("test.zip")

        // when
        underTest(
            source = sourceFile,
            destination = sourceDir,
        )

        // then
        sourceDir.resolve("test").resolve("test.txt").toFile().readText() shouldBe "test"
    }
}
