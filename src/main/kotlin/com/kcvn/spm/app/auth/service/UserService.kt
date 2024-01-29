package com.kcvn.spm.app.auth.service

import com.kcvn.spm.common.enums.EPermission
import com.kcvn.spm.common.enums.EUserStatus
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthPasswordResetToken
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.repository.PasswordResetTokenRepository
import com.kcvn.spm.repository.RoleRepository
import com.kcvn.spm.repository.UserRepository
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.*

@Service
@Transactional
class UserService(
    private val userRep: UserRepository,
    private val roleRep: RoleRepository,
    private val encoder: PasswordEncoder,
    private val passwordResetTokenRep: PasswordResetTokenRepository,
    private val mailSender: JavaMailSender,
    private val env: Environment
) {
    fun getPaginatedUsers(search: String?, pageable: Pageable): PaginatedResponse {
        val result = UserRepository.findByKeywordPaginated(userRep, search, pageable)
        val positionsByUser = userRep.findPositions(result.first.map { it.id!! })
        return PaginatedResponse(
            result.first.map { user ->
                com.kcvn.spm.app.auth.payload.response.UserResponse(
                    user.id!!,
                    user.username!!,
                    user.employeeCode,
                    user.email,
                    user.phoneNumber,
                    user.fullName,
                    user.dateOfBirth,
                    user.avatar,
                    user.status,
                    user.isSuperAdmin!!,
                    positionsByUser.getOrDefault(user.id!!, listOf())
                )
            },
            result.second
        )
    }

    fun findById(id: String): com.kcvn.spm.app.auth.payload.response.UserResponse? {
        val user = userRep.findById(id)
        return if (user == null) null
        else {
            val roles = roleRep.findByUserId(user.id!!)
            val positionsByUser = userRep.findPositions(listOf(user.id!!))
            com.kcvn.spm.app.auth.payload.response.UserResponse(
                user.id!!,
                user.username!!,
                user.employeeCode,
                user.email,
                user.phoneNumber,
                user.fullName,
                user.dateOfBirth,
                user.avatar,
                user.status,
                user.isSuperAdmin!!,
                positionsByUser.getOrDefault(user.id!!, listOf()),
                roles.map { it.id!! },
                roleRep.findPermissionsByRoleIds(roles.map { it.id!! }).map { p -> EPermission.valueOf(p.name).value }
            )
        }
    }

    fun createUser(request: com.kcvn.spm.app.auth.payload.request.UserRequest): com.kcvn.spm.app.auth.payload.response.UserResponse? {
        if (userRep.findByUsername(request.username!!) != null) {
            throw BusinessException(CommonUtils.getMessage("user.error.usernameTaken"))
        }

        // Create new user's account
        val user = AuthUser(
            null,
            request.username,
            encoder.encode(request.password),
            request.employeeCode,
            request.email,
            request.phoneNumber,
            request.fullName,
            CommonUtils.removeAccent(request.fullName),
            request.dateOfBirth,
            request.avatar,
            if (request.status == null || request.status == EUserStatus.ACTIVE.value) EUserStatus.ACTIVE.value else EUserStatus.INACTIVE.value
        )
        val roleIds: Set<String> = request.roleIds ?: setOf()
        val allRoles = roleRep.findAll()
        roleIds.forEach { roleId: String ->
            allRoles.find { it.id.equals(roleId) }
                ?: throw BusinessException(CommonUtils.getMessage("role.error.notFound"))
        }
        val createdUser = userRep.save(user)
        return if (createdUser != null) {
            roleRep.saveUserRoles(createdUser.id!!, roleIds)
            userRep.savePositions(createdUser.id!!, request.positions ?: setOf())
            com.kcvn.spm.app.auth.payload.response.UserResponse(
                createdUser.id!!,
                createdUser.username!!,
                isAdmin = createdUser.isSuperAdmin!!
            )
        } else null
    }

    fun updateInfo(userId: String, request: com.kcvn.spm.app.auth.payload.request.UserRequest): com.kcvn.spm.app.auth.payload.response.UserResponse {
        val user = userRep.findById(userId) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        user.employeeCode = request.employeeCode
        user.email = request.email
        user.phoneNumber = request.phoneNumber
        user.fullName = request.fullName
        user.fullNameUnsigned = CommonUtils.removeAccent(request.fullName)
        user.dateOfBirth = request.dateOfBirth
        user.avatar = request.avatar
        user.status = request.status

        val roleIds: Set<String> = request.roleIds ?: setOf()
        val allRoles = roleRep.findAll()
        roleIds.forEach { roleId: String ->
            allRoles.find { it.id.equals(roleId) }
                ?: throw BusinessException(CommonUtils.getMessage("role.error.notFound"))
        }
        roleRep.saveUserRoles(userId, roleIds)
        userRep.savePositions(userId, request.positions ?: setOf())
        userRep.updateInfo(user)
        return com.kcvn.spm.app.auth.payload.response.UserResponse(
            user.id!!,
            user.username!!,
            isAdmin = user.isSuperAdmin!!
        )
    }

    fun validateOldPassword(userId: String, oldPassword: String): Boolean {
        val user = userRep.findById(userId) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        return encoder.matches(oldPassword, user.password)
    }

    fun updatePassword(userId: String, password: String): com.kcvn.spm.app.auth.payload.response.UserResponse {
        val user = userRep.findById(userId) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        user.password = encoder.encode(password)
        userRep.updatePassword(user)
        return com.kcvn.spm.app.auth.payload.response.UserResponse(
            user.id!!,
            user.username!!,
            isAdmin = user.isSuperAdmin!!
        )
    }

    fun deleteById(id: String) {
        userRep.deleteById(id)
    }

    fun createPasswordResetToken(email: String, siteUrl: String) {
        val user = userRep.findByEmail(email) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        val token: String = UUID.randomUUID().toString()
        passwordResetTokenRep.save(
            AuthPasswordResetToken(
                null,
                user.id,
                token,
                OffsetDateTime.now().plusMinutes(60L)
            )
        )
        val resetPasswordLink = "$siteUrl?token=$token"
//        sendEmail(email, resetPasswordLink)
        mailSender.send(constructResetTokenEmail(resetPasswordLink, email))
    }

//    @Throws(MessagingException::class, UnsupportedEncodingException::class)
//    private fun sendEmail(recipientEmail: String?, link: String) {
//        val message: MimeMessage = mailSender.createMimeMessage()
//        val helper = MimeMessageHelper(message)
//        helper.setFrom(env.getProperty("support.email")?: "", "KCVN Support")
//        helper.setTo(recipientEmail!!)
//        val subject = messageSource.getMessage("mail.resetPassword.subject", null, LocaleContextHolder.getLocale())
//        val content = messageSource.getMessage("mail.resetPassword.content", arrayOf<Any>(link), LocaleContextHolder.getLocale())
//        helper.setSubject(subject)
//        helper.setText(content, true)
//        mailSender.send(message)
//    }

    private fun constructResetTokenEmail(resetPasswordLink: String, userEmail: String): SimpleMailMessage {
        val message: String = CommonUtils.getMessage("message.resetPassword")
        return constructEmail(message, "$message \r\n$resetPasswordLink", userEmail)
    }

    private fun constructEmail(
        subject: String, body: String,
        userMail: String
    ): SimpleMailMessage {
        val email = SimpleMailMessage()
        email.subject = subject
        email.text = body
        email.setTo(userMail)
        email.from = env.getProperty("support.email")
        return email
    }

    fun validatePasswordResetToken(token: String) {
        val passToken = passwordResetTokenRep.findByToken(token)
            ?: throw BusinessException(CommonUtils.getMessage("login.resetPassword.error.invalidToken"))
        if (passToken.expiredDate!!.isBefore(OffsetDateTime.now()))
            throw BusinessException(CommonUtils.getMessage("login.resetPassword.error.tokenExpired"))
    }

    fun getUserByPasswordResetToken(token: String): AuthUser {
        val passToken = passwordResetTokenRep.findByToken(token)
            ?: throw BusinessException(CommonUtils.getMessage("login.resetPassword.error.invalidToken"))
        return userRep.findById(passToken.userId!!)
            ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
    }
}