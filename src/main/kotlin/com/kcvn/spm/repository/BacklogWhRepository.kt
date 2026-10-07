package com.kcvn.spm.repository

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchForSendingRequest
import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.request.ImportBacklogWh
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.NewBacklogWh
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import com.kcvn.spm.model.tables.references.NEW_BACKLOG_WH
import com.kcvn.spm.model.tables.references.SPLITTING
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class BacklogWhRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(
        request: BacklogWhSearchRequest,
        pageable: Pageable?,
        fetchAll: Boolean = false,
        exactMatch: Boolean = false,
    ) : Pair<List<BacklogWhResponse>, Int> {
        val (sql, params) = createGetListSqlQuery(request, exactMatch)

        var paginatedSql = "$sql order by ${createGetListOrderBy(pageable)}"
        val paginatedParams = params.toMutableList()
        if (pageable != null && !fetchAll) {
            paginatedSql = "$paginatedSql limit ? offset ?"
            paginatedParams += pageable.pageSize
            paginatedParams += pageable.offset
        }

        val records = context.fetch(paginatedSql, *paginatedParams.toTypedArray())
        val totalCount = records.firstOrNull()?.get("total_count", Int::class.java) ?: 0

        return records.into(BacklogWhResponse::class.java) to totalCount
    }

    private fun createGetListSqlQuery(request: BacklogWhSearchRequest, exactMatch: Boolean): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf("nbw.backlog_qty > 0 and s.is_deleted = false")

        fun addCondition(column: String, values: List<String>, exact: Boolean) {
            conditions += if (exact) "$column in (${values.joinToString(", ") { "?" }})"
                else values.joinToString(" or ", "(", ")") { "$column ilike '%' || ? || '%'" }
            params.addAll(values.map { it.trim() })
        }

        request.listLocationCode?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("s.location_code", it, false) }
        request.listPoNumber?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.po_number", it, exactMatch) }
        request.listPackageCode?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.package_code", it, exactMatch) }
        request.lotNo?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.lot_no", it, false) }
        if (request.fromDate != null && request.toDate != null) {
            conditions += "nbw.receiving_date between ? and ?"
            params += request.fromDate!!
            params += request.toDate!!
        }

        val sql = """
            select
                s.location_code,
                nbw.*,
                count(*) over() as total_count
            from new_backlog_wh nbw
            inner join splitting s
            on nbw.package_code = s.package_code
            where ${conditions.joinToString(" and ")}
        """.trimIndent()

        return sql to params
    }

    private fun createGetListOrderBy(pageable: Pageable?): String {
        val sortColumns = mapOf(
            "createdDate" to "nbw.created_date",
            "receivingDate" to "nbw.receiving_date",
            "poNumber" to "nbw.po_number",
            "packageCode" to "nbw.package_code",
            "lotNo" to "nbw.lot_no",
        )
        val orderBy = pageable?.sort
            ?.mapNotNull { order -> sortColumns[order.property]?.let { "$it ${if (order.isAscending) "asc" else "desc"}" } }
            ?.toList()
            .orEmpty()
        return orderBy.ifEmpty { listOf("nbw.created_date desc") }.joinToString(", ")
    }

    fun getListForSendingRequest(
        request: BacklogWhSearchForSendingRequest,
        pageable: Pageable?,
        fetchAll: Boolean = false,
        exactMatch: Boolean = false,
    ) : Pair<List<BacklogWhResponse>, Int> {
        val (sql, params) = createGetListForSendingRequestSqlQuery(request, exactMatch)

        var paginatedSql = "$sql order by ${createGetListForSendingRequestOrderBy(pageable)}"
        val paginatedParams = params.toMutableList()
        if (pageable != null && !fetchAll) {
            paginatedSql = "$paginatedSql limit ? offset ?"
            paginatedParams += pageable.pageSize
            paginatedParams += pageable.offset
        }

        val records = context.fetch(paginatedSql, *paginatedParams.toTypedArray())
        val totalCount = records.firstOrNull()?.get("total_count", Int::class.java) ?: 0

        return records.into(BacklogWhResponse::class.java) to totalCount
    }

    private fun createGetListForSendingRequestSqlQuery(request: BacklogWhSearchForSendingRequest, exactMatch: Boolean): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf("nbw.backlog_qty > 0")

        fun addCondition(column: String, values: List<String>, exact: Boolean) {
            conditions += if (exact) "$column in (${values.joinToString(", ") { "?" }})"
            else values.joinToString(" or ", "(", ")") { "$column ilike '%' || ? || '%'" }
            params.addAll(values.map { it.trim() })
        }

        request.itemCode?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.item_code", it, false) }
        request.poNumber?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.po_number", it, exactMatch) }
        request.productionGroup?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.production_group", it, exactMatch) }
        request.itemName?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.item_name", it, exactMatch) }
        request.lotNo?.takeIf { it.isNotBlank() }?.split(",")?.let { addCondition("nbw.lot_no", it, false) }
        if (request.fromDate != null && request.toDate != null) {
            conditions += "nbw.receiving_date between ? and ?"
            params += request.fromDate!!
            params += request.toDate!!
        }

        val sql = """
            select
		nbw.po_number,
		nbw.item_code,
		nbw.production_group,
		nbw.receiving_date,
		nbw.item_name,
		nbw.lot_no,
		sum(nbw.backlog_qty) as backlog_qty,
		count(*) over() as total_count
	from
		new_backlog_wh nbw
    where ${conditions.joinToString(" and ")}
    group by
		nbw.po_number,
		nbw.item_code,
		nbw.production_group,
		nbw.receiving_date,
		nbw.item_name,
		nbw.lot_no
        """.trimIndent()

        return sql to params
    }

    private fun createGetListForSendingRequestOrderBy(pageable: Pageable?): String {
        val sortColumns = mapOf(
            "receivingDate" to "nbw.receiving_date",
            "poNumber" to "nbw.po_number",
            "lotNo" to "nbw.lot_no",
        )
        val orderBy = pageable?.sort
            ?.mapNotNull { order -> sortColumns[order.property]?.let { "$it ${if (order.isAscending) "asc" else "desc"}" } }
            ?.toList()
            .orEmpty()
        return orderBy.ifEmpty { listOf("nbw.receiving_date") }.joinToString(", ")
    }

    fun getBinEntryList(pageable: Pageable) : Pair<List<BacklogWh>, Int> {
        var condition: Condition = DSL.noCondition()
        condition = condition.and(BACKLOG_WH.IS_ENTRIED.eq(false)).and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
        val query = context.select(
            DSL.min(BACKLOG_WH.LOCATION_CODE.cast(SQLDataType.INTEGER)).`as`("MIN_LOCATION_CODE"),
            BACKLOG_WH.PO_NUMBER,
            BACKLOG_WH.RECEIVING_DATE,
            DSL.sum(BACKLOG_WH.BACKLOG_QTY).`as`("SUM_BACKLOG_QTY"),
            BACKLOG_WH.INSPECTION_DATE
        )
            .from(BACKLOG_WH)
            .where(condition)
            .groupBy(BACKLOG_WH.PO_NUMBER, BACKLOG_WH.RECEIVING_DATE, BACKLOG_WH.INSPECTION_DATE)
            .orderBy(BACKLOG_WH.RECEIVING_DATE.asc())

        val data = query.fetch { record ->
            BacklogWh(
                locationCode = record.get("MIN_LOCATION_CODE", BigDecimal::class.java).toString(),
                poNumber = record[BACKLOG_WH.PO_NUMBER],
                receivingDate = record[BACKLOG_WH.RECEIVING_DATE],
                backlogQty = record.get("SUM_BACKLOG_QTY", BigDecimal::class.java) ?: BigDecimal.ZERO,
                inspectionDate = record[BACKLOG_WH.INSPECTION_DATE],
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
            BACKLOG_WH.BOX_QTY, BACKLOG_WH.RECEIVING_DATE, BACKLOG_WH.CREATED_BY, BACKLOG_WH.IS_ENTRIED, BACKLOG_WH.INSPECTION_DATE,
            BACKLOG_WH.LOT_NO, BACKLOG_WH.ISSUE_DATE
        )
            .values(
                data.locationCode, data.poNumber, data.packageCode, data.backlogQty,
                data.boxQty, data.receivingDate, CommonUtils.loggedInUser() ?: Constants.SYSTEM, data.isEntried, data.inspectionDate,
                data.lotNo, data.issueDate
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

    fun findByPackageCode(packageCode: String): BacklogWh? {
        return context.selectFrom(BACKLOG_WH)
            .where(
                BACKLOG_WH.PACKAGE_CODE.eq(packageCode)
            )
            .fetchInto(BacklogWh::class.java)
            .firstOrNull()
    }

    fun findExistBacklogByPackageCode(packageCode: String): BacklogWh? {
        return context.selectFrom(BACKLOG_WH)
            .where(
                BACKLOG_WH.PACKAGE_CODE.eq(packageCode)
                    .and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
            )
            .fetchInto(BacklogWh::class.java)
            .firstOrNull()
    }

    fun update(data: BacklogWh) {
            context.update(BACKLOG_WH)
                .set(BACKLOG_WH.BACKLOG_QTY, data.backlogQty)
                .set(BACKLOG_WH.BOX_QTY, data.boxQty)
                //.set(BACKLOG_WH.INSPECTION_DATE, data.inspectionDate)
                .set(BACKLOG_WH.IS_ENTRIED, data.isEntried)
                .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .set(BACKLOG_WH.LOT_NO, data.lotNo)
                .set(BACKLOG_WH.ISSUE_DATE, data.issueDate)
                .where(
                    BACKLOG_WH.LOCATION_CODE.eq(data.locationCode)
                        .and(BACKLOG_WH.PACKAGE_CODE.eq(data.packageCode))
                        .and(BACKLOG_WH.PO_NUMBER.eq(data.poNumber))
                )
                .execute()
    }

    fun updateQty(packageCode: String, backlogQty: BigDecimal, boxQty: Int) {


            context.update(BACKLOG_WH)
                .set(BACKLOG_WH.BACKLOG_QTY, backlogQty)
                .set(BACKLOG_WH.BOX_QTY, boxQty)
                .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    BACKLOG_WH.PACKAGE_CODE.eq(packageCode)
                        .and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
                )
                .execute()

    }

    fun updateInspectionDate(data: ImportBacklogWh) {

            val affectedRows = context.update(BACKLOG_WH)
                .set(BACKLOG_WH.INSPECTION_DATE, data.inspectionDate)
                .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .set(BACKLOG_WH.IS_ENTRIED, data.isEntried)
                .set(BACKLOG_WH.ITEM_NAME, data.itemName)
                .where(
                    BACKLOG_WH.PO_NUMBER.eq(data.poNumber)
                        .and(BACKLOG_WH.RECEIVING_DATE.eq(data.receivingDate))
                        .and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
                )
                .execute()

    }

    fun updateIsEntried(isEntried: Boolean, poNumber: String, receivingDate: LocalDate, inspectionDate: LocalDate?) {
        context.update(BACKLOG_WH)
            .set(BACKLOG_WH.IS_ENTRIED, isEntried)
            .set(BACKLOG_WH.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(BACKLOG_WH.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(
                BACKLOG_WH.PO_NUMBER.eq(poNumber)
                    .and(BACKLOG_WH.RECEIVING_DATE.eq(receivingDate))
                    .and(BACKLOG_WH.INSPECTION_DATE.eq(inspectionDate))
            )
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> NEW_BACKLOG_WH.CREATED_DATE
            else -> NEW_BACKLOG_WH.CREATED_DATE
        }
        return sortField
    }
}