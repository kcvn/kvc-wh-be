package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.Permissions
import com.kcvn.spm.model.tables.pojos.RolePermissions
import com.kcvn.spm.model.tables.references.PERMISSIONS
import com.kcvn.spm.model.tables.references.ROLE_PERMISSIONS
import com.kcvn.spm.model.tables.references.USER_ROLES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PermissionDAO(private val context: DSLContext) {
    fun findAll(): List<Permissions> = context.selectFrom(PERMISSIONS).fetchInto(Permissions::class.java)

    fun findById(id: Int): Permissions? =
        context.selectFrom(PERMISSIONS).where(PERMISSIONS.PERMISSION_ID.eq(id)).fetchInto(Permissions::class.java)
            .firstOrNull()

    fun findByRoleIds(roleIds: List<Int>): List<Permissions> {
        val rolePermissions = context.selectFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.`in`(roleIds)).fetchInto(RolePermissions::class.java)
        return context.selectFrom(PERMISSIONS).where(PERMISSIONS.PERMISSION_ID.`in`(rolePermissions.map { it.permissionId })).fetchInto(Permissions::class.java)
    }

    fun saveRolePermissions(roleId: Int, permissions: Set<Permissions>) {
        context.deleteFrom(ROLE_PERMISSIONS).where(ROLE_PERMISSIONS.ROLE_ID.eq(roleId)).execute()
        for (permission in permissions) {
            context.insertInto(ROLE_PERMISSIONS, ROLE_PERMISSIONS.ROLE_ID, ROLE_PERMISSIONS.PERMISSION_ID)
                .values(roleId, permission.permissionId)
                .execute()
        }
    }
}