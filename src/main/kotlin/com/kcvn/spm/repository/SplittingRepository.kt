package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.NewCheckingTransaction
import com.kcvn.spm.model.tables.pojos.Splitting
import com.kcvn.spm.model.tables.references.NEW_CHECKING_TRANSACTION
import com.kcvn.spm.model.tables.references.SPLITTING
import com.kcvn.spm.model.tables.references.TEMP_NEW_BACKLOG_WH
import com.kcvn.spm.app.splitting.payload.respone.SplittingSapExportResponse
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SplittingRepository(private val context: DSLContext) : SortingRepository() {
    fun findByPackage(packageCode: String): Splitting? {
        return context.selectFrom(SPLITTING)
            .where(
                SPLITTING.PACKAGE_CODE.eq(packageCode)
                    .and(SPLITTING.IS_DELETED.eq(false))
            )
            .fetchOneInto(Splitting::class.java)
    }

    fun findByLocationAndPackage(locationCode: String, packageCode: String): Splitting? {
        return context.selectFrom(SPLITTING)
            .where(
                SPLITTING.LOCATION_CODE.eq(locationCode)
                    .and(SPLITTING.PACKAGE_CODE.eq(packageCode))
            )
            .fetchInto(Splitting::class.java)
            .firstOrNull()
    }

    fun save(data: Splitting) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(SPLITTING, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    fun delete(data: Splitting) {
        context.update(SPLITTING)
            .set(SPLITTING.IS_DELETED, true)
            .set(SPLITTING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(SPLITTING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(SPLITTING.ID.eq(data.id))
            .execute()
    }

    /**
     * Splitting chua sync SAP, chi tiet lot lay tu temp_new_backlog_wh cua temp_splitting tuong ung (splitting.temp_splitting_id).
     * Gop theo lot (1 lot co the bi tach nhieu dong tam).
     */
    fun getNotSyncedSapForExport(): List<SplittingSapExportResponse> {
        val quantity = DSL.sum(TEMP_NEW_BACKLOG_WH.BACKLOG_QTY).`as`("quantity")
        return context.select(
            TEMP_NEW_BACKLOG_WH.ITEM_CODE,
            SPLITTING.PACKAGE_CODE,
            TEMP_NEW_BACKLOG_WH.LOT_NO,
            SPLITTING.UPDATED_DATE,
            TEMP_NEW_BACKLOG_WH.PRODUCTION_GROUP,
            TEMP_NEW_BACKLOG_WH.STORAGE_LOCATION,
            quantity,
            TEMP_NEW_BACKLOG_WH.UNIT,
        )
            .from(SPLITTING)
            .innerJoin(TEMP_NEW_BACKLOG_WH)
            .on(TEMP_NEW_BACKLOG_WH.TEMP_SPLITTING_ID.eq(SPLITTING.TEMP_SPLITTING_ID))
            .where(
                SPLITTING.IS_SYNCED_SAP.eq(false)
                    .and(SPLITTING.IS_DELETED.eq(false))
            )
            .groupBy(
                TEMP_NEW_BACKLOG_WH.ITEM_CODE,
                SPLITTING.PACKAGE_CODE,
                TEMP_NEW_BACKLOG_WH.LOT_NO,
                SPLITTING.UPDATED_DATE,
                TEMP_NEW_BACKLOG_WH.PRODUCTION_GROUP,
                TEMP_NEW_BACKLOG_WH.STORAGE_LOCATION,
                TEMP_NEW_BACKLOG_WH.UNIT,
            )
            .orderBy(SPLITTING.UPDATED_DATE, SPLITTING.PACKAGE_CODE, TEMP_NEW_BACKLOG_WH.LOT_NO)
            .fetch {
                SplittingSapExportResponse(
                    itemCode = it.get(TEMP_NEW_BACKLOG_WH.ITEM_CODE),
                    packageCode = it.get(SPLITTING.PACKAGE_CODE),
                    lotNo = it.get(TEMP_NEW_BACKLOG_WH.LOT_NO),
                    updatedDate = it.get(SPLITTING.UPDATED_DATE),
                    productionGroup = it.get(TEMP_NEW_BACKLOG_WH.PRODUCTION_GROUP),
                    storageLocation = it.get(TEMP_NEW_BACKLOG_WH.STORAGE_LOCATION),
                    quantity = it.get(quantity),
                    unit = it.get(TEMP_NEW_BACKLOG_WH.UNIT),
                )
            }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> SPLITTING.LOCATION_CODE
            "createdDate" -> SPLITTING.CREATED_DATE
            else -> SPLITTING.CREATED_DATE
        }
        return sortField
    }
}