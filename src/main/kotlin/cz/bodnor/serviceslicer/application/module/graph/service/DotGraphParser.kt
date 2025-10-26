package cz.bodnor.serviceslicer.application.module.graph.service

import org.jgrapht.Graph
import org.jgrapht.Graphs
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.nio.dot.DOTImporter
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Function

object DotGraphParser {
    // Collapse "io.spring.Foo (realworld-example-app-0.0.1-SNAPSHOT.jar)" -> "io.spring.Foo"
    private val jarSuffixRegex = Regex("""\s+\([^)]+\.jar\)$""")

    // Fold "io.spring.Foo$1" -> "io.spring.Foo"
    private val anonymousClassRegex = Regex("""\$\d+$""")

    private val importer = DOTImporter<String, DefaultEdge>().also {
        it.vertexFactory =
            Function<String, String> { id: String -> normalize(id) }
    }

    data class Result(
        val vertices: List<String>,
        val edges: List<Pair<String, String>>,
    )

    fun parse(dotFile: Path): Result {
        // Parse the .dot file and build the graph
        val graph = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)

        Files.newBufferedReader(dotFile).use { importer.importGraph(graph, it) }

        return Result(
            vertices = graph.vertexSet().toList(),
            edges = graph.edgeSet().map { edge ->
                Pair(
                    graph.getEdgeSource(edge),
                    graph.getEdgeTarget(edge),
                )
            },
        )
    }

    private fun normalize(id: String): String = id
        .replace(jarSuffixRegex, "")
        .replace(anonymousClassRegex, "")
}
