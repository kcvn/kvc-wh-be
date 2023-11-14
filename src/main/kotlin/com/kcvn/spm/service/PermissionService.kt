package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Permissions
import com.kcvn.spm.model.tables.references.PERMISSIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class PermissionService(private val context: DSLContext) {
    fun findAll() = context.selectFrom(PERMISSIONS).fetchInto(Permissions::class.java)

    fun findById(id: Int): Permissions? =
        context.selectFrom(PERMISSIONS).where(PERMISSIONS.PERMISSION_ID.eq(id)).fetchInto(Permissions::class.java)
            .firstOrNull()

    fun save(permission: Permissions) =
        context.insertInto(PERMISSIONS, PERMISSIONS.PERMISSION_NAME, PERMISSIONS.PERMISSION_DESCRIPTION)
            .values(permission.permissionName, permission.permissionDescription)
            .returningResult(PERMISSIONS.PERMISSION_ID)
            .fetchOne()?.value1()

    fun update(permission: Permissions) = context.update(PERMISSIONS)
        .set(PERMISSIONS.PERMISSION_NAME, permission.permissionName)
        .set(PERMISSIONS.PERMISSION_DESCRIPTION, permission.permissionDescription)
        .where(PERMISSIONS.PERMISSION_ID.eq(permission.permissionId))
        .returningResult(PERMISSIONS)
        .fetchInto(Permissions::class.java).firstOrNull()

    fun deleteById(id: Int) = context.deleteFrom(PERMISSIONS).where(PERMISSIONS.PERMISSION_ID.eq(id)).execute()
}