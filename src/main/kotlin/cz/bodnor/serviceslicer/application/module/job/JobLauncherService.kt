package cz.bodnor.serviceslicer.application.module.job

import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParameters
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class JobLauncherService(
    private val jobLauncher: JobLauncher,
) {

    @Async
    @Transactional(propagation = Propagation.NEVER)
    fun launch(
        job: Job,
        parameters: JobParameters,
    ) {
        jobLauncher.run(job, parameters)
    }
}
