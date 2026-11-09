package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.receiving.payload.request.CheckingReceivingCompareRequest
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransSearchRequest
import com.kcvn.spm.app.transaction.receiving.payload.response.CheckingReceivingCompareResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.model.tables.references.RECEIVING_TRANSACTIONS
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST
import org.jooq.Condition
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
class ReceivingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    fun findByRecDate(recDate: LocalDate): List<ReceivingTransactions> {
        return context.selectFrom(RECEIVING_TRANSACTIONS)
            .where(RECEIVING_TRANSACTIONS.RECEIVING_DATE.eq(recDate)
            )
            .fetchInto(ReceivingTransactions::class.java)
    }

    fun getList(request: RecTransSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<ReceivingTransactions>, Int> {
        var condition: Condition = DSL.noCondition()
        if(!request.locationCode.isNullOrEmpty()){
            val locationCodes = request.locationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            locationCodes.forEach { locationCode ->
                condition1 = condition1.or(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(locationCode.trim()))
            }
            condition = condition.and(condition1)
        }
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(RECEIVING_TRANSACTIONS.PO_NUMBER.containsIgnoreCase(request.poNumber!!.trim()))
        }
        if (request.fromDate != null && request.toDate != null)
            condition = condition.and(RECEIVING_TRANSACTIONS.CREATED_DATE.between(request.fromDate, request.toDate))

        val query = context.selectFrom(RECEIVING_TRANSACTIONS).where(condition.and(RECEIVING_TRANSACTIONS.IS_CANCELED.eq(false)))

        if (isExport) {
            val data = query
                .orderBy(getSortFields(pageable.sort, RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE))
                .fetchInto(ReceivingTransactions::class.java)

            return Pair(data, data.size)
        } else {
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, RECEIVING_TRANSACTIONS.CREATED_DATE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(ReceivingTransactions::class.java)

            return Pair(data, count)
        }
    }

    fun updateIsCanceled(data: ReceivingTransactions) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(RECEIVING_TRANSACTIONS)
                .set(RECEIVING_TRANSACTIONS.IS_CANCELED, true)
                .set(RECEIVING_TRANSACTIONS.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(RECEIVING_TRANSACTIONS.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(RECEIVING_TRANSACTIONS.SOURCE_LOCATION_CODE.eq(data.sourceLocationCode)
                    .and(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(data.destLocationCode))
                    .and(RECEIVING_TRANSACTIONS.SOURCE_PACKAGE_CODE.eq(data.sourcePackageCode))
                    .and(RECEIVING_TRANSACTIONS.DEST_PACKAGE_CODE.eq(data.destPackageCode))
                    .and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(data.poNumber))
                    .and(RECEIVING_TRANSACTIONS.SEQ_NO.eq(data.seqNo))
                    .and(RECEIVING_TRANSACTIONS.IS_CANCELED.eq(false))
                )
                .execute()
        }
    }

    fun findRecTrans(locationCode: String, packageCode: String, poNumber: String, qty: BigDecimal, seqNo: Int): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS)
            .where(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(locationCode)
                .and(RECEIVING_TRANSACTIONS.SOURCE_PACKAGE_CODE.eq(packageCode))
                .and(RECEIVING_TRANSACTIONS.DEST_PACKAGE_CODE.eq(packageCode))
                .and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                .and(RECEIVING_TRANSACTIONS.QTY.eq(qty))
                .and(RECEIVING_TRANSACTIONS.SEQ_NO.eq(seqNo)))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun findLatestByLocationCodeAndPO(destLocationCode: String, poNumber: String, todayUtc: LocalDate): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS)
            .where(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(destLocationCode)
                .and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                .and(RECEIVING_TRANSACTIONS.CREATED_DATE.cast(LocalDate::class.java).eq(todayUtc))
            )
            .orderBy(RECEIVING_TRANSACTIONS.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun save(rec: ReceivingTransactions): String{
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val record = context.newRecord(RECEIVING_TRANSACTIONS, rec).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }
        return context.insertInto(RECEIVING_TRANSACTIONS)
            .set(record)
            .returning(RECEIVING_TRANSACTIONS.ID)
            .fetchOne()!!.id!!
    }


    fun findOneRecordById(id: String?): ReceivingTransactions?{
        return context.selectFrom(RECEIVING_TRANSACTIONS)
            .where(RECEIVING_TRANSACTIONS.ID.eq(id)
            )
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    /**
     * Tong so da check (new_checking_transaction: da approve, L000/L001) theo (ngay nhan hang, PO, lot, invoice).
     * Dung cho inquiry so sanh check - nhan hang; checkedQty dat vao field checkedQty.
     */
    fun sumCheckedByInvoice(request: CheckingReceivingCompareRequest): List<CheckingReceivingCompareResponse> {
        val (conditions, params) = compareKeyConditions(request, "n")
        val sql = """
            select n.receiving_date, n.po_number, n.lot_no, n.invoice_number,
                max(n.item_code) as item_code,
                max(n.item_name) as item_name,
                sum(n.scan_qty) as checked_qty
            from new_checking_transaction n
            where n.is_approve = true
            and n.storage_location in ('L000', 'L001')$conditions
            group by n.receiving_date, n.po_number, n.lot_no, n.invoice_number
        """.trimIndent()
        return context.fetch(sql, *params.toTypedArray()).into(CheckingReceivingCompareResponse::class.java)
    }

    /**
     * Tong so da nhan (receiving_transactions chua huy) theo (ngay nhan hang, PO, lot).
     * Chuyen lot: ben checking lot_no = packageCode, receiving lot_no = lot moi
     * => dong thuoc package chuyen lot (temp_splitting.is_transfer_lot da duyet) lay dest_package_code lam lot de khop voi checking.
     */
    fun sumReceivedByLot(request: CheckingReceivingCompareRequest): Map<Triple<LocalDate?, String?, String?>, BigDecimal> {
        val (conditions, params) = compareKeyConditions(request, "r")
        val sql = """
            select r.receiving_date, r.po_number, r.lot_no, sum(r.qty) as received_qty
            from (
                select rt.receiving_date, rt.po_number, rt.qty,
                    case when exists (
                        select 1 from temp_splitting ts
                        where ts.package_code = rt.dest_package_code
                        and ts.receiving_date = rt.receiving_date
                        and ts.is_transfer_lot = true
                        and ts.is_approved = true
                        and ts.is_deleted = false
                    ) then rt.dest_package_code else rt.lot_no end as lot_no
                from receiving_transactions rt
                where coalesce(rt.is_canceled, false) = false
                and rt.created_date >= timestamptz '2026-11-01 00:00:00+07'
            ) r
            where true$conditions
            group by r.receiving_date, r.po_number, r.lot_no
        """.trimIndent()
        return context.fetch(sql, *params.toTypedArray()).associate {
            Triple(
                it.get("receiving_date", LocalDate::class.java),
                it.get("po_number", String::class.java),
                it.get("lot_no", String::class.java)
            ) to (it.get("received_qty", BigDecimal::class.java) ?: BigDecimal.ZERO)
        }
    }

    /** Dieu kien theo key so sanh (ngay, PO, lot) => loc truoc khi phan bo khong lam sai ket qua */
    private fun compareKeyConditions(request: CheckingReceivingCompareRequest, alias: String): Pair<String, List<Any>> {
        val conds = mutableListOf<String>()
        val params = mutableListOf<Any>()
        request.fromDate?.let { conds += "$alias.receiving_date >= ?"; params += it }
        request.toDate?.let { conds += "$alias.receiving_date <= ?"; params += it }
        request.poNumber?.takeIf { it.isNotBlank() }?.let { conds += "$alias.po_number ilike '%' || ? || '%'"; params += it.trim() }
        request.lotNo?.takeIf { it.isNotBlank() }?.let { conds += "$alias.lot_no ilike '%' || ? || '%'"; params += it.trim() }
        return conds.joinToString("") { " and $it" } to params
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> RECEIVING_TRANSACTIONS.CREATED_DATE
            else -> RECEIVING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}