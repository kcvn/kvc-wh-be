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
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class BacklogWhRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: BacklogWhSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<BacklogWh>, Int> {
        var condition: Condition = DSL.noCondition()
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
        if (request.receivingDate != null)
            condition = condition.and(BACKLOG_WH.RECEIVING_DATE.eq(request.receivingDate))
        if (request.issueDate != null)
            condition = condition.and(BACKLOG_WH.ISSUE_DATE.eq(request.issueDate))

        if (isExport) {
            condition = condition.and(BACKLOG_WH.ISSUE_DATE.isNull).and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
            val query = context.select(
                BACKLOG_WH.LOCATION_CODE,
                BACKLOG_WH.PO_NUMBER,
                BACKLOG_WH.RECEIVING_DATE,
                DSL.sum(BACKLOG_WH.BACKLOG_QTY).`as`("SUM_BACKLOG_QTY"),
//                DSL.max(BACKLOG_WH.CREATED_DATE).`as`("LATEST_CREATED_DATE")
            )
                .from(BACKLOG_WH)
                .where(condition)
                .groupBy(BACKLOG_WH.LOCATION_CODE, BACKLOG_WH.PO_NUMBER, BACKLOG_WH.RECEIVING_DATE)
                .orderBy(getSortFields(pageable.sort, BACKLOG_WH.LOCATION_CODE))

            val data = query.fetch { record ->
                BacklogWh(
                    locationCode = record[BACKLOG_WH.LOCATION_CODE],
                    poNumber = record[BACKLOG_WH.PO_NUMBER],
                    receivingDate = record[BACKLOG_WH.RECEIVING_DATE],
                    backlogQty = record.get("SUM_BACKLOG_QTY", BigDecimal::class.java) ?: BigDecimal.ZERO,
//                    createdDate = record.get("LATEST_CREATED_DATE", OffsetDateTime::class.java)
                )
            }
            return Pair(data, data.size)
        } else {
            val query = context.selectFrom(BACKLOG_WH).where(condition)
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, BACKLOG_WH.LOCATION_CODE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(BacklogWh::class.java)

            return Pair(data, count)
        }
    }

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

    fun updateIssueDate(data: ImportBacklogWh): Boolean {
        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val affectedRows = transactionalContext.update(BACKLOG_WH)
                .set(BACKLOG_WH.ISSUE_DATE, data.issueDate)
                .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    BACKLOG_WH.LOCATION_CODE.eq(data.locationCode)
                        .and(BACKLOG_WH.PO_NUMBER.eq(data.poNumber))
                        .and(BACKLOG_WH.RECEIVING_DATE.eq(data.receivingDate))
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