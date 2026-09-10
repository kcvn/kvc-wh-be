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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
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

    fun getListForDropDown(status: String, isIncludeGe1Days: Boolean): List<PurchaseOrderBacklog> {
        val offset = OffsetDateTime.now().offset
        val threeDaysAgo = OffsetDateTime.of(LocalDate.now().minusDays(1), LocalTime.MIDNIGHT, offset)
        val tomorrow = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT, offset)
        return context.select(PURCHASE_ORDER_BACKLOG)
            .from(PURCHASE_ORDER_BACKLOG)
            .where(
                    //.and(TEMP_CHECKING_IMPORTED.CREATED_DATE.ge(threeDaysAgo))
                    //.and(if (formStatus == "ALL") DSL.noCondition() else if (formStatus == "APPROVED") TEMP_CHECKING_IMPORTED.IS_APPROVED.eq(true) else TEMP_CHECKING_IMPORTED.IS_APPROVED.eq(false))
                    PURCHASE_ORDER_BACKLOG.CREATED_DATE.lt(tomorrow)
                    .and(if (isIncludeGe1Days) DSL.noCondition() else PURCHASE_ORDER_BACKLOG.CREATED_DATE.ge(threeDaysAgo))
                        .and (if (status == "ALL") DSL.noCondition() else PURCHASE_ORDER_BACKLOG.APPROVED.eq(status.toBoolean()))
            )
            .orderBy(PURCHASE_ORDER_BACKLOG.CREATED_DATE.desc())
            .fetchInto(PurchaseOrderBacklog::class.java)
            .filterNotNull()
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