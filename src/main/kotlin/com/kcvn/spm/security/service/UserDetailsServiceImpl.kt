package com.kcvn.spm.security.service

import com.kcvn.spm.repository.PermissionDAO
import com.kcvn.spm.repository.RoleDAO
import com.kcvn.spm.repository.UserDAO
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDetailsServiceImpl(
    private val userDAO: UserDAO,
    private val roleDAO: RoleDAO,
    private val permissionDAO: PermissionDAO
) : UserDetailsService {
    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userDAO.findByUsername(username)
            ?: throw UsernameNotFoundException("User Not Found with username: $username")
        val roles = roleDAO.findByUserId(user.userId!!)
        val permissions = permissionDAO.findByRoleIds(roles.map { it.roleId!! })
        return UserDetailsImpl.build(user, roles, permissions)
    }
}