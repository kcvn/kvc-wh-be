package com.kcvn.spm.security.service

import com.kcvn.spm.model.tables.pojos.Permissions
import com.kcvn.spm.model.tables.pojos.Roles
import com.kcvn.spm.model.tables.pojos.Users
import com.kcvn.spm.security.EPermission
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserDetailsImpl(
    private val id: Long,
    private val username: String,
    private val password: String,
    private val authorities: Collection<out GrantedAuthority>
) : UserDetails {
    companion object {
        private const val serialVersionUID = 1L

        fun build(user: Users, roles: List<Roles>, permissions: List<Permissions>): UserDetailsImpl {
            val authorities = mutableListOf<GrantedAuthority>()
            authorities.addAll(roles.map { role -> SimpleGrantedAuthority(role.roleName) })
            authorities.addAll(permissions.map { SimpleGrantedAuthority(EPermission.valueOf(it.permissionName!!).value) })
            return UserDetailsImpl(
                user.userId?: 0L,
                user.username.toString(),
                user.password.toString(),
                authorities
            )
        }
    }

    override fun getAuthorities(): Collection<out GrantedAuthority> {
        return authorities
    }

    fun getId(): Long {
        return id
    }

    override fun getPassword(): String {
        return password
    }

    override fun getUsername(): String {
        return username
    }

    override fun isAccountNonExpired(): Boolean {
        return true
    }

    override fun isAccountNonLocked(): Boolean {
        return true
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return true
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val user = o as UserDetailsImpl
        return id == user.id
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + password.hashCode()
        result = 31 * result + authorities.hashCode()
        return result
    }
}