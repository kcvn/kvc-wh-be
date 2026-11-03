package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.ScanRequest
import com.kcvn.spm.app.stocktaking.payload.request.StartActualRequest
import com.kcvn.spm.app.stocktaking.payload.request.StockTakingMonthlyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingMonthlyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PurchaseOrderBacklog
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.model.tables.pojos.StockTaking
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import com.kcvn.spm.model.tables.references.PURCHASE_ORDER_BACKLOG
import com.kcvn.spm.model.tables.references.SPLITTING
import com.kcvn.spm.model.tables.references.STOCK_TAKING
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class StockTakingRepository(private val context: DSLContext) : SortingRepository() {
    fun getListOnGoingByRawSql(
        request: StockTakingMonthlyRequest,
        pageable: Pageable?
    ): Pair<List<StockTakingMonthlyResponse>, Int> {
        val (sql, params) = createSqlQuery(request)

        val countSql = "SELECT COUNT(*) FROM (${sql}) AS count_table"
        val totalCount = context.fetchOne(countSql, *params.toTypedArray())?.get(0, Int::class.java) ?: 0

        var paginatedSql = sql
        var paginatedParams = params.toMutableList()
        if (pageable != null)
        {
            paginatedSql = "$paginatedSql LIMIT ? OFFSET ?"
            paginatedParams = params.toMutableList().apply {
                add(pageable.pageSize)
                add(pageable.offset.toInt())
            }
        }


        val result = context
            .fetch(paginatedSql, *paginatedParams.toTypedArray())
            .map {
                StockTakingMonthlyResponse(
                    poNumber = it.get("po_number", String::class.java),
                    packageCode = it.get("package_code", String::class.java),
                    lotNo = it.get("lot_no", String::class.java),
                    itemName = it.get("item_name", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    actualLocationCode = it.get("actual_location_code", String::class.java),
                    checkingLocationCode = it.get("checking_location_code", String::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    actualQty = it.get("actual_qty", BigDecimal::class.java),
                    checkingQty = it.get("checking_qty", BigDecimal::class.java),
                    systemBoxQty = it.get("system_box_qty", Int::class.java),
                    actualBoxQty = it.get("actual_box_qty", Int::class.java),
                    checkingBoxQty = it.get("checking_box_qty", Int::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultBoxQty = it.get("result_box_qty", String::class.java)
                )
            }

        return result to totalCount
    }

    fun getListCompletedByRawSql(
        request: StockTakingMonthlyRequest,
        pageable: Pageable?
    ): Pair<List<StockTakingMonthlyResponse>, Int> {
        val (sql, params) = createSqlQueryCompleted(request)

        val countSql = "SELECT COUNT(*) FROM (${sql}) AS count_table"
        val totalCount = context.fetchOne(countSql, *params.toTypedArray())?.get(0, Int::class.java) ?: 0

        var paginatedSql = sql
        var paginatedParams = params.toMutableList()
        if (pageable != null)
        {
            paginatedSql = "$paginatedSql LIMIT ? OFFSET ?"
            paginatedParams = params.toMutableList().apply {
                add(pageable.pageSize)
                add(pageable.offset.toInt())
            }
        }

        val result = context
            .fetch(paginatedSql, *paginatedParams.toTypedArray())
            .map {
                StockTakingMonthlyResponse(
                    poNumber = it.get("po_number", String::class.java),
                    packageCode = it.get("package_code", String::class.java),
                    lotNo = it.get("lot_no", String::class.java),
                    itemName = it.get("item_name", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    actualLocationCode = it.get("actual_location_code", String::class.java),
                    checkingLocationCode = it.get("checking_location_code", String::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    actualQty = it.get("actual_qty", BigDecimal::class.java),
                    checkingQty = it.get("checking_qty", BigDecimal::class.java),
                    systemBoxQty = it.get("system_box_qty", Int::class.java),
                    actualBoxQty = it.get("actual_box_qty", Int::class.java),
                    checkingBoxQty = it.get("checking_box_qty", Int::class.java),
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
                    lotNo = it.get("lot_no", String::class.java),
                    itemName = it.get("item_name", String::class.java),
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
                    lotNo = it.get("lot_no", String::class.java),
                    itemName = it.get("item_name", String::class.java),
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
        val params = mutableListOf<Any>()

        // filter month before joining, otherwise backlog-only rows (no year/month) are dropped
        val periodWhere = mutableListOf<String>()
        val periodParams = mutableListOf<Any>()
        if (request.yearNumber != null) {
            periodWhere.add("year_number = ?")
            periodParams.add(request.yearNumber!!)
        }
        if (request.monthNumber != null) {
            periodWhere.add("month_number = ?")
            periodParams.add(request.monthNumber!!)
        }
        val periodWhereClause = if (periodWhere.isNotEmpty()) "WHERE ${periodWhere.joinToString(" AND ")}" else ""
        // once for stock_data, once for checking_data
        params.addAll(periodParams)
        params.addAll(periodParams)

        val sql = """
        WITH stock_data AS (
            SELECT
                st.po_number,
                st.package_code,
                st.lot_no,
                st.actual_location_code,
                st.actual_qty,
                st.actual_box_qty,
                st.year_number,
                st.month_number
            FROM stock_taking st
            $periodWhereClause
        ),
        system_data AS (
            SELECT
                nbw.po_number,
                nbw.package_code,
                nbw.lot_no,
                MAX(nbw.item_name) AS item_name,
                MAX(s.location_code) AS location_code,
                SUM(nbw.backlog_qty) AS backlog_qty,
                SUM(nbw.box_qty) AS box_qty
            FROM new_backlog_wh nbw
            LEFT JOIN (
                SELECT package_code, MAX(location_code) AS location_code
                FROM splitting
                WHERE is_deleted = false
                GROUP BY package_code
            ) s ON s.package_code = nbw.package_code
            WHERE nbw.backlog_qty > 0
            GROUP BY nbw.po_number, nbw.package_code, nbw.lot_no
        ),
        temp_data AS (
            SELECT
                COALESCE(sd.po_number, bw.po_number) AS po_number,
                COALESCE(sd.package_code, bw.package_code) AS package_code,
                COALESCE(sd.lot_no, bw.lot_no) AS lot_no,
                bw.item_name,
                bw.location_code AS system_location_code,
                bw.backlog_qty AS system_qty,
                bw.box_qty AS system_box_qty,
                sd.actual_location_code,
                sd.actual_qty,
                sd.actual_box_qty,
                sd.year_number,
                sd.month_number
            FROM stock_data sd
            FULL OUTER JOIN system_data bw
                ON sd.package_code = bw.package_code
                AND COALESCE(sd.lot_no, '') = COALESCE(bw.lot_no, '')
        ),
        checking_data AS (
            SELECT
                stc.po_number,
                stc.package_code,
                stc.lot_no,
                stc.actual_location_code,
                stc.actual_qty,
                stc.actual_box_qty,
                stc.year_number,
                stc.month_number
            FROM stock_taking_checking stc
            $periodWhereClause
        )
        SELECT * FROM (
            SELECT
                COALESCE(td.po_number, cd.po_number) AS po_number,
                COALESCE(td.package_code, cd.package_code) AS package_code,
                COALESCE(td.lot_no, cd.lot_no) AS lot_no,
                td.item_name,
                td.system_location_code,
                td.system_qty,
                td.system_box_qty,
                td.actual_location_code,
                td.actual_qty,
                td.actual_box_qty,
                COALESCE(td.year_number, cd.year_number) AS year_number,
                COALESCE(td.month_number, cd.month_number) AS month_number,
                cd.actual_location_code AS checking_location_code,
                cd.actual_qty AS checking_qty,
                cd.actual_box_qty AS checking_box_qty,
                CASE
                    WHEN td.actual_location_code = td.system_location_code
                        AND (cd.actual_location_code IS NULL OR cd.actual_location_code = td.actual_location_code)
                    THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_location_code,
                CASE
                    WHEN td.actual_qty = td.system_qty
                        AND (cd.actual_qty IS NULL OR cd.actual_qty = td.actual_qty)
                    THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_qty,
                CASE
                    WHEN td.actual_box_qty = td.system_box_qty
                        AND (cd.actual_box_qty IS NULL OR cd.actual_box_qty = td.actual_box_qty)
                    THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_box_qty
            FROM temp_data td
            FULL OUTER JOIN checking_data cd
                ON td.package_code = cd.package_code
                AND COALESCE(td.lot_no, '') = COALESCE(cd.lot_no, '')
        ) final_data
    """.trimIndent()

        val sqlBuilder = StringBuilder(sql)
        sqlBuilder.appendLine()

        val finalWhere = mutableListOf<String>()

        if (!request.poNumber.isNullOrBlank()) {
            finalWhere.add("final_data.po_number ilike '%' || ? || '%'")
            params.add(request.poNumber!!.trim())
        }

        if (!request.lotNo.isNullOrBlank()) {
            finalWhere.add("final_data.lot_no ilike '%' || ? || '%'")
            params.add(request.lotNo!!.trim())
        }

        if (!request.packageCode.isNullOrBlank()) {
            finalWhere.add("final_data.package_code ilike '%' || ? || '%'")
            params.add(request.packageCode!!.trim())
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

        if (request.isChecked == "CHECKED") {
            finalWhere.add("""
            final_data.checking_location_code IS NOT NULL
            AND final_data.checking_qty IS NOT NULL
            AND final_data.checking_box_qty IS NOT NULL
        """.trimIndent())
        }

        if (request.isChecked == "UNCHECKED") {
            finalWhere.add("""
            final_data.checking_location_code IS NULL
            AND final_data.checking_qty IS NULL
            AND final_data.checking_box_qty IS NULL
        """.trimIndent())
        }

        if (finalWhere.isNotEmpty()) {
            sqlBuilder.appendLine("WHERE ${finalWhere.joinToString(" AND ")}")
        }

        return sqlBuilder.toString() to params
    }

    fun createSqlQueryCompleted(request: StockTakingMonthlyRequest): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()

        // filter month inside each side before the full join, same as the on-going query
        val periodWhere = mutableListOf<String>()
        val periodParams = mutableListOf<Any>()
        if (request.yearNumber != null) {
            periodWhere.add("year_number = ?")
            periodParams.add(request.yearNumber!!)
        }
        if (request.monthNumber != null) {
            periodWhere.add("month_number = ?")
            periodParams.add(request.monthNumber!!)
        }
        val periodWhereClause = if (periodWhere.isNotEmpty()) "WHERE ${periodWhere.joinToString(" AND ")}" else ""
        // once for stock_data, once for checking_data
        params.addAll(periodParams)
        params.addAll(periodParams)

        // completed month reads the snapshot saved in stock_taking at stop time, plus checking results
        val sql = """
        WITH stock_data AS (
            SELECT
                st.po_number,
                st.package_code,
                st.lot_no,
                st.item_name,
                st.system_location_code,
                st.system_qty,
                st.system_box_qty,
                st.actual_location_code,
                st.actual_qty,
                st.actual_box_qty,
                st.year_number,
                st.month_number
            FROM stock_taking st
            $periodWhereClause
        ),
        checking_data AS (
            SELECT
                stc.po_number,
                stc.package_code,
                stc.lot_no,
                stc.actual_location_code,
                stc.actual_qty,
                stc.actual_box_qty,
                stc.year_number,
                stc.month_number
            FROM stock_taking_checking stc
            $periodWhereClause
        )
        SELECT * FROM (
            SELECT
                COALESCE(sd.po_number, cd.po_number) AS po_number,
                COALESCE(sd.package_code, cd.package_code) AS package_code,
                COALESCE(sd.lot_no, cd.lot_no) AS lot_no,
                sd.item_name,
                sd.system_location_code,
                sd.system_qty,
                sd.system_box_qty,
                sd.actual_location_code,
                sd.actual_qty,
                sd.actual_box_qty,
                COALESCE(sd.year_number, cd.year_number) AS year_number,
                COALESCE(sd.month_number, cd.month_number) AS month_number,
                cd.actual_location_code AS checking_location_code,
                cd.actual_qty AS checking_qty,
                cd.actual_box_qty AS checking_box_qty,
                CASE
                    WHEN sd.actual_location_code = sd.system_location_code
                        AND (cd.actual_location_code IS NULL OR cd.actual_location_code = sd.actual_location_code)
                    THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_location_code,
                CASE
                    WHEN sd.actual_qty = sd.system_qty
                        AND (cd.actual_qty IS NULL OR cd.actual_qty = sd.actual_qty)
                    THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_qty,
                CASE
                    WHEN sd.actual_box_qty = sd.system_box_qty
                        AND (cd.actual_box_qty IS NULL OR cd.actual_box_qty = sd.actual_box_qty)
                    THEN 'SAME'
                    ELSE 'DIFFERENT'
                END AS result_box_qty
            FROM stock_data sd
            FULL OUTER JOIN checking_data cd
                ON sd.package_code = cd.package_code
                AND COALESCE(sd.lot_no, '') = COALESCE(cd.lot_no, '')
        ) final_data
    """.trimIndent()

        val finalWhere = mutableListOf<String>()

        if (request.isChecked == "CHECKED") {
            finalWhere.add("""
            final_data.checking_location_code IS NOT NULL
            AND final_data.checking_qty IS NOT NULL
            AND final_data.checking_box_qty IS NOT NULL
        """.trimIndent())
        }

        if (request.isChecked == "UNCHECKED") {
            finalWhere.add("""
            final_data.checking_location_code IS NULL
            AND final_data.checking_qty IS NULL
            AND final_data.checking_box_qty IS NULL
        """.trimIndent())
        }

        if (!request.poNumber.isNullOrBlank()) {
            finalWhere.add("final_data.po_number ilike '%' || ? || '%'")
            params.add(request.poNumber!!.trim())
        }

        if (!request.lotNo.isNullOrBlank()) {
            finalWhere.add("final_data.lot_no ilike '%' || ? || '%'")
            params.add(request.lotNo!!.trim())
        }

        if (!request.packageCode.isNullOrBlank()) {
            finalWhere.add("final_data.package_code ilike '%' || ? || '%'")
            params.add(request.packageCode!!.trim())
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

        val sqlBuilder = StringBuilder(sql)

        if (finalWhere.isNotEmpty()) {
            sqlBuilder.appendLine()
            sqlBuilder.appendLine("WHERE ${finalWhere.joinToString(" AND ")}")
        }

        return sqlBuilder.toString() to params
    }


    fun getListForAndroid(yearNumber: Int, monthNumber: Int, pageable: Pageable): Pair<List<StockTaking>, Int> {
        val query = context.selectFrom(STOCK_TAKING)
            .where(STOCK_TAKING.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber)))
        val count = query.count()
        val data = query
            .orderBy(getSortFields(pageable.sort, STOCK_TAKING.CREATED_DATE))
            .fetchInto(StockTaking::class.java)

        return Pair(data, count)
    }

    fun findByPackageCodeAndPoAndLot(yearNumber: Int, monthNumber: Int, packageCode: String, poNumber: String, lotNo: String): StockTaking? {
        return context.selectFrom(STOCK_TAKING)
            .where(
                STOCK_TAKING.YEAR_NUMBER.eq(yearNumber)
                    .and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber))
                    .and(STOCK_TAKING.PACKAGE_CODE.eq(packageCode))
                    .and(STOCK_TAKING.PO_NUMBER.eq(poNumber))
                    .and(STOCK_TAKING.LOT_NO.eq(lotNo))
            )
            .fetchOneInto(StockTaking::class.java)
    }

    fun update(data: StockTaking) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(STOCK_TAKING, data).apply {
            updatedBy = user
            updatedDate = currentTime
            changed(PURCHASE_ORDER_BACKLOG.CREATED_BY, false)
            changed(PURCHASE_ORDER_BACKLOG.CREATED_DATE, false)
        }.update()
    }

    fun lockByYearAndMonth(yearNumber: Int, monthNumber: Int) {
        context.select(STOCK_TAKING.ID)
            .from(STOCK_TAKING)
            .where(STOCK_TAKING.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber)))
            .forUpdate()
            .fetch()
    }

    fun updateSystem(yearNumber: Int, monthNumber: Int, request: StockTakingMonthlyResponse): Int {
        return context.update(STOCK_TAKING)
                // keep the existing name when the package is no longer in backlog at stop time
                .set(STOCK_TAKING.ITEM_NAME, DSL.coalesce(DSL.`val`(request.itemName), STOCK_TAKING.ITEM_NAME))
                .set(STOCK_TAKING.SYSTEM_LOCATION_CODE, request.systemLocationCode)
                .set(STOCK_TAKING.SYSTEM_QTY, request.systemQty)
                .set(STOCK_TAKING.SYSTEM_BOX_QTY, request.systemBoxQty)
                .set(STOCK_TAKING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(STOCK_TAKING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    STOCK_TAKING.YEAR_NUMBER.eq(yearNumber)
                        .and(STOCK_TAKING.MONTH_NUMBER.eq(monthNumber))
                        .and(STOCK_TAKING.PACKAGE_CODE.eq(request.packageCode))
                        .and(STOCK_TAKING.LOT_NO.isNotDistinctFrom(request.lotNo))
                )
                .execute()

    }

    fun save(data: StockTaking) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(STOCK_TAKING, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
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

    fun copyFromBacklogWhToStockTaking(request: StartActualRequest) {
        val year = request.yearNumber
        val month = request.monthNumber

        if (year != null && month != null) {
            val insertQuery = context.insertInto(STOCK_TAKING)
                .columns(
                    STOCK_TAKING.YEAR_NUMBER,
                    STOCK_TAKING.MONTH_NUMBER,
                    STOCK_TAKING.PO_NUMBER,
                    STOCK_TAKING.PACKAGE_CODE,
                    STOCK_TAKING.ITEM_NAME,
                    STOCK_TAKING.SYSTEM_LOCATION_CODE,
                    STOCK_TAKING.ACTUAL_LOCATION_CODE,
                    STOCK_TAKING.SYSTEM_QTY,
                    STOCK_TAKING.ACTUAL_QTY,
                    STOCK_TAKING.SYSTEM_BOX_QTY,
                    STOCK_TAKING.ACTUAL_BOX_QTY,
                    STOCK_TAKING.CREATED_BY
                )
                .select(
                    context.select(
                        DSL.`val`(year),
                        DSL.`val`(month),
                        BACKLOG_WH.PO_NUMBER,
                        BACKLOG_WH.PACKAGE_CODE,
                        BACKLOG_WH.ITEM_NAME,
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
//            val insertQuery = context.insertInto(STOCK_TAKING)
//                .columns(
//                    STOCK_TAKING.YEAR_NUMBER,
//                    STOCK_TAKING.MONTH_NUMBER,
//                    STOCK_TAKING.INSPECTION_DATE,
//                    STOCK_TAKING.PO_NUMBER,
//                    STOCK_TAKING.AMOEBA_LOCATION_CODE,
//                    STOCK_TAKING.ACTUAL_LOCATION_CODE,
//                    STOCK_TAKING.AMOEBA_QTY,
//                    STOCK_TAKING.ACTUAL_QTY
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
            "yearNumber" -> STOCK_TAKING.YEAR_NUMBER
            "monthNumber" -> STOCK_TAKING.MONTH_NUMBER
            else -> STOCK_TAKING.CREATED_DATE
        }
        return sortField
    }
}