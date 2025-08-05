package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.sending.payload.response.TempSendingInquiryResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempSendingTransactions
import com.kcvn.spm.model.tables.references.SENDING_TRANSACTIONS
import com.kcvn.spm.model.tables.references.TEMP_SENDING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class TempSendingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(formCode: String, pageable: Pageable, isExport: Boolean = false) : Pair<List<TempSendingInquiryResponse>, Int> {
        val sql = """
                    WITH temp1 AS (SELECT
                        COALESCE (tsi.form_code, tst_summary.form_code) AS form_code,
                        COALESCE (tsi.inspection_date, tst_summary.inspection_date) as inspection_date,
                        COALESCE (tsi.location_code, tst_summary.location_code) AS location_code,
                        COALESCE (tsi.po_number, tst_summary.po_number) AS po_number,
                        COALESCE (tsi.qty,0) AS request_qty,
                        COALESCE(tst_summary.qty, 0) AS actual_qty,
                        CASE
                            WHEN COALESCE (tsi.qty,0) = COALESCE(tst_summary.qty, 0) THEN 'SAME'
                            ELSE 'DIFFERENT'
                        END AS status,
                        tst_summary.is_approved
                    FROM temp_sending_imported tsi
                    FULL OUTER JOIN (
                        SELECT form_code, po_number, source_location_code AS location_code, inspection_date, SUM(qty) AS qty, is_approved
                        FROM temp_sending_transactions
                        WHERE form_code = ?
                        GROUP BY form_code, po_number, inspection_date, source_location_code, is_approved
                    ) tst_summary
                        ON tsi.form_code = tst_summary.form_code
                        AND tsi.po_number = tst_summary.po_number
                        AND tsi.inspection_date = tst_summary.inspection_date
                        AND tsi.location_code = tst_summary.location_code
                        WHERE (tsi.form_code = ? OR tst_summary.form_code = ?) )
                        
SELECT COALESCE (temp1.form_code, tsct_summary.form_code) AS form_code,
                        COALESCE (temp1.inspection_date, tsct_summary.inspection_date) as inspection_date,
                        COALESCE (temp1.location_code, tsct_summary.location_code) AS location_code,
                        COALESCE (temp1.po_number, tsct_summary.po_number) AS po_number,
                        COALESCE (temp1.request_qty,0) AS request_qty,
                        COALESCE(temp1.actual_qty, 0) AS actual_qty,
                        COALESCE(tsct_summary.qty, 0) AS double_check_qty,
                        CASE
                            WHEN COALESCE (temp1.request_qty,0) = COALESCE(temp1.actual_qty, 0) and COALESCE (temp1.request_qty,0) = COALESCE(tsct_summary.qty, 0)  THEN 'SAME'
                            ELSE 'DIFFERENT'
                        END AS status,
                        temp1.is_approved
                        FROM temp1
FULL OUTER JOIN (
                        SELECT form_code, po_number, source_location_code AS location_code, inspection_date, SUM(qty) AS qty
                        FROM temp_sending_checking_transactions tsct
                        WHERE form_code = ?
                        GROUP BY form_code, po_number, inspection_date, source_location_code 
                    ) tsct_summary
                        ON temp1.form_code = tsct_summary.form_code
                        AND temp1.po_number = tsct_summary.po_number
                        AND temp1.inspection_date = tsct_summary.inspection_date
                        AND temp1.location_code = tsct_summary.location_code
                        WHERE (temp1.form_code = ? OR tsct_summary.form_code = ?)
            """.trimIndent()

                val result = context
                    .resultQuery(sql, formCode, formCode, formCode, formCode, formCode, formCode)
                    .fetch()
                    .map {
                        TempSendingInquiryResponse(
                            formCode = it.get("form_code", String::class.java),
                            inspectionDate = it.get("inspection_date", LocalDate::class.java),
                            locationCode = it.get("location_code", String::class.java),
                            poNumber = it.get("po_number", String::class.java),
                            requestQty = it.get("request_qty", BigDecimal::class.java),
                            actualQty = it.get("actual_qty", BigDecimal::class.java),
                            doubleCheckQty = it.get("double_check_qty", BigDecimal::class.java),
                            result = it.get("status", String::class.java),
                            isApproved = it.get("is_approved", Boolean::class.java),
                        )
                    }

                return result to result.size
    }

    fun saveTempSendingTrans(record: TempSendingTransactions) {
        context.insertInto(
            TEMP_SENDING_TRANSACTIONS, TEMP_SENDING_TRANSACTIONS.FORM_CODE, TEMP_SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE, TEMP_SENDING_TRANSACTIONS.DEST_LOCATION_CODE,
            TEMP_SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE, TEMP_SENDING_TRANSACTIONS.DEST_PACKAGE_CODE, TEMP_SENDING_TRANSACTIONS.PO_NUMBER,
            TEMP_SENDING_TRANSACTIONS.QTY, TEMP_SENDING_TRANSACTIONS.SEQ_NO, TEMP_SENDING_TRANSACTIONS.TRANSACTION_TYPE,
            TEMP_SENDING_TRANSACTIONS.RECEIVING_DATE, TEMP_SENDING_TRANSACTIONS.INSPECTION_DATE, TEMP_SENDING_TRANSACTIONS.CREATED_BY
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

    fun deleteSendingTrans(formCode: String?, poNumber: String?, inspectionDate: LocalDate?) {
        context.deleteFrom(TEMP_SENDING_TRANSACTIONS)
            .where(
                TEMP_SENDING_TRANSACTIONS.FORM_CODE.eq(formCode)
                    .and(TEMP_SENDING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                    .and(TEMP_SENDING_TRANSACTIONS.INSPECTION_DATE.eq(inspectionDate))
            )
            .execute()
    }




    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> TEMP_SENDING_TRANSACTIONS.CREATED_DATE
            else -> TEMP_SENDING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}