package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.sending.payload.request.NewTempSendingSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.response.NewTempSendingResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.references.NEW_TEMP_SENDING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class NewTempSendingTransactionRepository(private val context: DSLContext) : SortingRepository() {

    ///
    fun getList(
        request: NewTempSendingSearchRequest,
        pageable: Pageable?,
        fetchAll: Boolean = false,
    ) : Pair<List<NewTempSendingResponse>, Int> {
        val (sql, params) = createGetListSqlQuery(request)

        var paginatedSql = "$sql order by ${createGetListOrderBy(pageable)}"
        val paginatedParams = params.toMutableList()
        if (pageable != null && !fetchAll) {
            paginatedSql = "$paginatedSql limit ? offset ?"
            paginatedParams += pageable.pageSize
            paginatedParams += pageable.offset
        }

        val records = context.fetch(paginatedSql, *paginatedParams.toTypedArray())
        val totalCount = records.firstOrNull()?.get("total_count", Int::class.java) ?: 0

        return records.into(NewTempSendingResponse::class.java) to totalCount
    }

    private fun createGetListSqlQuery(request: NewTempSendingSearchRequest): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        // Bỏ picking của request đã decline (3) / hoàn tất (4)
        val conditions = mutableListOf("list.status not in (3, 4)", "picking.is_deleted = false")

        // (column ilike %?% or ...)
        fun addCondition(column: String, values: List<String>) {
            conditions += values.joinToString(" or ", "(", ")") { "$column ilike '%' || ? || '%'" }
            params.addAll(values.map { it.trim() })
        }

        // form code so khớp chính xác vì đây là phiếu đang chọn để scan
        request.formCode?.takeIf { it.isNotBlank() }?.let { conditions += "form.form_code = ?"; params += it.trim() }
        request.poNumber?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("list.po_number", it) }
        request.lotNo?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("picking.lot_no", it) }
        request.packageCode?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("picking.package_code", it) }

        val sql = """
                        select
	list.receiving_date,
	list.po_number,
	list.item_name,
	list.production_group,
	locations.location_code,
	picking.package_code,
    picking.lot_no,
	picking.request_qty,
	picking.issue_date,
	coalesce(scanned.scanned_qty, 0) as scanned_qty,
	coalesce(checking.checked_qty, 0) as checked_qty,
    picking.id as sending_picking_list_id,
    CASE
                            WHEN COALESCE (picking.request_qty,0) = COALESCE(scanned.scanned_qty, 0) and COALESCE (scanned.scanned_qty,0) = COALESCE(checking.checked_qty, 0)  THEN 'SAME'
                            ELSE 'DIFFERENT'
                             END AS status,
count(*) over() as total_count
from
	public.sending_form_list form
inner join public.sending_request_list list 
on
	form.id = list.sending_form_id
inner join public.sending_request_list_detail detail
on
	list.id = detail.sending_request_id
inner join public.sending_picking_list picking
on
	detail.id = picking.sending_request_detail_id
inner join public.splitting locations
on picking.package_code = locations.package_code
left join (
	select
		sending_picking_list_id,
		SUM(scanned_qty) as scanned_qty
	from
		public.new_temp_sending_transactions
where is_deleted = false
		group by sending_picking_list_id) scanned
on
	picking.id = scanned.sending_picking_list_id
	left join (
	select
		sending_picking_list_id,
		SUM(checked_qty ) as checked_qty
	from
		public.new_temp_sending_transactions_checking
where is_deleted = false
		group by sending_picking_list_id) checking
on
	picking.id = checking.sending_picking_list_id
            where ${conditions.joinToString(" and ")}
        """.trimIndent()

        return sql to params
    }

    // Chỉ sort theo cột trong whitelist để tránh SQL injection, mặc định po_number, lot_no, package_code
    private fun createGetListOrderBy(pageable: Pageable?): String {
        val sortColumns = mapOf(
            "createdDate" to "list.created_date",
            "receivingDate" to "list.receiving_date",
            "poNumber" to "list.po_number",
            "lotNo" to "picking.lot_no",
            "packageCode" to "picking.package_code",
        )
        val orderBy = pageable?.sort
            ?.mapNotNull { order -> sortColumns[order.property]?.let { "$it ${if (order.isAscending) "asc" else "desc"}" } }
            ?.toList()
            .orEmpty()
        return orderBy.ifEmpty { listOf("list.po_number asc", "picking.lot_no asc", "picking.package_code asc") }.joinToString(", ")
    }
    ///

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> NEW_TEMP_SENDING_TRANSACTIONS.CREATED_DATE
            else -> NEW_TEMP_SENDING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}
