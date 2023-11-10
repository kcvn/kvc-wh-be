package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Roles
import com.kcvn.spm.model.tables.pojos.UserRoles
import com.kcvn.spm.model.tables.references.ROLES
import com.kcvn.spm.model.tables.references.USER_ROLES
import com.kcvn.spm.security.ERole
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class RoleService(private val context: DSLContext) {
    fun findAll() = context.selectFrom(ROLES).fetchInto(Roles::class.java)

    fun findById(id: Int) = context.selectFrom(ROLES).where(ROLES.ROLE_ID.eq(id)).fetchInto(Roles::class.java).firstOrNull()

    fun findByName(name: ERole): Roles? =
        context.selectFrom(ROLES).where(ROLES.ROLE_NAME.eq(name.name)).fetchInto(Roles::class.java).firstOrNull()

    fun findByUser(userId: Long): MutableList<Roles> {
        val userRoles = context.selectFrom(USER_ROLES).where(USER_ROLES.USER_ID.eq(userId))
            .fetchInto(UserRoles::class.java)
        return context.selectFrom(ROLES).where(ROLES.ROLE_ID.`in`(userRoles.map { role -> role.roleId }))
            .fetchInto(Roles::class.java)
    }

    fun saveUserRoles(userId: Long, roles: Set<Roles>) {
        for (role in roles) {
            context.insertInto(USER_ROLES, USER_ROLES.USER_ID, USER_ROLES.ROLE_ID)
                .values(userId, role.roleId)
                .execute()
        }
    }
}