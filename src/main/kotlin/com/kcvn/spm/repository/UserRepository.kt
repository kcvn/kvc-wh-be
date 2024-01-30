package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.pojos.AuthUserClaim
import com.kcvn.spm.model.tables.references.AUTH_USER
import com.kcvn.spm.model.tables.references.AUTH_USER_CLAIM
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class UserRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val POSITION_TYPE = "position"
    }

    fun findByKeywordPaginated(keyword: String?, pageable: Pageable): Pair<List<AuthUser>, Int> {
        var condition: Condition = DSL.noCondition()
        if (keyword != null) {
            condition = condition.and(
                AUTH_USER.USERNAME.ne("admin")
                    .and(AUTH_USER.USERNAME.contains(keyword)
                        .or(AUTH_USER.FULL_NAME.contains(keyword))
                        .or(AUTH_USER.FULL_NAME_UNSIGNED.contains(keyword)))
            )
        }else {
            condition = condition.and(AUTH_USER.USERNAME.ne("admin"))
        }
        val users = context.selectFrom(AUTH_USER)
            .where(condition.and(AUTH_USER.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, AUTH_USER.CREATED_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(AuthUser::class.java)
        val total =
            context.fetchCount(AUTH_USER, condition.and(AUTH_USER.IS_DELETED.eq(false)))
        return Pair(users, total)
    }
    fun findById(id: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.ID.eq(id)).fetchInto(AuthUser::class.java).firstOrNull()

    fun findByUsername(userName: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.USERNAME.eq(userName).and(AUTH_USER.IS_DELETED.eq(false)))
            .fetchInto(AuthUser::class.java).firstOrNull()

    fun findByListUsername(userNames: List<String>): List<AuthUser?> {
        return context.selectFrom(AUTH_USER).where(AUTH_USER.USERNAME.`in`(userNames))
            .and(AUTH_USER.IS_DELETED.eq(false))
            .fetchInto(AuthUser::class.java)
    }


    fun findByEmail(email: String): AuthUser? =
        context.selectFrom(AUTH_USER).where(AUTH_USER.EMAIL.eq(email).and(AUTH_USER.IS_DELETED.eq(false)))
            .fetchInto(AuthUser::class.java).firstOrNull()



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
            CommonUtils.loggedInUser() ?: "SYSTEM"
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
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
        .where(AUTH_USER.ID.eq(user.id))
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun updatePassword(user: AuthUser): AuthUser? = context.update(AUTH_USER)
        .set(AUTH_USER.PASSWORD, user.password)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
        .where(AUTH_USER.ID.eq(user.id))
        .returningResult(AUTH_USER)
        .fetchInto(AuthUser::class.java).firstOrNull()

    fun deleteById(id: String) = context.update(AUTH_USER)
        .set(AUTH_USER.IS_DELETED, true)
        .set(AUTH_USER.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
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
                CommonUtils.loggedInUser() ?: "SYSTEM"
            ).execute()
        }
    }

    fun getUserClaim(userId: String, claimType: String?): List<AuthUserClaim> {
        var condition: Condition = DSL.noCondition().and(AUTH_USER_CLAIM.USER_ID.eq(userId)).and(AUTH_USER_CLAIM.IS_DELETED.eq(false))
        if (!claimType.isNullOrEmpty()) {
            condition = condition.and(AUTH_USER_CLAIM.CLAIM_TYPE.eq(claimType))
        }
        return context.selectFrom(AUTH_USER_CLAIM).where(condition).fetchInto(AuthUserClaim::class.java)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "id" -> {
                AUTH_USER.ID
            }
            "username" -> {
                AUTH_USER.USERNAME
            }
            "employeeCode" -> {
                AUTH_USER.EMPLOYEE_CODE
            }
            "email" -> {
                AUTH_USER.EMAIL
            }
            "phoneNumber" -> {
                AUTH_USER.PHONE_NUMBER
            }
            "fullName" -> {
                AUTH_USER.FULL_NAME
            }
            "dateOfBirth" -> {
                AUTH_USER.DATE_OF_BIRTH
            }
            "createdDate" -> {
                AUTH_USER.CREATED_DATE
            }
            else -> {
                val errorMessage = java.lang.String.format("Could not find table field: $sortFieldName")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }
        return sortField
    }
}