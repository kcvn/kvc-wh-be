package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.Roles
import com.kcvn.spm.model.tables.pojos.UserRoles
import com.kcvn.spm.model.tables.references.ROLES
import com.kcvn.spm.model.tables.references.USERS
import com.kcvn.spm.model.tables.references.USER_ROLES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class RoleDAO(private val context: DSLContext) {
    fun findAll(): List<Roles> = context.selectFrom(ROLES).fetchInto(Roles::class.java)

    fun findById(id: Int): Roles? =
        context.selectFrom(ROLES).where(ROLES.ROLE_ID.eq(id)).fetchInto(Roles::class.java).firstOrNull()

    fun findByName(roleName: String): Roles? =
        context.selectFrom(ROLES).where(ROLES.ROLE_NAME.eq(roleName)).fetchInto(Roles::class.java).firstOrNull()

    fun findByUserId(userId: Long): List<Roles> {
        val userRoles = context.selectFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(userId))
            .fetchInto(UserRoles::class.java)
        return context.selectFrom(ROLES).where(ROLES.ROLE_ID.`in`(userRoles.map { role -> role.roleId }))
            .fetchInto(Roles::class.java)
    }

    fun save(role: Roles): Int? =
        context.insertInto(ROLES, ROLES.ROLE_NAME, ROLES.ROLE_DESCRIPTION)
            .values(role.roleName, role.roleDescription)
            .returningResult(ROLES.ROLE_ID)
            .fetchOne()?.value1()

    fun update(role: Roles): Roles? =
        context.update(ROLES)
            .set(ROLES.ROLE_NAME, role.roleName)
            .set(ROLES.ROLE_DESCRIPTION, role.roleDescription)
            .returningResult(USERS)
            .fetchInto(Roles::class.java).firstOrNull()

    fun deleteById(roleId: Int) = context.deleteFrom(ROLES).where(ROLES.ROLE_ID.eq(roleId)).execute()

    fun saveUserRoles(userId: Long, roles: Set<Roles>) {
        context.deleteFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(userId)).execute()
        for (role in roles) {
            context.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE_ID)
                .values(userId, role.roleId)
                .execute()
        }
    }
}