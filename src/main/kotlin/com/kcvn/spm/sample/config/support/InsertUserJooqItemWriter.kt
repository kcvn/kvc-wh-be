package com.kcvn.spm.sample.config.support

import com.kcvn.spm.common.batch.db.AbstractJooqItemWriter
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.references.AUTH_USER
import org.jooq.DSLContext
import org.springframework.batch.item.Chunk
import org.springframework.beans.factory.InitializingBean

class InsertUserJooqItemWriter(private val dsl: DSLContext) : AbstractJooqItemWriter<AuthUser?>(dsl), InitializingBean {
    override fun write(chunk: Chunk<out AuthUser?>) {
        for (user in chunk) {
            logger.info("insert ${user!!.username}")
            dsl.insertInto(
                AUTH_USER,
                AUTH_USER.USERNAME, AUTH_USER.PASSWORD, AUTH_USER.EMPLOYEE_CODE, AUTH_USER.EMAIL,
                AUTH_USER.PHONE_NUMBER, AUTH_USER.FULL_NAME, AUTH_USER.FULL_NAME_UNSIGNED,
                AUTH_USER.DATE_OF_BIRTH, AUTH_USER.AVATAR, AUTH_USER.STATUS, AUTH_USER.CREATED_BY
            )
                .values(
                    user.username,
                    user.password,
                    user.employeeCode,
                    user.email,
                    user.phoneNumber,
                    user.fullName,
                    user.fullNameUnsigned,
                    user.dateOfBirth,
                    user.avatar,
                    user.status,
                    CommonUtils.loggedInUser() ?: Constants.SYSTEM
                ).execute()
        }
    }
}