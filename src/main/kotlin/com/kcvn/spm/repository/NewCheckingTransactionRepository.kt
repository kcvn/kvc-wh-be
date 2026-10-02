package com.kcvn.spm.repository

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.NewCheckingTransaction
import com.kcvn.spm.model.tables.references.CHECKING_HISTORY
import com.kcvn.spm.model.tables.references.NEW_CHECKING_TRANSACTION
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class NewCheckingTransactionRepository(private val context: DSLContext) : SortingRepository() {
        fun findByLotAndSeq(input: CheckingHistoryRequest, seqNo: Int): NewCheckingTransaction? {
        return context.selectFrom(NEW_CHECKING_TRANSACTION)
            .where(
                NEW_CHECKING_TRANSACTION.INVOICE_NUMBER.eq(input.invoiceNumber)
                    .and(NEW_CHECKING_TRANSACTION.PO_NUMBER.eq(input.poNumber))
                    .and(NEW_CHECKING_TRANSACTION.LOT_NO.eq(input.lotNo))
                    .and(NEW_CHECKING_TRANSACTION.CHECK_TIMES.eq(seqNo))
            )
            .fetchInto(NewCheckingTransaction::class.java)
            .firstOrNull()
    }

    fun findLatestSeqOfOrder(poNumber: String, invoiceNumber: String): NewCheckingTransaction? {
        return context.selectFrom(NEW_CHECKING_TRANSACTION)
            .where(
                NEW_CHECKING_TRANSACTION.INVOICE_NUMBER.eq(invoiceNumber)
                    .and(NEW_CHECKING_TRANSACTION.PO_NUMBER.eq(poNumber))
            )
            .orderBy(NEW_CHECKING_TRANSACTION.CHECK_TIMES.sort(SortOrder.DESC))
            .fetchInto(NewCheckingTransaction::class.java)
            .firstOrNull()
    }

    fun save(data: NewCheckingTransaction) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(NEW_CHECKING_TRANSACTION, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    fun update(rec: NewCheckingTransaction) {
        context.update(NEW_CHECKING_TRANSACTION)
            .set(NEW_CHECKING_TRANSACTION.SCAN_QTY, rec.scanQty)
            .set(NEW_CHECKING_TRANSACTION.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(NEW_CHECKING_TRANSACTION.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(NEW_CHECKING_TRANSACTION.ID.eq(rec.id))
            .execute()
    }

    fun searchCheckingHistory(request: CheckingHistorySearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<CheckingHistoryResponse>, Int> {
        val (sql, params) = createSqlQuery(request)

        var paginatedSql = "$sql order by pob.invoice_number, pob.po_number, pob.seq_no, check_times"
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
                CheckingHistoryResponse(
                    poNumber = it.get("po_number", String::class.java),
                    invoiceNumber = it.get("invoice_number", String::class.java),
                    orderDate = it.get("order_date", LocalDate::class.java),
                    seqNo = it.get("seq_no", Int::class.java),
                    itemCd = it.get("item_code", String::class.java),
                    itemName = it.get("item_name", String::class.java),
                    department = it.get("prod_group", String::class.java),
                    storageLocation = it.get("storage_location", String::class.java),
                    unit = it.get("unit", String::class.java),
                    itemType = it.get("item_type", String::class.java),
                    orderQty = it.get("order_qty", BigDecimal::class.java),
                    scanQty = it.get("scan_qty", BigDecimal::class.java),
                    scannedDate = it.get("scan_date", OffsetDateTime::class.java)?.toLocalDate(),
                    result = it.get("result", Int::class.java),
                    status = it.get("approved", Boolean::class.java)
                )
            }

        return result to totalCount
    }

    private fun createSqlQuery(request: CheckingHistorySearchRequest): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        request.invoiceNumber?.takeIf { it.isNotBlank() }?.let { conditions += "pob.invoice_number = ?"; params += it }
        request.poNumber?.takeIf { it.isNotBlank() }?.let { conditions += "pob.po_number = ?"; params += it }
        request.storageLocation?.takeIf { it.isNotBlank() }?.let { conditions += "pob.storage_location = ?"; params += it }
        request.itemType?.takeIf { it.isNotBlank() }?.let { conditions += "pob.item_type = ?"; params += it }
        request.fromDate?.let { conditions += "pob.created_date >= ?"; params += it }
        request.toDate?.let { conditions += "pob.created_date < ?"; params += it }

        val whereClause = if (conditions.isNotEmpty()) "where " + conditions.joinToString(" and ") else ""

        val sql = """
            select
                pob.po_number,
                pob.invoice_number,
                pob.order_date,
                pob.seq_no,
                pob.item_code,
                pob.item_name,
                pob.prod_group,
                pob.storage_location,
                pob.unit,
                pob.item_type,
                pob.order_qty,
                coalesce(scanned_data.check_times, 0) as check_times,
                coalesce(scanned_data.scan_qty, 0) as scan_qty,
                scanned_data.scan_date,
                case
                    when scanned_data.scan_qty is null then 4
                    when scanned_data.scan_qty < pob.order_qty then 1
                    when scanned_data.scan_qty = pob.order_qty then 2
                    else 3
                end as result,
                pob.approved,
                count(*) over() as total_count
            from purchase_order_backlog pob
                left join lateral (
                    select
                        nct.check_times,
                        sum(nct.scan_qty) as scan_qty,
                        max(coalesce(nct.updated_date, nct.created_date)) as scan_date
                    from new_checking_transaction nct
                    where nct.invoice_number = pob.invoice_number
                        and nct.po_number = pob.po_number
                    group by nct.check_times
                ) as scanned_data on true
            $whereClause
        """.trimIndent()

        return sql to params
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> CHECKING_HISTORY.CREATED_DATE
            else -> CHECKING_HISTORY.CREATED_DATE
        }
        return sortField
    }
}