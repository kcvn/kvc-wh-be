package com.kcvn.spm.repository

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.checkinghistory.payload.response.CheckingHistoryResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.model.tables.references.CHECKING_HISTORY
import com.kcvn.spm.model.tables.references.SENDING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CheckingHistoryRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: CheckingHistorySearchRequest, pageable: Pageable) : Pair<List<CheckingHistoryResponse>, Int> {
        val keywordPoNumber = request.poNumber?.let { "%$it%" } ?: "%"
        val keywordInvoice = request.invoiceNo?.let { "%$it%" } ?: "%"

        var sql = """
            select *,
	case
		when final_data.order_qty > final_data.scan_qty then 1
		when final_data.order_qty = final_data.scan_qty then 2
		when final_data.order_qty < final_data.scan_qty then 3
	end as result
from (select
	pob.po_no,
	pob.invoice,
	pob.order_date,
	pob.item_code,
	pob.item_name,
	pob.prod_group,
	pob.storage_location,
	pob.unit,
	pob.item_type,
	pob.order_qty,
	coalesce(rc.scan_qty,0) as scan_qty,
	pob.approved
from
	purchase_order_backlog pob
left join (
	select
		rc.order_backlog_id,
		sum(rc.scan_qty) as scan_qty
	from
		receiving_checking rc
	group by
		rc.order_backlog_id) rc
on
	pob.id = rc.order_backlog_id) final_data
            WHERE final_data.invoice ilike ?
            AND final_data.po_no ilike ?
        """.trimIndent()
        if (request.fromDate != null && request.toDate != null) {
            sql += "\nAND (final_data.scan_date between '${request.fromDate?.toLocalDate()}' and '${request.toDate?.toLocalDate()}')"
        }
        if (request.storageLocation != null) {
            sql += "\nAND final_data.storage_location = '${request.storageLocation}'"
        }
        if (request.itemType != null) {
            sql += "\nAND final_data.item_type = '${request.itemType}'"
        }
        if (request.status == "APPROVED")
            sql += "\nAND final_data.approved = true"
        else if (request.status == "NOT_APPROVED")
            sql += "\nAND final_data.approved = false"
        val result = context
            .resultQuery(sql, keywordInvoice, keywordPoNumber)
            .fetch()
            .map {
                CheckingHistoryResponse(
                    poNumber = it.get("po_no", String::class.java),
                    invoiceNo = it.get("invoice", String::class.java),
                    importQty = it.get("order_qty", BigDecimal::class.java),
                    scanQty = it.get("scan_qty", BigDecimal::class.java),
                    seqNo = it.get("scan_qty", Int::class.java),
                )
            }
        return Pair(result, result.size)
    }

    fun findByScanDateAndPOAndSeqNoAndFormCode(scanDate: LocalDate, poNumber: String, seqNo: Int, formCode: String): CheckingHistory? {
        return context.selectFrom(CHECKING_HISTORY)
            .where(
                CHECKING_HISTORY.SCAN_DATE.eq(scanDate)
                    .and(CHECKING_HISTORY.PO_NUMBER.eq(poNumber))
                    .and(CHECKING_HISTORY.SEQ_NO.eq(seqNo))
                    .and(CHECKING_HISTORY.FORM_CODE.eq(formCode))
            )
            .fetchInto(CheckingHistory::class.java)
            .firstOrNull()
    }

    fun findLatestByScanDateAndPOAndFormCode(scanDate: LocalDate, poNumber: String, formCode: String): CheckingHistory? {
        return context.selectFrom(CHECKING_HISTORY)
            .where(
                CHECKING_HISTORY.SCAN_DATE.eq(scanDate)
                    .and(CHECKING_HISTORY.PO_NUMBER.eq(poNumber))
                    .and(CHECKING_HISTORY.FORM_CODE.eq(formCode))
            )
            .orderBy(CHECKING_HISTORY.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(CheckingHistory::class.java)
            .firstOrNull()
    }

    fun save(data: CheckingHistory) {
        context.insertInto(
            CHECKING_HISTORY, CHECKING_HISTORY.PO_NUMBER, CHECKING_HISTORY.IMPORT_QTY,
            CHECKING_HISTORY.SCAN_QTY, CHECKING_HISTORY.SEQ_NO, CHECKING_HISTORY.FORM_CODE, SENDING_TRANSACTIONS.CREATED_BY,
            CHECKING_HISTORY.UPDATED_DATE, CHECKING_HISTORY.UPDATED_BY
        )
            .values(
                data.poNumber, data.importQty,
                data.scanQty, data.seqNo, data.formCode, CommonUtils.loggedInUser() ?: Constants.SYSTEM,
                OffsetDateTime.now(ZoneOffset.UTC), CommonUtils.loggedInUser() ?: Constants.SYSTEM,
            )
            .execute()
    }

    fun update(scanQty: BigDecimal, scanDate: LocalDate, poNumber: String, seqNo: Int, formCode: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(CHECKING_HISTORY)
                .set(CHECKING_HISTORY.SCAN_QTY, scanQty)
                .set(CHECKING_HISTORY.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(CHECKING_HISTORY.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    CHECKING_HISTORY.SCAN_DATE.eq(scanDate)
                        .and(CHECKING_HISTORY.PO_NUMBER.eq(poNumber))
                        .and(CHECKING_HISTORY.SEQ_NO.eq(seqNo))
                        .and(CHECKING_HISTORY.FORM_CODE.eq(formCode))
                )
                .execute()
        }
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