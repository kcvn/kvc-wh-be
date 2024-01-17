package com.kcvn.spm.app.auth.security.service

import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.RoleRepository
import com.kcvn.spm.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDetailsServiceImpl(
    private val userRep: UserRepository,
    private val roleRep: RoleRepository
) : UserDetailsService {
    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRep.findByUsername(username)
            ?: throw UsernameNotFoundException(CommonUtils.getMessage("login.error.wrongUsername"))
        val roles = roleRep.findByUserId(user.id!!)
        val permissions = roleRep.findPermissionsByRoleIds(roles.map { it.id!! })
        return UserDetailsImpl.build(user, roles, permissions)
    }
}