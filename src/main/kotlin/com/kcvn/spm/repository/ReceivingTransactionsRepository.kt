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

        val query = context.selectFrom(RECEIVING_TRANSACTIONS).where(condition)

        if (isExport) {
            val data = query
                .orderBy(getSortFields(pageable.sort, RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE))
                .fetchInto(ReceivingTransactions::class.java)

            return Pair(data, data.size)
        } else {
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(ReceivingTransactions::class.java)

            return Pair(data, count)
        }
    }

    fun findRecTrans(locationCode: String, poNumber: String, qty: Int, seqNo: Int): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS)
            .where(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(locationCode)
                .and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                .and(RECEIVING_TRANSACTIONS.QTY.eq(qty))
                .and(RECEIVING_TRANSACTIONS.SEQ_NO.eq(seqNo)))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun findLatestByLocationCodeAndPO(locationCode: String, poNumber: String): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS).where(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(locationCode).and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber)))
            .orderBy(RECEIVING_TRANSACTIONS.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun save(rec: ReceivingTransactions) {
        context.insertInto(RECEIVING_TRANSACTIONS, RECEIVING_TRANSACTIONS.SOURCE_LOCATION_CODE, RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE, RECEIVING_TRANSACTIONS.PO_NUMBER,
            RECEIVING_TRANSACTIONS.QTY, RECEIVING_TRANSACTIONS.SEQ_NO, RECEIVING_TRANSACTIONS.CREATED_BY)
            .values(rec.sourceLocationCode, rec.destLocationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE
            "createdDate" -> RECEIVING_TRANSACTIONS.CREATED_DATE
            else -> RECEIVING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}