package com.kcvn.spm.sample.config

import com.kcvn.spm.common.batch.excel.RowMapper
import com.kcvn.spm.common.batch.excel.mapping.BeanWrapperRowMapper
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
import org.springframework.batch.item.file.FlatFileItemWriter
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
class ImportUserExcelJobConfiguration(private val dsl: DSLContext) {
    @Bean
    @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun excelPersonReader(@Value("#{jobParameters[fullPathFileName]}") pathToFile: String): PoiItemReader<UserDto> {
        val reader: PoiItemReader<UserDto> = PoiItemReader()
        reader.setLinesToSkip(1)
        reader.setResource(FileSystemResource(pathToFile))
        reader.setRowMapper(excelRowMapper())
        return reader
    }

    private fun excelRowMapper(): RowMapper<UserDto> {
        val rowMapper: BeanWrapperRowMapper<UserDto> = BeanWrapperRowMapper()
        rowMapper.setTargetType(UserDto::class.java)
        return rowMapper
    }

    @Bean
    fun excelProcessor(): UserItemProcessor = UserItemProcessor()

    @Bean
    fun excelErrorProcessor(): ErrorUserItemProcessor = ErrorUserItemProcessor()

    @Bean
    fun writerExcel(): InsertUserJooqItemWriter = InsertUserJooqItemWriter(dsl)

    @Bean
    @Scope(value = "step", proxyMode = ScopedProxyMode.TARGET_CLASS)
    fun itemErrorCsvWriter2(
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
    fun step1Excel(
        jobRepository: JobRepository,
        transactionManager: DataSourceTransactionManager,
        excelPersonReader: PoiItemReader<UserDto>,
        excelProcessor: UserItemProcessor,
        writerExcel: InsertUserJooqItemWriter
    ): Step {
        return StepBuilder("step1Excel", jobRepository)
            .chunk<UserDto, AuthUser>(10, transactionManager)
            .reader(excelPersonReader)
            .processor(excelProcessor)
            .writer(writerExcel)
            .faultTolerant()
            .skipLimit(10)
            .skip(ParseException::class.java)
            .build()
    }

    @Bean
    fun step2Excel(
        jobRepository: JobRepository,
        transactionManager: DataSourceTransactionManager,
        excelPersonReader: PoiItemReader<UserDto>,
        excelErrorProcessor: ErrorUserItemProcessor,
        itemErrorCsvWriter2: FlatFileItemWriter<UserDto>
    ): Step {
        return StepBuilder("return error file", jobRepository)
            .chunk<UserDto, UserDto>(10, transactionManager)
            .reader(excelPersonReader)
            .processor(excelErrorProcessor)
            .writer(itemErrorCsvWriter2)
            .build()
    }

    @Bean
    fun importExcelJob(jobRepository: JobRepository,
                       clearDummyStep: Step,
                       step1Excel: Step,
                       listener: JobCompletionNotificationListener,
                       step2Excel: Step): Job {
        return JobBuilder("importExcelJob", jobRepository)
            .listener(listener)
            .start(clearDummyStep)
            .next(step1Excel)
            .on("*").end()
            .from(step1Excel).on("FAILED").to(step2Excel).end()
            .build()
    }
}