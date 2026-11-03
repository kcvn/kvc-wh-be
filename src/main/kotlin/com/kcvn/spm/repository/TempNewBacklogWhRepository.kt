package com.kcvn.spm.repository

import com.kcvn.spm.app.splitting.payload.request.TempBacklogWhSearchRequest
import com.kcvn.spm.app.splitting.payload.respone.TempBacklogWhResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempNewBacklogWh
import com.kcvn.spm.model.tables.references.TEMP_NEW_BACKLOG_WH
import com.kcvn.spm.model.tables.references.TEMP_SPLITTING
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class TempNewBacklogWhRepository(private val context: DSLContext) : SortingRepository() {
    /** Backlog tam cua cac package dang cho duyet (temp_splitting chua approve, chua xoa) theo ngay nhap */
    fun findPendingByRecDate(receivingDate: LocalDate): List<TempNewBacklogWh> {
        return context.select(TEMP_NEW_BACKLOG_WH.asterisk())
            .from(TEMP_NEW_BACKLOG_WH)
            .where(TEMP_NEW_BACKLOG_WH.RECEIVING_DATE.eq(receivingDate))
            .andExists(
                context.selectOne()
                    .from(TEMP_SPLITTING)
                    .where(
                        TEMP_SPLITTING.ID.eq(TEMP_NEW_BACKLOG_WH.TEMP_SPLITTING_ID)
                            .and(TEMP_SPLITTING.IS_DELETED.eq(false))
                            .and(TEMP_SPLITTING.IS_APPROVED.eq(false))
                    )
            )
            .fetchInto(TempNewBacklogWh::class.java)
    }

    /** Chi tiet cac dong backlog tam cua 1 package (temp_splitting) */
    fun findDetailByTempSplittingId(tempSplittingId: String, pageable: Pageable?): Pair<List<TempBacklogWhResponse>, Int> {
        var sql = """
            select
                ts.location_code,
                ts.is_approved,
                tnbw.*,
                count(*) over() as total_count
            from temp_new_backlog_wh tnbw
            inner join temp_splitting ts
            on tnbw.temp_splitting_id = ts.id
            where ts.id = ? and ts.is_deleted = false
            order by ${createDetailOrderBy(pageable)}
        """.trimIndent()
        val params = mutableListOf<Any>(tempSplittingId)
        if (pageable != null) {
            sql = "$sql limit ? offset ?"
            params += pageable.pageSize
            params += pageable.offset
        }

        val records = context.fetch(sql, *params.toTypedArray())
        val totalCount = records.firstOrNull()?.get("total_count", Int::class.java) ?: 0

        return records.into(TempBacklogWhResponse::class.java) to totalCount
    }

    private fun createDetailOrderBy(pageable: Pageable?): String {
        val sortColumns = mapOf(
            "lotNo" to "tnbw.lot_no",
            "backlogQty" to "tnbw.backlog_qty",
            "issueDate" to "tnbw.issue_date",
            "itemCode" to "tnbw.item_code",
            "createdDate" to "tnbw.created_date",
        )
        val orderBy = pageable?.sort
            ?.mapNotNull { order -> sortColumns[order.property]?.let { "$it ${if (order.isAscending) "asc" else "desc"}" } }
            ?.toList()
            .orEmpty()
            .ifEmpty { listOf("tnbw.lot_no asc", "tnbw.created_date asc") }
        //them id de thu tu on dinh giua cac trang
        return (orderBy + "tnbw.id asc").joinToString(", ")
    }

    fun save(data: TempNewBacklogWh) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(TEMP_NEW_BACKLOG_WH, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "packagecode" -> TEMP_NEW_BACKLOG_WH.PACKAGE_CODE
            "createddate" -> TEMP_NEW_BACKLOG_WH.CREATED_DATE
            else -> TEMP_NEW_BACKLOG_WH.CREATED_DATE
        }
        return sortField
    }
}
