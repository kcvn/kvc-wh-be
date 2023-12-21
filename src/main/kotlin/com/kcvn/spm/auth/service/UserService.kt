package com.kcvn.spm.auth.service

import com.kcvn.spm.auth.EUserStatus
import com.kcvn.spm.auth.payload.request.UserRequest
import com.kcvn.spm.auth.payload.response.UserResponse
import com.kcvn.spm.auth.security.EPermission
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthPasswordResetToken
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.repository.PasswordResetTokenDAO
import com.kcvn.spm.repository.RoleDAO
import com.kcvn.spm.repository.UserDAO
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
    private val userDAO: UserDAO,
    private val roleDAO: RoleDAO,
    private val encoder: PasswordEncoder,
    private val passwordResetTokenDAO: PasswordResetTokenDAO,
    private val mailSender: JavaMailSender,
    private val env: Environment
) {
//    fun getUsers(search: String?): List<UserResponse> {
//        val userList = if (search == null) userDAO.findAll()
//        else userDAO.findByKeyword(search)
//        val positionsByUser = userDAO.findPositions(userList.map { it.id!! })
//        return userList.map { user ->
//            UserResponse(
//                user.id!!,
//                user.username!!,
//                user.employeeCode,
//                user.email,
//                user.phoneNumber,
//                user.fullName,
//                user.dateOfBirth,
//                user.avatar,
//                user.status,
//                user.isSuperAdmin!!,
//                positionsByUser.getOrDefault(user.id!!, listOf())
//            )
//        }
//    }

    fun getPaginatedUsers(search: String?, pageable: Pageable): PaginatedResponse {
        val result = if (search == null) userDAO.findAllPaginated(pageable)
        else userDAO.findByKeywordPaginated(search, pageable)
        val positionsByUser = userDAO.findPositions(result.first.map { it.id!! })
        return PaginatedResponse(
            result.first.map { user ->
                UserResponse(
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

    fun findById(id: String): UserResponse? {
        val user = userDAO.findById(id)
        return if (user == null) null
        else {
            val roles = roleDAO.findByUserId(user.id!!)
            val positionsByUser = userDAO.findPositions(listOf(user.id!!))
            UserResponse(
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
                roleDAO.findPermissionsByRoleIds(roles.map { it.id!! }).map { p -> EPermission.valueOf(p.name).value }
            )
        }
    }

    fun createUser(request: UserRequest): UserResponse? {
        if (userDAO.findByUsername(request.username!!) != null) {
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
        val allRoles = roleDAO.findAll()
        roleIds.forEach { roleId: String ->
            allRoles.find { it.id.equals(roleId) }
                ?: throw BusinessException(CommonUtils.getMessage("role.error.notFound"))
        }
        val createdUser = userDAO.save(user)
        return if (createdUser != null) {
            roleDAO.saveUserRoles(createdUser.id!!, roleIds)
            userDAO.savePositions(createdUser.id!!, request.positions ?: setOf())
            UserResponse(
                createdUser.id!!,
                createdUser.username!!,
                isAdmin = createdUser.isSuperAdmin!!
            )
        } else null
    }

    fun updateInfo(userId: String, request: UserRequest): UserResponse {
        val user = userDAO.findById(userId) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        user.employeeCode = request.employeeCode
        user.email = request.email
        user.phoneNumber = request.phoneNumber
        user.fullName = request.fullName
        user.fullNameUnsigned = CommonUtils.removeAccent(request.fullName)
        user.dateOfBirth = request.dateOfBirth
        user.avatar = request.avatar
        user.status = request.status

        val roleIds: Set<String> = request.roleIds ?: setOf()
        val allRoles = roleDAO.findAll()
        roleIds.forEach { roleId: String ->
            allRoles.find { it.id.equals(roleId) }
                ?: throw BusinessException(CommonUtils.getMessage("role.error.notFound"))
        }
        roleDAO.saveUserRoles(userId, roleIds)
        userDAO.savePositions(userId, request.positions ?: setOf())
        userDAO.updateInfo(user)
        return UserResponse(
            user.id!!,
            user.username!!,
            isAdmin = user.isSuperAdmin!!
        )
    }

    fun validateOldPassword(userId: String, oldPassword: String): Boolean {
        val user = userDAO.findById(userId) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        return encoder.matches(oldPassword, user.password)
    }

    fun updatePassword(userId: String, password: String): UserResponse {
        val user = userDAO.findById(userId) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        user.password = encoder.encode(password)
        userDAO.updatePassword(user)
        return UserResponse(
            user.id!!,
            user.username!!,
            isAdmin = user.isSuperAdmin!!
        )
    }

    fun deleteById(id: String) {
        userDAO.deleteById(id)
    }

    fun createPasswordResetToken(email: String, siteUrl: String) {
        val user = userDAO.findByEmail(email) ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
        val token: String = UUID.randomUUID().toString()
        passwordResetTokenDAO.save(
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
        val passToken = passwordResetTokenDAO.findByToken(token)
            ?: throw BusinessException(CommonUtils.getMessage("login.resetPassword.error.invalidToken"))
        if (passToken.expiredDate!!.isBefore(OffsetDateTime.now()))
            throw BusinessException(CommonUtils.getMessage("login.resetPassword.error.tokenExpired"))
    }

    fun getUserByPasswordResetToken(token: String): AuthUser {
        val passToken = passwordResetTokenDAO.findByToken(token)
            ?: throw BusinessException(CommonUtils.getMessage("login.resetPassword.error.invalidToken"))
        return userDAO.findById(passToken.userId!!)
            ?: throw BusinessException(CommonUtils.getMessage("user.error.notFound"))
    }
}