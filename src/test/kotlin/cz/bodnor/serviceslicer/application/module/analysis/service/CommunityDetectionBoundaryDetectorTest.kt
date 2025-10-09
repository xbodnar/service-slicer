package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.IntegrationTest
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.communitydetection.LabelPropagationCommunityDetectionStrategy
import cz.bodnor.serviceslicer.application.module.microservicesuggestion.service.CommunityBoundaryDetector
import cz.bodnor.serviceslicer.domain.analysis.graph.ClassNodeRepository
import cz.bodnor.serviceslicer.domain.project.Project
import cz.bodnor.serviceslicer.domain.project.ProjectRepository
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.Test

class CommunityDetectionBoundaryDetectorTest(
    @Autowired private val underTest: CommunityBoundaryDetector,
    @Autowired private val projectRepository: ProjectRepository,
    @Autowired private val classNodeRepository: ClassNodeRepository,
) : IntegrationTest() {

    @Test
    fun `Should detect correct boundaries`() {
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
    }
}
