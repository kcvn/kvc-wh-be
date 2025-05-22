package com.kcvn.spm.repository

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.request.ImportBacklogWh
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class BacklogWhRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: BacklogWhSearchRequest, pageable: Pageable) : Pair<List<BacklogWh>, Int> {
        var condition: Condition = DSL.noCondition()
        val receivingDate = BACKLOG_WH.field("receiving_date", java.time.OffsetDateTime::class.java)
        if(!request.listLocationCode.isNullOrEmpty()){
            val locationCodes = request.listLocationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            locationCodes.forEach { lc ->
                condition1 = condition1.or(BACKLOG_WH.LOCATION_CODE.eq(lc.trim()))
            }
            condition = condition.and(condition1)
        }
        if(!request.listPoNumber.isNullOrEmpty()){
            val poNumbers = request.listPoNumber!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            poNumbers.forEach { pn ->
                condition1 = condition1.or(BACKLOG_WH.PO_NUMBER.eq(pn.trim()))
            }
            condition = condition.and(condition1)
        }
        if(!request.listPackageCode.isNullOrEmpty()){
            val packageCodes = request.listPackageCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            packageCodes.forEach { pc ->
                condition1 = condition1.or(BACKLOG_WH.PACKAGE_CODE.eq(pc.trim()))
            }
            condition = condition.and(condition1)
        }
        if (request.fromDate != null && request.toDate != null) {
            condition = condition.and(receivingDate?.between(request.fromDate, request.toDate))
        }
            val query = context.selectFrom(BACKLOG_WH).where(condition.and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO)))
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, BACKLOG_WH.LOCATION_CODE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(BacklogWh::class.java)

            return Pair(data, count)
    }

    fun getBinEntryList(pageable: Pageable) : Pair<List<BacklogWh>, Int> {
        var condition: Condition = DSL.noCondition()
        condition = condition.and(BACKLOG_WH.INSPECTION_DATE.isNull).and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
        val query = context.select(
            DSL.min(BACKLOG_WH.LOCATION_CODE.cast(SQLDataType.INTEGER)).`as`("MIN_LOCATION_CODE"),
            BACKLOG_WH.PO_NUMBER,
            BACKLOG_WH.RECEIVING_DATE,
            DSL.sum(BACKLOG_WH.BACKLOG_QTY).`as`("SUM_BACKLOG_QTY")
        )
            .from(BACKLOG_WH)
            .where(condition)
            .groupBy(BACKLOG_WH.PO_NUMBER, BACKLOG_WH.RECEIVING_DATE)
            .orderBy(BACKLOG_WH.RECEIVING_DATE.asc())

        val data = query.fetch { record ->
            BacklogWh(
                locationCode = record.get("MIN_LOCATION_CODE", BigDecimal::class.java).toString(),
                poNumber = record[BACKLOG_WH.PO_NUMBER],
                receivingDate = record[BACKLOG_WH.RECEIVING_DATE],
                backlogQty = record.get("SUM_BACKLOG_QTY", BigDecimal::class.java) ?: BigDecimal.ZERO
            )
        }
        return Pair(data, data.size)
    }

    fun getListWithQtyGtZero(): List<BacklogWh> =
        context.selectFrom(BACKLOG_WH)
        .where(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
        .orderBy(BACKLOG_WH.LOCATION_CODE.sort(SortOrder.ASC))
        .fetchInto(BacklogWh::class.java)

    fun save(data: BacklogWh): Int? =
        context.insertInto(
            BACKLOG_WH, BACKLOG_WH.LOCATION_CODE, BACKLOG_WH.PO_NUMBER, BACKLOG_WH.PACKAGE_CODE, BACKLOG_WH.BACKLOG_QTY,
            BACKLOG_WH.BOX_QTY, BACKLOG_WH.RECEIVING_DATE, BACKLOG_WH.CREATED_BY
        )
            .values(
                data.locationCode, data.poNumber, data.packageCode, data.backlogQty,
                data.boxQty, data.receivingDate, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()

    fun findByLocationAndPackageAndPO(locationCode: String, packageCode: String, poNumber: String): BacklogWh? {
        return context.selectFrom(BACKLOG_WH)
            .where(
                BACKLOG_WH.LOCATION_CODE.eq(locationCode)
                    .and(BACKLOG_WH.PACKAGE_CODE.eq(packageCode))
                    .and(BACKLOG_WH.PO_NUMBER.eq(poNumber))
            )
            .fetchInto(BacklogWh::class.java)
            .firstOrNull()
    }

    fun update(data: BacklogWh) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(BACKLOG_WH)
                .set(BACKLOG_WH.BACKLOG_QTY, data.backlogQty)
                .set(BACKLOG_WH.BOX_QTY, data.boxQty)
                .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    BACKLOG_WH.LOCATION_CODE.eq(data.locationCode)
                        .and(BACKLOG_WH.PACKAGE_CODE.eq(data.packageCode))
                        .and(BACKLOG_WH.PO_NUMBER.eq(data.poNumber))
                )
                .execute()
        }
    }

    fun updateInspectionDate(data: ImportBacklogWh): Boolean {
        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val affectedRows = transactionalContext.update(BACKLOG_WH)
                .set(BACKLOG_WH.INSPECTION_DATE, data.inspectionDate)
                .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    BACKLOG_WH.PO_NUMBER.eq(data.poNumber)
                        .and(BACKLOG_WH.RECEIVING_DATE.eq(data.receivingDate))
                        .and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
                )
                .execute()

            affectedRows > 0 // Trả về true nếu có ít nhất 1 dòng bị cập nhật
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> BACKLOG_WH.LOCATION_CODE
            "createdDate" -> BACKLOG_WH.CREATED_DATE
            else -> BACKLOG_WH.CREATED_DATE
        }
        return sortField
    }
}