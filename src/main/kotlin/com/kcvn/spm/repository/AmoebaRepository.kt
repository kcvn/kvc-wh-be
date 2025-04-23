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
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class AmoebaRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: StockTakingDailyRequest, pageable: Pageable): Pair<List<StockTakingDailyResponse>, Int> {
        val backlogWh = BACKLOG_WH
        val amoeba = AMOEBA

        val subQuery = context.select(backlogWh.LOCATION_CODE, backlogWh.PO_NUMBER)
            .from(backlogWh)
            .union(
                context.select(amoeba.LOCATION_CODE, amoeba.PO_NUMBER)
                    .from(amoeba)
            ).asTable("loc_po")

        // Field alias
        val locationField = subQuery.field("location_code", String::class.java)
        val poNumberField = subQuery.field("po_number", String::class.java)
        val amoebaQtyField = DSL.max(amoeba.QTY).`as`("amoebaQty")
        val backlogQtyField = DSL.max(backlogWh.BACKLOG_QTY).`as`("systemQty")
        val resultField = DSL.`when`(DSL.max(amoeba.QTY).eq(DSL.max(backlogWh.BACKLOG_QTY)), "SAME")
            .otherwise("DIFFERENT").`as`("result")

        var whereCondition  = DSL.noCondition()
        if (!request.locationCode.isNullOrEmpty()) {
            whereCondition  = whereCondition .and(locationField?.eq(request.locationCode))
        }
        if (!request.poNumber.isNullOrEmpty()) {
            whereCondition  = whereCondition .and(poNumberField?.eq(request.poNumber))
        }

        var havingCondition = DSL.noCondition()
        if (request.isDifferentBacklog == true) {
            val maxAmoebaQty = DSL.max(amoeba.QTY)
            val maxBacklogQty = DSL.max(backlogWh.BACKLOG_QTY)
            havingCondition = havingCondition.and(maxAmoebaQty.ne(maxBacklogQty).or(maxAmoebaQty.isNull).or(maxBacklogQty.isNull))
        }

        val querySql = context.select(
            locationField,
            poNumberField,
            backlogQtyField,
            amoebaQtyField,
            resultField
        )
            .from(subQuery)
            .leftJoin(backlogWh).on(
                backlogWh.LOCATION_CODE.eq(locationField)
                    .and(backlogWh.PO_NUMBER.eq(poNumberField))
            )
            .leftJoin(amoeba).on(
                amoeba.LOCATION_CODE.eq(locationField)
                    .and(amoeba.PO_NUMBER.eq(poNumberField))
            )
            .where(whereCondition )
            .groupBy(locationField, poNumberField)
            .having(havingCondition)
            .orderBy(locationField?.asc())
            .limit(pageable.pageSize)
            .offset(pageable.offset)

        val data = querySql.fetchInto(StockTakingDailyResponse::class.java)
        val count = querySql.count()

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