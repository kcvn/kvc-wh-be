package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.StockTakingDailyRequest
import com.kcvn.spm.app.stocktaking.payload.response.StockTakingDailyResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Amoeba
import com.kcvn.spm.model.tables.references.AMOEBA
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class AmoebaRepository(private val context: DSLContext) : SortingRepository() {
    fun getListByRawSql(request: StockTakingDailyRequest, pageable: Pageable): Pair<List<StockTakingDailyResponse>, Int> {
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
                StockTakingDailyResponse(
                    inspectionDate = it.get("inspection_date", LocalDate::class.java),
                    poNumber = it.get("po_number", String::class.java),
                    amoebaLocationCode = it.get("amoeba_location_code", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    amoebaQty = it.get("amoeba_qty", BigDecimal::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                )
            }

        return result to totalCount
    }

    fun getAll(
        request: StockTakingDailyRequest
    ): Pair<List<StockTakingDailyResponse>, Int> {
        val (sql, params) = createSqlQuery(request)

        val countSql = "SELECT COUNT(*) FROM (${sql}) AS count_table"
        val totalCount = context.fetchOne(countSql, *params.toTypedArray())?.get(0, Int::class.java) ?: 0

        val result = context
            .fetch(sql, *params.toTypedArray())
            .map {
                StockTakingDailyResponse(
                    inspectionDate = it.get("inspection_date", LocalDate::class.java),
                    poNumber = it.get("po_number", String::class.java),
                    amoebaLocationCode = it.get("amoeba_location_code", String::class.java),
                    systemLocationCode = it.get("system_location_code", String::class.java),
                    amoebaQty = it.get("amoeba_qty", BigDecimal::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    resultQty = it.get("result_qty", String::class.java),
                    resultLocationCode = it.get("result_location_code", String::class.java),
                )
            }

        return result to totalCount
    }

    fun createSqlQuery(request: StockTakingDailyRequest): Pair<String, List<Any>> {
        val sql = """
    SELECT * FROM (
  WITH temp1 AS (
    SELECT 
      MIN(CAST(b.location_code AS INTEGER)) AS MIN_LOCATION_CODE,
      b.po_number,
      SUM(b.backlog_qty) AS SUM_BACKLOG_QTY,
      b.receiving_date,
      MAX(b.inspection_date) AS INSPECTION_DATE
    FROM 
      public.backlog_wh b
    WHERE 
      b.backlog_qty > 0
    GROUP BY 
      b.po_number, 
      b.receiving_date
    ORDER BY 
      b.receiving_date ASC
  )

  SELECT
    COALESCE(a.inspection_date, temp1.inspection_date) AS inspection_date,
    COALESCE(a.po_number, temp1.po_number) AS po_number,

    a.location_code AS amoeba_location_code,
    a.qty AS amoeba_qty,

    temp1.MIN_LOCATION_CODE AS system_location_code,
    temp1.SUM_BACKLOG_QTY AS system_qty,
    
    CASE 
      WHEN CAST(NULLIF(a.location_code, '') AS INTEGER) = temp1.MIN_LOCATION_CODE THEN 'SAME'
      ELSE 'DIFFERENT'
    END AS result_location_code,
    
    CASE 
      WHEN a.qty = temp1.SUM_BACKLOG_QTY THEN 'SAME'
      ELSE 'DIFFERENT'
    END AS result_qty

  FROM public.amoeba a
  FULL OUTER JOIN temp1 
    ON a.po_number = temp1.po_number
    AND a.inspection_date = temp1.inspection_date
) AS final_data
""".trimIndent()

        val sqlBuilder = StringBuilder()
        val params = mutableListOf<Any>()
        sqlBuilder.appendLine(sql)

        val whereConditions = mutableListOf<String>()
        if (!request.poNumber.isNullOrBlank()) {
            whereConditions.add(" final_data.po_number = ?")
            params.add(request.poNumber!!)
        }
        if (request.conditionQuery == "DIFFERENT") {
            whereConditions.add("(result_location_code = 'DIFFERENT'\n" +
                    "or result_qty = 'DIFFERENT')")
        }
        if (request.conditionQuery == "SAME") {
            whereConditions.add("result_location_code = 'SAME'\n" +
                    "and result_qty = 'SAME'")
        }
        if (whereConditions.isNotEmpty()) {
            sqlBuilder.appendLine("WHERE")
            sqlBuilder.appendLine(whereConditions.joinToString("\nAND "))
        }

        return sqlBuilder.toString() to params
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