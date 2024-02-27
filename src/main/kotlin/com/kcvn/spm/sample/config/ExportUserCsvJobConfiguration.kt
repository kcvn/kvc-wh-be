package com.kcvn.spm.sample.config

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.sample.config.support.UserJooqItemReader
import org.jooq.DSLContext
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.core.io.FileSystemResource
import org.springframework.jdbc.support.JdbcTransactionManager

@Configuration
@EnableBatchProcessing
class ExportUserCsvJobConfiguration(private val dslContext: DSLContext) {
    @Bean
    fun itemReaderDB(): UserJooqItemReader {
        return UserJooqItemReader(dslContext)
    }

    @Bean
    @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun itemCsvWriter(
        @Value("#{jobParameters[outputFile]}") pathToFile: String
    ): FlatFileItemWriter<AuthUser> {
        return FlatFileItemWriterBuilder<AuthUser>().name("itemWriter")
            .resource(FileSystemResource(pathToFile))
            .delimited()
            .names("id", "username", "password", "employeeCode", "email", "phoneNumber", "fullName", "fullNameUnsigned", "dateOfBirth", "status")
            .headerCallback { writer ->
                writer.append("id,username,password,employeeCode,email,phoneNumber,fullName,fullNameUnsigned,dateOfBirth,status")
            }
            .build()
    }

    @Bean
    fun exportCsvUserStep(jobRepository: JobRepository,
                          transactionManager: JdbcTransactionManager,
                          itemReaderDB: UserJooqItemReader,
                          itemCsvWriter: FlatFileItemWriter<AuthUser>): Step {
        return StepBuilder("step1", jobRepository).chunk<AuthUser, AuthUser>(3, transactionManager)
            .reader(itemReaderDB)
            .writer(itemCsvWriter)
            .build()
    }

    @Bean
    fun exportCsvJob(
        jobRepository: JobRepository,
        exportCsvUserStep: Step
    ): Job {
        return JobBuilder("exportCsvSampleJob", jobRepository)
            .start(exportCsvUserStep)
            .build()
    }
}