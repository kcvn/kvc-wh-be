package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.sending.payload.request.SendingSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SendingTransactions
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
class SendingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: SendingSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<SendingTransactions>, Int> {
        var condition: Condition = DSL.noCondition()
        if(!request.listSourceLocationCode.isNullOrEmpty()){
            val sourceLocationCodes = request.listSourceLocationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            sourceLocationCodes.forEach { sourceLocationCode ->
                condition1 = condition1.or(SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE.eq(sourceLocationCode.trim()))
            }
            condition = condition.and(condition1)
        }
        if(!request.listDestLocationCode.isNullOrEmpty()){
            val destLocationCodes = request.listDestLocationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            destLocationCodes.forEach { destLocationCode ->
                condition1 = condition1.or(SENDING_TRANSACTIONS.DEST_LOCATION_CODE.eq(destLocationCode.trim()))
            }
            condition = condition.and(condition1)
        }
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(SENDING_TRANSACTIONS.PO_NUMBER.containsIgnoreCase(request.poNumber!!.trim()))
        }
        if (request.fromDate != null && request.toDate != null)
            condition = condition.and(SENDING_TRANSACTIONS.CREATED_DATE.between(request.fromDate, request.toDate))

        val query = context.selectFrom(SENDING_TRANSACTIONS).where(condition.and(SENDING_TRANSACTIONS.IS_CANCELED.eq(false)))

        if (isExport) {
            val data = query
                .orderBy(getSortFields(pageable.sort, SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE))
                .fetchInto(SendingTransactions::class.java)

            return Pair(data, data.size)
        } else {
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(SendingTransactions::class.java)

            return Pair(data, count)
        }
    }

    fun updateIsCanceled(data: SendingTransactions) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(SENDING_TRANSACTIONS)
                .set(SENDING_TRANSACTIONS.IS_CANCELED, true)
                .set(SENDING_TRANSACTIONS.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(SENDING_TRANSACTIONS.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE.eq(data.sourceLocationCode)
                        .and(SENDING_TRANSACTIONS.DEST_LOCATION_CODE.eq(data.destLocationCode))
                        .and(SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE.eq(data.sourcePackageCode))
                        .and(SENDING_TRANSACTIONS.DEST_PACKAGE_CODE.eq(data.destPackageCode))
                        .and(SENDING_TRANSACTIONS.PO_NUMBER.eq(data.poNumber))
                        .and(SENDING_TRANSACTIONS.SEQ_NO.eq(data.seqNo))
                        .and(SENDING_TRANSACTIONS.IS_CANCELED.eq(false))
                )
                .execute()
        }
    }

    fun findMoving(sourceLocationCode: String, destLocationCode: String, sourcePackageCode: String, destPackageCode: String, poNumber: String, qty: BigDecimal, seqNo: Int): SendingTransactions? {
        return context.selectFrom(SENDING_TRANSACTIONS)
            .where(
                SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(SENDING_TRANSACTIONS.DEST_LOCATION_CODE.eq(destLocationCode))
                    .and(SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE.eq(sourcePackageCode))
                    .and(SENDING_TRANSACTIONS.DEST_PACKAGE_CODE.eq(destPackageCode))
                    .and(SENDING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                    .and(SENDING_TRANSACTIONS.QTY.eq(qty))
                    .and(SENDING_TRANSACTIONS.SEQ_NO.eq(seqNo))
            )
            .fetchInto(SendingTransactions::class.java)
            .firstOrNull()
    }

    fun findLatestMoving(sourceLocationCode: String, sourcePackageCode: String, poNumber: String, todayUtc: LocalDate): SendingTransactions? {
        return context.selectFrom(SENDING_TRANSACTIONS)
            .where(
                SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE.eq(sourcePackageCode))
                    .and(SENDING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                    .and(SENDING_TRANSACTIONS.CREATED_DATE.cast(LocalDate::class.java).eq(todayUtc))
            )
            .orderBy(SENDING_TRANSACTIONS.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(SendingTransactions::class.java)
            .firstOrNull()
    }

    fun saveSendingTrans(moving: SendingTransactions) {
        context.insertInto(
            SENDING_TRANSACTIONS, SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE, SENDING_TRANSACTIONS.DEST_LOCATION_CODE,
            SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE, SENDING_TRANSACTIONS.DEST_PACKAGE_CODE, SENDING_TRANSACTIONS.PO_NUMBER,
            SENDING_TRANSACTIONS.QTY, SENDING_TRANSACTIONS.SEQ_NO, SENDING_TRANSACTIONS.TRANSACTION_TYPE, SENDING_TRANSACTIONS.RECEIVING_DATE, SENDING_TRANSACTIONS.CREATED_BY
        )
            .values(
                moving.sourceLocationCode, moving.destLocationCode, moving.sourcePackageCode, moving.destPackageCode, moving.poNumber, moving.qty, moving.seqNo, moving.transactionType, moving.receivingDate, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE
            "createdDate" -> SENDING_TRANSACTIONS.CREATED_DATE
            else -> SENDING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}