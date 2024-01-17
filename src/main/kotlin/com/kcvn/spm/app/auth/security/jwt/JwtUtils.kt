package com.kcvn.spm.app.auth.security.jwt

import com.kcvn.spm.app.auth.security.service.UserDetailsImpl
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class JwtUtils {
    private val logger = LoggerFactory.getLogger(JwtUtils::class.java)

    @Value("\${kcvn.spm.jwtSecret}")
    private lateinit var jwtSecret: String

    @Value("\${kcvn.spm.jwtExpirationMs}")
    private var jwtExpirationMs: Int = 0

    fun generateJwtToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetailsImpl
        return Jwts.builder()
            .setSubject(userPrincipal.username)
            .setIssuedAt(Date())
            .setExpiration(Date(Date().time + jwtExpirationMs.toLong()))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact()
    }

    private fun key(): Key {
        return Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(jwtSecret))
    }

    fun getUserNameFromJwtToken(token: String): String {
        return Jwts.parserBuilder().setSigningKey(key()).build()
            .parseClaimsJws(token).body.subject
    }

    fun validateJwtToken(authToken: String): Boolean {
        try {
            Jwts.parserBuilder().setSigningKey(key()).build().parse(authToken)
            return true
        } catch (e: MalformedJwtException) {
            logger.error("Invalid JWT token: {}", e.message)
        } catch (e: ExpiredJwtException) {
            logger.error("JWT token is expired: {}", e.message)
        } catch (e: UnsupportedJwtException) {
            logger.error("JWT token is unsupported: {}", e.message)
        } catch (e: IllegalArgumentException) {
            logger.error("JWT claims string is empty: {}", e.message)
        }
        return false
    }
}