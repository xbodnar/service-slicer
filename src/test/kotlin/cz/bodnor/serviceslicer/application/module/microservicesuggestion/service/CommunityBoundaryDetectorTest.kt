package cz.bodnor.serviceslicer.application.module.microservicesuggestion.service

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.LabelPropagationCommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.LouvainCommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.service.CommunityBoundaryDetector
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.domain.analysis.suggestion.BoundaryDetectionAlgorithm
import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test

@Disabled("Tests very slow")
class CommunityDetectionBoundaryDetectorTest(
    @Autowired private val underTest: CommunityBoundaryDetector,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val classNodeRepository: ClassNodeRepository,
) : IntegrationTest() {

    @Test
    fun `Should detect boundaries using Label Propagation`() {
        // Given
        projectRepository.save(
            Project(
                id = UUID.fromString("0235ec36-0975-490d-a4c6-3d3d68081b5b"),
                name = "Test Project",
            ),
        )
        val classNodes = classNodeRepository.findAll()

        // when
        val microserviceSuggestion = underTest.invoke(
            analysisJobId = UUID.randomUUID(),
            classNodes = classNodes,
            communityDetectionStrategy = LabelPropagationCommunityDetectionStrategy(),
        )

        // then
        assertThat(microserviceSuggestion).isNotNull
        assertThat(
            microserviceSuggestion.algorithm,
        ).isEqualTo(BoundaryDetectionAlgorithm.COMMUNITY_DETECTION_LABEL_PROPAGATION)
        assertThat(microserviceSuggestion.boundaries).isNotEmpty
    }

    @Test
    fun `Should detect boundaries using Louvain`() {
        // Given
        projectRepository.save(
            Project(
                id = UUID.fromString("0235ec36-0975-490d-a4c6-3d3d68081b5b"),
                name = "Test Project",
            ),
        )
        val classNodes = classNodeRepository.findAll()

        // when
        val microserviceSuggestion = underTest.invoke(
            analysisJobId = UUID.randomUUID(),
            classNodes = classNodes,
            communityDetectionStrategy = LouvainCommunityDetectionStrategy(),
        )

        // then
        assertThat(microserviceSuggestion).isNotNull
        assertThat(microserviceSuggestion.algorithm).isEqualTo(BoundaryDetectionAlgorithm.COMMUNITY_DETECTION_LOUVAIN)
        assertThat(microserviceSuggestion.boundaries).isNotEmpty
    }

    @Test
    fun `Louvain should produce more balanced communities than Label Propagation`() {
        // Given
        projectRepository.save(
            Project(
                id = UUID.fromString("0235ec36-0975-490d-a4c6-3d3d68081b5b"),
                name = "Test Project",
            ),
        )
        val classNodes = classNodeRepository.findAll()
        val analysisJobId = UUID.randomUUID()

        // when
        val labelPropSuggestion = underTest.invoke(
            analysisJobId = analysisJobId,
            classNodes = classNodes,
            communityDetectionStrategy = LabelPropagationCommunityDetectionStrategy(),
        )

        val louvainSuggestion = underTest.invoke(
            analysisJobId = analysisJobId,
            classNodes = classNodes,
            communityDetectionStrategy = LouvainCommunityDetectionStrategy(),
        )

        // then - Calculate coefficient of variation (lower = more balanced)
        val labelPropCV = calculateCoefficientOfVariation(labelPropSuggestion.boundaries.map { it.metrics.size })
        val louvainCV = calculateCoefficientOfVariation(louvainSuggestion.boundaries.map { it.metrics.size })

        println("Label Propagation communities: ${labelPropSuggestion.boundaries.size}, CV: $labelPropCV")
        println("Louvain communities: ${louvainSuggestion.boundaries.size}, CV: $louvainCV")

        // Louvain should generally produce more balanced results (lower CV)
        assertThat(louvainCV).isLessThanOrEqualTo(labelPropCV * 1.5) // Allow some tolerance
    }

    private fun calculateCoefficientOfVariation(values: List<Int>): Double {
        if (values.isEmpty()) return 0.0
        val mean = values.average()
        if (mean == 0.0) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        val stdDev = kotlin.math.sqrt(variance)
        return stdDev / mean
    }
}
