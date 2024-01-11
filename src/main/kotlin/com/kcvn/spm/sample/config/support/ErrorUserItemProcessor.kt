package com.kcvn.spm.sample.config.support

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.sample.dto.UserDto
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.FlatFileParseException
import java.time.LocalDate

class ErrorUserItemProcessor : ItemProcessor<UserDto, UserDto> {
    private val logger = LoggerFactory.getLogger(ErrorUserItemProcessor::class.java)

    override fun process(user: UserDto): UserDto {
        logger.info("processing user ${user.username}")
        try {
            if (!StringUtils.isEmpty(user.dateOfBirth)) LocalDate.parse(user.dateOfBirth)
        } catch (e: Exception) {
            user.message = "wrong date format"
        }
        return user
    }
}