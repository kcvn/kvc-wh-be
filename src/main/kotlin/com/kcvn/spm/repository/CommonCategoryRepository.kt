package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.CommonCategory
import com.kcvn.spm.model.tables.pojos.ProcessMasterData
import com.kcvn.spm.model.tables.references.COMMON_CATEGORY
import com.kcvn.spm.model.tables.references.PROCESS_MASTER_DATA
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class CommonCategoryRepository(private val context: DSLContext) {

    fun getByType(types: List<String>) : List<CommonCategory> {
        var condition: Condition = DSL.noCondition()
        if (types.isNotEmpty()) condition = condition.and(COMMON_CATEGORY.TYPE.`in`(types))
        condition = condition.and(COMMON_CATEGORY.IS_DELETED.eq(false))

        return context.selectFrom(COMMON_CATEGORY).where(condition).fetchInto(CommonCategory::class.java)
    }

    fun add(request: ProcessMasterData) {
        val record = context.newRecord(PROCESS_MASTER_DATA, request)
        context.insertInto(PROCESS_MASTER_DATA).set(record).execute()
    }
}