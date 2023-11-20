package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.Functions
import com.kcvn.spm.model.tables.references.FUNCTIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class FunctionDAO(private val context: DSLContext) {
    fun findAll() = context.selectFrom(FUNCTIONS).fetchInto(Functions::class.java)

    fun findById(id: Int): Functions? =
        context.selectFrom(FUNCTIONS).where(FUNCTIONS.FUNCTION_ID.eq(id))
            .fetchInto(Functions::class.java).firstOrNull()
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