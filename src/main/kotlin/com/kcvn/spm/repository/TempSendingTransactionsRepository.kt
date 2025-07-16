package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.sending.payload.response.TempSendingInquiryResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempSendingTransactions
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
                    SELECT
                        *,
                        CASE
                            WHEN request_qty = actual_qty THEN 'SAME'
                            ELSE 'DIFFERENT'
                        END AS status
                    FROM (
                        SELECT
                            tst.form_code,
                            tsi.inspection_date,
                            tsi.location_code,
                            tsi.po_number,
                            tsi.qty AS request_qty,
                            COALESCE(tst.qty, 0) AS actual_qty,
                            tst.created_date
                        FROM
                            temp_sending_imported tsi
                        LEFT JOIN temp_sending_transactions tst 
                            ON tsi.inspection_date = tst.inspection_date
                            AND tsi.po_number = tst.po_number
                            AND tsi.form_code = tst.form_code
                    ) final_data
                    WHERE form_code like ?
                    ORDER BY status, created_date desc
            """.trimIndent()

                val result = context
                    .resultQuery(sql, "%$formCode%")
                    .fetch()
                    .map {
                        TempSendingInquiryResponse(
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

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> TEMP_SENDING_TRANSACTIONS.CREATED_DATE
            else -> TEMP_SENDING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}