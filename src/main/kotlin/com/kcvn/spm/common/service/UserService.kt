package com.kcvn.spm.common.service

import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.common.payload.request.UserRequest
import com.kcvn.spm.common.payload.response.UserResponse
import com.kcvn.spm.common.repository.RoleDAO
import com.kcvn.spm.common.repository.UserDAO
import com.kcvn.spm.common.CommonUtils
import com.kcvn.spm.common.payload.response.PaginatedResponse
import com.kcvn.spm.common.security.EPermission
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserService(
    private val userDAO: UserDAO,
    private val roleDAO: RoleDAO,
    private val encoder: PasswordEncoder
) {
    fun getUsers(uName: String?): List<UserResponse> {
        val userList = if (uName == null) userDAO.findAll()
        else userDAO.findByUsernameContaining(uName)

        return userList.map { user ->
            UserResponse(
                user.id!!,
                user.username!!,
                user.email,
                user.phoneNumber,
                user.fullName,
                user.dateOfBirth,
                user.avatar,
                user.isSuperAdmin!!
            )
        }
    }

    fun getPaginatedUsers(uName: String?, page: Int, size: Int): PaginatedResponse {
        val result = if (uName == null) userDAO.findAllPaginated(page, size)
        else userDAO.findByUsernameContainingPaginated(uName, page, size)

        return PaginatedResponse(
            result.first.map { user ->
                UserResponse(
                    user.id!!,
                    user.username!!,
                    user.email,
                    user.phoneNumber,
                    user.fullName,
                    user.dateOfBirth,
                    user.avatar,
                    user.isSuperAdmin!!
                )
            },
            result.second
        )
    }

    fun findById(id: String): UserResponse? {
        val user = userDAO.findById(id)
        return if (user == null) null
        else {
            val roles = roleDAO.findByUserId(user.id!!)
            UserResponse(
                user.id!!,
                user.username!!,
                user.email,
                user.phoneNumber,
                user.fullName,
                user.dateOfBirth,
                user.avatar,
                user.isSuperAdmin!!,
                roles.map { it.id!! },
                roleDAO.findPermissionsByRoleIds(roles.map { it.id!! }).map { p -> EPermission.valueOf(p.name!!).value }
            )
        }
    }

    fun createUser(request: UserRequest): UserResponse? {
        if (userDAO.findByUsername(request.username!!) != null) {
            throw RuntimeException("Error: Username is already taken!")
        }

        // Create new user's account
        val user = AuthUser(
            null,
            request.username,
            encoder.encode(request.password),
            request.email,
            request.phoneNumber,
            request.fullName,
            CommonUtils.removeAccent(request.fullName),
            request.dateOfBirth,
            request.avatar
        )
        val roleIds: Set<String> = request.roleIds ?: setOf()
        val allRoles = roleDAO.findAll()
        roleIds.forEach { roleId: String ->
            allRoles.find { it.id.equals(roleId) }
                ?: throw RuntimeException("Error: Role $roleId is not found.")
        }
        val createdUser = userDAO.save(user)
        return if (createdUser != null) {
            roleDAO.saveUserRoles(createdUser.id!!, roleIds)
            UserResponse(
                createdUser.id!!,
                createdUser.username!!,
                isAdmin = createdUser.isSuperAdmin!!
            )
        } else null
    }

    fun updateInfo(userId: String, request: UserRequest): UserResponse? {
        var user = userDAO.findById(userId)
        return if (user == null) {
            throw RuntimeException("Error: User is not found.")
        } else {
            user.email = request.email
            user.phoneNumber = request.phoneNumber
            user.fullName = request.fullName
            user.fullNameUnsigned = CommonUtils.removeAccent(request.fullName)
            user.dateOfBirth = request.dateOfBirth
            user.avatar = request.avatar

            val roleIds: Set<String> = request.roleIds ?: setOf()
            val allRoles = roleDAO.findAll()
            roleIds.forEach { roleId: String ->
                allRoles.find { it.id.equals(roleId) }
                    ?: throw RuntimeException("Error: Role $roleId is not found.")
            }
            roleDAO.saveUserRoles(userId, roleIds)

            user = userDAO.updateInfo(user)
            UserResponse(
                user?.id!!,
                user.username!!,
                isAdmin = user.isSuperAdmin!!
            )
        }
    }

    fun updatePassword(userId: String, request: UserRequest): UserResponse? {
        var user = userDAO.findById(userId)
        return if (user == null) {
            throw RuntimeException("Error: User is not found.")
        } else {
            user.password = encoder.encode(request.password)
            user = userDAO.updatePassword(user)
            UserResponse(
                user?.id!!,
                user.username!!,
                isAdmin = user.isSuperAdmin!!
            )
        }
    }

    fun deleteById(id: String) {
        userDAO.deleteById(id)
    }
}