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
    fun excelProcessor(): UserItemProcessor {
        return UserItemProcessor()
    }

    @Bean
    fun writerExcel(): InsertUserJooqItemWriter = InsertUserJooqItemWriter(dsl)


    @Bean
    fun importExcelJob(jobRepository: JobRepository, step1Excel: Step, listener: JobCompletionNotificationListener): Job {
        return JobBuilder("importExcelJob", jobRepository)
            .listener(listener)
            .start(step1Excel)
            .build()
    }

    @Bean
    fun step1Excel(
        jobRepository: JobRepository,
        transactionManager: DataSourceTransactionManager,
        reader: PoiItemReader<UserDto>,
        excelProcessor: UserItemProcessor,
        writerExcel: InsertUserJooqItemWriter
    ): Step {
        return StepBuilder("step1Excel", jobRepository)
            .chunk<UserDto, AuthUser>(10, transactionManager)
            .reader(reader)
            .processor(excelProcessor)
            .writer(writerExcel)
            .build()
    }
}