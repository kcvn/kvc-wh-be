package com.kcvn.spm.common.repository

import com.kcvn.spm.model.tables.pojos.AuthRole
import com.kcvn.spm.model.tables.references.AUTH_ROLE
import com.kcvn.spm.model.tables.references.AUTH_ROLE_CLAIM
import com.kcvn.spm.model.tables.references.AUTH_USER_ROLE
import com.kcvn.spm.common.CommonUtils
import com.kcvn.spm.common.security.EPermission
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RoleDAO(private val context: DSLContext) {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun findAll(): List<AuthRole> =
        context.selectFrom(AUTH_ROLE).where(AUTH_ROLE.IS_DELETED.eq(false))
            .orderBy(AUTH_ROLE.CREATED_DATE)
            .fetchInto(AuthRole::class.java)

    fun findAllPaginated(page: Int, size: Int): Pair<List<AuthRole>, Int> {
        val roles = context.selectFrom(AUTH_ROLE).where(AUTH_ROLE.IS_DELETED.eq(false))
            .orderBy(AUTH_ROLE.CREATED_DATE)
            .limit(size).offset((page - 1) * size)
            .fetchInto(AuthRole::class.java)
        val total = context.fetchCount(AUTH_ROLE, AUTH_ROLE.IS_DELETED.eq(false))
        return Pair(roles, total)
    }

    fun findById(id: String): AuthRole? =
        context.selectFrom(AUTH_ROLE).where(AUTH_ROLE.ID.eq(id)).fetchInto(AuthRole::class.java).firstOrNull()

    fun findByName(roleName: String): AuthRole? = context.selectFrom(AUTH_ROLE)
        .where(AUTH_ROLE.NAME.eq(roleName).and(AUTH_ROLE.IS_DELETED.eq(false)))
        .fetchInto(AuthRole::class.java).firstOrNull()

    fun save(role: AuthRole): String? =
        context.insertInto(AUTH_ROLE, AUTH_ROLE.NAME, AUTH_ROLE.DESCRIPTION, AUTH_ROLE.CREATED_BY)
            .values(role.name, role.description, CommonUtils.loggedInUser())
            .returningResult(AUTH_ROLE.ID)
            .fetchOne()?.value1()

    fun update(role: AuthRole): AuthRole? =
        context.update(AUTH_ROLE)
            .set(AUTH_ROLE.NAME, role.name)
            .set(AUTH_ROLE.DESCRIPTION, role.description)
            .set(AUTH_ROLE.UPDATED_BY, CommonUtils.loggedInUser())
            .where(AUTH_ROLE.ID.eq(role.id))
            .returningResult(AUTH_ROLE)
            .fetchInto(AuthRole::class.java).firstOrNull()

    fun deleteById(roleId: String) = context.update(AUTH_ROLE)
        .set(AUTH_ROLE.IS_DELETED, true)
        .set(AUTH_ROLE.UPDATED_BY, CommonUtils.loggedInUser())
        .where(AUTH_ROLE.ID.eq(roleId)).execute()

    fun findByUserId(userId: String): List<AuthRole> {
        val userRoles =
            context.select(AUTH_USER_ROLE.ROLE_ID).from(AUTH_USER_ROLE)
                .where(AUTH_USER_ROLE.USER_ID.eq(userId).and(AUTH_USER_ROLE.IS_DELETED.eq(false)))
                .fetch { it.value1() }
        return context.selectFrom(AUTH_ROLE).where(AUTH_ROLE.ID.`in`(userRoles).and(AUTH_ROLE.IS_DELETED.eq(false)))
            .fetchInto(AuthRole::class.java)
    }

    fun saveUserRoles(userId: String, roleIds: Set<String>) {
        context.deleteFrom(AUTH_USER_ROLE).where(AUTH_USER_ROLE.USER_ID.eq(userId)).execute()
        for (role in roleIds) {
            context.insertInto(
                AUTH_USER_ROLE,
                AUTH_USER_ROLE.USER_ID,
                AUTH_USER_ROLE.ROLE_ID,
                AUTH_USER_ROLE.CREATED_BY
            )
                .values(userId, role, CommonUtils.loggedInUser())
                .execute()
        }
    }

    fun findPermissionsByRoleIds(roleIds: List<String>): List<EPermission> {
        val roleClaims = context.select(AUTH_ROLE_CLAIM.CLAIM_VALUE).from(AUTH_ROLE_CLAIM)
            .where(
                AUTH_ROLE_CLAIM.ROLE_ID.`in`(roleIds)
                    .and(AUTH_ROLE_CLAIM.CLAIM_TYPE.eq(PERMISSION_TYPE))
                    .and(AUTH_ROLE_CLAIM.IS_DELETED.eq(false))
            )
            .fetch { it.value1() }
        return EPermission.values().filter { roleClaims.contains(it.value) }
    }

    fun saveRolePermissions(roleId: String, permissionCodes: Set<String>) {
        context.deleteFrom(AUTH_ROLE_CLAIM).where(AUTH_ROLE_CLAIM.ROLE_ID.eq(roleId)).execute()
        for (permission in permissionCodes) {
            context.insertInto(
                AUTH_ROLE_CLAIM,
                AUTH_ROLE_CLAIM.ROLE_ID,
                AUTH_ROLE_CLAIM.CLAIM_TYPE,
                AUTH_ROLE_CLAIM.CLAIM_VALUE,
                AUTH_ROLE_CLAIM.CREATED_BY
            )
                .values(roleId, PERMISSION_TYPE, permission, CommonUtils.loggedInUser())
                .execute()
        }
    }
}