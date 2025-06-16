package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.StockTakingDailyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingDailyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.references.AMOEBA
import com.kcvn.spm.model.tables.references.BACKLOG_BIN_ENTRY
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AmoebaRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: StockTakingDailyRequest, pageable: Pageable): Pair<List<StockTakingDailyResponse>, Int> {
        val backlogBinEntry = BACKLOG_BIN_ENTRY
        val amoeba = AMOEBA

        val inspectionDateField = DSL.coalesce(amoeba.INSPECTION_DATE, backlogBinEntry.INSPECTION_DATE).`as`("inspectionDate")
        val poNumberField = DSL.coalesce(amoeba.PO_NUMBER, backlogBinEntry.PO_NUMBER).`as`("poNumber")
        val amoebaQtyField = DSL.max(amoeba.QTY).`as`("amoebaQty")
        val systemQtyField = DSL.max(backlogBinEntry.BACKLOG_QTY).`as`("systemQty")
        val amoebaLocationCodeField = DSL.max(amoeba.LOCATION_CODE).`as`("amoebaLocationCode")
        val systemLocationCodeField = DSL.max(backlogBinEntry.LOCATION_CODE).`as`("systemLocationCode")
        val resultQtyField = DSL.`when`(DSL.max(amoeba.QTY).eq(DSL.max(backlogBinEntry.BACKLOG_QTY)), "SAME")
            .otherwise("DIFFERENT").`as`("resultQty")
        val resultLocationCodeField = DSL.`when`(DSL.max(amoeba.LOCATION_CODE).eq(DSL.max(backlogBinEntry.LOCATION_CODE)), "SAME")
            .otherwise("DIFFERENT").`as`("resultLocationCode")

        val coalescedPoNumber = DSL.coalesce(amoeba.PO_NUMBER, backlogBinEntry.PO_NUMBER)
        val coalescedInspectionDate = DSL.coalesce(amoeba.INSPECTION_DATE, backlogBinEntry.INSPECTION_DATE)

        var whereCondition  = DSL.noCondition()
        if (request.inspectionDate != null) {
            whereCondition  = whereCondition .and(coalescedInspectionDate.eq(request.inspectionDate))
        }
        if (!request.poNumber.isNullOrEmpty()) {
            whereCondition  = whereCondition .and(coalescedPoNumber.eq(request.poNumber))
        }

        var havingCondition = DSL.noCondition()
        if (request.conditionQuery == "DIFFERENT") {
            val amoebaQty = DSL.max(amoeba.QTY)
            val systemQty = DSL.max(backlogBinEntry.BACKLOG_QTY)
            val amoebaLocationCode = DSL.max(amoeba.LOCATION_CODE)
            val systemLocationCode = DSL.max(backlogBinEntry.LOCATION_CODE)
            havingCondition = havingCondition.and(
                amoebaQty.ne(systemQty).or(amoebaQty.isNull).or(systemQty.isNull)
                    .or(amoebaLocationCode.ne(systemLocationCode)).or(amoebaLocationCode.eq("")).or(systemLocationCode.eq(""))
            )
        }
        if (request.conditionQuery == "SAME") {
            val amoebaQty = DSL.max(amoeba.QTY)
            val systemQty = DSL.max(backlogBinEntry.BACKLOG_QTY)
            val amoebaLocationCode = DSL.max(amoeba.LOCATION_CODE)
            val systemLocationCode = DSL.max(backlogBinEntry.LOCATION_CODE)
            havingCondition = havingCondition.and(
                amoebaQty.eq(systemQty).and(amoebaLocationCode.eq(systemLocationCode))
            )
        }

        val querySql = context.select(
            inspectionDateField,
            poNumberField,
            amoebaQtyField,
            systemQtyField,
            amoebaLocationCodeField,
            systemLocationCodeField,
            resultQtyField,
            resultLocationCodeField
        )
            .from(amoeba)
            .fullOuterJoin(backlogBinEntry)
            .on(
                amoeba.PO_NUMBER.eq(backlogBinEntry.PO_NUMBER)
                    .and(amoeba.INSPECTION_DATE.eq(backlogBinEntry.INSPECTION_DATE))
            )
            .where(whereCondition )
            .groupBy(coalescedInspectionDate, coalescedPoNumber)
            .having(havingCondition)
            .orderBy(coalescedPoNumber.asc())

        val count = querySql.count()
        val data = querySql
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(StockTakingDailyResponse::class.java)

        return Pair(data, count)
    }


    fun saveAll(dataList: List<Amoeba>): Int {
        if (dataList.isEmpty()) return 0

        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val result = transactionalContext.batchInsert(
                dataList.map { data ->
                    AMOEBA.newRecord().apply {
                        this.inspectionDate = data.inspectionDate
                        this.poNumber = data.poNumber
                        this.locationCode = data.locationCode
                        this.qty = data.qty
                        this.createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                    }
                }
            ).execute()

            result.sum()
        }
    }

    fun delete() {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(AMOEBA)
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> AMOEBA.CREATED_DATE
            else -> AMOEBA.CREATED_DATE
        }
        return sortField
    }
}