package com.kcvn.spm.security.service

import com.kcvn.spm.service.RoleService
import com.kcvn.spm.service.UserService
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserDetailsServiceImpl(private val userService: UserService, private val roleService: RoleService) : UserDetailsService {
    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userService.findByUsername(username)
            ?: throw UsernameNotFoundException("User Not Found with username: $username")
        val roles = roleService.findByUser(userId = user.userId ?: 0L)
        return UserDetailsImpl.build(user, roles)
    }
}