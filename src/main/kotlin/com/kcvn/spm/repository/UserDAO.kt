package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.references.AUTH_USER
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthUserClaim
import com.kcvn.spm.model.tables.references.AUTH_USER_CLAIM
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class UserDAO(private val context: DSLContext) {
    companion object {
        const val POSITION_TYPE = "position"
    }

    fun findAll(): List<AuthUser> =
        context.selectFrom(AUTH_USER).where(AUTH_USER.IS_DELETED.eq(false))
            .orderBy(AUTH_USER.CREATED_DATE)
            .fetchInto(AuthUser::class.java)

    fun findAllPaginated(page: Int, size: Int): Pair<List<AuthUser>, Int> {
        val users = context.selectFrom(AUTH_USER).where(AUTH_USER.IS_DELETED.eq(false))
            .orderBy(AUTH_USER.CREATED_DATE)
            .limit(size).offset((page - 1) * size)
            .fetchInto(AuthUser::class.java)
        val total = context.fetchCount(AUTH_USER, AUTH_USER.IS_DELETED.eq(false))
        return Pair(users, total)
    }


    fun findById(id: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.ID.eq(id)).fetchInto(AuthUser::class.java).firstOrNull()

    fun findByUsername(userName: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.USERNAME.eq(userName).and(AUTH_USER.IS_DELETED.eq(false)))
            .fetchInto(AuthUser::class.java).firstOrNull()

    fun findByEmail(email: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.EMAIL.eq(email).and(AUTH_USER.IS_DELETED.eq(false)))
            .fetchInto(AuthUser::class.java).firstOrNull()

    fun findByKeyword(keyword: String): List<AuthUser> =
        context.selectFrom(AUTH_USER).where(
            AUTH_USER.USERNAME.contains(keyword)
                .or(AUTH_USER.FULL_NAME.contains(keyword))
                .or(AUTH_USER.FULL_NAME_UNSIGNED.contains(keyword))
        )
            .and(AUTH_USER.IS_DELETED.eq(false))
            .fetchInto(AuthUser::class.java)

    fun findByKeywordPaginated(keyword: String, page: Int, size: Int): Pair<List<AuthUser>, Int> {
        val users = context.selectFrom(AUTH_USER).where(
            AUTH_USER.USERNAME.contains(keyword)
                .or(AUTH_USER.FULL_NAME.contains(keyword))
                .or(AUTH_USER.FULL_NAME_UNSIGNED.contains(keyword))
        )
            .and(AUTH_USER.IS_DELETED.eq(false))
            .orderBy(AUTH_USER.CREATED_DATE)
            .limit(size).offset((page - 1) * size)
            .fetchInto(AuthUser::class.java)
        val total = context.fetchCount(AUTH_USER, AUTH_USER.USERNAME.contains(keyword).and(AUTH_USER.IS_DELETED.eq(false)))
        return Pair(users, total)
    }

    fun save(user: AuthUser): AuthUser? = context
        .insertInto(
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
            CommonUtils.loggedInUser()?: "SYSTEM"
        )
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun updateInfo(user: AuthUser): AuthUser? = context.update(AUTH_USER)
        .set(AUTH_USER.EMPLOYEE_CODE, user.employeeCode)
        .set(AUTH_USER.EMAIL, user.email)
        .set(AUTH_USER.PHONE_NUMBER, user.phoneNumber)
        .set(AUTH_USER.FULL_NAME, user.fullName)
        .set(AUTH_USER.FULL_NAME_UNSIGNED, user.fullNameUnsigned)
        .set(AUTH_USER.DATE_OF_BIRTH, user.dateOfBirth)
        .set(AUTH_USER.AVATAR, user.avatar)
        .set(AUTH_USER.STATUS, user.status)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser()?: "SYSTEM")
        .where(AUTH_USER.ID.eq(user.id))
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun updatePassword(user: AuthUser): AuthUser? = context.update(AUTH_USER)
        .set(AUTH_USER.PASSWORD, user.password)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser()?: "SYSTEM")
        .where(AUTH_USER.ID.eq(user.id))
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun deleteById(id: String) = context.update(AUTH_USER)
        .set(AUTH_USER.IS_DELETED, true)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser()?: "SYSTEM")
        .where(AUTH_USER.ID.eq(id)).execute()

    fun findPositions(userIds: List<String>): Map<String, List<String>> =
        context.selectFrom(AUTH_USER_CLAIM).where(
            AUTH_USER_CLAIM.USER_ID.`in`(userIds)
                .and(AUTH_USER_CLAIM.CLAIM_TYPE.eq(POSITION_TYPE))
                .and(AUTH_USER_CLAIM.IS_DELETED.eq(false))
        ).fetchInto(AuthUserClaim::class.java).groupBy({ it.userId!! }, { it.claimValue!! })

    fun savePositions(userId: String, positions: Set<String>) {
        context.deleteFrom(AUTH_USER_CLAIM).where(
            AUTH_USER_CLAIM.USER_ID.eq(userId)
                .and(AUTH_USER_CLAIM.CLAIM_TYPE.eq(POSITION_TYPE))
                .and(AUTH_USER_CLAIM.IS_DELETED.eq(false))
        )
            .execute()
        positions.forEach {
            context.insertInto(
                AUTH_USER_CLAIM,
                AUTH_USER_CLAIM.USER_ID,
                AUTH_USER_CLAIM.CLAIM_TYPE,
                AUTH_USER_CLAIM.CLAIM_VALUE,
                AUTH_USER_CLAIM.CREATED_BY
            ).values(
                userId,
                POSITION_TYPE,
                it,
                CommonUtils.loggedInUser()?: "SYSTEM"
            ).execute()
        }
    }
}