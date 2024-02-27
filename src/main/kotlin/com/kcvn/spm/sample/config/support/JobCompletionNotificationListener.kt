package com.kcvn.spm.sample.config.support

import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobExecutionListener
import org.springframework.stereotype.Component


@Component
class JobCompletionNotificationListener(private val context: DSLContext) : JobExecutionListener {
    private val logger = LoggerFactory.getLogger(JobCompletionNotificationListener::class.java)

    override fun afterJob(jobExecution: JobExecution) {
        if (jobExecution.status == BatchStatus.COMPLETED) {
            logger.info("!!! JOB FINISHED! Time to verify the results")
//            context.selectFrom()
//            jdbcTemplate
//                .query("SELECT first_name, last_name FROM people", DataClassRowMapper<T>(Person::class.java))
//                .forEach { person ->
//                    log.info(
//                        "Found <{{}}> in the database.",
//                        person
//                    )
//                }
        }
    }
}