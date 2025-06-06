package com.kcvn.spm.repository

import com.kcvn.spm.app.transaction.inquiry.payload.request.InquirySearchRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.model.tables.references.MOVING
import com.kcvn.spm.model.tables.references.RECEIVING_TRANSACTIONS
import com.kcvn.spm.model.tables.references.SENDING_TRANSACTIONS
import org.jooq.*
import org.jooq.impl.DSL
import org.jooq.impl.DSL.table
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class InquiryTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: InquirySearchRequest, pageable: Pageable) : Pair<List<ReceivingTransactions>, Int> {
        val conditionReceiving = searchCondition(request, RECEIVING_TRANSACTIONS)
        val conditionSending = searchCondition(request, SENDING_TRANSACTIONS)
        val conditionMoving = searchCondition(request, MOVING)

        val subQuery = context.select(
            RECEIVING_TRANSACTIONS.SOURCE_LOCATION_CODE.`as`("sourceLocationCode"),
            RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.`as`("destLocationCode"),
            RECEIVING_TRANSACTIONS.SOURCE_PACKAGE_CODE.`as`("sourcePackageCode"),
            RECEIVING_TRANSACTIONS.DEST_PACKAGE_CODE.`as`("destPackageCode"),
            RECEIVING_TRANSACTIONS.PO_NUMBER.`as`("poNumber"),
            RECEIVING_TRANSACTIONS.QTY.`as`("qty"),
            RECEIVING_TRANSACTIONS.SEQ_NO.`as`("seqNo"),
            RECEIVING_TRANSACTIONS.TRANSACTION_TYPE.`as`("transactionType"),
            RECEIVING_TRANSACTIONS.CREATED_DATE.`as`("createdDate")
        ).from(RECEIVING_TRANSACTIONS)
            .where(conditionReceiving.and(RECEIVING_TRANSACTIONS.IS_CANCELED.eq(false)))
            .unionAll(
                context.select(
                    SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE.`as`("sourceLocationCode"),
                    SENDING_TRANSACTIONS.DEST_LOCATION_CODE.`as`("destLocationCode"),
                    SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE.`as`("sourcePackageCode"),
                    SENDING_TRANSACTIONS.DEST_PACKAGE_CODE.`as`("destPackageCode"),
                    SENDING_TRANSACTIONS.PO_NUMBER.`as`("poNumber"),
                    SENDING_TRANSACTIONS.QTY.`as`("qty"),
                    SENDING_TRANSACTIONS.SEQ_NO.`as`("seqNo"),
                    SENDING_TRANSACTIONS.TRANSACTION_TYPE.`as`("transactionType"),
                    SENDING_TRANSACTIONS.CREATED_DATE.`as`("createdDate")
                ).from(SENDING_TRANSACTIONS)
                    .where(conditionSending.and(SENDING_TRANSACTIONS.IS_CANCELED.eq(false)))
            )
            .unionAll(
                context.select(
                    MOVING.SOURCE_LOCATION_CODE.`as`("sourceLocationCode"),
                    MOVING.DEST_LOCATION_CODE.`as`("destLocationCode"),
                    MOVING.SOURCE_PACKAGE_CODE.`as`("sourcePackageCode"),
                    MOVING.DEST_PACKAGE_CODE.`as`("destPackageCode"),
                    MOVING.PO_NUMBER.`as`("poNumber"),
                    MOVING.QTY.`as`("qty"),
                    MOVING.SEQ_NO.`as`("seqNo"),
                    MOVING.TRANSACTION_TYPE.`as`("transactionType"),
                    MOVING.CREATED_DATE.`as`("createdDate")
                ).from(MOVING)
                    .where(conditionMoving.and(MOVING.IS_CANCELED.eq(false)))
            )

        val aliasTable = table(subQuery).`as`("aliasTable")

        val count = context.fetchCount(aliasTable)
        val createdDateField = aliasTable.field("createdDate") as Field<*>

        val data = context.select(
            aliasTable.field("sourceLocationCode"),
            aliasTable.field("destLocationCode"),
            aliasTable.field("sourcePackageCode"),
            aliasTable.field("destPackageCode"),
            aliasTable.field("poNumber"),
            aliasTable.field("qty"),
            aliasTable.field("seqNo"),
            aliasTable.field("transactionType"),
            createdDateField
        ).from(aliasTable)
            .orderBy(createdDateField.sort(SortOrder.DESC))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(ReceivingTransactions::class.java)

        return Pair(data, count)
    }

    private fun searchCondition(request: InquirySearchRequest, table: Table<*>): Condition {
        var condition: Condition = DSL.noCondition()

        val sourceLocationCode = table.field("source_location_code", String::class.java)
        val destLocationCode = table.field("dest_location_code", String::class.java)
        val sourcePackageCode = table.field("source_package_code", String::class.java)
        val destPackageCode = table.field("dest_package_code", String::class.java)
        val poNumber = table.field("po_number", String::class.java)
        val createdDate = table.field("created_date", java.time.OffsetDateTime::class.java)

        if (!request.sourceLocationCode.isNullOrEmpty() && sourceLocationCode != null) {
            condition = condition.and(sourceLocationCode.containsIgnoreCase(request.sourceLocationCode!!.trim()))
        }
        if (!request.destLocationCode.isNullOrEmpty() && destLocationCode != null) {
            condition = condition.and(destLocationCode.containsIgnoreCase(request.destLocationCode!!.trim()))
        }
        if (!request.sourcePackageCode.isNullOrEmpty() && sourcePackageCode != null) {
            condition = condition.and(sourcePackageCode.containsIgnoreCase(request.sourcePackageCode!!.trim()))
        }
        if (!request.destPackageCode.isNullOrEmpty() && destPackageCode != null) {
            condition = condition.and(destPackageCode.containsIgnoreCase(request.destPackageCode!!.trim()))
        }
        if (!request.poNumber.isNullOrEmpty() && poNumber != null) {
            condition = condition.and(poNumber.containsIgnoreCase(request.poNumber!!.trim()))
        }
        if (request.fromDate != null && request.toDate != null && createdDate != null) {
            condition = condition.and(createdDate.between(request.fromDate, request.toDate))
        }

        return condition
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