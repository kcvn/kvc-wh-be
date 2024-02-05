package com.kcvn.spm.repository

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.WORK_RESULT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class WorkResultRepository (
    private val context: DSLContext,
    private val processMasterRepository: ProcessMasterRepository
) : SortingRepository() {
    fun getListWorkResult(request: WorkResultSearchRequest?, pageable: Pageable): Pair<List<WorkResult>, Int> {
        var condition : Condition = DSL.noCondition()

        val listProcess = context.select().from(PROCESS_PROCEDURE_STRUCTURE)
            .join(PROCESS_MASTER)
            .on(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))



        if (request!= null) {
            if(!request.order.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.ORDER_CODE.contains(request.order))

            if(!request.productName.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.ITEM_NAME.contains(request.productName))

            if(!request.listProcessGroup.isNullOrEmpty()) {
                request.listProcessGroup.forEach { processGroup ->
                    condition = condition.and(WORK_RESULT.PROCESS_GRP.eq(processGroup))
                }
            }
            else{

            }

            if(!request.listProcessName.isNullOrEmpty()){
                request.listProcessName.forEach { processName ->
                    condition = condition.and(WORK_RESULT.PROCESS_NAME.eq(processName))
                }
            }
            else{

            }

            if(!request.tapeLot.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.TAPE_LOT_NO.contains(request.tapeLot))

            if(!request.code.isNullOrEmpty())
                condition = condition.and(WORK_RESULT.CODE.contains(request.code))

            if(request.fromDate!=null &&request.toDate!=null)
                condition = condition.and(WORK_RESULT.SUMMARY_RESULT_DATE.between(request.fromDate, request.toDate))
        }

        val data = context.selectFrom(WORK_RESULT)
            .where(condition)
            .orderBy(getSortFields(pageable.sort, WORK_RESULT.SUMMARY_RESULT_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(WorkResult::class.java)

        val total = context.fetchCount(WORK_RESULT,condition)

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
            "order_code" -> {
                WORK_RESULT.ORDER_CODE
            }
            "tape_lot_no" -> {
                WORK_RESULT.TAPE_LOT_NO
            }
            "code" -> {
                WORK_RESULT.CODE
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
}