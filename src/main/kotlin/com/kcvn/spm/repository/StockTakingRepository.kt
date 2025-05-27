package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.ScanRequest
import com.kcvn.spm.app.stocktaking.payload.request.StartActualRequest
import com.kcvn.spm.app.stocktaking.payload.request.StockTakingMonthlyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingMonthlyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.StockTaking
import com.kcvn.spm.model.tables.references.AMOEBA
import com.kcvn.spm.model.tables.references.STOCK_TAKING
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class StockTakingRepository(private val context: DSLContext) : SortingRepository() {
    fun getListForAndroid(yearNumber: Int, monthNumber: Int, pageable: Pageable) : Pair<List<StockTaking>, Int> {
        val query = context.selectFrom(STOCK_TAKING)
            .where(STOCK_TAKING.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber)))
        val count = query.count()
        val data = query
            .orderBy(getSortFields(pageable.sort, STOCK_TAKING.INSPECTION_DATE))
            .fetchInto(StockTaking::class.java)

        return Pair(data, count)
    }

    fun getList(request: StockTakingMonthlyRequest, pageable: Pageable): Pair<List<StockTakingMonthlyResponse>, Int> {
        val stockTaking = STOCK_TAKING

        val inspectionDateField = stockTaking.INSPECTION_DATE.`as`("inspectionDate")
        val poNumberField = stockTaking.PO_NUMBER.`as`("poNumber")
        val amoebaLocationCodeField = DSL.max(stockTaking.AMOEBA_LOCATION_CODE).`as`("amoebaLocationCode")
        val actualLocationCodeField = DSL.max(stockTaking.ACTUAL_LOCATION_CODE).`as`("actualLocationCode")
        val amoebaQtyField = DSL.max(stockTaking.AMOEBA_QTY).`as`("amoebaQty")
        val actualQtyField = DSL.max(stockTaking.ACTUAL_QTY).`as`("actualQty")
        val resultQtyField = DSL.`when`(DSL.max(stockTaking.AMOEBA_QTY).eq(DSL.max(stockTaking.ACTUAL_QTY)), "SAME")
            .otherwise("DIFFERENT").`as`("resultQty")
        val resultLocationCodeField = DSL.`when`(DSL.max(stockTaking.AMOEBA_LOCATION_CODE).eq(DSL.max(stockTaking.ACTUAL_LOCATION_CODE)), "SAME")
            .otherwise("DIFFERENT").`as`("resultLocationCode")

        var whereCondition  = DSL.noCondition()
        if (request.yearNumber != null) {
            whereCondition = whereCondition.and(stockTaking.YEAR_NUMBER.eq(request.yearNumber))
        }
        if (request.monthNumber != null) {
            whereCondition = whereCondition.and(stockTaking.MONTH_NUMBER.eq(request.monthNumber))
        }
        if (request.inspectionDate != null) {
            whereCondition  = whereCondition .and(stockTaking.INSPECTION_DATE.eq(request.inspectionDate))
        }
        if (!request.poNumber.isNullOrEmpty()) {
            whereCondition  = whereCondition .and(stockTaking.PO_NUMBER.eq(request.poNumber))
        }

        var havingCondition = DSL.noCondition()
        if (request.isDifferentBacklog == true) {
            val amoebaQty = DSL.max(stockTaking.AMOEBA_QTY)
            val actualQty = DSL.max(stockTaking.ACTUAL_QTY)
            val amoebaLocationCode = DSL.max(stockTaking.AMOEBA_LOCATION_CODE)
            val actualLocationCode = DSL.max(stockTaking.ACTUAL_LOCATION_CODE)
            havingCondition = havingCondition.and(
                amoebaQty.ne(actualQty).or(amoebaQty.isNull).or(actualQty.isNull)
                    .or(amoebaLocationCode.ne(actualLocationCode)).or(amoebaLocationCode.eq("")).or(actualLocationCode.eq(""))
            )
        }

        val querySql = context.select(
            inspectionDateField,
            poNumberField,
            amoebaQtyField,
            actualQtyField,
            amoebaLocationCodeField,
            actualLocationCodeField,
            resultQtyField,
            resultLocationCodeField
        )
            .from(stockTaking)
            .where(whereCondition )
            .groupBy(inspectionDateField, poNumberField)
            .having(havingCondition)
            .orderBy(poNumberField.asc())

        val count = querySql.count()
        val data = querySql
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(StockTakingMonthlyResponse::class.java)

        return Pair(data, count)
    }

    fun findByInspectionDateAndPO(inspectionDate: LocalDate, poNumber: String): StockTaking? {
        return context.selectFrom(STOCK_TAKING)
            .where(STOCK_TAKING.INSPECTION_DATE.eq(inspectionDate).and(STOCK_TAKING.PO_NUMBER.eq(poNumber)))
            .fetchInto(StockTaking::class.java)
            .firstOrNull()
    }

    fun update(yearNumber: Int, monthNumber: Int, request: ScanRequest) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(STOCK_TAKING)
                .set(STOCK_TAKING.ACTUAL_LOCATION_CODE, request.actualLocationCode)
                .set(STOCK_TAKING.ACTUAL_QTY, request.actualQty)
                .set(STOCK_TAKING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(STOCK_TAKING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    STOCK_TAKING.YEAR_NUMBER.eq(yearNumber)
                        .and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber))
                        .and(STOCK_TAKING.INSPECTION_DATE.eq(request.inspectionDate))
                        .and(STOCK_TAKING.PO_NUMBER.eq(request.poNumber))
                )
                .execute()
        }
    }

    fun save(domain: StockTaking) {
        context.insertInto(
            STOCK_TAKING, STOCK_TAKING.YEAR_NUMBER, STOCK_TAKING.MONTH_NUMBER, STOCK_TAKING.INSPECTION_DATE,
            STOCK_TAKING.PO_NUMBER, STOCK_TAKING.AMOEBA_LOCATION_CODE, STOCK_TAKING.ACTUAL_LOCATION_CODE,
            STOCK_TAKING.AMOEBA_QTY, STOCK_TAKING.ACTUAL_QTY, STOCK_TAKING.CREATED_BY)
            .values(domain.yearNumber, domain.monthNumber, domain.inspectionDate, domain.poNumber, domain.amoebaLocationCode,
                domain.actualLocationCode, domain.amoebaQty, domain.actualQty, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    fun deleteByYearAndMonth(yearNumber: Int, monthNumber: Int) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(STOCK_TAKING)
                .where(
                    STOCK_TAKING.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber))
                )
                .execute()
        }
    }

    fun copyFromAmoebaToStockTaking(request: StartActualRequest) {
        val year = request.yearNumber
        val month = request.monthNumber

        if (year != null && month != null) {
            val insertQuery = context.insertInto(STOCK_TAKING)
                .columns(
                    STOCK_TAKING.YEAR_NUMBER,
                    STOCK_TAKING.MONTH_NUMBER,
                    STOCK_TAKING.INSPECTION_DATE,
                    STOCK_TAKING.PO_NUMBER,
                    STOCK_TAKING.AMOEBA_LOCATION_CODE,
                    STOCK_TAKING.ACTUAL_LOCATION_CODE,
                    STOCK_TAKING.AMOEBA_QTY,
                    STOCK_TAKING.ACTUAL_QTY
                )
                .select(
                    context.select(
                        DSL.`val`(year),
                        DSL.`val`(month),
                        AMOEBA.INSPECTION_DATE,
                        AMOEBA.PO_NUMBER,
                        AMOEBA.LOCATION_CODE,
                        DSL.inline(""),
                        AMOEBA.QTY,
                        DSL.inline(BigDecimal.ZERO)
                    ).from(AMOEBA)
                )

            insertQuery.execute()
        } else {
            throw IllegalArgumentException("yearNumber and monthNumber must not be null")
        }
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "yearNumber" -> STOCK_TAKING.YEAR_NUMBER
            "monthNumber" -> STOCK_TAKING.MONTH_NUMBER
            else -> STOCK_TAKING.YEAR_NUMBER
        }
        return sortField
    }
}