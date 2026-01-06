package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.model.tables.references.RECEIVING_TRANSACTIONS
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
class ReceivingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
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

    fun save(rec: ReceivingTransactions): String? =
        context.insertInto(
            RECEIVING_TRANSACTIONS, RECEIVING_TRANSACTIONS.SOURCE_LOCATION_CODE, RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE,
            RECEIVING_TRANSACTIONS.SOURCE_PACKAGE_CODE, RECEIVING_TRANSACTIONS.DEST_PACKAGE_CODE, RECEIVING_TRANSACTIONS.PO_NUMBER,
            RECEIVING_TRANSACTIONS.QTY, RECEIVING_TRANSACTIONS.SEQ_NO, RECEIVING_TRANSACTIONS.TRANSACTION_TYPE, RECEIVING_TRANSACTIONS.CREATED_BY
        )
            .values(rec.sourceLocationCode, rec.destLocationCode, rec.sourcePackageCode, rec.destPackageCode, rec.poNumber, rec.qty, rec.seqNo, rec.transactionType, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(RECEIVING_TRANSACTIONS.ID)
            .fetchOne()?.value1()

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> RECEIVING_TRANSACTIONS.CREATED_DATE
            else -> RECEIVING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}