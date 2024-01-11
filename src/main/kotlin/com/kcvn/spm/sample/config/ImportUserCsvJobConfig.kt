package com.kcvn.spm.sample.config

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.sample.dto.UserDto
import com.kcvn.spm.sample.config.support.ClearDummyUserJooqItemReader
import com.kcvn.spm.sample.config.support.ClearDummyUserJooqItemWriter
import com.kcvn.spm.sample.config.support.InsertUserJooqItemWriter
import com.kcvn.spm.sample.config.support.UserItemProcessor
import org.jooq.DSLContext
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.context.annotation.ScopedProxyMode
import org.springframework.core.io.FileSystemResource
import org.springframework.jdbc.datasource.DataSourceTransactionManager

@Configuration
@EnableBatchProcessing
class ImportUserCsvJobConfig(private val dslContext: DSLContext) {
    @Bean
    fun clearDummyReader(): ClearDummyUserJooqItemReader = ClearDummyUserJooqItemReader(dslContext)

    @Bean
    fun clearDummyWriter(): ClearDummyUserJooqItemWriter = ClearDummyUserJooqItemWriter(dslContext)

    @Bean
    fun clearDummyStep(
        jobRepository: JobRepository,
        transactionManager: DataSourceTransactionManager,
        clearDummyReader: ClearDummyUserJooqItemReader,
        clearDummyWriter: ClearDummyUserJooqItemWriter
    ): Step {
        return StepBuilder("clear dummy users", jobRepository)
            .chunk<AuthUser, AuthUser>(10, transactionManager)
            .reader(clearDummyReader)
            .writer(clearDummyWriter)
            .build()
    }

    @Bean
    @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun importUserReader(@Value("#{jobParameters[fullPathFileName]}") pathToFile: String
    ): FlatFileItemReader<UserDto> {
        return FlatFileItemReaderBuilder<UserDto>()
            .name("personItemReader")
            .resource(FileSystemResource(pathToFile))
            .delimited()
            .names("id", "username", "password", "employeeCode", "email", "phoneNumber", "fullName", "fullNameUnsigned", "dateOfBirth", "status")
            .linesToSkip(1)
            .targetType(UserDto::class.java)
            .build()
    }

    @Bean
    fun importUserWriter(): InsertUserJooqItemWriter = InsertUserJooqItemWriter(dslContext)

    @Bean
    fun userItemProcessor(): UserItemProcessor = UserItemProcessor()

    @Bean
    fun importCsvStep(
        jobRepository: JobRepository,
        transactionManager: DataSourceTransactionManager,
        importUserReader: FlatFileItemReader<UserDto>,
        importUserWriter: InsertUserJooqItemWriter,
        userItemProcessor: UserItemProcessor
    ): Step {
        return StepBuilder("validate users", jobRepository)
            .chunk<UserDto, AuthUser>(10, transactionManager)
            .reader(importUserReader)
            .processor(userItemProcessor)
            .writer(importUserWriter)
            .build()
    }

    @Bean
    fun importUserJob(jobRepository: JobRepository, clearDummyStep: Step, importCsvStep: Step): Job {
        return JobBuilder("importCsvJob", jobRepository)
            .start(clearDummyStep)
            .next(importCsvStep)
            .build()
    }
}