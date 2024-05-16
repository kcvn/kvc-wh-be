package com.kcvn.spm.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.MessageSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.LocaleContextResolver
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import org.springframework.web.servlet.support.RequestContextUtils
import java.util.*


@Configuration
class LocaleResourcesConfig : WebMvcConfigurer {
    @Bean
    fun localeResolver(): LocaleResolver {
        val localeResolver = CookieLocaleResolver()
        localeResolver.setDefaultLocale(Locale.forLanguageTag("vi"))
        return localeResolver
    }

//    @Bean
//    fun localeChangeInterceptor(): LocaleChangeInterceptor {
//        val localeChangeInterceptor = LocaleChangeInterceptor()
//        localeChangeInterceptor.paramName = "lang"
//        return localeChangeInterceptor
//    }

    @Bean
    fun localeChangeInterceptor(): HandlerInterceptor {
        return object : HandlerInterceptor {
            override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
                var acceptLanguageHeader = request.getHeader("Accept-Language")
                if (acceptLanguageHeader.isNullOrEmpty()) acceptLanguageHeader = "vi"
                val newLocale = Locale.forLanguageTag(acceptLanguageHeader)
                (RequestContextUtils.getLocaleResolver(request) as? LocaleContextResolver)?.setLocale(request, response, newLocale)
                return true
            }
        }
    }

    @Bean
    fun messageSource(): MessageSource {
        val messageSource = ResourceBundleMessageSource()
        messageSource.setBasenames("messages")
        messageSource.setDefaultEncoding("UTF-8")
        return messageSource
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(localeChangeInterceptor())
    }
}