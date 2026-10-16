package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.sending.payload.request.NewTempSendingSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.response.NewTempSendingResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.NewTempSendingTransactions
import com.kcvn.spm.model.tables.pojos.NewTempSendingTransactionsChecking
import com.kcvn.spm.model.tables.references.NEW_TEMP_SENDING_TRANSACTIONS_CHECKING
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class NewTempSendingCheckingTransactionRepository(private val context: DSLContext) : SortingRepository() {

    fun findByClientScanId(clientScanId: String): NewTempSendingTransactions? {
        return context.selectFrom(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING)
            .where(
                NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.CLIENT_SCAN_ID.eq(clientScanId)
            )
            .fetchAnyInto(NewTempSendingTransactions::class.java)
    }

    // Tổng đã scan (chưa huỷ) theo từng dòng picking
    fun sumScannedQtyByPickingIds(pickingIds: List<String>): Map<String, BigDecimal> {
        if (pickingIds.isEmpty()) return emptyMap()
        val sumQty = DSL.sum(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.CHECKED_QTY)
        return context.select(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.SENDING_PICKING_LIST_ID, sumQty)
            .from(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING)
            .where(
                NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.SENDING_PICKING_LIST_ID.`in`(pickingIds)
                    .and (NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.IS_DELETED.eq(false))
            )
            .groupBy(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.SENDING_PICKING_LIST_ID)
            .fetch()
            .associate { it.value1()!! to (it.value2() ?: BigDecimal.ZERO) }
    }

    /**
     * Khoá theo package + lot + ngày đến hết transaction (advisory lock), để 2 lần lưu đồng thời
     * không cùng đọc ra 1 max(seq_no). Các phiếu khác nhau có thể cùng scan 1 package-lot nên không khoá theo picking được.
     */
    fun lockSeqNo(packageCode: String, lotNo: String, date: LocalDate) {
        context.fetch("select pg_advisory_xact_lock(hashtext(?))", "new_temp_sending|$packageCode|$lotNo|$date")
    }

    // seq_no lớn nhất của package + lot trong ngày (giờ VN); tính cả bản ghi đã huỷ để không dùng lại số cũ
    fun findMaxSeqNo(lotNo: String, date: LocalDate): BigDecimal? {
        val vnZone = ZoneId.of("Asia/Ho_Chi_Minh")
        val from = date.atStartOfDay(vnZone).toOffsetDateTime()
        val to = date.plusDays(1).atStartOfDay(vnZone).toOffsetDateTime()
        return context.select(DSL.max(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.SEQ_NO))
            .from(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING)
            .where(
                NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.LOT_NO.eq(lotNo)
                    .and (NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.CREATED_DATE.ge(from))
                    .and (NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.CREATED_DATE.lt(to))
            )
            .fetchOne(0, BigDecimal::class.java)
    }

    fun findByPackageAndLot(packageCode: String, lotNo: String): NewTempSendingTransactions?{
        return context.selectFrom(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING)
            .where(
                NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.LOT_NO.eq(lotNo)
                    .and (NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.IS_DELETED.eq(false))
            )
            .fetchInto(NewTempSendingTransactions::class.java)
            .firstOrNull()
    }

    fun save(data: NewTempSendingTransactionsChecking) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(NEW_TEMP_SENDING_TRANSACTIONS_CHECKING, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.CREATED_DATE
            else -> NEW_TEMP_SENDING_TRANSACTIONS_CHECKING.CREATED_DATE
        }
        return sortField
    }
}
