package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.AuthPasswordResetToken
import com.kcvn.spm.model.tables.references.AUTH_PASSWORD_RESET_TOKEN
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class PasswordResetTokenDAO(private val context: DSLContext) {
    fun save(resetToken: AuthPasswordResetToken) {
        context.update(AUTH_PASSWORD_RESET_TOKEN)
            .set(AUTH_PASSWORD_RESET_TOKEN.EXPIRED_DATE, OffsetDateTime.now())
            .set(AUTH_PASSWORD_RESET_TOKEN.UPDATED_BY, "SYSTEM")
            .where(AUTH_PASSWORD_RESET_TOKEN.USER_ID.eq(resetToken.userId))
            .execute()

        context.insertInto(AUTH_PASSWORD_RESET_TOKEN,
            AUTH_PASSWORD_RESET_TOKEN.USER_ID,
            AUTH_PASSWORD_RESET_TOKEN.TOKEN,
            AUTH_PASSWORD_RESET_TOKEN.EXPIRED_DATE,
            AUTH_PASSWORD_RESET_TOKEN.CREATED_BY)
            .values(resetToken.userId, resetToken.token, resetToken.expiredDate, "SYSTEM")
            .execute()
    }

    fun findByToken(token: String): AuthPasswordResetToken? =
        context.selectFrom(AUTH_PASSWORD_RESET_TOKEN).where(AUTH_PASSWORD_RESET_TOKEN.TOKEN.eq(token))
            .fetchInto(AuthPasswordResetToken::class.java).firstOrNull()
}