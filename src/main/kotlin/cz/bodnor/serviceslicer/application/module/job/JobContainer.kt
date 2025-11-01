package cz.bodnor.serviceslicer.application.module.job

import cz.bodnor.serviceslicer.domain.job.JobType
import org.springframework.batch.core.Job
import org.springframework.stereotype.Component

@Component
class JobContainer(
    jobBeans: List<Job>,
) {
    private val jobs: MutableMap<JobType, Job> = mutableMapOf()

    init {
        jobBeans.forEach { job ->
            jobs[JobType.valueOf(job.name)] = job
        }
    }

    operator fun get(jobType: JobType): Job = jobs[jobType] ?: error("Job $jobType not found")
}
