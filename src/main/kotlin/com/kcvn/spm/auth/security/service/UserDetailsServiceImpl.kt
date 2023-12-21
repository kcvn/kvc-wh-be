package com.kcvn.spm.auth.security.service

import com.kcvn.spm.common.util.CommonUtils
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
    private val roleDAO: RoleDAO
) : UserDetailsService {
    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userDAO.findByUsername(username)
            ?: throw UsernameNotFoundException(CommonUtils.getMessage("login.error.wrongUsername"))
        val roles = roleDAO.findByUserId(user.id!!)
        val permissions = roleDAO.findPermissionsByRoleIds(roles.map { it.id!! })
        return UserDetailsImpl.build(user, roles, permissions)
    }
}