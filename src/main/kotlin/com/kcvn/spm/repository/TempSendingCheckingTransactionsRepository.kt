package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.sending.payload.response.TempSendingResultInquiryResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempSendingCheckingTransactions
import com.kcvn.spm.model.tables.pojos.TempSendingTransactions
import com.kcvn.spm.model.tables.references.TEMP_SENDING_CHECKING_TRANSACTIONS
import com.kcvn.spm.model.tables.references.TEMP_SENDING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class TempSendingCheckingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(formCode: String, pageable: Pageable, isExport: Boolean = false) : Pair<List<TempSendingResultInquiryResponse>, Int> {
        val sql = """
                    SELECT
                        COALESCE (tsi.form_code, tst_summary.form_code) AS form_code,
                        COALESCE (tsi.inspection_date, tst_summary.inspection_date) as inspection_date,
                        COALESCE (tsi.location_code, tst_summary.location_code) AS location_code,
                        COALESCE (tsi.po_number, tst_summary.po_number) AS po_number,
                        COALESCE (tsi.qty,0) AS request_qty,
                        COALESCE(tst_summary.qty, 0) AS actual_qty,
                        CASE
                            WHEN COALESCE (tsi.qty,0) = COALESCE(tst_summary.qty, 0) THEN 'SAME'
                            ELSE 'DIFFERENT'
                        END AS status
                    FROM temp_sending_imported tsi
                    FULL OUTER JOIN (
                        SELECT form_code, po_number, source_location_code AS location_code, inspection_date, SUM(qty) AS qty
                        FROM TEMP_SENDING_CHECKING_TRANSACTIONS
                        WHERE form_code = ?
                        GROUP BY form_code, po_number, inspection_date, source_location_code
                        
                    ) tst_summary
                        ON tsi.form_code = tst_summary.form_code
                        AND tsi.po_number = tst_summary.po_number
                        AND tsi.inspection_date = tst_summary.inspection_date
                        AND tsi.location_code = tst_summary.location_code
                        WHERE (tsi.form_code = ? OR tst_summary.form_code = ?)
            """.trimIndent()

                val result = context
                    .resultQuery(sql, formCode, formCode, formCode)
                    .fetch()
                    .map {
                        TempSendingResultInquiryResponse(
                            formCode = it.get("form_code", String::class.java),
                            inspectionDate = it.get("inspection_date", LocalDate::class.java),
                            locationCode = it.get("location_code", String::class.java),
                            poNumber = it.get("po_number", String::class.java),
                            requestQty = it.get("request_qty", BigDecimal::class.java),
                            actualQty = it.get("actual_qty", BigDecimal::class.java),
                            result = it.get("status", String::class.java),
                        )
                    }

                return result to result.size
    }

    fun saveTempSendingCheckingTrans(record: TempSendingCheckingTransactions) {
        context.insertInto(
            TEMP_SENDING_CHECKING_TRANSACTIONS, TEMP_SENDING_CHECKING_TRANSACTIONS.FORM_CODE, TEMP_SENDING_CHECKING_TRANSACTIONS.SOURCE_LOCATION_CODE, TEMP_SENDING_CHECKING_TRANSACTIONS.DEST_LOCATION_CODE,
            TEMP_SENDING_CHECKING_TRANSACTIONS.SOURCE_PACKAGE_CODE, TEMP_SENDING_CHECKING_TRANSACTIONS.DEST_PACKAGE_CODE, TEMP_SENDING_CHECKING_TRANSACTIONS.PO_NUMBER,
            TEMP_SENDING_CHECKING_TRANSACTIONS.QTY, TEMP_SENDING_CHECKING_TRANSACTIONS.SEQ_NO, TEMP_SENDING_CHECKING_TRANSACTIONS.TRANSACTION_TYPE,
            TEMP_SENDING_CHECKING_TRANSACTIONS.RECEIVING_DATE, TEMP_SENDING_CHECKING_TRANSACTIONS.INSPECTION_DATE, TEMP_SENDING_CHECKING_TRANSACTIONS.CREATED_BY
        )
            .values(
                record.formCode,
                record.sourceLocationCode, record.destLocationCode,
                record.sourcePackageCode, record.destPackageCode, record.poNumber,
                record.qty, record.seqNo, record.transactionType,
                record.receivingDate, record.inspectionDate, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    fun getListByFormCode(formCode: String): List<TempSendingCheckingTransactions>? {
        return context.selectFrom(TEMP_SENDING_CHECKING_TRANSACTIONS)
            .where(
                TEMP_SENDING_CHECKING_TRANSACTIONS.FORM_CODE.eq(formCode)
            )
            .fetchInto(TempSendingCheckingTransactions::class.java)
    }

    fun deleteSendingTrans(formCode: String?, poNumber: String?) {
        context.deleteFrom(TEMP_SENDING_CHECKING_TRANSACTIONS)
            .where(
                TEMP_SENDING_CHECKING_TRANSACTIONS.FORM_CODE.eq(formCode)
                    .and(TEMP_SENDING_CHECKING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
            )
            .execute()
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> TEMP_SENDING_CHECKING_TRANSACTIONS.CREATED_DATE
            else -> TEMP_SENDING_CHECKING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}