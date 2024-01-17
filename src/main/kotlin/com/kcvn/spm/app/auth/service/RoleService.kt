package com.kcvn.spm.app.auth.service

import com.kcvn.spm.app.auth.payload.request.RoleRequest
import com.kcvn.spm.app.auth.payload.response.RoleResponse
import com.kcvn.spm.common.enums.EPermission
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthRole
import com.kcvn.spm.repository.RoleRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RoleService(private val roleRep: RoleRepository) {
    fun findAllPaginated(search: String?, pageable: Pageable): PaginatedResponse {
        val result = roleRep.findByKeywordPaginated(search, pageable)
        return PaginatedResponse(
            result.first.map { RoleResponse(it.id!!, it.name!!, it.description) },
            result.second
        )
    }

    fun findById(id: String): RoleResponse? {
        val role = roleRep.findById(id)
        return if (role == null) {
            null
        } else {
            RoleResponse(
                role.id!!,
                role.name!!,
                role.description,
                roleRep.findPermissionsByRoleIds(listOf(role.id!!)).map { EPermission.valueOf(it.name).value }.toSet()
            )
        }
    }

    fun createRole(request: RoleRequest): RoleResponse? {
        if (roleRep.findByName(request.name!!) != null) {
            throw BusinessException(CommonUtils.getMessage("role.error.nameTaken"))
        }

        val role = AuthRole(
            null,
            request.name,
            request.description
        )
        val permissionCodes: Set<String> = request.permissionCodes ?: setOf()
        permissionCodes.forEach { p: String ->
            if (EPermission.values().none { it.value == p })
                throw BusinessException(CommonUtils.getMessage("permission.error.notFound", arrayOf(p)))
        }
        val roleId = roleRep.save(role)
        return if (roleId != null) {
            roleRep.saveRolePermissions(roleId, permissionCodes)
            com.kcvn.spm.app.auth.payload.response.RoleResponse(
                roleId,
                role.name!!,
                role.description,
                permissionCodes
            )
        } else null
    }

    fun updateRole(roleId: String, request: RoleRequest): RoleResponse {
        val role = roleRep.findById(roleId) ?: throw BusinessException(CommonUtils.getMessage("role.error.notFound"))
        role.name = request.name
        role.description = request.description

        val permissionCodes: Set<String> = request.permissionCodes ?: setOf()
        permissionCodes.forEach { p: String ->
            if (EPermission.values().none { it.value == p })
                throw BusinessException(CommonUtils.getMessage("permission.error.notFound", arrayOf(p)))
        }
        roleRep.saveRolePermissions(roleId, permissionCodes)
        roleRep.update(role)
        return RoleResponse(
            role.id!!,
            role.name!!,
            role.description,
            permissionCodes
        )
    }

    fun deleteById(roleId: String) {
        if (roleRep.isRoleUsed(roleId))
            throw BusinessException(CommonUtils.getMessage("role.error.inUse"))
        roleRep.deleteById(roleId)
    }
}