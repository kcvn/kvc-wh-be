package com.kcvn.spm.auth.security.service

import com.kcvn.spm.model.tables.pojos.AuthRole
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.auth.security.EPermission
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserDetailsImpl(
    private val id: String,
    private val username: String,
    private val password: String,
    private val authorities: Collection<GrantedAuthority>,
    private val isDeleted: Boolean
) : UserDetails {
    companion object {
        private const val serialVersionUID = 1L

        fun build(user: AuthUser, roles: List<AuthRole>, permissions: List<EPermission>): UserDetailsImpl {
            val authorities = mutableListOf<GrantedAuthority>()
            if (user.isSuperAdmin!!) authorities.add(SimpleGrantedAuthority("ROLE_ADMIN"))
            authorities.addAll(roles.map { role -> SimpleGrantedAuthority(role.name) })
            authorities.addAll(permissions.map { SimpleGrantedAuthority(it.value) })
            return UserDetailsImpl(
                user.id!!,
                user.username.toString(),
                user.password.toString(),
                authorities,
                user.isDeleted!!
            )
        }
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return authorities
    }

    fun getId(): String {
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
        return !isDeleted
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val user = other as UserDetailsImpl
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