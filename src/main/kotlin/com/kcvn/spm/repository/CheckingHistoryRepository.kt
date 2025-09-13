package com.kcvn.spm.repository

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.model.tables.references.CHECKING_HISTORY
import com.kcvn.spm.model.tables.references.SENDING_TRANSACTIONS
import org.jooq.Condition
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
    fun getList(request: CheckingHistorySearchRequest, pageable: Pageable) : Pair<List<CheckingHistory>, Int> {
        val keywordPoNumber = request.poNumber?.let { "%$it%" } ?: "%"

        val sql = """
            SELECT 
                ch.scan_date,
                COALESCE(tci.form_code, ch.form_code) AS form_code,
                COALESCE(tci.po_number, ch.po_number) AS po_number,
                COALESCE(tci.qty, 0) AS imported_qty,
                COALESCE(ch.scan_qty, 0) AS scanned_qty,
                ch.seq_no
            FROM temp_checking_imported tci
            FULL OUTER JOIN checking_history ch
                ON tci.form_code = ch.form_code
               AND tci.po_number = ch.po_number
            WHERE (tci.form_code = ? OR ch.form_code = ?)
            AND (tci.po_number ilike ? OR ch.po_number ilike ?)
        """.trimIndent()
        val result = context
            .resultQuery(sql, request.formCode, request.formCode, keywordPoNumber, keywordPoNumber)
            .fetch()
            .map {
                CheckingHistory(
                    scanDate = it.get("scan_date", LocalDate::class.java),
                    formCode = it.get("form_code", String::class.java),
                    poNumber = it.get("po_number", String::class.java),
                    importQty = it.get("imported_qty", BigDecimal::class.java),
                    scanQty = it.get("scanned_qty", BigDecimal::class.java),
                    seqNo = it.get("seq_no", Int::class.java),
                )
            }

        val notDuplicate = result.groupBy { it.poNumber to it.seqNo }
            .filter { it.value.size == 1 }

        val duplicates = result.groupBy { it.poNumber to it.seqNo }
            .filter { it.value.size > 1 }

        val finalData = mutableListOf<CheckingHistory>()

        notDuplicate.forEach { (key, records) ->
            records.forEach { record ->
                val a = CheckingHistory(
                    scanDate = record.scanDate,
                    formCode = record.formCode,
                    poNumber = record.poNumber,
                    importQty = record.importQty,
                    scanQty = record.scanQty,
                    seqNo = record.seqNo
                )
                finalData.add(a)
            }
        }

        duplicates.forEach { (key, records) ->
            val (poNumber, seqNo) = key

            var totalScannedQty = records[0].scanQty

            for (record in records) {
                if (record.importQty!! <= totalScannedQty) {
                    val a = CheckingHistory(
                        scanDate = record.scanDate,
                        formCode = record.formCode,
                        poNumber = record.poNumber,
                        importQty = record.importQty,
                        scanQty = record.importQty,
                        seqNo = record.seqNo
                    )
                    finalData.add(a)
                    totalScannedQty = totalScannedQty?.minus(record.importQty!!)
                } else {
                    val a = CheckingHistory(
                        scanDate = record.scanDate,
                        formCode = record.formCode,
                        poNumber = record.poNumber,
                        importQty = record.importQty,
                        scanQty = totalScannedQty,
                        seqNo = record.seqNo
                    )
                    finalData.add(a)
                    totalScannedQty = BigDecimal.ZERO
                }
            }
        }
        finalData.sortWith(compareBy<CheckingHistory> { it.poNumber }.thenBy { it.seqNo })
        return Pair(finalData, finalData.size)
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