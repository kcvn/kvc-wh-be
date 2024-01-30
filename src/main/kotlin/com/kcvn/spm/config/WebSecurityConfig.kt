package com.kcvn.spm.config

import com.kcvn.spm.app.auth.security.jwt.AuthEntryPointJwt
import com.kcvn.spm.app.auth.security.jwt.AuthTokenFilter
import com.kcvn.spm.app.auth.security.jwt.JwtUtils
import com.kcvn.spm.app.auth.security.service.UserDetailsServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.web.config.EnableSpringDataWebSupport
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
@EnableSpringDataWebSupport
class WebSecurityConfig(
    private val userDetailsService: UserDetailsServiceImpl,
    private val unauthorizedHandler: AuthEntryPointJwt,
    private val jwtUtils: JwtUtils
) {
    @Bean
    fun authenticationJwtTokenFilter(): AuthTokenFilter {
        return AuthTokenFilter(jwtUtils, userDetailsService)
    }

    @Bean
    fun authenticationProvider(): DaoAuthenticationProvider {
        val authProvider = DaoAuthenticationProvider()
        authProvider.setUserDetailsService(userDetailsService)
        authProvider.setPasswordEncoder(passwordEncoder())
//        authProvider.isHideUserNotFoundExceptions = false
        return authProvider
    }

    @Bean
    @Throws(Exception::class)
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @Throws(Exception::class)
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http.cors { } // Enable CORS
            // Disable CSRF
            .csrf { csrf: CsrfConfigurer<HttpSecurity> -> csrf.disable() }
            // Set unauthorized requests exception handler
            .exceptionHandling { exception: ExceptionHandlingConfigurer<HttpSecurity?> ->
                exception.authenticationEntryPoint(
                    unauthorizedHandler
                )
            }
            // Set session management to stateless
            .sessionManagement { session: SessionManagementConfigurer<HttpSecurity?> ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            }
            // Set permissions on endpoints
            .authorizeHttpRequests { auth ->
                // ROLE_ is automatically prepended when using hasRole
                auth.requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers("/api/user/**").authenticated()
                    .requestMatchers("/api/role/**").authenticated()
                    .requestMatchers("/api/product-process/**").authenticated()
                    .anyRequest().permitAll()
            }
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowCredentials = false
        config.maxAge = 60 * 60
        config.addAllowedOrigin(CorsConfiguration.ALL)
        config.addAllowedHeader(CorsConfiguration.ALL)
        config.addAllowedMethod(CorsConfiguration.ALL)
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

}