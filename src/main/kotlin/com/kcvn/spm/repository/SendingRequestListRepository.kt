package com.kcvn.spm.repository

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchForSendingRequest
import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.request.ImportBacklogWh
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.app.transaction.sending.payload.request.SendingRequestListSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.response.SendingRequestListResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.NewBacklogWh
import com.kcvn.spm.model.tables.pojos.PurchaseOrderBacklog
import com.kcvn.spm.model.tables.pojos.SendingRequestList
import com.kcvn.spm.model.tables.pojos.SendingRequestListDetail
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import com.kcvn.spm.model.tables.references.NEW_BACKLOG_WH
import com.kcvn.spm.model.tables.references.PURCHASE_ORDER_BACKLOG
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST_DETAIL
import com.kcvn.spm.model.tables.references.SPLITTING
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SendingRequestListRepository(private val context: DSLContext) : SortingRepository() {

    fun findByReceivingDatePO(poNumber: String, receivingDate: LocalDate): SendingRequestList? {
        return context.selectFrom(SENDING_REQUEST_LIST)
            .where(
                SENDING_REQUEST_LIST.PO_NUMBER.eq(poNumber)
                    .and (SENDING_REQUEST_LIST.RECEIVING_DATE.eq(receivingDate))
            )
            .orderBy(SENDING_REQUEST_LIST.SEQ_NO.desc())
            .fetchInto(SendingRequestList::class.java)
            .firstOrNull()
    }

    fun findByReceivingDatePOAndSeqNo(poNumber: String, receivingDate: LocalDate, seqNo: Int): SendingRequestList? {
        return context.selectFrom(SENDING_REQUEST_LIST)
            .where(
                SENDING_REQUEST_LIST.PO_NUMBER.eq(poNumber)
                    .and (SENDING_REQUEST_LIST.RECEIVING_DATE.eq(receivingDate))
                    .and (SENDING_REQUEST_LIST.SEQ_NO.eq(seqNo))
            )
            .orderBy(SENDING_REQUEST_LIST.SEQ_NO.desc())
            .fetchInto(SendingRequestList::class.java)
            .firstOrNull()
    }

    fun save(data: SendingRequestList): String {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val record = context.newRecord(SENDING_REQUEST_LIST, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }
        return context.insertInto(SENDING_REQUEST_LIST)
            .set(record)
            .returning(SENDING_REQUEST_LIST.ID)
            .fetchOne()!!.id!!
    }

    ///
    fun getList(
        request: SendingRequestListSearchRequest,
        pageable: Pageable?,
        fetchAll: Boolean = false,
    ) : Pair<List<SendingRequestListResponse>, Int> {
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

        return records.into(SendingRequestListResponse::class.java) to totalCount
    }

    private fun createGetListSqlQuery(request: SendingRequestListSearchRequest): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        // (column ilike %?% or ...)
        fun addCondition(column: String, values: List<String>) {
            conditions += values.joinToString(" or ", "(", ")") { "$column ilike '%' || ? || '%'" }
            params.addAll(values.map { it.trim() })
        }

        request.poNumber?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("srl.po_number", it) }
        request.itemCode?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("srl.item_code", it) }
        request.itemName?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("srl.item_name", it) }
        request.productionGroup?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("srl.production_group", it) }
        request.status?.let { conditions += "srl.status = ?"; params += it }

        val whereClause = if (conditions.isNotEmpty()) "where " + conditions.joinToString(" and ") else ""

        // backlog được gộp theo receiving_date + po_number trong subquery trước khi join, tránh nhân dòng
        val sql = """
            select
                srl.*,
                coalesce(backlog.backlog_qty, 0) as backlog_qty,
                count(*) over() as total_count
            from sending_request_list srl
                left join (
                    select
                        nbw.receiving_date,
                        nbw.po_number,
                        sum(nbw.backlog_qty) as backlog_qty
                    from new_backlog_wh nbw
                    group by nbw.receiving_date, nbw.po_number
                ) as backlog
                    on backlog.receiving_date = srl.receiving_date
                    and backlog.po_number = srl.po_number
            $whereClause
        """.trimIndent()

        return sql to params
    }

    // Chỉ sort theo cột trong whitelist để tránh SQL injection, mặc định created_date desc
    private fun createGetListOrderBy(pageable: Pageable?): String {
        val sortColumns = mapOf(
            "createdDate" to "srl.created_date",
            "receivingDate" to "srl.receiving_date",
            "poNumber" to "srl.po_number",
            "seqNo" to "srl.seq_no",
            "status" to "srl.status",
        )
        val orderBy = pageable?.sort
            ?.mapNotNull { order -> sortColumns[order.property]?.let { "$it ${if (order.isAscending) "asc" else "desc"}" } }
            ?.toList()
            .orEmpty()
        return orderBy.ifEmpty { listOf("srl.created_date desc") }.joinToString(", ")
    }
    ///


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SENDING_REQUEST_LIST.CREATED_DATE
            else -> SENDING_REQUEST_LIST.CREATED_DATE
        }
        return sortField
    }
}