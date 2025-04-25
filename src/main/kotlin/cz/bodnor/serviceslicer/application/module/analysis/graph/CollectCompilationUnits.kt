package cz.bodnor.serviceslicer.application.module.analysis.graph

import com.github.javaparser.JavaParser
import com.github.javaparser.ast.CompilationUnit
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

@Component
class CollectCompilationUnits {

    operator fun invoke(
        javaParser: JavaParser,
        javaProjectDir: Path,
    ): List<CompilationUnit> {
        val sourcesDir = javaProjectDir.resolve("src").resolve("main")

        return Files.walk(sourcesDir)
            .filter { it.extension == "java" }
            .map { javaParser.parse(it).result.orElse(null) }
            .toList()
            .mapNotNull { it }
    }
}
