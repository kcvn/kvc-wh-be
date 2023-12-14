package com.kcvn.spm.common.repository

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.references.AUTH_USER
import com.kcvn.spm.common.CommonUtils
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class UserDAO(private val context: DSLContext) {
    fun findAll(): List<AuthUser> =
        context.selectFrom(AUTH_USER).where(AUTH_USER.IS_DELETED.eq(false)).fetchInto(AuthUser::class.java)

    fun findById(id: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.ID.eq(id)).fetchInto(AuthUser::class.java).firstOrNull()

    fun findByUsername(userName: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.USERNAME.eq(userName).and(AUTH_USER.IS_DELETED.eq(false)))
            .fetchInto(AuthUser::class.java).firstOrNull()

    fun findByUsernameContaining(userName: String): List<AuthUser> =
        context.selectFrom(AUTH_USER).where(AUTH_USER.USERNAME.contains(userName).and(AUTH_USER.IS_DELETED.eq(false)))
            .fetchInto(AuthUser::class.java)

    fun save(user: AuthUser): AuthUser? = context
        .insertInto(
            AUTH_USER,
            AUTH_USER.USERNAME, AUTH_USER.PASSWORD, AUTH_USER.EMAIL,
            AUTH_USER.PHONE_NUMBER, AUTH_USER.FULL_NAME, AUTH_USER.FULL_NAME_UNSIGNED,
            AUTH_USER.DATE_OF_BIRTH, AUTH_USER.AVATAR, AUTH_USER.CREATED_BY
        )
        .values(
            user.username,
            user.password,
            user.email,
            user.phoneNumber,
            user.fullName,
            user.fullNameUnsigned,
            user.dateOfBirth,
            user.avatar,
            CommonUtils.loggedInUser()
        )
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun updateInfo(user: AuthUser): AuthUser? = context.update(AUTH_USER)
        .set(AUTH_USER.EMAIL, user.email)
        .set(AUTH_USER.PHONE_NUMBER, user.phoneNumber)
        .set(AUTH_USER.FULL_NAME, user.fullName)
        .set(AUTH_USER.FULL_NAME_UNSIGNED, user.fullNameUnsigned)
        .set(AUTH_USER.DATE_OF_BIRTH, user.dateOfBirth)
        .set(AUTH_USER.AVATAR, user.avatar)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser())
        .where(AUTH_USER.ID.eq(user.id))
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun updatePassword(user: AuthUser): AuthUser? = context.update(AUTH_USER)
        .set(AUTH_USER.PASSWORD, user.password)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser())
        .where(AUTH_USER.ID.eq(user.id))
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun deleteById(id: String) = context.update(AUTH_USER)
        .set(AUTH_USER.IS_DELETED, true)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser())
        .where(AUTH_USER.ID.eq(id)).execute()
}