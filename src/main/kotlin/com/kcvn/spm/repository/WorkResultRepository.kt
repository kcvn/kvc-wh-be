package com.kcvn.spm.repository

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.ProcessGroupResponse
import com.kcvn.spm.app.workresult.payload.response.ProcessResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.WORK_RESULT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.DSL.length
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class WorkResultRepository (
    private val context: DSLContext
) : SortingRepository() {
    fun getPagingListWorkResult(request: WorkResultSearchRequest?, pageable: Pageable): Pair<List<WorkResult>, Int> {
        var condition : Condition = DSL.noCondition()

        if (request!= null) {
            if(!request.order.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.ORDER_CODE.contains(request.order))

            if(!request.itemName.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.ITEM_NAME.contains(request.itemName))

            if(!request.listProcessGroup.isNullOrEmpty()) {
                request.listProcessGroup?.forEach { processGroup ->
                    condition = condition.or(WORK_RESULT.PROCESS_GRP.eq(processGroup))
                }
            }

            if(!request.listProcessName.isNullOrEmpty()){
                request.listProcessName?.forEach { processName ->
                    condition = condition.or(WORK_RESULT.PROCESS_NAME.eq(processName))
                }
            }

            if(!request.tapeLot.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.TAPE_LOT_NO.contains(request.tapeLot))

            if(!request.code.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.CODE.contains(request.code))

            if(request.fromDate!=null &&request.toDate!=null)
                condition = condition.and(WORK_RESULT.SUMMARY_RESULT_DATE.between(request.fromDate, request.toDate))
        }

        val data = context.selectFrom(WORK_RESULT)
            .where(condition.and(WORK_RESULT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, WORK_RESULT.SUMMARY_RESULT_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(WorkResult::class.java)

        val total = context.fetchCount(WORK_RESULT,condition.and(WORK_RESULT.IS_DELETED.eq(false)))

        return Pair(data,total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "summary_result_date" -> {
                WORK_RESULT.SUMMARY_RESULT_DATE
            }
            "item_name" -> {
                WORK_RESULT.ITEM_NAME
            }
            "process_name" -> {
                WORK_RESULT.PROCESS_NAME
            }
            "process_code" -> {
                WORK_RESULT.PROCESS_CODE
            }
            "layer_code" -> {
                WORK_RESULT.LAYER_CODE
            }
            "total_tape_quantity" -> {
                WORK_RESULT.TOTAL_TAPE_QUANTITY
            }
            "total_sheet_quantity" -> {
                WORK_RESULT.TOTAL_SHEET_QUANTITY
            }
            "good_tape_quantity" -> {
                WORK_RESULT.GOOD_TAPE_QUANTITY
            }
            "good_sheet_quantity" -> {
                WORK_RESULT.GOOD_SHEET_QUANTITY
            }
            "order_code" -> {
                WORK_RESULT.ORDER_CODE
            }
            "tape_lot_no" -> {
                WORK_RESULT.TAPE_LOT_NO
            }
            "code" -> {
                WORK_RESULT.CODE
            }
            "work_implement_by" -> {
                WORK_RESULT.WORK_IMPLEMENT_BY
            }
            "equipment_name" -> {
                WORK_RESULT.EQUIPMENT_NAME
            }

            else -> {
                val errorMessage = java.lang.String.format("Could not find table field: $sortFieldName")
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

    fun getListProcessByGroupCode(groupCode: Array<String>): List<ProcessResponse> {
        val response : MutableList<ProcessResponse> = mutableListOf()
        groupCode.forEach { code  ->
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
                response.addAll(listProcess)
            }
        }

        return response
    }
}