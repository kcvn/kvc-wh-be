package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.ScanRequest
import com.kcvn.spm.app.stocktaking.payload.request.StartActualRequest
import com.kcvn.spm.app.stocktaking.payload.request.StockTakingMonthlyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingMonthlyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.StockTaking
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import com.kcvn.spm.model.tables.references.STOCK_TAKING_CHECKING
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class StockTakingCheckingRepository(private val context: DSLContext) : SortingRepository() {
    fun getListOnGoingByRawSql(
        request: StockTakingMonthlyRequest,
        pageable: Pageable
    ): Pair<List<StockTakingMonthlyResponse>, Int> {
        val (sql, params) = createSqlQuery(request)

        val countSql = "SELECT COUNT(*) FROM (${sql}) AS count_table"
        val totalCount = context.fetchOne(countSql, *params.toTypedArray())?.get(0, Int::class.java) ?: 0

        val paginatedSql = "$sql LIMIT ? OFFSET ?"
        val paginatedParams = params.toMutableList().apply {
            add(pageable.pageSize)
            add(pageable.offset.toInt())
        }

        val result = context
            .fetch(paginatedSql, *paginatedParams.toTypedArray())
            .map {
                StockTakingMonthlyResponse(
                    poNumber = it.get("po_number", String::class.java),
                    packageCode = it.get("package_code", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    actualLocationCode = it.get("actual_location_code", String::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    actualQty = it.get("actual_qty", BigDecimal::class.java),
                    systemBoxQty = it.get("system_box_qty", Int::class.java),
                    actualBoxQty = it.get("actual_box_qty", Int::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultBoxQty = it.get("result_box_qty", String::class.java)
                )
            }

        return result to totalCount
    }

    fun getListCompletedByRawSql(
        request: StockTakingMonthlyRequest,
        pageable: Pageable
    ): Pair<List<StockTakingMonthlyResponse>, Int> {
        val (sql, params) = createSqlQueryCompleted(request)

        val countSql = "SELECT COUNT(*) FROM (${sql}) AS count_table"
        val totalCount = context.fetchOne(countSql, *params.toTypedArray())?.get(0, Int::class.java) ?: 0

        val paginatedSql = "$sql LIMIT ? OFFSET ?"
        val paginatedParams = params.toMutableList().apply {
            add(pageable.pageSize)
            add(pageable.offset.toInt())
        }

        val result = context
            .fetch(paginatedSql, *paginatedParams.toTypedArray())
            .map {
                StockTakingMonthlyResponse(
                    poNumber = it.get("po_number", String::class.java),
                    packageCode = it.get("package_code", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    actualLocationCode = it.get("actual_location_code", String::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    actualQty = it.get("actual_qty", BigDecimal::class.java),
                    systemBoxQty = it.get("system_box_qty", Int::class.java),
                    actualBoxQty = it.get("actual_box_qty", Int::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultBoxQty = it.get("result_box_qty", String::class.java)
                )
            }

        return result to totalCount
    }

    fun getListOnGoingForStopByRawSql(
        request: StockTakingMonthlyRequest,
    ): List<StockTakingMonthlyResponse> {
        val (sql, params) = createSqlQuery(request)
        val paginatedParams = params.toMutableList().apply {}
        val result = context
            .fetch(sql, *paginatedParams.toTypedArray())
            .map {
                StockTakingMonthlyResponse(
                    poNumber = it.get("po_number", String::class.java),
                    packageCode = it.get("package_code", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    actualLocationCode = it.get("actual_location_code", String::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    actualQty = it.get("actual_qty", BigDecimal::class.java),
                    systemBoxQty = it.get("system_box_qty", Int::class.java),
                    actualBoxQty = it.get("actual_box_qty", Int::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultBoxQty = it.get("result_box_qty", String::class.java)
                )
            }

        return result
    }

    fun getAll(
        request: StockTakingMonthlyRequest
    ): Pair<List<StockTakingMonthlyResponse>, Int> {
        val (sql, params) = createSqlQuery(request)

        val countSql = "SELECT COUNT(*) FROM (${sql}) AS count_table"
        val totalCount = context.fetchOne(countSql, *params.toTypedArray())?.get(0, Int::class.java) ?: 0

        val result = context
            .fetch(sql, *params.toTypedArray())
            .map {
                StockTakingMonthlyResponse(
                    poNumber = it.get("po_number", String::class.java),
                    packageCode = it.get("package_code", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    actualLocationCode = it.get("actual_location_code", String::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    actualQty = it.get("actual_qty", BigDecimal::class.java),
                    systemBoxQty = it.get("system_box_qty", Int::class.java),
                    actualBoxQty = it.get("actual_box_qty", Int::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultBoxQty = it.get("result_box_qty", String::class.java)
                )
            }

        return result to totalCount
    }

    fun createSqlQuery(request: StockTakingMonthlyRequest): Pair<String, List<Any>> {
        val sql = """
        SELECT * FROM (
            SELECT 
                COALESCE(stock_data.po_number, bw.po_number) AS po_number,
                COALESCE(stock_data.package_code, bw.package_code) AS package_code,
                bw.location_code AS system_location_code,
                bw.backlog_qty AS system_qty,
                bw.box_qty AS system_box_qty,
                stock_data.actual_location_code,
                stock_data.actual_qty,
                stock_data.actual_box_qty,
                stock_data.year_number,
                stock_data.month_number,
                CASE 
                    WHEN stock_data.actual_location_code = bw.location_code THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_location_code,
                CASE 
                    WHEN stock_data.actual_qty = bw.backlog_qty THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_qty,
                CASE 
                    WHEN stock_data.actual_box_qty = bw.box_qty THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_box_qty
            FROM (
                SELECT
                    st.po_number,
                    st.package_code,
                    st.actual_location_code,
                    st.actual_qty,
                    st.actual_box_qty,
                    st.year_number,
                    st.month_number
                FROM STOCK_TAKING_CHECKING st
                WHERE 1 = 1 
    """.trimIndent()

        val params = mutableListOf<Any>()
        val stockTakingWhere = mutableListOf<String>()

        if (request.yearNumber != null) {
            stockTakingWhere.add("st.year_number = ?")
            params.add(request.yearNumber!!)
        }

        if (request.monthNumber != null) {
            stockTakingWhere.add("st.month_number = ?")
            params.add(request.monthNumber!!)
        }

        val sqlBuilder = StringBuilder(sql)

        if (stockTakingWhere.isNotEmpty()) {
            sqlBuilder.appendLine("AND ${stockTakingWhere.joinToString(" AND ")}")
        }

        sqlBuilder.appendLine("""
            ) stock_data
            FULL OUTER JOIN (
                SELECT * FROM backlog_wh bw WHERE bw.backlog_qty > 0
            ) bw ON stock_data.package_code = bw.package_code
        ) final_data
    """.trimIndent())

        val finalWhere = mutableListOf<String>()

        if (!request.poNumber.isNullOrBlank()) {
            finalWhere.add("final_data.po_number = ?")
            params.add(request.poNumber!!)
        }

        if (request.conditionQuery == "DIFFERENT") {
            finalWhere.add("""
            (
                final_data.result_location_code = 'DIFFERENT' OR 
                final_data.result_qty = 'DIFFERENT' OR 
                final_data.result_box_qty = 'DIFFERENT'
            )
        """.trimIndent())
        }

        if (request.conditionQuery == "SAME") {
            finalWhere.add("""
            final_data.result_location_code = 'SAME' AND 
            final_data.result_qty = 'SAME' AND 
            final_data.result_box_qty = 'SAME'
        """.trimIndent())
        }

        if (finalWhere.isNotEmpty()) {
            sqlBuilder.appendLine("WHERE ${finalWhere.joinToString(" AND ")}")
        }

        return sqlBuilder.toString() to params
    }

    fun createSqlQueryCompleted(request: StockTakingMonthlyRequest): Pair<String, List<Any>> {
        val sql = """
    SELECT 
        po_number,
        package_code,
        system_location_code,
        system_qty,
        system_box_qty,
        actual_location_code,
        actual_qty,
        actual_box_qty,
        year_number,
        month_number,
        CASE 
            WHEN actual_location_code = system_location_code THEN 'SAME'
            ELSE 'DIFFERENT'
        END AS result_location_code,
        CASE 
            WHEN actual_qty = system_qty THEN 'SAME'
            ELSE 'DIFFERENT'
        END AS result_qty,
        CASE 
            WHEN actual_box_qty = system_box_qty THEN 'SAME'
            ELSE 'DIFFERENT'
        END AS result_box_qty
    FROM STOCK_TAKING_CHECKING st
    WHERE 1 = 1 
""".trimIndent()

        val params = mutableListOf<Any>()
        val whereConditions = mutableListOf<String>()

        if (request.yearNumber != null) {
            whereConditions.add("st.year_number = ?")
            params.add(request.yearNumber!!)
        }

        if (request.monthNumber != null) {
            whereConditions.add("st.month_number = ?")
            params.add(request.monthNumber!!)
        }

        if (!request.poNumber.isNullOrBlank()) {
            whereConditions.add("st.po_number = ?")
            params.add(request.poNumber!!)
        }

        if (request.conditionQuery == "DIFFERENT") {
            whereConditions.add("""
        (
            actual_location_code != system_location_code OR 
            actual_qty != system_qty OR 
            actual_box_qty != system_box_qty
        )
    """.trimIndent())
        }

        if (request.conditionQuery == "SAME") {
            whereConditions.add("""
        actual_location_code = system_location_code AND 
        actual_qty = system_qty AND 
        actual_box_qty = system_box_qty
    """.trimIndent())
        }

        val sqlBuilder = StringBuilder(sql)

        if (whereConditions.isNotEmpty()) {
            sqlBuilder.appendLine("AND ${whereConditions.joinToString(" AND ")}")
        }

        return sqlBuilder.toString() to params
    }


    fun getListForAndroid(yearNumber: Int, monthNumber: Int, pageable: Pageable): Pair<List<StockTaking>, Int> {
        val query = context.selectFrom(STOCK_TAKING_CHECKING)
            .where(STOCK_TAKING_CHECKING.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING_CHECKING.MONTH_NUMBER.eq(monthNumber)))
        val count = query.count()
        val data = query
            .orderBy(getSortFields(pageable.sort, STOCK_TAKING_CHECKING.CREATED_DATE))
            .fetchInto(StockTaking::class.java)

        return Pair(data, count)
    }

    fun findByPackageCode(packageCode: String): StockTaking? {
        return context.selectFrom(STOCK_TAKING_CHECKING)
            .where(
                STOCK_TAKING_CHECKING.PACKAGE_CODE.eq(packageCode)
            )
            .fetchInto(StockTaking::class.java)
            .firstOrNull()
    }

    fun update(yearNumber: Int, monthNumber: Int, request: ScanRequest) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(STOCK_TAKING_CHECKING)
                .set(STOCK_TAKING_CHECKING.ACTUAL_LOCATION_CODE, request.actualLocationCode)
                .set(STOCK_TAKING_CHECKING.ACTUAL_QTY, request.actualQty)
                .set(STOCK_TAKING_CHECKING.ACTUAL_BOX_QTY, request.actualBoxQty)
                .set(STOCK_TAKING_CHECKING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(STOCK_TAKING_CHECKING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    STOCK_TAKING_CHECKING.YEAR_NUMBER.eq(yearNumber)
                        .and(STOCK_TAKING_CHECKING.MONTH_NUMBER.eq(monthNumber))
                        .and(STOCK_TAKING_CHECKING.PACKAGE_CODE.eq(request.packageCode))
                )
                .execute()
        }
    }

    fun updateSystem(yearNumber: Int, monthNumber: Int, request: StockTakingMonthlyResponse) {
        context.update(STOCK_TAKING_CHECKING)
                .set(STOCK_TAKING_CHECKING.SYSTEM_LOCATION_CODE, request.systemLocationCode)
                .set(STOCK_TAKING_CHECKING.SYSTEM_QTY, request.systemQty)
                .set(STOCK_TAKING_CHECKING.SYSTEM_BOX_QTY, request.systemBoxQty)
                .set(STOCK_TAKING_CHECKING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(STOCK_TAKING_CHECKING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    STOCK_TAKING_CHECKING.YEAR_NUMBER.eq(yearNumber)
                        .and(STOCK_TAKING_CHECKING.MONTH_NUMBER.eq(monthNumber))
                        .and(STOCK_TAKING_CHECKING.PACKAGE_CODE.eq(request.packageCode))
                )
                .execute()

    }

    fun save(domain: StockTaking) {
        context.insertInto(
            STOCK_TAKING_CHECKING, STOCK_TAKING_CHECKING.YEAR_NUMBER, STOCK_TAKING_CHECKING.MONTH_NUMBER, STOCK_TAKING_CHECKING.PO_NUMBER,
            STOCK_TAKING_CHECKING.PACKAGE_CODE, STOCK_TAKING_CHECKING.ACTUAL_LOCATION_CODE, STOCK_TAKING_CHECKING.ACTUAL_QTY,
            STOCK_TAKING_CHECKING.ACTUAL_BOX_QTY, STOCK_TAKING_CHECKING.CREATED_BY
        )
            .values(
                domain.yearNumber, domain.monthNumber, domain.poNumber,
                domain.packageCode, domain.actualLocationCode, domain.actualQty,
                domain.actualBoxQty, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    fun deleteByYearAndMonth(yearNumber: Int, monthNumber: Int) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(STOCK_TAKING_CHECKING)
                .where(
                    STOCK_TAKING_CHECKING.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING_CHECKING.MONTH_NUMBER.eq(monthNumber))
                )
                .execute()
        }
    }

    fun copyFromBacklogWhToStockTaking(request: StartActualRequest) {
        val year = request.yearNumber
        val month = request.monthNumber

        if (year != null && month != null) {
            val insertQuery = context.insertInto(STOCK_TAKING_CHECKING)
                .columns(
                    STOCK_TAKING_CHECKING.YEAR_NUMBER,
                    STOCK_TAKING_CHECKING.MONTH_NUMBER,
                    STOCK_TAKING_CHECKING.PO_NUMBER,
                    STOCK_TAKING_CHECKING.PACKAGE_CODE,
                    STOCK_TAKING_CHECKING.SYSTEM_LOCATION_CODE,
                    STOCK_TAKING_CHECKING.ACTUAL_LOCATION_CODE,
                    STOCK_TAKING_CHECKING.SYSTEM_QTY,
                    STOCK_TAKING_CHECKING.ACTUAL_QTY,
                    STOCK_TAKING_CHECKING.SYSTEM_BOX_QTY,
                    STOCK_TAKING_CHECKING.ACTUAL_BOX_QTY,
                    STOCK_TAKING_CHECKING.CREATED_BY
                )
                .select(
                    context.select(
                        DSL.`val`(year),
                        DSL.`val`(month),
                        BACKLOG_WH.PO_NUMBER,
                        BACKLOG_WH.PACKAGE_CODE,
                        BACKLOG_WH.LOCATION_CODE,
                        DSL.inline(null as String?),
                        BACKLOG_WH.BACKLOG_QTY,
                        DSL.inline(null as BigDecimal?),
                        BACKLOG_WH.BOX_QTY,
                        DSL.inline(null as Int?),
                        DSL.inline(CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                    ).from(BACKLOG_WH)
                        .where(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
                )

            insertQuery.execute()
        } else {
            throw IllegalArgumentException("yearNumber and monthNumber must not be null")
        }
    }

//    fun copyFromAmoebaToStockTaking(request: StartActualRequest) {
//        val year = request.yearNumber
//        val month = request.monthNumber
//
//        if (year != null && month != null) {
//            val insertQuery = context.insertInto(STOCK_TAKING_CHECKING)
//                .columns(
//                    STOCK_TAKING_CHECKING.YEAR_NUMBER,
//                    STOCK_TAKING_CHECKING.MONTH_NUMBER,
//                    STOCK_TAKING_CHECKING.INSPECTION_DATE,
//                    STOCK_TAKING_CHECKING.PO_NUMBER,
//                    STOCK_TAKING_CHECKING.AMOEBA_LOCATION_CODE,
//                    STOCK_TAKING_CHECKING.ACTUAL_LOCATION_CODE,
//                    STOCK_TAKING_CHECKING.AMOEBA_QTY,
//                    STOCK_TAKING_CHECKING.ACTUAL_QTY
//                )
//                .select(
//                    context.select(
//                        DSL.`val`(year),
//                        DSL.`val`(month),
//                        AMOEBA.INSPECTION_DATE,
//                        AMOEBA.PO_NUMBER,
//                        AMOEBA.LOCATION_CODE,
//                        DSL.inline(""),
//                        AMOEBA.QTY,
//                        DSL.inline(BigDecimal.ZERO)
//                    ).from(AMOEBA)
//                )
//
//            insertQuery.execute()
//        } else {
//            throw IllegalArgumentException("yearNumber and monthNumber must not be null")
//        }
//    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "yearNumber" -> STOCK_TAKING_CHECKING.YEAR_NUMBER
            "monthNumber" -> STOCK_TAKING_CHECKING.MONTH_NUMBER
            else -> STOCK_TAKING_CHECKING.CREATED_DATE
        }
        return sortField
    }
}