package com.kcvn.spm.repository

import com.kcvn.spm.app.stocktaking.payload.request.SapStockTakingSearchRequest
import com.kcvn.spm.app.stocktaking.payload.response.SapStockTakingResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.references.SAP_BACKLOG
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.Internal.result
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class SapStockTakingRepository(private val context: DSLContext) : SortingRepository() {


    fun searchSapStockTaking(request: SapStockTakingSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<SapStockTakingResponse>, Int> {
        val (sql, params) = createSqlQuery(request)

        var paginatedSql = "$sql order by lot_no, item_code, production_group, storage_location, item_type"
        val paginatedParams = params.toMutableList()
        if (!isExport) {
            paginatedSql = "$paginatedSql limit ? offset ?"
            paginatedParams += pageable.pageSize
            paginatedParams += pageable.offset
        }

        val records = context.fetch(paginatedSql, *paginatedParams.toTypedArray())
        val totalCount = records.firstOrNull()?.get("total_count", Int::class.java) ?: 0

        val result = records
            .map {
                SapStockTakingResponse(
                    lotNo = it.get("lot_no", String::class.java),
                    itemCode = it.get("item_code", String::class.java),
                    productionGroup = it.get("production_group", String::class.java),
                    storageLocation = it.get("storage_location", String::class.java),
                    itemType = it.get("item_type", String::class.java),
                    sapQty = it.get("sap_qty", BigDecimal::class.java),
                    systemQty = it.get("system_qty", BigDecimal::class.java),
                    result = it.get("result", String::class.java),
                )
            }

        return result to totalCount
    }

    private fun createSqlQuery(request: SapStockTakingSearchRequest): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        val statusCondition = when (request.conditionQuery) {
            "SAME" -> "coalesce(sap.qty, 0) = coalesce(sys.qty, 0)"
            "DIFFERENT" -> "coalesce(sap.qty, 0) <> coalesce(sys.qty, 0)"
            else -> null
        }

        request.lotNo?.takeIf { it.isNotBlank() }?.let { conditions += "coalesce(sap.lot_no, sys.lot_no) = ?"; params += it }
        statusCondition?.let { conditions += it }

        val whereClause = if (conditions.isNotEmpty()) "where " + conditions.joinToString(" and ") else ""

        val sql = """
            with sap as (
select
	coalesce(lot_no, '') as lot_no,
	coalesce(item_code, '') as item_code,
	coalesce(production_group, '') as production_group,
	coalesce(storage_location, '') as storage_location,
	coalesce(item_type, '') as item_type,
	sum(backlog_qty) as qty
from
	sap_backlog
group by
	1,
	2,
	3,
	4,
	5
),
sys as (
select
	coalesce(lot_no, '') as lot_no,
	coalesce(item_code, '') as item_code,
	coalesce(production_group, '') as production_group,
	coalesce(storage_location, '') as storage_location,
	coalesce(item_type, '') as item_type,
	sum(backlog_qty) as qty
from
	new_backlog_wh
group by
	1,
	2,
	3,
	4,
	5
)
select
	coalesce(sap.lot_no, sys.lot_no) as lot_no,
	coalesce(sap.item_code, sys.item_code) as item_code,
	coalesce(sap.production_group, sys.production_group) as production_group,
	coalesce(sap.storage_location, sys.storage_location) as storage_location,
	coalesce(sap.item_type, sys.item_type) as item_type,
	coalesce(sap.qty, 0) as sap_qty,
	coalesce(sys.qty, 0) as system_qty,
	case when coalesce(sap.qty, 0) = coalesce(sys.qty, 0) then 'SAME' else 'DIFFERENT' end as result,
	count(*) over() as total_count
from
	sap
full outer join sys
            	on
	sap.item_code = sys.item_code
	and sap.production_group = sys.production_group
	and sap.storage_location = sys.storage_location
	and sap.lot_no = sys.lot_no
	and sap.item_type = sys.item_type
            $whereClause
        """.trimIndent()

        return sql to params
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SAP_BACKLOG.CREATED_DATE
            else -> SAP_BACKLOG.CREATED_DATE
        }
        return sortField
    }
}