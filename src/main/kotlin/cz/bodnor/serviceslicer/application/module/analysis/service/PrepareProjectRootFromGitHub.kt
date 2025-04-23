package cz.bodnor.serviceslicer.application.module.analysis.service

import cz.bodnor.serviceslicer.domain.projectsource.GitHubProjectSource
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class PrepareProjectRootFromGitHub {

    operator fun invoke(source: GitHubProjectSource): Path {
        TODO()
    }
}
