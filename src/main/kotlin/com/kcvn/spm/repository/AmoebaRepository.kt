package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.StockTakingDailyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingDailyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.references.AMOEBA
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AmoebaRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: StockTakingDailyRequest, pageable: Pageable) : Pair<List<StockTakingDailyResponse>, Int> {
        var condition = DSL.noCondition()
        if (!request.locationCode.isNullOrEmpty()) {
            condition = condition.and(AMOEBA.LOCATION_CODE.eq(request.locationCode))
        }
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(AMOEBA.PO_NUMBER.eq(request.poNumber))
        }
        if (request.isDifferentBacklog == true) {
            condition = condition.and(AMOEBA.QTY.notEqual(BACKLOG_WH.BACKLOG_QTY))
        }

        val resultField = DSL.`when`(AMOEBA.QTY.eq(BACKLOG_WH.BACKLOG_QTY), "SAME")
            .otherwise("DIFFERENT")
            .`as`("result")

        val querySql = context.select(
            AMOEBA.LOCATION_CODE.`as`("locationCode"),
            AMOEBA.PO_NUMBER.`as`("poNumber"),
            AMOEBA.QTY.`as`("amoebaQty"),
            BACKLOG_WH.BACKLOG_QTY.`as`("systemQty"),
            resultField
        ).from(AMOEBA)
            .join(BACKLOG_WH).on(AMOEBA.LOCATION_CODE.eq(BACKLOG_WH.LOCATION_CODE).and(AMOEBA.PO_NUMBER.eq(BACKLOG_WH.PO_NUMBER)))
            .where(condition)
            .orderBy(AMOEBA.LOCATION_CODE.sort(SortOrder.ASC))

        val query = context.select().from(querySql)
        val data = query.fetchInto(StockTakingDailyResponse::class.java)
        val count = query.count()

        return Pair(data, count)
    }

    fun saveAll(dataList: List<Amoeba>): Int {
        if (dataList.isEmpty()) return 0

        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val result = transactionalContext.batchInsert(
                dataList.map { data ->
                    AMOEBA.newRecord().apply {
                        this.locationCode = data.locationCode
                        this.poNumber = data.poNumber
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
            "locationCode" -> AMOEBA.LOCATION_CODE
            else -> AMOEBA.LOCATION_CODE
        }
        return sortField
    }
}