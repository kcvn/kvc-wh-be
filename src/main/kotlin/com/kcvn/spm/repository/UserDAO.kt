package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.Users
import com.kcvn.spm.model.tables.references.USERS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class UserDAO(private val context: DSLContext) {
    fun findAll(): List<Users> = context.selectFrom(USERS).fetchInto(Users::class.java)

    fun findById(id: Long): Users? =
        context.selectFrom(USERS).where(USERS.USER_ID.eq(id)).fetchInto(Users::class.java).firstOrNull()

    fun findByUsername(userName: String): Users? =
        context.selectFrom(USERS).where(USERS.USERNAME.eq(userName)).fetchInto(Users::class.java).firstOrNull()

    fun findByUsernameContaining(userName: String): List<Users> =
        context.selectFrom(USERS).where(USERS.USERNAME.contains(userName)).fetchInto(Users::class.java)

    fun save(user: Users): Long? = context.insertInto(USERS, USERS.USERNAME, USERS.PASSWORD)
        .values(user.username, user.password)
        .returningResult(USERS.USER_ID)
        .fetchOne()?.value1()

    fun update(user: Users): Users? = context.update(USERS)
        .set(USERS.USERNAME, user.username)
        .set(USERS.PASSWORD, user.password)
        .where(USERS.USER_ID.eq(user.userId))
        .returningResult(USERS)
        .fetchInto(Users::class.java).firstOrNull()

    fun deleteById(id: Long) = context.deleteFrom(USERS).where(USERS.USER_ID.eq(id)).execute()
}