package com.kcvn.spm.sample.config.support

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.sample.dto.UserDto
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ParseException
import org.springframework.batch.item.file.FlatFileParseException
import java.time.LocalDate

class UserItemProcessor : ItemProcessor<UserDto, AuthUser> {
    private val logger = LoggerFactory.getLogger(UserItemProcessor::class.java)

    override fun process(userDto: UserDto): AuthUser {
        logger.info("processing user ${userDto.username}")
        val user = AuthUser()
        user.id = userDto.id
        user.username = userDto.username
        user.password = userDto.password
        user.employeeCode = userDto.employeeCode
        user.email = userDto.email
        user.phoneNumber = userDto.phoneNumber
        user.fullName = userDto.fullName
        user.fullNameUnsigned = userDto.fullNameUnsigned
        try {
            user.dateOfBirth = if (StringUtils.isEmpty(userDto.dateOfBirth)) null else LocalDate.parse(userDto.dateOfBirth)
        } catch (e: Exception) {
            throw ParseException("error parsing file")
        }

        user.status = userDto.status!!.toShort()
        return user
    }
}