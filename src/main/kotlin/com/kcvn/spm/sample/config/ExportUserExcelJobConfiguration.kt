package com.kcvn.spm.sample.config

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.sample.config.support.UserExcelItemWriter
import com.kcvn.spm.sample.config.support.UserJooqItemReader
import org.jooq.DSLContext
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.core.io.FileSystemResource
import org.springframework.jdbc.support.JdbcTransactionManager

@Configuration
@EnableBatchProcessing
class ExportUserExcelJobConfiguration(private val dslContext: DSLContext) {
    @Bean
    fun itemReaderDB2(): UserJooqItemReader {
        return UserJooqItemReader(dslContext)
    }

    @Bean
    @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun itemExcelWriter(
        @Value("#{jobParameters[outputFile]}") pathToFile: String
    ): UserExcelItemWriter {
        val writer = UserExcelItemWriter()
        writer.setResource(FileSystemResource(pathToFile))
        writer.setHeaders(listOf("id", "username", "password", "employeeCode", "email", "phoneNumber", "fullName", "fullNameUnsigned", "dateOfBirth", "status"))
        return writer
    }

    @Bean
    fun exportExcelUserStep(jobRepository: JobRepository,
                          transactionManager: JdbcTransactionManager,
                          itemReaderDB2: UserJooqItemReader,
                          itemExcelWriter: UserExcelItemWriter): Step {
        return StepBuilder("step1", jobRepository).chunk<AuthUser, AuthUser>(3, transactionManager)
            .reader(itemReaderDB2)
            .writer(itemExcelWriter)
            .build()
    }

    @Bean
    fun exportExcelJob(
        jobRepository: JobRepository,
        exportExcelUserStep: Step
    ): Job {
        return JobBuilder("exportExcelSampleJob", jobRepository)
            .start(exportExcelUserStep)
            .build()
    }
}