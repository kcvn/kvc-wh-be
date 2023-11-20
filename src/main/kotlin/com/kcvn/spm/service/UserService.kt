package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Roles
import com.kcvn.spm.model.tables.pojos.Users
import com.kcvn.spm.payload.request.UserRequest
import com.kcvn.spm.payload.response.UserResponse
import com.kcvn.spm.repository.PermissionDAO
import com.kcvn.spm.repository.RoleDAO
import com.kcvn.spm.repository.UserDAO
import com.kcvn.spm.security.EPermission
import com.kcvn.spm.security.ERole
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userDAO: UserDAO,
    private val roleDAO: RoleDAO,
    private val permissionDAO: PermissionDAO,
    private val encoder: PasswordEncoder
) {
    fun getUsers(uName: String?): List<UserResponse> {
        val userList =
            if (uName == null) userDAO.findAll()
            else userDAO.findByUsernameContaining(uName)

        return userList.map { u ->
            UserResponse(
                u.userId!!,
                u.username!!,
                false
            )
        }
    }

    fun findById(id: Long): UserResponse? {
        val user = userDAO.findById(id)
        return if (user == null) null
        else {
            val roles = roleDAO.findByUserId(user.userId!!)
            UserResponse(
                user.userId!!,
                user.username!!,
                roles.stream().anyMatch{ it.roleName.equals(ERole.ROLE_ADMIN.name) },
                permissionDAO.findByRoleIds(roles.map { it.roleId!! })
                    .map { p -> EPermission.valueOf(p.permissionName!!).value }
            )
        }
    }

    fun createUser(request: UserRequest): UserResponse? {
        if (userDAO.findByUsername(request.username!!) != null) {
            throw RuntimeException("Error: Username is already taken!")
        }

        // Create new user's account
        val user = Users(
            null,
            request.username,
            encoder.encode(request.password)
        )
        val roleIds: Set<Int> = request.roles ?: setOf()
        val roles: MutableSet<Roles> = mutableSetOf()
        if (roleIds.isEmpty()) {
            val userRole: Roles = roleDAO.findByName(ERole.ROLE_USER.name)
                ?: throw RuntimeException("Error: Role is not found.")
            roles.add(userRole)
        } else {
            roleIds.forEach { roleId: Int ->
                val role: Roles = roleDAO.findById(roleId)
                    ?: throw RuntimeException("Error: Role is not found.")
                roles.add(role)
            }
        }
        val userId = userDAO.save(user)
        return if (userId != null) {
            roleDAO.saveUserRoles(userId, roles)
            UserResponse(
                userId,
                user.username!!,
                roles.stream().anyMatch{ it.roleName.equals(ERole.ROLE_ADMIN.name) }
            )
        } else null
    }

    fun update(userId: Long, request: UserRequest): UserResponse? {
        var user = userDAO.findById(userId)
        return if (user == null) {
            null
        } else {
            user.password = request.password!!

            val roleIds: Set<Int> = request.roles ?: setOf()
            val roles: MutableSet<Roles> = mutableSetOf()
            if (roleIds.isEmpty()) {
                val userRole: Roles = roleDAO.findByName(ERole.ROLE_USER.name)
                    ?: throw RuntimeException("Error: Role is not found.")
                roles.add(userRole)
            } else {
                roleIds.forEach { roleId: Int ->
                    val role: Roles = roleDAO.findById(roleId)
                        ?: throw RuntimeException("Error: Role is not found.")
                    roles.add(role)
                }
            }
            roleDAO.saveUserRoles(userId, roles)

            user = userDAO.update(user)
            UserResponse(
                user?.userId!!,
                user.username!!,
                roles.stream().anyMatch{ it.roleName.equals(ERole.ROLE_ADMIN.name) }
            )
        }
    }

    fun deleteById(id: Long) {
        val roles = roleDAO.findByUserId(id)
        if (roles.stream().anyMatch{ r -> r.roleName.equals(ERole.ROLE_ADMIN.name)}) {
            throw RuntimeException("Error: Cannot delete admin!")
        }
        userDAO.deleteById(id)
    }
}