package com.kcvn.spm.repository

import com.kcvn.spm.app.splitting.payload.request.TempBacklogWhSearchRequest
import com.kcvn.spm.app.splitting.payload.respone.TempSplittingResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempSplitting
import com.kcvn.spm.model.tables.references.TEMP_SPLITTING
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class TempSplittingRepository(private val context: DSLContext) : SortingRepository() {
    /** Package dang cho duyet (chua approve, chua xoa) */
    fun findPendingByPackage(packageCode: String): TempSplitting? {
        return context.selectFrom(TEMP_SPLITTING)
            .where(
                TEMP_SPLITTING.PACKAGE_CODE.eq(packageCode)
                    .and(TEMP_SPLITTING.IS_DELETED.eq(false))
                    .and(TEMP_SPLITTING.IS_APPROVED.eq(false))
            )
            .fetchInto(TempSplitting::class.java)
            .firstOrNull()
    }

    fun findById(id: String): TempSplitting? {
        return context.selectFrom(TEMP_SPLITTING)
            .where(
                TEMP_SPLITTING.ID.eq(id)
                    .and(TEMP_SPLITTING.IS_DELETED.eq(false))
            )
            .fetchOneInto(TempSplitting::class.java)
    }

    /**
     * Xoa mem package dang cho duyet (chi cap nhat is_deleted o temp_splitting).
     * @return so dong bi anh huong; 0 = khong ton tai / da xoa / da duyet
     */
    fun softDeletePending(id: String): Int {
        return context.update(TEMP_SPLITTING)
            .set(TEMP_SPLITTING.IS_DELETED, true)
            .set(TEMP_SPLITTING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(TEMP_SPLITTING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(
                TEMP_SPLITTING.ID.eq(id)
                    .and(TEMP_SPLITTING.IS_DELETED.eq(false))
                    .and(TEMP_SPLITTING.IS_APPROVED.eq(false))
            )
            .execute()
    }

    /**
     * Danh dau da duyet cho package dang cho duyet (giu row lock den het transaction).
     * @return so dong bi anh huong; 0 = khong ton tai / da xoa / da duyet
     */
    fun approvePending(id: String): Int {
        return context.update(TEMP_SPLITTING)
            .set(TEMP_SPLITTING.IS_APPROVED, true)
            .set(TEMP_SPLITTING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(TEMP_SPLITTING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(
                TEMP_SPLITTING.ID.eq(id)
                    .and(TEMP_SPLITTING.IS_DELETED.eq(false))
                    .and(TEMP_SPLITTING.IS_APPROVED.eq(false))
            )
            .execute()
    }

    fun getPackageList(request: TempBacklogWhSearchRequest, pageable: Pageable?): Pair<List<TempSplittingResponse>, Int> {
        val (sql, params) = createGetPackageListSqlQuery(request)

        var paginatedSql = "$sql order by ${createGetPackageListOrderBy(pageable)}"
        val paginatedParams = params.toMutableList()
        if (pageable != null) {
            paginatedSql = "$paginatedSql limit ? offset ?"
            paginatedParams += pageable.pageSize
            paginatedParams += pageable.offset
        }

        val records = context.fetch(paginatedSql, *paginatedParams.toTypedArray())
        val totalCount = records.firstOrNull()?.get("total_count", Int::class.java) ?: 0

        return records.into(TempSplittingResponse::class.java) to totalCount
    }

    private fun createGetPackageListSqlQuery(request: TempBacklogWhSearchRequest): Pair<String, List<Any>> {
        val params = mutableListOf<Any>()
        val conditions = mutableListOf("ts.is_deleted = false")

        fun likeAny(column: String, values: List<String>): String {
            params.addAll(values.map { it.trim() })
            return values.joinToString(" or ", "(", ")") { "$column ilike '%' || ? || '%'" }
        }

        //PO, package, lot: chi 1 gia tri, tim gan dung
        fun likeOne(column: String, value: String): String {
            params += value.trim()
            return "$column ilike '%' || ? || '%'"
        }

        request.listLocationCode?.takeIf { it.isNotBlank() }?.split(",")?.let { conditions += likeAny("ts.location_code", it) }
        request.packageCode?.takeIf { it.isNotBlank() }?.let { conditions += likeOne("ts.package_code", it) }
        //loc PO / lot bang exists de khong lam sai tong so luong cua package
        request.poNumber?.takeIf { it.isNotBlank() }?.let {
            conditions += "exists (select 1 from temp_new_backlog_wh x where x.temp_splitting_id = ts.id and ${likeOne("x.po_number", it)})"
        }
        request.lotNo?.takeIf { it.isNotBlank() }?.let {
            conditions += "exists (select 1 from temp_new_backlog_wh x where x.temp_splitting_id = ts.id and ${likeOne("x.lot_no", it)})"
        }
        //ngay nhap: tu ngay / den ngay (bao gom 2 dau), truyen 1 ben cung duoc
        request.fromDate?.let {
            conditions += "ts.receiving_date >= ?"
            params += it
        }
        request.toDate?.let {
            conditions += "ts.receiving_date <= ?"
            params += it
        }
        request.isApproved?.let {
            conditions += "ts.is_approved = ?"
            params += it
        }
        //loai package: chia hang / chuyen lot
        request.isTransferLot?.let {
            conditions += "ts.is_transfer_lot = ?"
            params += it
        }

        val sql = """
            select
                ts.id,
                ts.location_code,
                ts.package_code,
                ts.receiving_date,
                ts.is_approved,
                ts.is_transfer_lot,
                ts.created_by,
                ts.created_date,
                string_agg(distinct tnbw.po_number, ',') as po_number,
                sum(tnbw.backlog_qty) as total_qty,
                count(*) over() as total_count
            from temp_splitting ts
            inner join temp_new_backlog_wh tnbw
            on tnbw.temp_splitting_id = ts.id
            where ${conditions.joinToString(" and ")}
            group by ts.id, ts.location_code, ts.package_code, ts.receiving_date, ts.is_approved, ts.is_transfer_lot, ts.created_by, ts.created_date
        """.trimIndent()

        return sql to params
    }

    private fun createGetPackageListOrderBy(pageable: Pageable?): String {
        val sortColumns = mapOf(
            "createdDate" to "ts.created_date",
            "receivingDate" to "ts.receiving_date",
            "packageCode" to "ts.package_code",
            "locationCode" to "ts.location_code",
            "poNumber" to "po_number",
            "totalQty" to "total_qty",
        )
        val orderBy = pageable?.sort
            ?.mapNotNull { order -> sortColumns[order.property]?.let { "$it ${if (order.isAscending) "asc" else "desc"}" } }
            ?.toList()
            .orEmpty()
        return orderBy.ifEmpty { listOf("ts.created_date desc") }.joinToString(", ")
    }

    /** @return id cua temp_splitting vua tao */
    fun save(data: TempSplitting): String {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val record = context.newRecord(TEMP_SPLITTING, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }
        return context.insertInto(TEMP_SPLITTING)
            .set(record)
            .returning(TEMP_SPLITTING.ID)
            .fetchOne()!!.id!!
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationcode" -> TEMP_SPLITTING.LOCATION_CODE
            "createddate" -> TEMP_SPLITTING.CREATED_DATE
            else -> TEMP_SPLITTING.CREATED_DATE
        }
        return sortField
    }
}
