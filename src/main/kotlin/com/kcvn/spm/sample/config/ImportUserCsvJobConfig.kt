package com.kcvn.spm.sample.config

import com.kcvn.spm.common.batch.excel.poi.PoiItemReader
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.sample.config.support.*
import com.kcvn.spm.sample.dto.UserDto
import org.jooq.DSLContext
import org.springframework.batch.core.Job
import org.springframework.batch.core.Step
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.FlatFileItemWriter
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder
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
    @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun itemErrorCsvWriter(
        @Value("#{jobParameters[outputFile]}") pathToFile: String
    ): FlatFileItemWriter<UserDto> {
        return FlatFileItemWriterBuilder<UserDto>().name("itemWriter")
            .resource(FileSystemResource(pathToFile))
            .delimited()
            .names("id", "username", "password", "employeeCode", "email", "phoneNumber", "fullName", "fullNameUnsigned", "dateOfBirth", "status", "message")
            .headerCallback { writer ->
                writer.append("id,username,password,employeeCode,email,phoneNumber,fullName,fullNameUnsigned,dateOfBirth,status,message")
            }
            .build()
    }

    @Bean
    fun step2Csv(
        jobRepository: JobRepository,
        transactionManager: DataSourceTransactionManager,
        reader: FlatFileItemReader<UserDto>,
        excelErrorProcessor: ErrorUserItemProcessor,
        itemErrorCsvWriter: FlatFileItemWriter<UserDto>
    ): Step {
        return StepBuilder("return error file", jobRepository)
            .chunk<UserDto, UserDto>(10, transactionManager)
            .reader(reader)
            .processor(excelErrorProcessor)
            .writer(itemErrorCsvWriter)
            .build()
    }

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
            .faultTolerant()
            .skipLimit(10)
            .skip(ParseException::class.java)
            .build()
    }

    @Bean
    fun importUserJob(jobRepository: JobRepository, clearDummyStep: Step, importCsvStep: Step, step2Csv: Step): Job {
        return JobBuilder("importCsvJob", jobRepository)
            .start(clearDummyStep)
            .next(importCsvStep)
            .on("*").end()
            .from(importCsvStep).on("FAILED").to(step2Csv).end()
            .build()
    }
}