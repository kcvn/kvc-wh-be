package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Functions
import com.kcvn.spm.model.tables.pojos.Users
import com.kcvn.spm.model.tables.references.FUNCTIONS
import com.kcvn.spm.model.tables.references.USERS
import org.jooq.DSLContext

class FunctionService (private val context: DSLContext){
    fun findAll() = context.selectFrom(FUNCTIONS).fetchInto(Users::class.java)

    fun findById(id: Int): Functions? =
        context.selectFrom(FUNCTIONS).where(FUNCTIONS.FUNCTION_ID.eq(id))
            .fetchInto(Functions::class.java).firstOrNull()

    fun findByFunctionName(functionName: String): Functions? =
        context.selectFrom(FUNCTIONS).where(FUNCTIONS.FUNCTION_NAME.eq(functionName)).fetchInto(Functions::class.java).firstOrNull()

    fun findByFunctionNameContaining(functionName: String): List<Functions> =
        context.selectFrom(FUNCTIONS).where(FUNCTIONS.FUNCTION_NAME.contains(functionName)).fetchInto(Functions::class.java)

    fun existsByFunctionName(functionName: String): Boolean = findByFunctionName(functionName) != null

    fun save(functions: Functions) = context.insertInto(FUNCTIONS, FUNCTIONS.FUNCTION_NAME)
        .values(functions.functionName)
        .returningResult(FUNCTIONS.FUNCTION_ID)
        .fetchOne()?.value1()

    fun update(functions: Functions) = context.update(FUNCTIONS)
        .set(FUNCTIONS.FUNCTION_NAME,functions.functionName)
        .returningResult(FUNCTIONS)
        .fetchInto(Functions::class.java).firstOrNull()

    fun deleteById(id: Int) = context.deleteFrom(FUNCTIONS).where(FUNCTIONS.FUNCTION_ID.eq(id)).execute()
}