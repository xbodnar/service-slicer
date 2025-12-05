package cz.bodnor.serviceslicer.application.module.graph.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.PrintWriter
import java.nio.file.Path
import java.util.spi.ToolProvider

@Service
class JdepsService {

    private val jdeps = ToolProvider.findFirst("jdeps").orElseThrow { IllegalStateException("jdeps not found") }

    // Exclude any FQN that contains a '$' (anonymous/inner/lombok builders, Kotlin helpers, etc.)
    private val noDollar = "(?!.*\\$)"

    // Runs the jdeps tool on the given JAR file and returns the output .dot file
    fun execute(
        jarFile: Path,
        basePackageName: String,
        excludePackages: List<String>,
    ): Path {
        val pkg = basePackageName.replace(".", "\\.")

        // Build (?:pat1|pat2|...) for excludes, where each pat matches the package and its subpackages/inners
        // Example: io\.spring\.graphql\.types(?:\..*|\$.*|$)
        val excludeAlt =
            if (excludePackages.isEmpty()) {
                "(?!)" // matches nothing
            } else {
                excludePackages.joinToString("|", prefix = "(?:", postfix = ")") { ep ->
                    val epEsc = escapePkg(ep.trim())
                    "$epEsc(?:\\..*|\\$.*|$)"
                }
            }

        // Analyze only our base package AND not excluded:
        //   ^(?!.*<excludeAlt>)(<basepkg>.*)
        val includeRegex = "^(?!.*$excludeAlt)$noDollar($pkg.*)"

        // Emit edges only if RHS is not "(not found)", not excluded, and without '$'
        val edgeRegex = "^(?!.*\\(not found\\))(?!.*$excludeAlt)$noDollar($pkg.*)"

        // jdeps -verbose:class -include '^(io\.spring\..*)' -e '^(io\.spring\..*)' -filter:none --dot-output build/jdeps build/libs/spring-boot-realworld-example-app-0.0.1-SNAPSHOT.jar
        val result = jdeps.run(
            PrintWriter(System.out, true),
            PrintWriter(System.err, true),
            "-verbose:class",
            "-filter:none",
            "-include", includeRegex,
            "-e", edgeRegex,
            "--dot-output", jarFile.parent.toString(),
            jarFile.toString(),
        )

        require(result == 0) { "jdeps failed with exit code $result" }

        return jarFile.parent.resolve(jarFile.fileName.toString() + ".dot")
    }

    private fun escapePkg(pkg: String): String = pkg.replace(".", "\\.")
}
