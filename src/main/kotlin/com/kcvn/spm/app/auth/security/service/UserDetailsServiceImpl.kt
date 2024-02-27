package com.kcvn.spm.app.auth.security.service

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.enums.EUserStatus
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
            ?: throw UsernameNotFoundException(CommonUtils.getMessage("login.error"))
        if (user.status == EUserStatus.INACTIVE.value) throw UsernameNotFoundException(CommonUtils.getMessage("login.error"))
        val position = userRep.getUserClaim(user.id!!, Constants.CLAIM_TYPE_POSITION.lowercase()).firstOrNull()
        val roles = roleRep.findByUserId(user.id!!)
        val permissions = roleRep.findPermissionsByRoleIds(roles.map { it.id!! })
        return UserDetailsImpl.build(user, roles, permissions, position?.claimValue)
    }
}