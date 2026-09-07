package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PurchaseOrderBacklog
import com.kcvn.spm.model.tables.references.PURCHASE_ORDER_BACKLOG
import com.kcvn.spm.model.tables.references.TEMP_CHECKING_IMPORTED
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class PurchaseOrderBacklogRepository(private val context: DSLContext) : SortingRepository() {

    fun findLotOfPoInvoiceOnDay(poNo: String, invoice: String, day: String): PurchaseOrderBacklog? {
        return context.selectFrom(PURCHASE_ORDER_BACKLOG)
            .where(
                PURCHASE_ORDER_BACKLOG.PO_NO.eq(poNo)
                    .and (PURCHASE_ORDER_BACKLOG.INVOICE.eq(invoice))
                    .and (PURCHASE_ORDER_BACKLOG.LOT_NO.contains(day))
            )
            .fetchInto(PurchaseOrderBacklog::class.java)
            .firstOrNull()
    }

    fun findLatestLotOfDay(day: String, productionGroup: String): PurchaseOrderBacklog? {
        return context.selectFrom(PURCHASE_ORDER_BACKLOG)
            .where(
                PURCHASE_ORDER_BACKLOG.LOT_NO.contains(day)
                    .and(PURCHASE_ORDER_BACKLOG.LOT_NO.startsWith(productionGroup))
            )
            .orderBy(PURCHASE_ORDER_BACKLOG.LOT_NO.desc())
            .fetchInto(PurchaseOrderBacklog::class.java)
            .firstOrNull()
    }

    fun saveAll(dataList: List<PurchaseOrderBacklog>): Int {
        if (dataList.isEmpty()) return 0
        val currentTime  = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user =CommonUtils.loggedInUser() ?: Constants.SYSTEM

        return context.batchInsert(
            dataList.map { data ->
                context.newRecord(PURCHASE_ORDER_BACKLOG, data).apply {
                    createdBy = user
                    updatedDate = currentTime
                }
            }
        ).execute().sum()
    }

    fun save(data: PurchaseOrderBacklog) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(PURCHASE_ORDER_BACKLOG, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    fun update(data: PurchaseOrderBacklog) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(PURCHASE_ORDER_BACKLOG, data).apply {
            updatedBy = user
            updatedDate = currentTime
            changed(PURCHASE_ORDER_BACKLOG.CREATED_BY, false)
            changed(PURCHASE_ORDER_BACKLOG.CREATED_DATE, false)
        }.update()
    }

    fun delete(userName: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(TEMP_CHECKING_IMPORTED).where(TEMP_CHECKING_IMPORTED.CREATED_BY.eq(userName))
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "poNumber" -> TEMP_CHECKING_IMPORTED.PO_NUMBER
            "createdDate" -> TEMP_CHECKING_IMPORTED.CREATED_DATE
            else -> TEMP_CHECKING_IMPORTED.CREATED_DATE
        }
        return sortField
    }
}