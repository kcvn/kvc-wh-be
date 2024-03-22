package com.kcvn.spm.repository

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.ProcessGroupResponse
import com.kcvn.spm.app.workresult.payload.response.ProcessResponse
import com.kcvn.spm.common.constants.ProcessCode
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.WORK_RESULT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.DSL.length
import org.jooq.impl.DSL.lower
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.util.*

@Repository
class WorkResultRepository(
    private val context: DSLContext,
) : SortingRepository() {
    fun getPagingListWorkResult(request: WorkResultSearchRequest?, pageable: Pageable): Pair<List<WorkResult>, Int> {
        var condition: Condition = DSL.noCondition()
        if (request != null) {
            if (!request.order.isNullOrEmpty())
                condition = condition.and(lower(WORK_RESULT.ORDER_CODE).contains(request.order!!.lowercase(Locale.getDefault())))

            if (!request.itemName.isNullOrEmpty())
                condition = condition.and(lower(WORK_RESULT.ITEM_NAME).contains(request.itemName!!.lowercase(Locale.getDefault())))

            if (!request.listProcessGroup.isNullOrEmpty()) {
                val processGroupCodes = request.listProcessGroup!!.split(",")
                var condition1: Condition = DSL.noCondition()
                processGroupCodes.forEach { processGroup ->
                    condition1 = condition1.or(WORK_RESULT.PROCESS_GRP.eq(processGroup))
                }
                condition = condition.and(condition1)
            }

            if (!request.listProcessCode.isNullOrEmpty()) {
                val processCodes = request.listProcessCode!!.split(",")
                var condition2: Condition = DSL.noCondition()
                processCodes.forEach { processCode ->
                    condition2 = condition2.or(WORK_RESULT.PROCESS_CODE.eq(processCode))
                }
                condition = condition.and(condition2)
            }

            if (!request.tapeLot.isNullOrEmpty())
                condition = condition.and(lower(WORK_RESULT.TAPE_LOT_NO).contains(request.tapeLot!!.lowercase(Locale.getDefault())))

            if (!request.code.isNullOrEmpty())
                condition = condition.and(lower( WORK_RESULT.CODE).contains(request.code!!.lowercase(Locale.getDefault())))

            if (request.fromDate != null && request.toDate != null)
                condition = condition.and(WORK_RESULT.SUMMARY_RESULT_DATE.between(request.fromDate, request.toDate))
        }

        val data = context.selectFrom(WORK_RESULT)
            .where(condition.and(WORK_RESULT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, WORK_RESULT.SUMMARY_RESULT_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(WorkResult::class.java)

        val total = context.fetchCount(WORK_RESULT, condition.and(WORK_RESULT.IS_DELETED.eq(false)))

        return Pair(data, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "summaryresultdate" -> {
                WORK_RESULT.SUMMARY_RESULT_DATE
            }

            "itemname" -> {
                WORK_RESULT.ITEM_NAME
            }

            "processname" -> {
                WORK_RESULT.PROCESS_NAME
            }

            "processcode" -> {
                WORK_RESULT.PROCESS_CODE
            }

            "layercode" -> {
                WORK_RESULT.LAYER_CODE
            }

            "totaltapequantity" -> {
                WORK_RESULT.TOTAL_TAPE_QUANTITY
            }

            "totalsheetquantity" -> {
                WORK_RESULT.TOTAL_SHEET_QUANTITY
            }

            "goodtapequantity" -> {
                WORK_RESULT.GOOD_TAPE_QUANTITY
            }

            "goodsheetquantity" -> {
                WORK_RESULT.GOOD_SHEET_QUANTITY
            }

            "ordercode" -> {
                WORK_RESULT.ORDER_CODE
            }

            "tapelotno" -> {
                WORK_RESULT.TAPE_LOT_NO
            }

            "code" -> {
                WORK_RESULT.CODE
            }

            "workimplementby" -> {
                WORK_RESULT.WORK_IMPLEMENT_BY
            }

            "equipmentname" -> {
                WORK_RESULT.EQUIPMENT_NAME
            }

            else -> {
                val errorMessage = CommonUtils.getMessage("sort.error.columnNotFound")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }

        return sortField
    }

    fun getListProcessGroup(): List<ProcessGroupResponse> {
        val listProcessGroup = context.select()
            .from(PROCESS_PROCEDURE_STRUCTURE)
            .join(PROCESS_MASTER)
            .on(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))
            .where(length(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE).eq(12))
            .orderBy(PROCESS_MASTER.PROCESS_NAME.asc())
            .fetchInto(ProcessGroupResponse::class.java)

        val x = listProcessGroup.distinctBy { x -> x.grpProcess }

        return x
    }

    fun getListProcessByGroupCode(groupCodes: String): List<ProcessResponse> {
        val response: MutableList<ProcessResponse> = mutableListOf()
        val groupCode = groupCodes.split(",")
        groupCode.forEach { code ->
            run {
                val listProcess = context.select()
                    .from(PROCESS_MASTER)
                    .join(PROCESS_PROCEDURE_STRUCTURE)
                    .on(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))
                    .where(
                        length(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE).eq(12)
                            .and(PROCESS_MASTER.GRP_PROCESS.eq(code))
                    )
                    .orderBy(PROCESS_MASTER.PROCESS_NAME)
                    .fetchInto(ProcessResponse::class.java)
                val result = listProcess.distinctBy { x -> x.processCode }
                response.addAll(result)
            }
        }

        return response
    }

    fun getList(request: WorkResultSearchRequest?, pageable: Pageable): List<WorkResult> {
        var condition: Condition = DSL.noCondition()
        if (request != null) {
            if (!request.order.isNullOrEmpty())
                condition = condition.and(lower(WORK_RESULT.ORDER_CODE).contains(request.order!!.lowercase(Locale.getDefault())))

            if (!request.itemName.isNullOrEmpty())
                condition = condition.and(lower(WORK_RESULT.ITEM_NAME).contains(request.itemName!!.lowercase(Locale.getDefault())))

            if (!request.listProcessGroup.isNullOrEmpty()) {
                val processGroupCodes = request.listProcessGroup!!.split(",")
                var condition1: Condition = DSL.noCondition()
                processGroupCodes.forEach { processGroup ->
                    condition1 = condition1.or(WORK_RESULT.PROCESS_GRP.eq(processGroup))
                }
                condition = condition.and(condition1)
            }

            if (!request.listProcessCode.isNullOrEmpty()) {
                val processCodes = request.listProcessCode!!.split(",")
                var condition2: Condition = DSL.noCondition()
                processCodes.forEach { processCode ->
                    condition2 = condition2.or(WORK_RESULT.PROCESS_CODE.eq(processCode))
                }
                condition = condition.and(condition2)
            }

            if (!request.tapeLot.isNullOrEmpty())
                condition = condition.and(lower(WORK_RESULT.TAPE_LOT_NO).contains(request.tapeLot!!.lowercase(Locale.getDefault())))

            if (!request.code.isNullOrEmpty())
                condition = condition.and(lower( WORK_RESULT.CODE).contains(request.code!!.lowercase(Locale.getDefault())))

            if (request.fromDate != null && request.toDate != null)
                condition = condition.and(WORK_RESULT.SUMMARY_RESULT_DATE.between(request.fromDate, request.toDate))
        }

        return context.selectFrom(WORK_RESULT)
            .where(condition.and(WORK_RESULT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, WORK_RESULT.SUMMARY_RESULT_DATE))
            .fetchInto(WorkResult::class.java)
    }

    fun findByObjectId(objectIds: List<Int>): List<WorkResult> {
        return context.selectFrom(WORK_RESULT)
            .where(WORK_RESULT.OBJECT_ID.`in`(objectIds))
            .fetchInto(WorkResult::class.java)
    }


    fun add(model: WorkResult) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val record = transactionalContext.newRecord(WORK_RESULT, model)
            transactionalContext.insertInto(WORK_RESULT).set(record).execute()
        }
    }

    fun delete(id: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(WORK_RESULT).where(WORK_RESULT.ID.eq(id)).execute()
        }
    }

    fun getMaxByDate(startDate: OffsetDateTime, endDate: OffsetDateTime): WorkResult? {
        return context.selectFrom(WORK_RESULT)
            .where(
                WORK_RESULT.SUMMARY_RESULT_DATE.ge(startDate).and(WORK_RESULT.SUMMARY_RESULT_DATE.le(endDate))
                    .and(WORK_RESULT.IS_DELETED.eq(false))
            )
            .orderBy(WORK_RESULT.SUMMARY_RESULT_DATE.sort(SortOrder.DESC))
            .fetchInto(WorkResult::class.java)
            .firstOrNull()
    }

    fun getForPlan(startDate: OffsetDateTime, endDate: OffsetDateTime, productNames: List<String>): List<WorkResult> {
        return context.selectFrom(WORK_RESULT)
            .where(
                WORK_RESULT.SUMMARY_RESULT_DATE.ge(startDate)
                    .and(WORK_RESULT.SUMMARY_RESULT_DATE.le(endDate))
                    .and(WORK_RESULT.ITEM_NAME.`in`(productNames))
                    .and(WORK_RESULT.IS_DELETED.eq(false))
            )
            .orderBy(WORK_RESULT.SUMMARY_RESULT_DATE.sort(SortOrder.ASC))
            .fetchInto(WorkResult::class.java)
    }

    fun getForReport(startDate: OffsetDateTime, endDate: OffsetDateTime, productNames: List<String?>): List<WorkResult> {
        return context.selectFrom(WORK_RESULT)
            .where(
                WORK_RESULT.SUMMARY_RESULT_DATE.ge(startDate)
                    .and(WORK_RESULT.SUMMARY_RESULT_DATE.le(endDate))
                    .and(WORK_RESULT.ITEM_NAME.`in`(productNames))
                    .and(WORK_RESULT.PROCESS_CODE.eq(ProcessCode.KTTN))
                    .and(WORK_RESULT.IS_DELETED.eq(false))
            )
            .orderBy(WORK_RESULT.SUMMARY_RESULT_DATE.sort(SortOrder.ASC))
            .fetchInto(WorkResult::class.java)
    }


}