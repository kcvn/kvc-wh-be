package com.kcvn.spm.repository

import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.model.tables.pojos.CommonCategory
import com.kcvn.spm.model.tables.pojos.ProcessMasterData
import com.kcvn.spm.model.tables.references.COMMON_CATEGORY
import com.kcvn.spm.model.tables.references.PROCESS_MASTER_DATA
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

    fun getListProcessCodeDropDown(processCode: String?, type: String) : List<DropdownResponse>? {
        return context.select(
            PROCESS_MASTER_DATA.VALUE.`as`("value"),
            PROCESS_MASTER_DATA.VALUE.`as`("label")
        )
            .from(PROCESS_MASTER_DATA)
            .where(PROCESS_MASTER_DATA.PROCESS_CODE.eq(processCode).and(PROCESS_MASTER_DATA.TYPE.eq(type)))
            .fetchInto(DropdownResponse::class.java)
    }

    fun getProcessMasterData() : List<ProcessMasterData> {
        return context.selectFrom(PROCESS_MASTER_DATA)
            .where(PROCESS_MASTER_DATA.IS_DELETED.eq(false))
            .fetchInto(ProcessMasterData::class.java)
    }
}